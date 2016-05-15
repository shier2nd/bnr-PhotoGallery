package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Created by Woodinner on 5/12/16.
 */
public class PhotoPageActivity extends SingleFragmentActivity {

    private PhotoPageFragment mFragment;

    public static Intent newIntent(Context context, Uri photoPageUri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(photoPageUri);
        return i;
    }

    @Override
    protected PhotoPageFragment createFragment() {
        mFragment = PhotoPageFragment.newInstance(getIntent().getData());
        return mFragment;
    }

    @Override
    public void onBackPressed() {
        if (mFragment.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
}
