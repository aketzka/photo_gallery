package com.example.user.photogallery;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
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
        mThumbnailDownloader = new ThumbnailDownloader<PhotoHolder>();
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        new FetchItemsTask().execute();
        Log.i(TAG, "Background thread started");
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
                    new FetchItemsTask().execute();
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
                int gridWidth = width/200;//awful (
                mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), gridWidth));
                setupAdapter();
                mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Log.i(TAG, "On global layout called; grid width : " + gridWidth);
            }
        });
        return view;
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>{

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            List<GalleryItem> items = new FlickrFetchr().fetchItems(mPageToDownload++);
            return items;
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
            holder.bindGalleryItem(getResources().getDrawable(R.drawable.waiting));
            mThumbnailDownloader.queueThumbnail(holder, mGalleryItems.get(position).getUrl());
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
