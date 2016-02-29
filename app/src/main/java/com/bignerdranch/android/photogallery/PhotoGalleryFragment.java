package com.bignerdranch.android.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mLastFetchedPage = 1;
        new FetchItemsTask().execute(mLastFetchedPage);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

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

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private TextView mTitleTextView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem item) {
            mTitleTextView.setText(item.toString());
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);
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
