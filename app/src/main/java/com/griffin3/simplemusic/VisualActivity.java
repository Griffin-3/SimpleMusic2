package com.griffin3.simplemusic;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class VisualActivity extends AppCompatActivity {
    private static final String TAG = "VisualActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private Visualizer mVisualizer;
    private VisualizerView mVisualizerView;
    private int audioSessionId;    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        audioSessionId = getIntent().getIntExtra("audioSessionId", 0);
        Log.d(TAG, "Audio session ID: " + audioSessionId);

        if (audioSessionId == 0) {
            Log.d(TAG, "Audio session ID is 0, finishing");
            finish();
            return;
        }

        mVisualizerView = new VisualizerView(this);
        setContentView(mVisualizerView);

        // Check and request RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "RECORD_AUDIO permission not granted, requesting...");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            Log.d(TAG, "RECORD_AUDIO permission already granted");
            setupVisualizer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "RECORD_AUDIO permission granted");
                setupVisualizer();
            } else {
                Log.d(TAG, "RECORD_AUDIO permission denied");
                Toast.makeText(this, "Audio recording permission is required for visualization", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    public void setupVisualizer() {
        try {
            mVisualizer = new Visualizer(audioSessionId);
            mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                    // Not used - using FFT instead
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                    mVisualizerView.updateVisualizerFFT(bytes);
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true);
            mVisualizer.setEnabled(true);
            Log.d(TAG, "Visualizer enabled with FFT");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Visualizer", e);
            Toast.makeText(this, "Failed to setup audio visualization", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing() && mVisualizer != null) {
            mVisualizer.release();
            mVisualizer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mVisualizer.release();
            mVisualizer = null;
        }
    }

    /**
     * A simple class that draws FFT frequency data as an LED equalizer
     */
    public static class VisualizerView extends View {
        private static final String VIZ_TYPE = "classic"; // "classic" or "fire"
        private byte[] mFFTBytes;
        private float[] mPoints;
        private Rect mRect = new Rect();
        private Paint mForePaint = new Paint();
        private Paint mBarPaint = new Paint();
        private int mNumBars;
        private int mMaxMagnitude = 1; // Start with 1 to avoid division by zero
        private float vis_vert_scale = 1.5f; // Vertical scaling factor
        private Visualizer mVisualizer;
        private int audioSessionId;

        public VisualizerView(Context context) {
            super(context);
            init();
        }

        private void init() {
            mFFTBytes = null;
            mForePaint.setStrokeWidth(1f);
            mForePaint.setAntiAlias(true);
            mForePaint.setColor(Color.rgb(0, 128, 255));

            mBarPaint.setStyle(Paint.Style.FILL);
            mBarPaint.setAntiAlias(true);

            updateNumBars();
        }

        private void updateNumBars() {
            int orientation = getResources().getConfiguration().orientation;
            mNumBars = (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) ? 32 : 16;
        }

        @Override
        protected void onConfigurationChanged(android.content.res.Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            updateNumBars();
            invalidate();
        }

        public void updateVisualizerFFT(byte[] bytes) {
            mFFTBytes = bytes;
            invalidate();
        }

        public void updateAudioSession(int sessionId) {
            if (mVisualizer != null) {
                mVisualizer.release();
            }
            audioSessionId = sessionId;
            setupVisualizer();
        }

        public void setupVisualizer() {
            try {
                mVisualizer = new Visualizer(audioSessionId);
                mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
                mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                    @Override
                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                        // Not used - using FFT instead
                    }

                    @Override
                    public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                        updateVisualizerFFT(bytes);
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true);
                mVisualizer.setEnabled(true);
            } catch (Exception e) {
                Log.e("VisualizerView", "Error setting up Visualizer", e);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (mFFTBytes == null) {
                return;
            }

            mRect.set(0, 0, getWidth(), getHeight());
            
            // Calculate spacing as 20% of bar width, then adjust bar width accordingly
            int totalSpacing = (int) (mRect.width() * 0.20f * mNumBars / (mNumBars + 0.20f * mNumBars)); // Approximate spacing
            int availableWidth = mRect.width() - totalSpacing;
            int barWidth = availableWidth / mNumBars;
            int spacing = (int) (barWidth * 0.20f);
            
            // Calculate total width of all bars and spacing, then center them
            int totalBarWidth = (barWidth * mNumBars) + (spacing * (mNumBars - 1));
            int startX = (mRect.width() - totalBarWidth) / 2;
            
            int maxHeight = mRect.height();

            // Calculate how many FFT bins represent frequencies below 4096Hz
            // Assuming sample rate of 44100Hz, Nyquist is 22050Hz
            // FFT size is typically 1024 or 2048 bins
            int fftSize = mFFTBytes.length / 2; // Divide by 2 because FFT data comes as real/imag pairs
            int maxBins = (int) ((4096.0 / 22050.0) * fftSize); // 4096Hz out of 22050Hz
            int binsPerBar = Math.max(1, maxBins / mNumBars);

            // Track the maximum magnitude for dynamic scaling
            int currentMaxMagnitude = 0;

            for (int i = 0; i < mNumBars; i++) {
                int fftStartIndex = i * binsPerBar * 2; // *2 because each bin has real and imaginary parts

                if (fftStartIndex >= mFFTBytes.length) continue;

                // Average the magnitude across the bins for this bar
                int totalMagnitude = 0;
                int binCount = 0;

                for (int j = 0; j < binsPerBar && (fftStartIndex + j * 2) < mFFTBytes.length - 1; j++) {
                    int real = mFFTBytes[fftStartIndex + j * 2];
                    int imag = mFFTBytes[fftStartIndex + j * 2 + 1];
                    int magnitude = (int) Math.sqrt(real * real + imag * imag);
                    totalMagnitude += magnitude;
                    binCount++;
                    if (magnitude > currentMaxMagnitude) {
                        currentMaxMagnitude = magnitude;
                    }
                }

                int avgMagnitude = binCount > 0 ? totalMagnitude / binCount : 0;

                // Update the global maximum magnitude for scaling
                if (currentMaxMagnitude > mMaxMagnitude) {
                    mMaxMagnitude = currentMaxMagnitude;
                }

                // Use dynamic scaling based on the maximum magnitude seen
                int scaleFactor = Math.max(1, mMaxMagnitude);
                int barHeight = (int)((avgMagnitude * maxHeight * vis_vert_scale) / scaleFactor);

                // Draw the bar as vertical stack of green rectangles with 15% spacing
                int left = startX + i * (barWidth + spacing);
                int right = left + barWidth;
                
                // Calculate rectangle height and spacing
                int rectHeight = Math.max(4, barWidth / 2); // Rectangle height is 1/2 of bar width
                int verticalSpacing = (int) (rectHeight * 0.30f);
                int rectWithSpacing = rectHeight + verticalSpacing;
                
                // Calculate maximum possible rectangles for full height
                int maxPossibleRects = maxHeight / rectWithSpacing;
                
                // Calculate how many rectangles to draw
                int numRects = barHeight / rectWithSpacing;
                if (numRects == 0 && barHeight > 0) numRects = 1; // At least one rectangle if there's any height
                
                // Draw rectangles from bottom up
                for (int r = 0; r < numRects; r++) {
                    int rectTop = maxHeight - (r + 1) * rectWithSpacing + verticalSpacing;
                    int rectBottom = rectTop + rectHeight;
                    
                    // Ensure we don't draw outside the bar bounds
                    if (rectTop < maxHeight - barHeight) break;
                    
                    // Set color based on visualization type
                    if (VIZ_TYPE.equals("fire")) {
                        // Fire mode: colors based on position within each bar
                        float positionRatio = (float) r / Math.max(1, numRects - 1);
                        
                        if (positionRatio >= 0.9f) { // Top 10%
                            mBarPaint.setColor(Color.RED);
                        } else if (positionRatio >= 0.7f) { // Next 20% (70-90%)
                            mBarPaint.setColor(Color.rgb(255, 165, 0)); // Orange
                        } else if (positionRatio >= 0.5f) { // Next 20% (50-70%)
                            mBarPaint.setColor(Color.YELLOW);
                        } else { // Bottom 50%
                            mBarPaint.setColor(Color.GREEN);
                        }
                    } else { // Classic mode: colors based on absolute window position
                        // Calculate position from top of window (1.0) to bottom (0.0)
                        float windowPositionRatio = 1.0f - ((float) rectTop / maxHeight);
                        
                        if (windowPositionRatio >= 0.9f) { // Top 10% of window
                            mBarPaint.setColor(Color.RED);
                        } else if (windowPositionRatio >= 0.7f) { // Next 20% of window (70-90%)
                            mBarPaint.setColor(Color.rgb(255, 165, 0)); // Orange
                        } else if (windowPositionRatio >= 0.5f) { // Next 20% of window (50-70%)
                            mBarPaint.setColor(Color.YELLOW);
                        } else { // Bottom 50% of window
                            mBarPaint.setColor(Color.GREEN);
                        }
                    }
                    
                    canvas.drawRect(left, rectTop, right, rectBottom, mBarPaint);
                }
            }

            // Gradually decrease the maximum magnitude over time to allow for new peaks
            mMaxMagnitude = Math.max(1, (int)(mMaxMagnitude * 0.95f));
            
            // Draw red 2px outline around the entire view
            Paint outlinePaint = new Paint();
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(2f);
            outlinePaint.setColor(Color.RED);
            outlinePaint.setAntiAlias(true);
            canvas.drawRect(1, 1, getWidth() - 1, getHeight() - 1, outlinePaint);
        }

        private int getBarColor(int barIndex, int totalBars) {
            float ratio = (float) barIndex / totalBars;

            if (ratio < 0.2f) {
                // Low frequencies - Red to Orange
                return Color.rgb(255, (int)(ratio * 5 * 255), 0);
            } else if (ratio < 0.4f) {
                // Mid-low frequencies - Orange to Yellow
                return Color.rgb(255, 165 + (int)((ratio - 0.2f) * 5 * 90), (int)((ratio - 0.2f) * 5 * 255));
            } else if (ratio < 0.6f) {
                // Mid frequencies - Yellow to Green
                return Color.rgb(255 - (int)((ratio - 0.4f) * 5 * 255), 255, (int)((ratio - 0.4f) * 5 * 255));
            } else if (ratio < 0.8f) {
                // Mid-high frequencies - Green to Cyan
                return Color.rgb(0, 255, (int)((ratio - 0.6f) * 5 * 255));
            } else {
                // High frequencies - Cyan to Blue
                return Color.rgb(0, 255 - (int)((ratio - 0.8f) * 5 * 255), 255);
            }
        }
    }
}
