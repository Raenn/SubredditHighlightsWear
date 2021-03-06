package com.raenn.subredditimages;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;
import com.raenn.subredditimages.pojo.Image;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "SubImages-Handheld";

    private GoogleApiClient mGoogleApiClient;
    private ImageView imageView;
    private ScheduledExecutorService scheduler;
    private String currentImageURL;

    ConnectivityManager connMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

        imageView = (ImageView) findViewById(R.id.subredditImageTest);

        scheduler = Executors.newScheduledThreadPool(1);

        final Runnable imageGetterRunnable = new Runnable() {
            private final String TAG = "Scheduled task";
            @Override
            public void run() {
                Log.w(TAG, "Scheduled task running!");

                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    //TODO: stop hardcoding subreddit
                    new SubredditJsonTask().execute("macroporn");
                }
            }
        };

        long interval = 30 * 60;
        scheduler.scheduleAtFixedRate(imageGetterRunnable, 5, interval, TimeUnit.SECONDS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();

            connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }
    }

    protected void handleRedditJson(String json) {
        try {
            List<Image> images = ImageRequester.parseRedditJSON(json);

            //TODO: don't hardcode number of images per request? stop always getting 1st image, etc
            List<Image> realImages = findValidImagesInList(images, 5, currentImageURL);

            if(realImages.size() == 0) {
                Log.e(TAG, "Failed to retrieve images");
            }

            int index = (int) Math.floor(Math.random() * realImages.size());
            Log.i("MainActivity", "Trying to load image at URL: " + realImages.get(index).getUrl());
            currentImageURL = realImages.get(index).getUrl();

            new ImageDownloadTask().execute(currentImageURL);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected List<Image> findValidImagesInList(List<Image> images, int count, String currentImageURL) {
        String imageRegex = "([^\\s]+(\\.(?i)(jpg|png|gif|bmp))$)";
        String indirectImgurRegex = "^[^\\s]*imgur\\.com\\/[\\w]+$"; // e.g. imgur.com/1Hi5qJP

        List<Image> ret = new ArrayList<>();

        for(Image image: images) {
            //prevent showing same image, and (currently) filter out anything NSFW
            if(!image.getUrl().equals(currentImageURL) && !image.isOver18()) {
                if(image.getUrl().matches(imageRegex)) {
                    ret.add(image);
                }
                if(image.getUrl().matches(indirectImgurRegex)) {
                    image.setUrl(image.getUrl() + ".jpg");
                    ret.add(image);
                }

                if(ret.size() > count) {
                    break;
                }
            }
        }
        return ret;
    }

    @Override
    protected void onStop() {
//        don't think these are needed here (binding done automatically) but may need on watch?
//        Wearable.DataApi.removeListener(mGoogleApiClient, this);
//        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        scheduler.shutdown();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Ignore
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to Google Play Services");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class SubredditJsonTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String subreddit = params[0];
            try {
                return getJson(subreddit);
            } catch (IOException error) {
                Log.e("SubredditJsonTask", error.getMessage());
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            /* got result JSON here, send it somewhere?
             * probably want to store this locally on phone, and query from watch
             * see "Add data to your watch face": https://developer.android.com/training/wearables/watch-faces/information.html
             */
            handleRedditJson(result);
        }

        private String getJson(String subreddit) throws IOException {
            String resultJSON = "";
            HttpURLConnection conn = null;
            BufferedReader reader = null;

            URL url = null;
            try {
                url = new URL("http://www.reddit.com/r/" + subreddit + "/hot.json?limit=20&raw_json=1");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                InputStream stream = conn.getInputStream();
                StringBuilder builder = new StringBuilder();
                Log.e("ImageRequester", "About to start reading stream");
                if(stream == null) {
                    //no data
                    Log.d("ImageRequester", "No data in stream");
                    return resultJSON;
                }
                reader = new BufferedReader(new InputStreamReader(stream));

                String line;
                while((line = reader.readLine()) != null) {
                    Log.i("ImageRequester", line);
                    builder.append(line).append("\n");
                }

                resultJSON = builder.toString();

                conn.disconnect();
                reader.close();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } finally {
                return resultJSON;
            }
        }
    }

    private class ImageDownloadTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            InputStream in = null;
            Bitmap bmp = null;

            HttpURLConnection con = null;
            try {
                URL imageURL = new URL(params[0]);
                con = (HttpURLConnection)imageURL.openConnection();
                con.setDoInput(true);
                con.connect();

                int responseCode = con.getResponseCode();
                Log.i("ImageDownloadTask", String.valueOf(responseCode));
                if(responseCode == HttpURLConnection.HTTP_OK) {
                    in = con.getInputStream();
                    bmp = BitmapFactory.decodeStream(in);
                    in.close();
                    //compress to 320x320; //TODO receive size from watch somehow
                    bmp = ThumbnailUtils.extractThumbnail(bmp, 320, 290);
                }
                con.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
//          got image at this point; probably want to store it somewhere
            imageView.setImageBitmap(result);
            Asset ass = ImageRequester.createAssetFromBitmap(result);
            Log.v(TAG, "Sending asset to wearable");
            ImageRequester.sendAssetToWearable(ass, mGoogleApiClient);
        }

    }
}
