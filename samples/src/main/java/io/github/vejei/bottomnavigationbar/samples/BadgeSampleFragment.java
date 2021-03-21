package io.github.vejei.bottomnavigationbar.samples;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.github.vejei.bottomnavigationbar.BadgeDrawable;
import io.github.vejei.bottomnavigationbar.BottomNavigationBar;

public class BadgeSampleFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.badge_sample, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BottomNavigationBar bottomNavigationBar = view.findViewById(R.id.bottom_navigation_bar);
        BadgeDrawable homeBadge = bottomNavigationBar.getOrCreateBadge(R.id.home);
        homeBadge.setNumber(-1);

        bottomNavigationBar.setOnNavigationItemSelectedListener(
                new BottomNavigationBar.OnNavigationItemSelectedListener() {
                    @Override
                    public void onNavigationItemSelected(MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.home) {
                            BadgeDrawable badgeDrawable = bottomNavigationBar.getOrCreateBadge(
                                    R.id.home);
                            badgeDrawable.setNumber(1000);
                        } else if (itemId == R.id.explore) {
                            BadgeDrawable badgeDrawable = bottomNavigationBar.getOrCreateBadge(
                                    R.id.explore);
                            badgeDrawable.setNumber(999);
                        } else if (itemId == R.id.collections) {
                            BadgeDrawable badgeDrawable = bottomNavigationBar.getOrCreateBadge(
                                    R.id.collections);
                            badgeDrawable.setNumber(9);
                        } else if (itemId == R.id.school) {
                            BadgeDrawable badgeDrawable = bottomNavigationBar.getOrCreateBadge(
                                    R.id.school);
                            badgeDrawable.setNumber(10);
                        } else if (itemId == R.id.account) {
                            bottomNavigationBar.getOrCreateBadge(R.id.account);
                        }
                    }
                });
        bottomNavigationBar.setOnNavigationItemReselectedListener(
                new BottomNavigationBar.OnNavigationItemReselectedListener() {
                    @Override
                    public void onNavigationItemReselected(MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.home) {
                            bottomNavigationBar.removeBadge(R.id.home);
                        } else if (itemId == R.id.explore) {
                            bottomNavigationBar.removeBadge(R.id.explore);
                        } else if (itemId == R.id.collections) {
                            bottomNavigationBar.removeBadge(R.id.collections);
                        } else if (itemId == R.id.school) {
                            bottomNavigationBar.removeBadge(R.id.school);
                        } else if (itemId == R.id.account) {
                            bottomNavigationBar.removeBadge(R.id.account);
                        }
                    }
                });
    }
}
