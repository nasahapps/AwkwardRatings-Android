package com.nasahapps.awkwardratings.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nasahapps.awkwardratings.R;
import com.nasahapps.awkwardratings.service.NetworkHelper;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class PosterActivity extends ActionBarActivity {

    public static final String EXTRA_URL = "url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        if (savedInstanceState == null) {
            String url = getIntent().getStringExtra(EXTRA_URL);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, PosterFragment.newInstance(url))
                    .commit();
        }
    }

    public static class PosterFragment extends Fragment {

        private ImageView mPoster;
        private ProgressBar mProgressBar;

        public static PosterFragment newInstance(String url) {
            Bundle args = new Bundle();
            args.putString(EXTRA_URL, url);

            PosterFragment fragment = new PosterFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_poster, container, false);
            setRetainInstance(true);

            mProgressBar = (ProgressBar) v.findViewById(R.id.progressBar);

            mPoster = (ImageView) v.findViewById(R.id.poster);
            Uri uri = Uri.parse("https://image.tmdb.org/t/p/w500" + getArguments().getString(EXTRA_URL)
                    + "?api_key=" + NetworkHelper.getInstance(getActivity()).getApiKey());
            Picasso.with(getActivity()).load(uri).into(mPoster, new Callback() {
                @Override
                public void onSuccess() {
                    mProgressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                    Toast.makeText(getActivity(), "Failed to load picture", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
            });

            return v;
        }
    }
}
