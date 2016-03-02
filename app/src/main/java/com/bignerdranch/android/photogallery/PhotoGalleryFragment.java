package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Woodinner on 2/23/16.
 */
public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private boolean mBackgroundIsLoading;
    private int mLastFetchedPage;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mLastFetchedPage = 1;
        new FetchItemsTask().execute(mLastFetchedPage);

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloaderListener(
                new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloader(PhotoHolder photoHolder, Bitmap bitmap) {
                        if (!isAdded()) return;
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                    }
                });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        final int numColumns = getContext().getResources().getInteger(R.integer.grid_columns);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), numColumns));
        /*mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Point size = new Point();
                getActivity().getWindowManager().getDefaultDisplay().getSize(size);
                int newNumColumns = (int) Math.floor(size.x * numColumns / 1440);   // assuming 1440 was the appropriate number for 3 columns
                if (newNumColumns != numColumns) {
                    GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
                    layoutManager.setSpanCount(newNumColumns);
                }
            }
        });*/

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            boolean isScrollingUp = false;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int lastItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
                    int totalItemsNumber = layoutManager.getItemCount();

                    if (lastItemPosition == (totalItemsNumber - 1) && isScrollingUp && !mBackgroundIsLoading) {
                        Log.i(TAG, "is loading page" + (mLastFetchedPage + 1) +
                                ", the current total items number is " + totalItemsNumber);
                        Toast.makeText(getActivity(), R.string.toast_loading_new_page, Toast.LENGTH_SHORT).show();
                        mLastFetchedPage++;
                        new FetchItemsTask().execute(mLastFetchedPage);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                isScrollingUp = (dy > 0);
            }
        });

        setupAdapter();

        return v;
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

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

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
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            holder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            mBackgroundIsLoading = true;
            return new FlickrFetchr().fetchItems(params[0]);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mBackgroundIsLoading = false;

            if (mLastFetchedPage > 1) {
                mItems.addAll(items);
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            } else {
                mItems = items;
                setupAdapter();
            }
        }
    }
}
