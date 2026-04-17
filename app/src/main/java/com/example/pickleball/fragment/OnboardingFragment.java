package com.example.pickleball.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.pickleball.R;

public class OnboardingFragment extends Fragment {

    private static final String ARG_IMAGE    = "image";
    private static final String ARG_TITLE    = "title";
    private static final String ARG_SUBTITLE = "subtitle";

    public static OnboardingFragment newInstance(int imageRes, String title, String subtitle) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE, imageRes);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUBTITLE, subtitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);

        if (getArguments() != null) {
            ImageView imgOnboard = view.findViewById(R.id.imgOnboard);
            TextView  tvTitle    = view.findViewById(R.id.tvOnboardTitle);
            TextView  tvSubtitle = view.findViewById(R.id.tvOnboardSubtitle);

            imgOnboard.setImageResource(getArguments().getInt(ARG_IMAGE));
            tvTitle.setText(getArguments().getString(ARG_TITLE));
            tvSubtitle.setText(getArguments().getString(ARG_SUBTITLE));
        }
        return view;
    }
}
