package com.example.pickleball.adapter;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.model.User;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class NearbyPlayerAdapter extends RecyclerView.Adapter<NearbyPlayerAdapter.PlayerViewHolder> {

    public interface OnChallengeClickListener {
        void onChallenge(User user);
    }

    private final Context context;
    private final List<User> playerList;
    private final LatLng myLatLng;
    private final OnChallengeClickListener challengeListener;

    public NearbyPlayerAdapter(Context context, List<User> playerList,
                                LatLng myLatLng, OnChallengeClickListener challengeListener) {
        this.context = context;
        this.playerList = playerList;
        this.myLatLng = myLatLng;
        this.challengeListener = challengeListener;
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nearby_player, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        User user = playerList.get(position);

        String name = user.getFullName() != null && !user.getFullName().isEmpty()
                ? user.getFullName() : "Người chơi";
        holder.tvPlayerName.setText(name);

        String skill = user.getSkillLevel();
        holder.tvSkillBadge.setText(formatSkill(skill));
        holder.tvSkillBadge.setBackgroundResource(skillBadgeBackground(skill));

        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(context).load(avatarUrl).circleCrop().into(holder.imgAvatar);
            holder.imgAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatarInitial.setVisibility(View.GONE);
        } else {
            holder.imgAvatar.setVisibility(View.GONE);
            holder.tvAvatarInitial.setVisibility(View.VISIBLE);
            String initial = name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();
            holder.tvAvatarInitial.setText(initial);
        }

        holder.tvDistance.setText(formatDistance(user));

        holder.btnChallenge.setOnClickListener(v -> {
            if (challengeListener != null) challengeListener.onChallenge(user);
        });
    }

    @Override
    public int getItemCount() {
        return playerList == null ? 0 : playerList.size();
    }

    private String formatSkill(String skill) {
        if (skill == null) return "Beginner";
        switch (skill.toLowerCase()) {
            case "intermediate": return "Trung bình";
            case "pro":          return "Chuyên nghiệp";
            default:             return "Người mới";
        }
    }

    private int skillBadgeBackground(String skill) {
        if (skill == null) return R.drawable.bg_skill_badge;
        switch (skill.toLowerCase()) {
            case "intermediate": return R.drawable.bg_badge_green;
            case "pro":          return R.drawable.bg_badge_green;
            default:             return R.drawable.bg_skill_badge;
        }
    }

    private String formatDistance(User user) {
        Double lat = user.getLat();
        Double lng = user.getLng();
        if (lat == null || lng == null || myLatLng == null) return "";
        float[] results = new float[1];
        Location.distanceBetween(myLatLng.latitude, myLatLng.longitude, lat, lng, results);
        float km = results[0] / 1000f;
        if (km < 1f) {
            return String.format(Locale.getDefault(), "%dm", Math.round(results[0]));
        }
        return String.format(Locale.getDefault(), "%.1fkm", km);
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgAvatar;
        TextView tvAvatarInitial, tvPlayerName, tvSkillBadge, tvDistance;
        MaterialButton btnChallenge;

        PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar       = itemView.findViewById(R.id.imgPlayerAvatar);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            tvPlayerName    = itemView.findViewById(R.id.tvPlayerName);
            tvSkillBadge    = itemView.findViewById(R.id.tvSkillBadge);
            tvDistance      = itemView.findViewById(R.id.tvDistance);
            btnChallenge    = itemView.findViewById(R.id.btnChallenge);
        }
    }
}
