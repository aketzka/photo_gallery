package com.example.user.photogallery;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;

/**
 * Created by User on 30.01.2018.
 */

public class GalleryItem {

    @SerializedName("title")
    private String mCaption;
    @SerializedName("id")
    private String mId;
    @SerializedName("url_s")
    private String mUrl;
    @SerializedName("owner")
    private String mOwner;

    public String getOwner() { return mOwner; }

    public void setOwner(String owner) { mOwner = owner; }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public Uri getPhotoPageUri() {
        return Uri.parse("http://www.flickr.com/photos/").buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }


    @Override
    public String toString() {
        return mCaption;
    }
}
