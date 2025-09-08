package com.griffin3.simplemusic;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.view.ViewGroup;
import android.widget.Button;
import com.griffin3.simplemusic.VisualActivity.VisualizerView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private TextView titleText;
    private SeekBar seekBar;
    private ImageButton back10Button;
    private ImageButton prevButton;
    private ImageButton playPauseButton;
    private ImageButton nextButton;
    private ImageButton forward10Button;
    private FrameLayout visualizationContainer;
    private Button visualizationButton;
    private VisualizerView embeddedVisualizerView;
    private TextView positionText;
    private TextView artistText;
    private DatabaseHelper dbHelper;
    private ArrayList<String> queue;
    private int currentPosition;
    private int audioSessionId = 0;
    private Handler handler = new Handler();
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DatabaseHelper(this);
        queue = dbHelper.getQueueItems();
        currentPosition = getIntent().getIntExtra("position", 0);

        initializeViews();
        initializePlayer();
        setupControls();
        updateUI();
        startUpdatingProgress();
    }

    private void initializeViews() {
        titleText = findViewById(R.id.title_text);
        artistText = findViewById(R.id.artist_text);
        positionText = findViewById(R.id.position_text);
        seekBar = findViewById(R.id.seek_bar);
        back10Button = findViewById(R.id.back_10_button);
        prevButton = findViewById(R.id.prev_button);
        playPauseButton = findViewById(R.id.play_pause_button);
        nextButton = findViewById(R.id.next_button);
        forward10Button = findViewById(R.id.forward_10_button);
        visualizationContainer = findViewById(R.id.visualization_container);

        // Create visualization button programmatically
        visualizationButton = new Button(this);
        visualizationButton.setText("VISUALIZATION");
        visualizationButton.setGravity(android.view.Gravity.CENTER);
        visualizationButton.setOnClickListener(v -> {
            Log.d("DEBUG: PlayerActivity", "Visualization button clicked, audioSessionId: " + audioSessionId + ", isPlaying: " + player.isPlaying());
            if (audioSessionId != 0 && player.isPlaying()) {
                Intent intent = new Intent(this, VisualActivity.class);
                intent.putExtra("audioSessionId", audioSessionId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Audio not playing", Toast.LENGTH_SHORT).show();
            }
        });

        // Create embedded visualizer
        embeddedVisualizerView = new VisualizerView(this);

        // Start with button visible
        showVisualizationButton();
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    playNext();
                }
                updatePlayPauseButton();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseButton();
                if (isPlaying) {
                    audioSessionId = player.getAudioSessionId();
                    // Show embedded visualizer when music starts playing
                    showEmbeddedVisualizer();
                } else {
                    // Show button and stop music when paused
                    showVisualizationButton();
                    player.stop();
                }
            }
        });
        playSong(currentPosition, true);
    }

    private void setupControls() {
        // Disable and gray out fast reverse button
        back10Button.setEnabled(false);
        back10Button.setAlpha(0.5f);
        // back10Button.setOnClickListener(v -> {
        //     long newPosition = player.getCurrentPosition() - 10000;
        //     player.seekTo(Math.max(0, newPosition));
        // });

        prevButton.setOnClickListener(v -> {
            if (currentPosition > 0) {
                currentPosition--;
                playSong(currentPosition, false);
            }
        });

        playPauseButton.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play();
            }
        });

        nextButton.setOnClickListener(v -> playNext(false));

        // Disable and gray out fast forward button
        forward10Button.setEnabled(false);
        forward10Button.setAlpha(0.5f);
        // forward10Button.setOnClickListener(v -> {
        //     long newPosition = player.getCurrentPosition() + 10000;
        //     player.seekTo(Math.min(player.getDuration(), newPosition));
        // });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    long duration = player.getDuration();
                    long newPosition = (progress * duration) / 100;
                    player.seekTo(newPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void playSong(int position) {
        playSong(position, true);
    }

    private void playSong(int position, boolean autoPlay) {
        if (position < 0 || position >= queue.size()) return;
        SongInfo songInfo = SongInfo.fromQueueString(queue.get(position));
        if (songInfo != null) {
            Uri uri = Uri.parse(songInfo.getData());
            MediaItem mediaItem = MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            player.prepare();
            if (autoPlay) {
                player.play();
            }
            currentPosition = position;
            dbHelper.setQueuePosition(currentPosition);
            updateUI();
        }
    }

    private void playNext() {
        playNext(true);
    }

    private void playNext(boolean autoPlay) {
        currentPosition++;
        if (currentPosition < queue.size()) {
            playSong(currentPosition, autoPlay);
        } else {
            player.stop();
            currentPosition = -1;
            dbHelper.setQueuePosition(currentPosition);
        }
    }

    private void updateUI() {
        if (currentPosition >= 0 && currentPosition < queue.size()) {
            SongInfo songInfo = SongInfo.fromQueueString(queue.get(currentPosition));
            if (songInfo != null) {
                titleText.setText(songInfo.getTitle());
                artistText.setText(songInfo.getArtist());
                titleText.setTextSize(24);
                artistText.setTextSize(14.4f);
            }
        }
    }

    private void updatePlayPauseButton() {
        if (player.isPlaying()) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void startUpdatingProgress() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateProgress();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateRunnable);
    }

    private void updateProgress() {
        long current = player.getCurrentPosition();
        long duration = player.getDuration();
        if (duration > 0) {
            int progress = (int) ((current * 100) / duration);
            seekBar.setProgress(progress);
            positionText.setText(formatTime(current) + " / " + formatTime(duration));
        }
    }

    private String formatTime(long millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
    }

    private void showVisualizationButton() {
        visualizationContainer.removeAllViews();
        visualizationContainer.addView(visualizationButton);
    }

    private void showEmbeddedVisualizer() {
        visualizationContainer.removeAllViews();
        embeddedVisualizerView.updateAudioSession(audioSessionId);
        visualizationContainer.addView(embeddedVisualizerView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Views are automatically updated due to resource-qualified layouts
    }

    @Override
    public void onBackPressed() {
        // Navigate to queue page (MediaListActivity) instead of finishing
        Intent intent = new Intent(this, MediaListActivity.class);
        startActivity(intent);
        finish();
    }
}
