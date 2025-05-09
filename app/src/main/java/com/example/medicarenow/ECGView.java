package com.example.medicarenow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ECGView extends View {
    private Paint paint;
    private Path path;
    private List<Float> ecgData;
    private Random random;
    private int index = 0;
    private float lastX = 0;
    private float lastY = 0;

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
        ecgData = new ArrayList<>();
        random = new Random();

        // Generate some initial ECG-like data
        for (int i = 0; i < 1000; i++) {
            ecgData.add(generateECGPoint(i));
        }
    }

    private float generateECGPoint(int index) {
        // Simulate ECG waveform
        if (index % 100 == 0) {
            return 0.8f; // QRS complex peak
        } else if (index % 100 == 2) {
            return -0.2f; // S wave
        } else if (index % 100 == 50) {
            return 0.3f; // T wave
        }
        return 0.1f * random.nextFloat(); // Baseline with small variations
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2;
        float scale = height * 0.4f;

        path.reset();
        path.moveTo(0, centerY);

        // Draw the ECG waveform
        for (int i = 0; i < width; i++) {
            float x = i;
            float y = centerY - (ecgData.get((index + i) % ecgData.size()) * scale);

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, paint);

        // Update the index for scrolling effect
        index = (index + 1) % ecgData.size();

        // Add new data point to simulate real-time updates
        ecgData.add(generateECGPoint(ecgData.size()));
        ecgData.remove(0);

        // Redraw every 50ms for animation
        postInvalidateDelayed(50);
    }
}