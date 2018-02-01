package com.example.user.photogallery;

import android.os.HandlerThread;
import android.util.Log;

/**
 * Created by aketza on 01.02.18.
 */

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    public ThumbnailDownloader() {
        super(TAG);
    }
    public void queueThumbnail(T target, String url){
        Log.i(TAG, "Got URL: " + url);
    }
}
