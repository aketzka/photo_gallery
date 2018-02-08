package com.example.user.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 28.01.2018.
 */

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    private static final int MAX_CASH_SIZE = 40;
    LruCache<String, Bitmap> mBitmapLruCache;

    private RecyclerView mRecyclerView;
    private List<GalleryItem> mGalleryItems = new ArrayList<GalleryItem>();
    private int mPageToDownload = 1;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mBitmapLruCache = new LruCache<>(MAX_CASH_SIZE);
        mThumbnailDownloader = new ThumbnailDownloader<PhotoHolder>(new Handler());
        mThumbnailDownloader.setThumbnailDownloadedListener(new ThumbnailDownloader.ThumbnailDownloadedListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail, String url) {
                mBitmapLruCache.put(url, thumbnail);
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                target.bindGalleryItem(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        updateItems();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
        MenuItem searchItem = (MenuItem)menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView)searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit: " + s);
                QueryPreferences.setStoredQuery(getActivity(), s);
                updateItems();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange: " + s);
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                PhotoAdapter adapter = (PhotoAdapter)mRecyclerView.getAdapter();
                if(adapter.isReachedEnd()){
                    updateItems();
                    adapter.resetGallery();
                }
            }
        });
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        setupAdapter();
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = mRecyclerView.getWidth();
                int gridWidth = width/400;//awful (
                mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), gridWidth));
                setupAdapter();
                mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Log.i(TAG, "On global layout called; grid width : " + gridWidth);
            }
        });
        return view;
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>{
        private String mQuery;

        public FetchItemsTask(String query){
            mQuery = query;
        }
        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            if(mQuery == null){
                return new FlickrFetchr().fetchRecentPhotos(mPageToDownload++);
            }else {
                return new FlickrFetchr().searchPhotos(mQuery, mPageToDownload++);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mGalleryItems.addAll(galleryItems);
            setupAdapter();
        }
    }
    private class PhotoHolder extends RecyclerView.ViewHolder{
        private ImageView mImageView;
        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView)itemView.findViewById(R.id.fragment_photo_gallery_item_view);
        }

        private void bindGalleryItem(Drawable drawable){
            mImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;
        private boolean mReachedEnd = false;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            if(position == getItemCount() - 1) mReachedEnd = true;
            Bitmap bitmap = mBitmapLruCache.get(mGalleryItems.get(position).getUrl());
            if(bitmap == null) {
                holder.bindGalleryItem(getResources().getDrawable(R.drawable.waiting));
                mThumbnailDownloader.queueThumbnail(holder, mGalleryItems.get(position).getUrl());
            } else {
                holder.bindGalleryItem(new BitmapDrawable(getResources(), bitmap));
            }
            for(int i = position-10; i <= position+10; i++){
                if(i == position || i < 0 || i >= mGalleryItems.size()) continue;
                if(mGalleryItems.get(i) == null) continue;
                String url = mGalleryItems.get(i).getUrl();
                if(url == null) return;
                if(mBitmapLruCache.get(url) == null)
                    mThumbnailDownloader.queueThumbnail(holder, mGalleryItems.get(position).getUrl());
            }
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }

        public boolean isReachedEnd(){
            return mReachedEnd;
        }

        public void resetGallery(){
            mReachedEnd = false;
        }
    }

    private void setupAdapter(){
        if(isAdded()){
            mRecyclerView.setAdapter(new PhotoAdapter(mGalleryItems));
        }
    }
}
