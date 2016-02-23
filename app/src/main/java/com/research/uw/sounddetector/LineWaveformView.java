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

import java.util.LinkedList;

public class LineWaveformView extends SurfaceView implements SurfaceHolder.Callback {

    private static final int HISTORY_SIZE = 1;

    private static final float MAX_HEIGHT_TO_DRAW = 8192.0f;

    private final LinkedList<short[]> audioHistory;

    private final Paint mPaint;

    private boolean toDraw;

    public LineWaveformView(Context context) {
        this(context, null, 0);
    }

    public LineWaveformView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineWaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        audioHistory = new LinkedList<short[]>();

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(0);
        mPaint.setAntiAlias(true);
        toDraw = false;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    public void surfaceCreated(SurfaceHolder holder) {
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(Color.BLACK);
        Log.e("hello", "ayy");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public synchronized void updateAudioData(short[] buffer) {
        short[] newBuffer;
        if (audioHistory.size() == HISTORY_SIZE) {
            newBuffer = audioHistory.removeFirst();
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
        } else {
            newBuffer = buffer.clone();
        }

        Log.e("LineWaveFormView", "Updated");

        audioHistory.addLast(newBuffer);

        // Update the display.
        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            drawWaveform(canvas);
            getHolder().unlockCanvasAndPost(canvas);
        } else {
            toDraw = true;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        Log.e("LineWaveFormView", "On Draw");
        if(toDraw) {
            if (canvas != null) {
                drawWaveform(canvas);
                getHolder().unlockCanvasAndPost(canvas);
            }
            toDraw = false;
        }
    }

    private void drawWaveform(Canvas canvas) {
        Log.e("LineWaveFormView", "Start drawing");
        canvas.drawColor(Color.BLACK);

        float width = getWidth() * 2;
        float height = getHeight();
        float centerY = height / 2;
        int brightness = 255;

        mPaint.setColor(Color.WHITE);
        for (short[] buffer : audioHistory) {
            mPaint.setColor(Color.argb(brightness, 128, 255, 192));

            float lastX = -1;
            float lastY = -1;

            // For efficiency, we don't draw all of the samples in the buffer, but only the ones
            // that align with pixel boundaries.
            for (int x = 0; x < width; x++) {
                int index = (int) ((x / width) * buffer.length);
                short sample = buffer[index];
                float y = (sample / MAX_HEIGHT_TO_DRAW) * centerY + centerY;

                if (lastX != -1) {
                    canvas.drawLine(lastX, lastY, x, y, mPaint);
                }

                lastX = x;
                lastY = y;
            }
        }
    }
}