package com.raenn.subredditimages;

import android.util.Log;

import com.raenn.subredditimages.pojo.Image;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ImageRequester {

    public static List<Image> parseRedditJSON(String response) throws JSONException {
        JSONObject json = new JSONObject(response);
        JSONArray stories = json.getJSONObject("data").getJSONArray("children");

        ArrayList<Image> ret = new ArrayList<>();

        for(int i = 0; i < stories.length(); i++) {
            JSONObject data = ((JSONObject) stories.get(i)).getJSONObject("data");
            //TODO: verify don't already have object, etc
            Image image = new Image(
                    data.getString("id"),
                    data.getString("url"),
                    data.getString("created_utc")
            );
            ret.add(image);
        }
        return ret;
    }

    public static String getImageUrl() {

        String resultJSON = "";
        HttpURLConnection conn = null;
        BufferedReader reader = null;

        //probably want to store this locally on phone, and query from watch
        //see "Add data to your watch face": https://developer.android.com/training/wearables/watch-faces/information.html
        try {
            //TODO: make subreddit (etc) configurable
            URL url = new URL("http://www.reddit.com/r/spaceporn/hot.json?limit=20&raw_json=1");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            InputStream stream = conn.getInputStream();
            StringBuilder builder = new StringBuilder();
            if(stream == null) {
                //no data
            }
            reader = new BufferedReader(new InputStreamReader(stream));

            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

            resultJSON = builder.toString();

        } catch (IOException e) {
            Log.e("ImageRequester", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("ImageRequester", "Error closing stream", e);
                }
            }

            return resultJSON;
        }

    }

}
