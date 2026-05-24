package com.example.pickleball.fragment.customer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.activity.court.CourtDetailActivity;
import com.example.pickleball.adapter.NearbyCourtAdapter;
import com.example.pickleball.model.Court;
import com.example.pickleball.utils.FirebaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.bumptech.glide.Glide;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng DEFAULT_CENTER = new LatLng(11.940419, 108.458313);
    private static final float DEFAULT_ZOOM = 13.5f;
    private static final float SEARCH_RADIUS_METERS = 15_000f;
    private static final long MAX_LAST_LOCATION_AGE_MS = 2 * 60 * 1000;

    private MapView mapView;
    private GoogleMap googleMap;

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private LatLng currentLatLng;

    private final Map<Marker, Court> markerCourtMap = new HashMap<>();

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private FloatingActionButton btnToggleSheet;
    private FloatingActionButton btnMyLocation;
    private EditText edtSearch;
    private RecyclerView rvNearbyCourts;
    private NearbyCourtAdapter adapter;

    private View layoutSheetList;
    private View layoutSheetDetail;
    private View btnSheetBack;

    private TextView tvSheetCourtName;
    private TextView tvSheetCourtAddress;
    private TextView tvSheetCourtDistance;
    private TextView tvSheetOpenHours;
    private TextView tvSheetPhone;
    private MaterialButton btnOpenCourtDetail;
    private ImageView imgSheetCourt;

    private BitmapDescriptor customMarkerIcon;

    private final java.util.List<Court> nearbyCourts = new ArrayList<>();
    private final java.util.List<Court> displayCourts = new ArrayList<>();

    @Nullable
    private Court selectedCourt;

    private boolean suppressAutoFitBounds;

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
                        fetchAndCenterToDevice(true, true);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_map, container, false);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        edtSearch = view.findViewById(R.id.edtSearchMap);
        rvNearbyCourts = view.findViewById(R.id.rvNearbyCourts);
        rvNearbyCourts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NearbyCourtAdapter(
                displayCourts,
                () -> currentLatLng,
                this::openCourtDetailSheet
        );
        rvNearbyCourts.setAdapter(adapter);

        layoutSheetList = view.findViewById(R.id.layoutSheetList);
        layoutSheetDetail = view.findViewById(R.id.layoutSheetDetail);
        btnSheetBack = view.findViewById(R.id.btnSheetBack);
        if (btnSheetBack != null) {
            btnSheetBack.setOnClickListener(v -> showSheetList());
        }

        tvSheetCourtName = view.findViewById(R.id.tvSheetCourtName);
        tvSheetCourtAddress = view.findViewById(R.id.tvSheetCourtAddress);
        tvSheetCourtDistance = view.findViewById(R.id.tvSheetCourtDistance);
        tvSheetOpenHours = view.findViewById(R.id.tvSheetOpenHours);
        tvSheetPhone = view.findViewById(R.id.tvSheetPhone);
        imgSheetCourt = view.findViewById(R.id.imgSheetCourt);
        btnOpenCourtDetail = view.findViewById(R.id.btnOpenCourtDetail);
        if (btnOpenCourtDetail != null) {
            btnOpenCourtDetail.setOnClickListener(v -> {
                if (selectedCourt == null) return;
                Intent intent = new Intent(requireContext(), CourtDetailActivity.class);
                intent.putExtra(CourtDetailActivity.EXTRA_COURT, selectedCourt);
                startActivity(intent);
            });
        }

        View bottomSheet = view.findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        btnToggleSheet = view.findViewById(R.id.btnToggleSheet);
        btnMyLocation = view.findViewById(R.id.btnMyLocation);

        btnToggleSheet.setOnClickListener(v -> toggleBottomSheet());
        btnMyLocation.setOnClickListener(v -> fetchAndCenterToDevice(true, true));

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                updateToggleIcon();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applySearchFilter(s == null ? "" : s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        updateToggleIcon();
        showSheetList();
        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        customMarkerIcon = buildCustomMarkerIcon();
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM));

        googleMap.setOnMarkerClickListener(marker -> {
            Court court = markerCourtMap.get(marker);
            if (court == null) return false;
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f));
            openCourtDetailSheet(court);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            return true;
        });

        fetchAndCenterToDevice(false, false);
    }

    private void fetchAndCenterToDevice(boolean moveCamera, boolean forceFresh) {
        if (googleMap == null) return;

        if (!hasLocationPermission()) {
            requestLocationPermission();
            if (moveCamera) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM));
            }
            return;
        }

        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException ignored) {
        }

        if (forceFresh) {
            requestFreshLocation(moveCamera);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && isLastLocationFresh(location)) {
                onDeviceLocationAvailable(location, moveCamera);
                return;
            }
            requestFreshLocation(moveCamera);
        });
    }

    private void requestFreshLocation(boolean moveCamera) {
        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        onDeviceLocationAvailable(loc, moveCamera);
                    } else {
                        if (googleMap != null) {
                            if (moveCamera) {
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM));
                            }
                            loadNearbyCourtsAndRender();
                        }
                    }
                });
    }

    private boolean isLastLocationFresh(@NonNull Location location) {
        long age = System.currentTimeMillis() - location.getTime();
        return age >= 0 && age <= MAX_LAST_LOCATION_AGE_MS;
    }

    private void onDeviceLocationAvailable(@NonNull Location location, boolean moveCamera) {
        suppressAutoFitBounds = moveCamera;
        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (googleMap != null && moveCamera) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));
        }
        loadNearbyCourtsAndRender();
    }

    private void loadNearbyCourtsAndRender() {
        if (googleMap == null) return;

        new FirebaseHelper().getAllCourts(task -> {
            if (!task.isSuccessful() || task.getResult() == null || googleMap == null) {
                return;
            }

            markerCourtMap.clear();
            googleMap.clear();
            nearbyCourts.clear();

            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            boolean hasAny = false;

            for (QueryDocumentSnapshot doc : task.getResult()) {
                LatLng courtLatLng = tryGetLatLngFromDoc(doc);
                if (courtLatLng == null) continue;

                if (currentLatLng != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            currentLatLng.latitude, currentLatLng.longitude,
                            courtLatLng.latitude, courtLatLng.longitude,
                            results
                    );
                    if (results[0] > SEARCH_RADIUS_METERS) continue;
                }

                Court court = doc.toObject(Court.class);
                if (court.getCourtId() == null || court.getCourtId().isEmpty()) {
                    court.setCourtId(doc.getId());
                }
                court.setLat(courtLatLng.latitude);
                court.setLng(courtLatLng.longitude);

                String name = court.getCourtName() != null ? court.getCourtName() : "Sân Pickleball";
                String address = court.getAddress() != null ? court.getAddress() : "";

                addMarker(boundsBuilder, court, name, address, courtLatLng);
                nearbyCourts.add(court);
                hasAny = true;
            }

            applySearchFilter(edtSearch == null ? "" : edtSearch.getText().toString());

            if (!suppressAutoFitBounds) {
                if (hasAny) {
                    try {
                        if (currentLatLng != null) boundsBuilder.include(currentLatLng);
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80));
                    } catch (Exception ignored) {
                        LatLng center = currentLatLng != null ? currentLatLng : DEFAULT_CENTER;
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, DEFAULT_ZOOM));
                    }
                } else {
                    LatLng center = currentLatLng != null ? currentLatLng : DEFAULT_CENTER;
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, DEFAULT_ZOOM));
                }
            }

            suppressAutoFitBounds = false;
        });
    }

    private void addMarker(LatLngBounds.Builder boundsBuilder, Court court, String name, String address, LatLng latLng) {
        if (googleMap == null) return;
        BitmapDescriptor icon = customMarkerIcon != null
                ? customMarkerIcon
                : BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(name)
                .snippet(address)
                .icon(icon));
        if (marker != null) {
            markerCourtMap.put(marker, court);
        }
        boundsBuilder.include(latLng);
    }

    @Nullable
    private LatLng tryGetLatLngFromDoc(@NonNull QueryDocumentSnapshot doc) {
        Double lat = doc.getDouble("lat");
        Double lng = doc.getDouble("lng");
        if (lat == null || lng == null) return null;
        return new LatLng(lat, lng);
    }

    private void applySearchFilter(@NonNull String query) {
        displayCourts.clear();
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            displayCourts.addAll(nearbyCourts);
        } else {
            for (Court c : nearbyCourts) {
                String name = c.getCourtName() == null ? "" : c.getCourtName();
                String addr = c.getAddress() == null ? "" : c.getAddress();
                if (name.toLowerCase().contains(q) || addr.toLowerCase().contains(q)) {
                    displayCourts.add(c);
                }
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void openCourtDetailSheet(@NonNull Court court) {
        selectedCourt = court;
        bindDetailSheet(court);
        showSheetDetail();
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void bindDetailSheet(@NonNull Court court) {
        if (tvSheetCourtName != null) {
            tvSheetCourtName.setText(court.getCourtName() != null ? court.getCourtName() : "Sân Pickleball");
        }
        if (tvSheetCourtAddress != null) {
            tvSheetCourtAddress.setText(court.getAddress() != null ? court.getAddress() : "");
        }
        if (tvSheetOpenHours != null) {
            tvSheetOpenHours.setText("🕐 " + court.getOpenHours());
        }
        if (tvSheetPhone != null) {
            String phone = court.getPhone();
            tvSheetPhone.setText(phone != null && !phone.isEmpty() ? "☎ " + phone : "");
        }
        if (imgSheetCourt != null) {
            String img = court.getImageUrl();
            if (img != null && !img.isEmpty()) {
                Glide.with(this).load(img).centerCrop().placeholder(R.color.divider).into(imgSheetCourt);
            } else {
                imgSheetCourt.setImageResource(R.color.divider);
            }
        }

        if (tvSheetCourtDistance != null) {
            tvSheetCourtDistance.setText(formatSelectedCourtDistance(court));
        }
    }

    @NonNull
    private String formatSelectedCourtDistance(@NonNull Court court) {
        if (currentLatLng == null || court.getLat() == null || court.getLng() == null) return "";

        float[] results = new float[1];
        Location.distanceBetween(
                currentLatLng.latitude, currentLatLng.longitude,
                court.getLat(), court.getLng(),
                results
        );
        float km = results[0] / 1000f;
        if (km < 1f) {
            return String.format(java.util.Locale.getDefault(), "Khoảng cách: %dm", Math.round(results[0]));
        }
        return String.format(java.util.Locale.getDefault(), "Khoảng cách: %.1fkm", km);
    }

    @androidx.annotation.Nullable
    private BitmapDescriptor buildCustomMarkerIcon() {
        try {
            android.graphics.drawable.Drawable d =
                    AppCompatResources.getDrawable(requireContext(), R.drawable.ic_court_marker);
            if (d == null) return null;
            Bitmap bmp = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            d.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bmp);
        } catch (Exception e) {
            return null;
        }
    }

    private void showSheetList() {
        selectedCourt = null;
        if (layoutSheetList != null) layoutSheetList.setVisibility(View.VISIBLE);
        if (layoutSheetDetail != null) layoutSheetDetail.setVisibility(View.GONE);
    }

    private void showSheetDetail() {
        if (layoutSheetList != null) layoutSheetList.setVisibility(View.GONE);
        if (layoutSheetDetail != null) layoutSheetDetail.setVisibility(View.VISIBLE);
    }

    private void toggleBottomSheet() {
        if (bottomSheetBehavior == null) return;
        int state = bottomSheetBehavior.getState();
        if (state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        updateToggleIcon();
    }

    private void updateToggleIcon() {
        if (btnToggleSheet == null || bottomSheetBehavior == null) return;
        int state = bottomSheetBehavior.getState();
        if (state == BottomSheetBehavior.STATE_HIDDEN) {
            btnToggleSheet.setImageResource(R.drawable.ic_chevron_up);
            return;
        }
        if (state == BottomSheetBehavior.STATE_EXPANDED) {
            btnToggleSheet.setImageResource(R.drawable.ic_chevron_down);
        } else {
            btnToggleSheet.setImageResource(R.drawable.ic_chevron_down);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (locationPermissionLauncher == null) return;
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    public void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        googleMap = null;
        suppressAutoFitBounds = false;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
