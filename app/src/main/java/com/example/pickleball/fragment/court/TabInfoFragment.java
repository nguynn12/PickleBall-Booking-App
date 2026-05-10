package com.example.pickleball.fragment.court;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pickleball.R;
import com.example.pickleball.model.Court;

public class TabInfoFragment extends Fragment {

    private static final String ARG_COURT = "court";

    public static TabInfoFragment newInstance(Court court) {
        TabInfoFragment f = new TabInfoFragment();
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
        return inflater.inflate(R.layout.fragment_tab_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Court court = getArguments() != null
                ? (Court) getArguments().getSerializable(ARG_COURT) : null;
        if (court == null) return;

        TextView tvLink = view.findViewById(R.id.tvBookingLink);
        TextView tvDesc = view.findViewById(R.id.tvDescription);

        // Link đặt sân (placeholder)
        String link = "Chưa có link đặt sân online";
        tvLink.setText(link);

        // Mô tả
        String desc = court.getDescription();
        tvDesc.setText(desc != null && !desc.isEmpty() ? desc : "Chưa có mô tả về sân.");

        // Copy link
        view.findViewById(R.id.btnCopyLink).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("link", tvLink.getText()));
            Toast.makeText(requireContext(), "Đã sao chép!", Toast.LENGTH_SHORT).show();
        });
    }
}
