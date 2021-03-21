package io.github.vejei.bottomnavigationbar.samples;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class SamplesActivity extends AppCompatActivity {
    private static final String TAG = SamplesActivity.class.getSimpleName();
    public static final String SAMPLE_INDEX = "sample_index";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_samples);

        Intent intent = getIntent();
        int sampleIndex = intent.getIntExtra(SAMPLE_INDEX, -1);
        Log.d(TAG, "sampleIndex: " + sampleIndex);

        Fragment fragment;
        switch (sampleIndex) {
            case 0:
                fragment = SamplesFragment.newInstance(R.layout.basic_samples);
                break;
            case 1:
                fragment = SamplesFragment.newInstance(R.layout.item_ripple_samples);
                break;
            case 2:
                fragment = SamplesFragment.newInstance(R.layout.item_color_samples);
                break;
            case 3:
                fragment = SamplesFragment.newInstance(R.layout.label_text_appearance_samples);
                break;
            case 4:
                fragment = SamplesFragment.newInstance(R.layout.label_visibility_mode_samples);
                break;
            case 5:
                fragment = SamplesFragment.newInstance(R.layout.change_icon_sample);
                break;
            case 6:
                fragment = new ActionViewSamplesFragment();
                break;
            case 7:
                fragment = new SetupFragmentSamplesFragment();
                break;
            case 8:
                fragment = new BadgeSampleFragment();
                break;
            default:
                fragment = null;
        }

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}
