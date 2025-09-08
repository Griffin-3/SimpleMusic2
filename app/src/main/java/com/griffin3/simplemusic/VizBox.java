package com.griffin3.simplemusic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * A reusable visualization component that displays FFT frequency data
 * as stacked rectangles in an LED-style equalizer
 */
public class VizBox extends View {
    private byte[] mFFTBytes;
    private Paint mBarPaint = new Paint();
    private int mNumBars;
    private int mMaxMagnitude = 1;
    private int mNumLevels = 8; // Number of stacked rectangles per bar

    public VizBox(Context context) {
        super(context);
        init();
    }

    private void init() {
        mFFTBytes = null;
        mBarPaint.setStyle(Paint.Style.FILL);
        mBarPaint.setAntiAlias(true);

        updateConfiguration();
    }

    private void updateConfiguration() {
        int orientation = getResources().getConfiguration().orientation;
        mNumBars = (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) ? 32 : 16;
    }

    @Override
    protected void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateConfiguration();
        invalidate();
    }

    public void updateFFTData(byte[] bytes) {
        mFFTBytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mFFTBytes == null) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // Calculate spacing: 10% of bar width for horizontal spacing
        int totalSpacing = (int) (width * 0.1f * (mNumBars - 1)); // Total spacing between bars
        int availableWidth = width - totalSpacing;
        int barWidth = availableWidth / mNumBars;

        // Calculate rectangle dimensions: half as high as wide
        int rectHeight = barWidth / 2;
        int totalVerticalSpacing = (int) (rectHeight * 0.1f * (mNumLevels - 1)); // 10% vertical spacing
        int availableHeight = height - totalVerticalSpacing;
        int actualRectHeight = availableHeight / mNumLevels;

        // Calculate how many FFT bins represent frequencies below 4096Hz
        int fftSize = mFFTBytes.length / 2;
        int maxBins = (int) ((4096.0 / 22050.0) * fftSize);
        int binsPerBar = Math.max(1, maxBins / mNumBars);

        // Track the maximum magnitude for dynamic scaling
        int currentMaxMagnitude = 0;

        for (int barIndex = 0; barIndex < mNumBars; barIndex++) {
            int fftStartIndex = barIndex * binsPerBar * 2;

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

            // Calculate how many levels to light up
            int scaleFactor = Math.max(1, mMaxMagnitude);
            float normalizedMagnitude = (float) avgMagnitude / scaleFactor;
            int levelsToLight = Math.round(normalizedMagnitude * mNumLevels);

            // Calculate bar position
            int barLeft = barIndex * (barWidth + (int)(width * 0.1f / (mNumBars - 1)));

            // Draw each level in the stack
            for (int level = 0; level < mNumLevels; level++) {
                int rectTop = level * (actualRectHeight + (int)(rectHeight * 0.1f));
                int rectBottom = rectTop + actualRectHeight;

                // Choose color: green for all, but dimmer for unlit levels
                int alpha = (level < levelsToLight) ? 255 : 60; // Full brightness for lit, dim for unlit
                mBarPaint.setColor(Color.argb(alpha, 0, 255, 0)); // Green

                canvas.drawRect(barLeft, rectTop, barLeft + barWidth, rectBottom, mBarPaint);
            }
        }

        // Gradually decrease the maximum magnitude over time
        mMaxMagnitude = Math.max(1, (int)(mMaxMagnitude * 0.95f));
    }
}
