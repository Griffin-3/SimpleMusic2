package com.griffin3.simplemusic;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1001;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 100, 16, 16);

        Button mediaButton = new Button(this);
        mediaButton.setText("Media");
        mediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
                } else {
                    queryMediaFiles();
                }
            }
        });
        layout.addView(mediaButton);

        Button clearButton = new Button(this);
        clearButton.setText("Clear Data");
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.clearAllData();
                finish();
            }
        });
        layout.addView(clearButton);

        TextView debugArea = new TextView(this);
        layout.addView(debugArea);

        boolean reloadedMedia = false;
        boolean reloadedQueue = false;

        if (dbHelper.getMediaCount() == 0) {
            long elapsed = dbHelper.loadMediaFromMediaStore(this);
            Toast.makeText(this, "Loaded media in " + elapsed + " ms", Toast.LENGTH_SHORT).show();
            reloadedMedia = true;
        }

        if (dbHelper.isQueueEmpty()) {
            dbHelper.fillQueueWithShuffledMedia();
            dbHelper.setQueuePosition(0);
            reloadedQueue = true;
        }

        int mediaCount = dbHelper.getMediaCount();
        String debugText = mediaCount > 0 ? "Files in st_media: " + mediaCount : "st_media database doesn't exist";
        if (reloadedMedia) debugText += " (reloaded)";
        ArrayList<String> queue = dbHelper.getQueueItems();
        if (!queue.isEmpty()) {
            int position = dbHelper.getQueuePosition();
            String songInfo = dbHelper.getCurrentSongInfo(position);
            debugText += "\nFiles in queue: " + queue.size() + "\nQueue position: " + position + "\nCurrent song: " + (songInfo != null ? songInfo : "N/A");
            if (reloadedQueue) debugText += " (reloaded)";
        } else {
            debugText += "\nQueue is empty";
        }
        debugArea.setText(debugText);

        setContentView(layout);
    }

    private void queryMediaFiles() {
        ArrayList<String> fileList = dbHelper.getQueueItems();
        Intent intent = new Intent(this, MediaListActivity.class);
        intent.putStringArrayListExtra("fileList", fileList);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                queryMediaFiles();
            } else {
                Toast.makeText(this, "No permission", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
