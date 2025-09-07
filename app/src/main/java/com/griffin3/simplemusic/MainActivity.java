package com.griffin3.simplemusic;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        FrameLayout layout = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.topMargin = 100;
        mediaButton.setLayoutParams(params);
        layout.addView(mediaButton);
        setContentView(layout);
    }

    private void queryMediaFiles() {
        long startTime = System.currentTimeMillis();
        String[] projection = { MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA };
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Audio.Media.DATE_ADDED + " DESC"
            );
            if (cursor == null) {
                Toast.makeText(this, "No files found", Toast.LENGTH_SHORT).show();
                return;
            }
            int count = cursor.getCount();
            if (count == 0) {
                Toast.makeText(this, "No files found", Toast.LENGTH_SHORT).show();
                cursor.close();
                return;
            }
            ArrayList<String> fileList = new ArrayList<>();
            int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int shown = 0;
            while (cursor.moveToNext() && shown < 20) {
                String title = cursor.getString(titleIndex);
                String artist = cursor.getString(artistIndex);
                long duration = cursor.getLong(durationIndex);
                String data = cursor.getString(dataIndex);
                String item = artist + "|" + title + "|" + duration + "|" + data;
                fileList.add(item);
                shown++;
            }
            cursor.close();
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            Intent intent = new Intent(this, MediaListActivity.class);
            intent.putStringArrayListExtra("fileList", fileList);
            intent.putExtra("elapsedTime", elapsedTime);
            startActivity(intent);
        } catch (SecurityException e) {
            Toast.makeText(this, "No permission", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
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
