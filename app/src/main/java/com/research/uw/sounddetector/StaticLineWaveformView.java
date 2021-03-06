package com.research.uw.sounddetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
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
    private double begin, end, progress;
    private long delay;
    private File file;
    private double add;

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
        progress = -1;
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

    public void updateBeginAndEnd(double begin, double end) {
        this.begin = begin;
        this.end = end;
        invalidate();
    }

    public void startPlaying() {
        int sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
        System.out.println("file length" + fileLength);
        System.out.println("sample Rate" + sampleRate);
        delay = (long)fileLength * 5 / sampleRate;
        add = 1;
        if (delay < 100) {
            add = 100.0 / delay;
            delay = 100;
        }
        progress = begin;
        System.out.println("add" + add);
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            public void run() {
                progress += add;
                invalidate();
                if (progress < end) {
                    System.out.println("delay " + delay);
                    postDelayed(this, delay);
                } else {
                    progress = -1;
                }
            }
        }, delay);
    }

    public void stopPlaying() {
        progress = -1;
    }

    @Override
    public void onDraw(Canvas canvas) {
        drawWaveform(canvas);
    }

    private void drawWaveform(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        float width = getWidth() * 2;
        float height = getHeight();
        float centerY = height / 2;
        int brightness = 255;

        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect((float) (width / 200 * begin), 0, (float) (width / 200 * end), height, mPaint);
        mPaint.setColor(Color.BLUE);
        if (progress != -1) {
            canvas.drawRect((float)(width / 200 * progress), 0, (float)(width / 200 * (progress + 1)), height, mPaint);
        }
        mPaint.setColor(Color.argb(brightness, 128, 255, 192));
        mPaint.setStyle(Paint.Style.STROKE);

        float lastX = -1;
        float lastY = -1;

        float maxHeight = 0;
        if (file != null) {
            try {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                for (int x = 0; x < width / 2; x++) {
                    int index = (int) ((x / (width / 2)) * (fileLength / 2));
                    raf.seek((long)index * 2);
                    short sample = raf.readShort();
                    sample = Short.reverseBytes(sample);
                    if (sample > maxHeight) {
                        maxHeight = (float)sample;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // For efficiency, we don't draw all of the samples in the buffer, but only the ones
            // that align with pixel boundaries.
            try {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                for (int x = 0; x < width / 2; x++) {
                    int index = (int) ((x / (width / 2)) * (fileLength / 2));
                    raf.seek((long)index * 2);
                    short sample = raf.readShort();
                    sample = Short.reverseBytes(sample);
                    float y = (sample / maxHeight) * centerY + centerY;

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
}