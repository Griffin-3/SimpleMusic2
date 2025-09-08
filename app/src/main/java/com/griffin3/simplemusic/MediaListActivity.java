package com.griffin3.simplemusic;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MediaListActivity extends AppCompatActivity {
    private ArrayList<String> fileList;
    private int currentPosition = -1;
    private DatabaseHelper dbHelper;
    private CustomAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_list);

        dbHelper = new DatabaseHelper(this);
        fileList = dbHelper.getQueueItems();
        currentPosition = dbHelper.getQueuePosition();

        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.action_bar_layout);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        ImageView closeIcon = getSupportActionBar().getCustomView().findViewById(R.id.closeIcon);
        closeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ListView listView = findViewById(R.id.listView);
        adapter = new CustomAdapter(this, fileList);
        listView.setAdapter(adapter);

        if (currentPosition > 0) {
            listView.setSelection(currentPosition - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class CustomAdapter extends ArrayAdapter<String> {
        private ArrayList<String> items;

        public CustomAdapter(MediaListActivity context, ArrayList<String> items) {
            super(context, 0, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }

            String item = items.get(position);
            String[] parts = item.split("\\|", -1);
            String artist = parts[0];
            String title = parts[1];
            long duration = Long.parseLong(parts[2]);

            TextView titleText = convertView.findViewById(R.id.titleText);
            TextView artistText = convertView.findViewById(R.id.artistText);
            TextView durationText = convertView.findViewById(R.id.durationText);

            titleText.setText((position + 1) + ". " + title);
            artistText.setText(artist);

            if (duration > 0) {
                String durationStr = String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(duration),
                        TimeUnit.MILLISECONDS.toSeconds(duration) % 60);
                durationText.setText(durationStr);
            } else {
                durationText.setText("");
            }

            if (position == currentPosition) {
                convertView.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            } else {
                convertView.setBackgroundColor(0);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MediaListActivity.this, PlayerActivity.class);
                    intent.putExtra("position", position);
                    startActivity(intent);
                }
            });

            return convertView;
        }
    }
}
