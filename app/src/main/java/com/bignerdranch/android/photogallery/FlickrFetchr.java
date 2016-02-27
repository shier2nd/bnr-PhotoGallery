package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Woodinner on 2/23/16.
 */
public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";

    private static final String API_KEY = "2e8ffb4622d66fb8d9d8e6866fdeaf5c";

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems() {

        List<GalleryItem> items = new ArrayList<>();

        try {
            String url = Uri.parse("https://api.flickr.com/services/rest")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            /*JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);*/

            /*
            challenge
             */
            parseItems(items, jsonString);
        } /*catch (JSONException je) {
            Log.e(TAG, "Failed to parse JSON: " + je);
        }*/ catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items" + ioe);
        }

        return items;
    }

    /*private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
        throws IOException, JSONException {

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photosJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photosJsonArray.length(); i++) {
            JSONObject photoJsonObject = photosJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            if (!photoJsonObject.has("url_s")) {
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }*/

    /*
    Challenge
     */
    private void parseItems(List<GalleryItem> items, String jsonString) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(GalleryItem[].class, new ChallengeDeserializer())
                .create();
        GalleryItem[] photosList = gson.fromJson(jsonString, GalleryItem[].class);

        // scan photoList
        for (GalleryItem item : photosList) {
            if (item.getUrl() != null) {
                items.add(item);
            }
        }
    }

    private class ChallengeDeserializer implements JsonDeserializer<GalleryItem[]> {

        @Override
        public GalleryItem[] deserialize(JsonElement je, Type type, JsonDeserializationContext jdc)
                throws JsonParseException
        {
            // Get the "photos" element from the parsed JSON
            JsonElement photos = je.getAsJsonObject().get("photos");
            JsonElement photoArray = photos.getAsJsonObject().get("photo");

            // Deserialize it. You use a new instance of Gson to avoid infinite recursion
            // to this deserializer
            Gson gson = new GsonBuilder()
                    .setFieldNamingStrategy(new ChallengeFieldNamingStrategy())
                    .create();
            return gson.fromJson(photoArray, GalleryItem[].class);
        }
    }

    private class ChallengeFieldNamingStrategy implements FieldNamingStrategy {

        @Override
        public String translateName(Field f) {
            switch (f.getName()) {
                case "mId":
                    return "id";
                case "mCaption":
                    return "title";
                case "mUrl":
                    return "url_s";
                default:
                    return f.getName();
            }
        }
    }
}
