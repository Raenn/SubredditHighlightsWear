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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.raenn.subredditimages.pojo.Image;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

//TODO: stop using everything static, once more stable
//TODO: split this class up, it's just random methods
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

    public static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    public static void sendAssetToWearable(Asset asset, GoogleApiClient apiClient) {
        PutDataMapRequest dataMap = PutDataMapRequest.create("/image");
        dataMap.getDataMap().putAsset("image", asset);
        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(apiClient, request);
    }



}
