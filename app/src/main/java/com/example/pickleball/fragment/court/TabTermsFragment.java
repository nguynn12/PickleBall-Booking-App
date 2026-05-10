package com.example.pickleball.fragment.court;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pickleball.R;
import com.example.pickleball.model.Court;

public class TabTermsFragment extends Fragment {

    private static final String ARG_COURT = "court";

    public static TabTermsFragment newInstance(Court court) {
        TabTermsFragment f = new TabTermsFragment();
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
        return inflater.inflate(R.layout.fragment_tab_terms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Court court = getArguments() != null
                ? (Court) getArguments().getSerializable(ARG_COURT) : null;

        TextView tvTerms = view.findViewById(R.id.tvTerms);

        if (court != null && court.getTerms() != null && !court.getTerms().isEmpty()) {
            tvTerms.setText(court.getTerms());
        }
        // Nếu không có, giữ text mặc định trong layout
    }
}
