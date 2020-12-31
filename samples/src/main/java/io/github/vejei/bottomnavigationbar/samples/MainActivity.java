package io.github.vejei.bottomnavigationbar.samples;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import static io.github.vejei.bottomnavigationbar.samples.SamplesActivity.SAMPLE_INDEX;

public class MainActivity extends AppCompatActivity {
    private final String[] sampleNames = new String[] {
            "Basic",
            "Setup ripple background",
            "Setup label and icon color",
            "Setup text appearance of label",
            "Setup label visibility mode",
            "Change icon when item status change",
            "Add action View",
            "Work with fragments"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView nameList = findViewById(R.id.list_name);
        nameList.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return sampleNames.length;
            }

            @Override
            public String getItem(int position) {
                return sampleNames[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1,
                            parent, false);
                }
                TextView textView = convertView.findViewById(android.R.id.text1);
                textView.setText(getItem(position));
                return convertView;
            }
        });

        nameList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, SamplesActivity.class);
                intent.putExtra(SAMPLE_INDEX, position);
                startActivity(intent);
            }
        });
    }
}