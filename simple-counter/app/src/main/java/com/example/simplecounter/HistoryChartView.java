package com.example.simplecounter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class HistoryChartView extends View {
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Integer> values = new ArrayList<Integer>();

    public HistoryChartView(Context context) {
        super(context);
        init();
    }

    public HistoryChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        axisPaint.setColor(0x66222222);
        axisPaint.setStrokeWidth(2f);
        linePaint.setColor(0xFF1976D2);
        linePaint.setStrokeWidth(5f);
        pointPaint.setColor(0xFF0D47A1);
    }

    public void setValues(List<Integer> newValues) {
        values.clear();
        values.addAll(newValues);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int left = 12;
        int right = width - 12;
        int top = 12;
        int bottom = height - 18;
        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);

        if (values.size() == 0) {
            return;
        }

        int min = values.get(0);
        int max = values.get(0);
        for (int value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (min == max) {
            min -= 1;
            max += 1;
        }

        float previousX = left;
        float previousY = yFor(values.get(0), min, max, top, bottom);
        canvas.drawCircle(previousX, previousY, 6f, pointPaint);
        for (int i = 1; i < values.size(); i++) {
            float x = left + ((right - left) * (i / (float) (values.size() - 1)));
            float y = yFor(values.get(i), min, max, top, bottom);
            canvas.drawLine(previousX, previousY, x, y, linePaint);
            canvas.drawCircle(x, y, 6f, pointPaint);
            previousX = x;
            previousY = y;
        }
    }

    private float yFor(int value, int min, int max, int top, int bottom) {
        float ratio = (value - min) / (float) (max - min);
        return bottom - ((bottom - top) * ratio);
    }
}
