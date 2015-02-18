package com.research.uw.sounddetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceView;

import java.util.LinkedList;

public class WaveformView extends SurfaceView {

    private static final int HISTORY_SIZE = 10;

    private static final float MAX_HEIGHT_TO_DRAW = 4096.0f;

    private final LinkedList<short[]> audioHistory;

    private final Paint mPaint;

    public WaveformView(Context context) {
        this(context, null, 0);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        audioHistory = new LinkedList<short[]>();

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(0);
        mPaint.setAntiAlias(true);
    }

    public synchronized void updateAudioData(short[] buffer) {
        short[] newBuffer;
        if (audioHistory.size() == HISTORY_SIZE) {
            newBuffer = audioHistory.removeFirst();
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
        } else {
            newBuffer = buffer.clone();
        }

        audioHistory.addLast(newBuffer);

        // Update the display.
        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            drawWaveform(canvas);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void drawWaveform(Canvas canvas) {
        canvas.drawColor(Color.BLACK);

        float width = getWidth();
        float height = getHeight();
        int brightness = 255;
        mPaint.setColor(Color.GREEN);

        int count = 0;
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.GREEN);
        float lastX = 0;
        for (short[] buffer : audioHistory) {
            int index = (int) ((width - 80) * count / HISTORY_SIZE);

            int sum = 0;
            for (int j = 0; j < buffer.length; j++) {
                sum += Math.abs(buffer[j]);
            }
            sum = sum / buffer.length;

            float y = (sum / MAX_HEIGHT_TO_DRAW) * height;
            canvas.drawRect(lastX, height, index, height - (y + 10), mPaint);
            lastX = index + 10;
            count++;
        }
    }
}