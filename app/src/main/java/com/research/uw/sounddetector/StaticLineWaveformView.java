package com.research.uw.sounddetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Random;

public class StaticLineWaveformView extends View {

    private static final float MAX_HEIGHT_TO_DRAW = 8192.0f * 4;

    private static String filename;

    private int fileLength;
    private File file;

    private final Paint mPaint;

    private boolean toDraw;

    public StaticLineWaveformView(Context context) {
        this(context, null, 0);
    }

    public StaticLineWaveformView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StaticLineWaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(0);
        mPaint.setAntiAlias(true);
        toDraw = false;
    }

    public void updateAudioData(String filename) {
        this.filename = filename;
        file = new File(filename);
        fileLength = (int)file.length();
        Log.e("File length", fileLength + "");

        Log.e("LineWaveFormView", "Updated");
    }

    @Override
    public void onDraw(Canvas canvas) {
        drawWaveform(canvas);
    }

    private void drawWaveform(Canvas canvas) {
        Log.e("LineWaveFormView", "Start drawing");
        canvas.drawColor(Color.BLACK);

        float width = getWidth() * 2;
        float height = getHeight();
        float centerY = height / 2;
        int brightness = 255;

        mPaint.setColor(Color.WHITE);
        mPaint.setColor(Color.argb(brightness, 128, 255, 192));

        float lastX = -1;
        float lastY = -1;

        // For efficiency, we don't draw all of the samples in the buffer, but only the ones
        // that align with pixel boundaries.
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            for (int x = 0; x < width; x++) {
                int index = (int) ((x / width) * (fileLength - 22));
                Log.e("index", index + "");
                raf.seek((long)index);
                short sample = raf.readShort();
                float y = (sample / MAX_HEIGHT_TO_DRAW) * centerY + centerY;

                if (lastX != -1) {
                    canvas.drawLine(lastX, lastY, x, y, mPaint);
                }

                lastX = x;
                lastY = y;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}