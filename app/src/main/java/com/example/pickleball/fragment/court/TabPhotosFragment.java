package com.example.pickleball.fragment.court;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.model.Court;

public class TabPhotosFragment extends Fragment {

    private static final String ARG_COURT = "court";

    public static TabPhotosFragment newInstance(Court court) {
        TabPhotosFragment f = new TabPhotosFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_COURT, court);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_photos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Court court = getArguments() != null
                ? (Court) getArguments().getSerializable(ARG_COURT) : null;

        ImageView imgMain  = view.findViewById(R.id.imgMainPhoto);
        ImageView imgPhoto2 = view.findViewById(R.id.imgPhoto2);
        ImageView imgPhoto3 = view.findViewById(R.id.imgPhoto3);
        TextView tvNoPhotos = view.findViewById(R.id.tvNoPhotos);

        if (court != null && court.getImageUrl() != null && !court.getImageUrl().isEmpty()) {
            String url = court.getImageUrl();
            Glide.with(this).load(url).centerCrop().into(imgMain);
            Glide.with(this).load(url).centerCrop().into(imgPhoto2);
            Glide.with(this).load(url).centerCrop().into(imgPhoto3);
            tvNoPhotos.setVisibility(View.GONE);
        } else {
            imgMain.setVisibility(View.GONE);
            imgPhoto2.setVisibility(View.GONE);
            imgPhoto3.setVisibility(View.GONE);
            tvNoPhotos.setVisibility(View.VISIBLE);
        }
    }
}
