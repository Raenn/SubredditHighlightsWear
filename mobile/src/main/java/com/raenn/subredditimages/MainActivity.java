package com.raenn.subredditimages;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
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
import java.util.List;


public class MainActivity extends Activity implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "SubImages-Handheld";

    private GoogleApiClient mGoogleApiClient;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d("aaaaa", "onConnected: " + connectionHint);
                        //now use data layer API
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d("aaaaaa", "onConnectionSuspended: " + cause);
                    }

                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.e(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();

        imageView = (ImageView) findViewById(R.id.subredditImageTest);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();

            ImageRequester ir = new ImageRequester();

            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                //TODO: stop hardcoding subreddit
                new SubredditJsonTask().execute("pics");
            }
        }
    }

    protected void handleRedditJson(String json) {
        //TODO: check that link is an image, request more if too few, etc
        try {
            List<Image> images = ImageRequester.parseRedditJSON(json);
            if(images.size() == 0) {
                Log.e(TAG, "Failed to retrieve images");
            }
            Log.i("MainActivity", "Trying to load image at URL: " + images.get(1).getUrl());
            String url = images.get(1).getUrl();

            new ImageDownloadTask().execute(url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
//        see quiz example, not sure if need these yet
//        Wearable.DataApi.removeListener(mGoogleApiClient, this);
//        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
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
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        //TODO: stuffs
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
            Log.i("GREAT SUCCESS", "YEAH");
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
//                    compress to 320x320
                    bmp = Bitmap.createScaledBitmap(bmp, 320,320, false);
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
        }

    }
}
