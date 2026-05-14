package com.example.pickleball.adapter;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.model.Court;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Locale;

public class NearbyCourtAdapter extends RecyclerView.Adapter<NearbyCourtAdapter.VH> {

    public interface OnCourtClickListener {
        void onCourtClick(Court court);
    }

    public interface CurrentLocationProvider {
        @Nullable
        LatLng getCurrentLatLng();
    }

    private final List<Court> courts;
    private final OnCourtClickListener clickListener;
    private final CurrentLocationProvider locationProvider;

    public NearbyCourtAdapter(
            @NonNull List<Court> courts,
            @NonNull CurrentLocationProvider locationProvider,
            @Nullable OnCourtClickListener clickListener
    ) {
        this.courts = courts;
        this.locationProvider = locationProvider;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_court_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Court court = courts.get(position);

        String name = court.getCourtName() != null ? court.getCourtName() : "Sân Pickleball";
        String address = court.getAddress() != null ? court.getAddress() : "";

        h.tvName.setText(name);
        h.tvAddress.setText(address);

        h.tvDistance.setText(formatDistanceKm(locationProvider.getCurrentLatLng(), court));

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCourtClick(court);
        });
    }

    @Override
    public int getItemCount() {
        return courts == null ? 0 : courts.size();
    }

    @NonNull
    private static String formatDistanceKm(@Nullable LatLng current, @NonNull Court court) {
        if (current == null) return "";
        Double lat = court.getLat();
        Double lng = court.getLng();
        if (lat == null || lng == null) return "";

        float[] results = new float[1];
        Location.distanceBetween(current.latitude, current.longitude, lat, lng, results);
        float km = results[0] / 1000f;

        if (km < 1f) {
            return String.format(Locale.getDefault(), "%dm", Math.round(results[0]));
        }
        return String.format(Locale.getDefault(), "%.1fkm", km);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAddress;
        TextView tvDistance;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCourtName);
            tvAddress = itemView.findViewById(R.id.tvCourtAddress);
            tvDistance = itemView.findViewById(R.id.tvDistance);
        }
    }
}
