package com.example.user.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

/**
 * Created by aketza on 09.02.18.
 */

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    private static final int POLL_INTERVAL = 1000 * 60;//60 seconds

    public static Intent newIntent(Context context){
        return new Intent(context, PollService.class);
    }

    public PollService(String name) {
        super(name);
    }

    public PollService(){
        super(TAG);
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(!isNetworkAvailable())
            return;
        String query = QueryPreferences.getStoredQuery(this);
        String lastId = QueryPreferences.getStoredQuery(this);
        List<GalleryItem> items;
        if(query == null){
            items = new FlickrFetchr().fetchRecentPhotos(0);
        } else {
            items = new FlickrFetchr().searchPhotos(query, 0);
        }

        if(items.size() == 0) return;
        String id = items.get(0).getId();
        if(id == lastId)
            Log.i(TAG, "Same result");
        else
            Log.i(TAG, "New result");
        QueryPreferences.setLastResultId(this, id);
    }

    private boolean isNetworkAvailable(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if(cm.getActiveNetworkInfo() == null) return false;
        return cm.getActiveNetworkInfo().isConnected();
    }

    public static void setServiceAlarm(Context context, boolean isOn){
        Intent intent = newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(ALARM_SERVICE);
        if(isOn){
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL, pendingIntent);
        } else{
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    public static boolean isServiceAlarmOn(Context context){
        Intent intent = newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;

    }
}