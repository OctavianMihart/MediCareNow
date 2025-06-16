package com.example.medicarenow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedList;
import java.util.Queue;

public class ECGView extends View {
    private static final int MAX_POINTS = 1000;
    private static final int SAMPLING_RATE_HZ = 200;

    private Paint paint;
    private Path path;
    private Queue<Float> ecgData;
    private int samplesPerBeat = (60 * SAMPLING_RATE_HZ) / 72; // Default 72 BPM
    private int sampleCount = 0;

    public ECGView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(4f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);

        path = new Path();
        ecgData = new LinkedList<>();

        // Initialize with flat line
        for (int i = 0; i < MAX_POINTS; i++) {
            ecgData.add(0f);
        }
    }

    public void setHeartRate(int bpm) {
        samplesPerBeat = (60 * SAMPLING_RATE_HZ) / bpm;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2;
        float scale = height * 0.4f;
        float pixelsPerPoint = width / MAX_POINTS;

        // Generate new ECG point
        float newPoint = generateECGPoint(sampleCount % samplesPerBeat);
        sampleCount++;

        // Update data queue
        ecgData.poll();
        ecgData.add(newPoint);

        // Draw the path
        path.reset();
        int i = 0;
        for (Float point : ecgData) {
            float x = i * pixelsPerPoint;
            float y = centerY - (point * scale);

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
            i++;
        }

        canvas.drawPath(path, paint);
        postInvalidateDelayed(1000 / SAMPLING_RATE_HZ);
    }

    private float generateECGPoint(int sampleInBeat) {
        float t = (float)sampleInBeat / samplesPerBeat;

        // ECG waveform components
        float pWave = 0, qrsComplex = 0, tWave = 0;

        // P Wave
        if (t >= 0.1 && t <= 0.2) {
            pWave = (float)(0.25 * Math.sin(Math.PI * (t - 0.1) / 0.1));
        }

        // QRS Complex
        if (t >= 0.25 && t <= 0.35) {
            if (t <= 0.27) {
                qrsComplex = -0.5f * (t - 0.25f)/0.02f;
            }
            else if (t <= 0.30) {
                qrsComplex = 1.0f - 2.5f * (t - 0.27f);
            }
            else {
                qrsComplex = -0.3f + 3.0f * (t - 0.30f)/0.05f;
            }
        }

        // T Wave
        if (t >= 0.4 && t <= 0.6) {
            tWave = (float)(0.3 * Math.sin(Math.PI * (t - 0.4) / 0.2));
        }

        return pWave + qrsComplex + tWave + 0.05f * (float)Math.sin(2 * Math.PI * t * 5);
    }
}