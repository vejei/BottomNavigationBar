package io.github.vejei.bottomnavigationbar.samples;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;

import io.github.vejei.bottomnavigationbar.BottomNavigationBar;

public class SetupFragmentSamplesFragment extends Fragment {
    private static final String TAG = SetupFragmentSamplesFragment.class.getSimpleName();
    private Fragment currentFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_fragment_samples, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentManager fragmentManager = getChildFragmentManager();
        BottomNavigationBar bottomNavigationBar = view.findViewById(R.id.bottom_navigation_bar);
        bottomNavigationBar.setOnNavigationItemSelectedListener(
                new BottomNavigationBar.OnNavigationItemSelectedListener() {
                    @Override
                    public void onNavigationItemSelected(MenuItem item) {
                        Fragment selectedFragment = null;
                        int itemId = item.getItemId();
                        if (itemId == R.id.home) {
                            selectedFragment = fragmentManager.findFragmentByTag(HomeFragment.TAG);
                        } else if (itemId == R.id.explore) {
                            selectedFragment = fragmentManager
                                    .findFragmentByTag(ExploreFragment.TAG);
                        } else if (itemId == R.id.collections) {
                            selectedFragment = fragmentManager
                                    .findFragmentByTag(CollectionsFragment.TAG);
                        } else if (itemId == R.id.school) {
                            selectedFragment = fragmentManager
                                    .findFragmentByTag(SchoolFragment.TAG);
                        } else if (itemId == R.id.account) {
                            selectedFragment = fragmentManager
                                    .findFragmentByTag(AccountFragment.TAG);
                        }

                        if (currentFragment != null && selectedFragment != null) {
                            fragmentManager.beginTransaction().show(selectedFragment)
                                    .hide(currentFragment).commit();
                            currentFragment = selectedFragment;
                        }
                    }
                });
        View actionView = bottomNavigationBar.getActionView();
        MaterialButton addButton = actionView.findViewById(R.id.button_add);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "clicked", Toast.LENGTH_SHORT).show();
            }
        });

        initializeFragments();
    }

    private void initializeFragments() {
        addAndHideFragment(new ExploreFragment(), ExploreFragment.TAG);
        addAndHideFragment(new CollectionsFragment(), CollectionsFragment.TAG);
        addAndHideFragment(new SchoolFragment(), SchoolFragment.TAG);
        addAndHideFragment(new AccountFragment(), AccountFragment.TAG);

        HomeFragment homeFragment = new HomeFragment();
        getChildFragmentManager().beginTransaction()
                .add(R.id.child_fragment_container, homeFragment, HomeFragment.TAG)
                .commit();
        currentFragment = homeFragment;
    }

    private void addAndHideFragment(Fragment fragment, String tag) {
        getChildFragmentManager().beginTransaction()
                .add(R.id.child_fragment_container, fragment, tag)
                .hide(fragment)
                .commit();
    }

    private static View createFragmentView(Context context, String name) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setGravity(Gravity.CENTER);
        TextView textView = new TextView(context);
        textView.setText(name);
        linearLayout.addView(textView);
        return linearLayout;
    }

    public static class HomeFragment extends Fragment {
        public static final String TAG = HomeFragment.class.getSimpleName();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return createFragmentView(getContext(), "Home");
        }
    }

    public static class ExploreFragment extends Fragment {
        public static final String TAG = ExploreFragment.class.getSimpleName();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return createFragmentView(getContext(), "Explore");
        }
    }

    public static class CollectionsFragment extends Fragment {
        public static final String TAG = CollectionsFragment.class.getSimpleName();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return createFragmentView(getContext(), "Collections");
        }
    }

    public static class SchoolFragment extends Fragment {
        public static final String TAG = SchoolFragment.class.getSimpleName();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return createFragmentView(getContext(), "School");
        }
    }

    public static class AccountFragment extends Fragment {
        public static final String TAG = AccountFragment.class.getSimpleName();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return createFragmentView(getContext(), "Account");
        }
    }
}
