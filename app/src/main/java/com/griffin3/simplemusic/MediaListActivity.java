package com.griffin3.simplemusic;

import android.content.Intent;
import android.media.MediaPlayer;
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
    private MediaPlayer mediaPlayer;
    private ArrayList<String> fileList;
    private int currentPosition = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_list);

        Intent intent = getIntent();
        fileList = intent.getStringArrayListExtra("fileList");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playNextSong();
            }
        });

        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.action_bar_layout);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        ImageView closeIcon = getSupportActionBar().getCustomView().findViewById(R.id.closeIcon);
        closeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                }
                finish();
            }
        });

        ListView listView = findViewById(R.id.listView);
        CustomAdapter adapter = new CustomAdapter(this, fileList);
        listView.setAdapter(adapter);
    }

    private void playSong(int position) {
        try {
            String item = fileList.get(position);
            String[] parts = item.split("\\|", -1);
            String data = parts[3];
            mediaPlayer.reset();
            mediaPlayer.setDataSource(data);
            mediaPlayer.prepare();
            mediaPlayer.start();
            currentPosition = position;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playNextSong() {
        currentPosition++;
        if (currentPosition < fileList.size()) {
            playSong(currentPosition);
        } else {
            mediaPlayer.stop();
            mediaPlayer.reset();
            currentPosition = -1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
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
            String data = parts[3];

            if (artist.equals("<unknown>") && title.contains(" - ")) {
                String[] titleParts = title.split(" - ", 2);
                artist = titleParts[0];
                title = titleParts[1];
            }

            TextView titleText = convertView.findViewById(R.id.titleText);
            TextView artistText = convertView.findViewById(R.id.artistText);
            TextView durationText = convertView.findViewById(R.id.durationText);

            titleText.setText(title);
            artistText.setText(artist);

            if (duration > 0) {
                String durationStr = String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(duration),
                        TimeUnit.MILLISECONDS.toSeconds(duration) % 60);
                durationText.setText(durationStr);
            } else {
                durationText.setText("");
            }

            convertView.setTag(position);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = (Integer) v.getTag();
                    playSong(pos);
                }
            });

            return convertView;
        }
    }
}
