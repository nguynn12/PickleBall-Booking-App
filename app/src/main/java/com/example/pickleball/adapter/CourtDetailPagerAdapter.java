package com.example.pickleball.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.pickleball.fragment.court.TabInfoFragment;
import com.example.pickleball.fragment.court.TabPhotosFragment;
import com.example.pickleball.fragment.court.TabReviewsFragment;
import com.example.pickleball.fragment.court.TabServiceFragment;
import com.example.pickleball.fragment.court.TabTermsFragment;
import com.example.pickleball.model.Court;

public class CourtDetailPagerAdapter extends FragmentStateAdapter {

    private final Court court;

    public CourtDetailPagerAdapter(@NonNull FragmentActivity fa, Court court) {
        super(fa);
        this.court = court;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:  return TabServiceFragment.newInstance(court);
            case 2:  return TabPhotosFragment.newInstance(court);
            case 3:  return TabTermsFragment.newInstance(court);
            case 4:  return TabReviewsFragment.newInstance(court);
            default: return TabInfoFragment.newInstance(court);
        }
    }

    @Override
    public int getItemCount() { return 5; }
}
