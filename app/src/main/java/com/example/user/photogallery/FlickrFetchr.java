package com.example.user.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 29.01.2018.
 */

public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "fb50d92cf1e21994bab357e667b1460b";

    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + " : with " + urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0){
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        }catch (IOException ioe){
                Log.e(TAG, ioe.getMessage());
        }
        finally {
            connection.disconnect();
        }
        return new byte[1024];
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems(int page){
        List<GalleryItem> items = new ArrayList<GalleryItem>();
        try {
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .appendQueryParameter("page", String.valueOf(page))
                    .build().toString();
            String jsonString = getUrlString(url);
            JSONObject jsonObject = new JSONObject(jsonString);
            parseItems(items, jsonObject);
            Log.i(TAG, "Received JSON: " + jsonString);
        }catch (IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parson json", e);
        }
        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonObject) throws IOException, JSONException{
        JSONObject photosJSONObject = jsonObject.getJSONObject("photos");
        JSONArray photoJSONArray = photosJSONObject.getJSONArray("photo");
        String galleryString = photoJSONArray.toString();
        Gson gson = new Gson();
        Type galleryItemType = new TypeToken<ArrayList<GalleryItem>>(){}.getType();
        List<GalleryItem> galleryItems = gson.fromJson(galleryString, galleryItemType);
        items.addAll(galleryItems);
        /*for(int i = 0; i < photoJSONArray.length(); i++){
            JSONObject photoJSONObject = photoJSONArray.getJSONObject(i);
            GalleryItem item = new GalleryItem();
            item.setCaption(photoJSONObject.getString("title"));
            item.setId(photoJSONObject.getString("id"));
            if(!photoJSONObject.has("url_s"))
                continue;
            item.setUrl(photoJSONObject.getString("url_s"));
            items.add(item);
        }*/
    }



}
