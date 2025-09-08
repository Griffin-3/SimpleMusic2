package com.griffin3.simplemusic;

import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class VisualActivity extends AppCompatActivity {
    private Visualizer visualizer;
    private VisualView visualView;
    private int audioSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioSessionId = getIntent().getIntExtra("audioSessionId", 0);
        Log.d("DEBUG: VisualActivity", "onCreate, audioSessionId: " + audioSessionId);
        if (audioSessionId == 0) {
            Log.d("DEBUG: VisualActivity", "audioSessionId is 0, finishing");
            finish();
            return;
        }

        visualView = new VisualView(this);
        setContentView(visualView);

        setupVisualizer();
    }

    private void setupVisualizer() {
        try {
            Log.d("DEBUG: VisualActivity", "Setting up Visualizer with sessionId: " + audioSessionId);
            visualizer = new Visualizer(audioSessionId);
            int[] range = Visualizer.getCaptureSizeRange();
            Log.d("DEBUG: VisualActivity", "Capture size range: " + range[0] + " to " + range[1]);
            visualizer.setCaptureSize(range[1]);
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    visualView.updateWaveform(waveform);
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    visualView.updateFft(fft);
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, true);
            visualizer.setEnabled(true);
            Log.d("DEBUG: VisualActivity", "Visualizer enabled successfully");
        } catch (Exception e) {
            Log.e("DEBUG: VisualActivity", "Error setting up Visualizer", e);
            e.printStackTrace();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visualizer != null) {
            visualizer.setEnabled(false);
            visualizer.release();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private class VisualView extends View {
        private byte[] fft = new byte[0];
        private Paint paint = new Paint();
        private int numColumns;
        private int rectHeight = 10; // dp to px
        private int numRects;

        public VisualView(VisualActivity context) {
            super(context);
            rectHeight = (int) (10 * getResources().getDisplayMetrics().density);
            updateNumColumns();
        }

        private void updateNumColumns() {
            int orientation = getResources().getConfiguration().orientation;
            numColumns = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? 24 : 12;
        }

        @Override
        protected void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            updateNumColumns();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (fft.length == 0) return;

            int width = getWidth();
            int height = getHeight();
            numRects = height / rectHeight;

            int columnWidth = width / numColumns;

            for (int i = 0; i < numColumns; i++) {
                int fftIndex = (i * fft.length / 2) / numColumns * 2; // approximate
                if (fftIndex >= fft.length) continue;
                int magnitude = Math.abs(fft[fftIndex]) + Math.abs(fft[fftIndex + 1]);
                int activeRects = (magnitude * numRects) / 256;

                int x = i * columnWidth;
                for (int j = 0; j < activeRects && j < numRects; j++) {
                    int y = height - (j + 1) * rectHeight;
                    int color = getColorForPosition(j, numRects);
                    paint.setColor(color);
                    canvas.drawRect(x, y, x + columnWidth, y + rectHeight, paint);
                }
            }
        }

        private int getColorForPosition(int position, int total) {
            float ratio = (float) position / total;
            if (ratio < 0.05f) return Color.RED;
            else if (ratio < 0.25f) return Color.rgb(255, 165, 0); // orange
            else if (ratio < 0.45f) return Color.YELLOW;
            else return Color.GREEN;
        }

        public void updateFft(byte[] fft) {
            this.fft = fft.clone();
            invalidate();
        }

        public void updateWaveform(byte[] waveform) {
            // not used
        }
    }
}
