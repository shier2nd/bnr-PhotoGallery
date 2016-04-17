package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Woodinner on 2/23/16.
 */
public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
//    private boolean mBackgroundIsLoading;
//    private int mLastFetchedPage;
    private SearchView mSearchView;
    private ProgressBar mProgressBar;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
//        mLastFetchedPage = 1;
//        new FetchItemsTask().execute(mLastFetchedPage);
        updateItems();
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

        /*mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

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
        });*/

        mProgressBar = (ProgressBar) v.findViewById(R.id.fragment_progress_bar);
        showProgressBar(true);

        setupAdapter();

        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clear cache
        clearImageDiskCache();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        mSearchView = (SearchView) searchItem.getActionView();

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);

                collapseSearchView(getActivity());

                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTextChange: " + newText);
                return false;
            }
        });

        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                mSearchView.setQuery(query, false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);

                if (!mSearchView.isIconified()) {
                    Log.d(TAG, "SearchView is not iconified, so collapse the SearchView");
                    collapseSearchView(getActivity());
                }

                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query, this).execute();
    }

    private void collapseSearchView(Context context) {
        // hide the soft keyboard
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        // collapse the SearchView
        mSearchView.setIconified(true);
        mSearchView.setIconified(true);
    }

    private void showProgressBar(boolean isShow) {
        if (isShow) {
            mProgressBar.setVisibility(View.VISIBLE);
            mPhotoRecyclerView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mPhotoRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private boolean clearImageDiskCache() {
        File cache = new File(getActivity().getCacheDir(), "picasso-cache");
        if (cache.exists() && cache.isDirectory()) {
            deleteDir(cache);
        }
        return false;
    }

    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            for (String fileName : dir.list()) {
                boolean success = deleteDir(new File(dir, fileName));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView) itemView
                    .findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            Picasso.with(getActivity())
                    .load(galleryItem.getUrl())
                    .placeholder(R.drawable.bill_up_close)
                    .into(mItemImageView);
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
            holder.bindGalleryItem(galleryItem);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {
        private String mQuery;
        private PhotoGalleryFragment mGalleryFragment;

        public FetchItemsTask(String query, PhotoGalleryFragment fragment) {
            mQuery = query;
            mGalleryFragment = fragment;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mGalleryFragment.isResumed()) {
                mGalleryFragment.showProgressBar(true);
            }
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            /*mBackgroundIsLoading = true;
            return new FlickrFetchr().fetchItems(params[0]);*/

            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos();
            } else {
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            /*mBackgroundIsLoading = false;

            if (mLastFetchedPage > 1) {
                mItems.addAll(items);
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            } else {
                mItems = items;
                setupAdapter();
            }*/
            mItems = items;
            setupAdapter();
            mGalleryFragment.showProgressBar(false);
        }
    }
}
