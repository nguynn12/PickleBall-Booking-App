package com.example.pickleball.fragment.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.activity.court.CourtDetailActivity;
import com.example.pickleball.adapter.CourtAdapter;
import com.example.pickleball.model.Court;
import com.example.pickleball.utils.FirebaseHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CourtListFragment extends Fragment {

    private RecyclerView rvCourts;
    private CourtAdapter adapter;
    private final List<Court> masterList  = new ArrayList<>();
    private final List<Court> displayList = new ArrayList<>();
    private TextView btnFilterNearMe, btnFilterAvailable, btnFilterIndoor;
    private EditText edtSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_court_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCourts          = view.findViewById(R.id.rvCourts);
        btnFilterNearMe   = view.findViewById(R.id.btnFilterNearMe);
        btnFilterAvailable = view.findViewById(R.id.btnFilterAvailable);
        btnFilterIndoor   = view.findViewById(R.id.btnFilterIndoor);
        edtSearch         = view.findViewById(R.id.edtSearch);

        rvCourts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CourtAdapter(requireContext(), displayList, court -> {
            Intent intent = new Intent(requireContext(), CourtDetailActivity.class);
            intent.putExtra(CourtDetailActivity.EXTRA_COURT, court);
            startActivity(intent);
        });
        rvCourts.setAdapter(adapter);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnFilterNearMe.setOnClickListener(v -> {
            filterAll();
            setChipSelected(btnFilterNearMe, btnFilterAvailable, btnFilterIndoor);
        });
        btnFilterAvailable.setOnClickListener(v -> {
            filterAvailable();
            setChipSelected(btnFilterAvailable, btnFilterNearMe, btnFilterIndoor);
        });
        btnFilterIndoor.setOnClickListener(v -> {
            filterIndoor();
            setChipSelected(btnFilterIndoor, btnFilterNearMe, btnFilterAvailable);
        });

        loadCourts();
    }

    private void loadCourts() {
        new FirebaseHelper().getAllCourts(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(requireContext(), "Không tải được danh sách sân!", Toast.LENGTH_SHORT).show();
                return;
            }
            masterList.clear();
            for (QueryDocumentSnapshot doc : task.getResult()) {
                Court court = doc.toObject(Court.class);
                if (court.getCourtId() == null || court.getCourtId().isEmpty()) {
                    court.setCourtId(doc.getId());
                }
                masterList.add(court);
            }
            filterAll();
        });
    }

    private void filterSearch(String query) {
        displayList.clear();
        if (query == null || query.trim().isEmpty()) {
            displayList.addAll(masterList);
        } else {
            String q = query.toLowerCase();
            for (Court c : masterList) {
                String name = c.getCourtName() == null ? "" : c.getCourtName();
                String addr = c.getAddress() == null ? "" : c.getAddress();
                if (name.toLowerCase().contains(q) || addr.toLowerCase().contains(q)) {
                    displayList.add(c);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterAll() {
        displayList.clear();
        displayList.addAll(masterList);
        adapter.notifyDataSetChanged();
    }

    private void filterAvailable() {
        displayList.clear();
        for (Court c : masterList) {
            if (c.getPricePerHour() > 0) displayList.add(c);
        }
        adapter.notifyDataSetChanged();
    }

    private void filterIndoor() {
        displayList.clear();
        for (Court c : masterList) {
            String type = c.getType() == null ? "" : c.getType();
            if (type.toLowerCase().contains("trong")) displayList.add(c);
        }
        adapter.notifyDataSetChanged();
    }

    private void setChipSelected(TextView selected, TextView u1, TextView u2) {
        selected.setBackgroundResource(R.drawable.bg_chip_selected);
        selected.setTextColor(requireContext().getColor(R.color.green_primary));
        u1.setBackgroundResource(R.drawable.bg_chip_unselected);
        u1.setTextColor(requireContext().getColor(android.R.color.white));
        u2.setBackgroundResource(R.drawable.bg_chip_unselected);
        u2.setTextColor(requireContext().getColor(android.R.color.white));
    }
}
