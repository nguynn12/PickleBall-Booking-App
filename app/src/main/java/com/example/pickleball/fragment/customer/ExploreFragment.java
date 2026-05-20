package com.example.pickleball.fragment.customer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.activity.player.RadarMatchActivity;
import com.example.pickleball.adapter.NearbyPlayerAdapter;
import com.example.pickleball.model.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExploreFragment extends Fragment {

    private static final float NEARBY_RADIUS_METERS = 5000f; // 5 km
    private static final long ONLINE_THRESHOLD_MS   = 10 * 60 * 1000L; // 10 phút

    private RecyclerView rvNearbyPlayers;
    private NearbyPlayerAdapter adapter;
    private final List<User> nearbyPlayerList = new ArrayList<>();

    private View layoutEmpty, layoutNoGps;
    private TextView tvNearbyCount;
    private MaterialButton btnFindPlayer, btnEnableGps;

    private FusedLocationProviderClient fusedLocationClient;
    private LatLng myLatLng;

    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                            || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (granted) {
                        if (layoutNoGps != null) layoutNoGps.setVisibility(View.GONE);
                        requestLocationAndLoadPlayers();
                    } else {
                        if (layoutNoGps != null) layoutNoGps.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvNearbyPlayers = view.findViewById(R.id.rvNearbyPlayers);
        layoutEmpty     = view.findViewById(R.id.layoutEmpty);
        layoutNoGps     = view.findViewById(R.id.layoutNoGps);
        tvNearbyCount   = view.findViewById(R.id.tvNearbyCount);
        btnFindPlayer   = view.findViewById(R.id.btnFindPlayer);
        btnEnableGps    = view.findViewById(R.id.btnEnableGps);

        rvNearbyPlayers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NearbyPlayerAdapter(requireContext(), nearbyPlayerList, myLatLng,
                user -> Toast.makeText(requireContext(),
                        "Tính năng thách đấu đang phát triển!", Toast.LENGTH_SHORT).show());
        rvNearbyPlayers.setAdapter(adapter);

        btnFindPlayer.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), RadarMatchActivity.class);
            startActivity(intent);
        });

        btnEnableGps.setOnClickListener(v -> requestLocationPermission());

        requestLocationAndLoadPlayers();
    }

    private void requestLocationAndLoadPlayers() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                updateMyLocationInFirestore(myLatLng);
                loadNearbyPlayers(myLatLng);
            } else {
                loadNearbyPlayers(null);
            }
        });
    }

    private void loadNearbyPlayers(@Nullable LatLng myLocation) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        FirebaseFirestore.getInstance()
                .collection("Users")
                .whereGreaterThan("lastSeen", System.currentTimeMillis() - ONLINE_THRESHOLD_MS)
                .get()
                .addOnSuccessListener(snap -> {
                    nearbyPlayerList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        String uid = doc.getId();
                        if (uid.equals(currentUid)) continue;

                        Double lat = doc.getDouble("lat");
                        Double lng = doc.getDouble("lng");
                        if (lat == null || lng == null) continue;

                        if (myLocation != null) {
                            float[] dist = new float[1];
                            Location.distanceBetween(
                                    myLocation.latitude, myLocation.longitude,
                                    lat, lng, dist);
                            if (dist[0] > NEARBY_RADIUS_METERS) continue;
                        }

                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUserId(uid);
                            user.setLat(lat);
                            user.setLng(lng);
                            nearbyPlayerList.add(user);
                        }
                    }

                    updateUI();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Không tải được danh sách người chơi", Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        if (!isAdded()) return;
        adapter.notifyDataSetChanged();
        if (nearbyPlayerList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvNearbyCount.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            tvNearbyCount.setVisibility(View.VISIBLE);
            tvNearbyCount.setText(nearbyPlayerList.size() + " người chơi trong vòng 5km");
        }
    }

    private void updateMyLocationInFirestore(@NonNull LatLng latLng) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("lat", latLng.latitude);
        updates.put("lng", latLng.longitude);
        updates.put("lastSeen", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("Users").document(uid)
                .update(updates);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }
}
