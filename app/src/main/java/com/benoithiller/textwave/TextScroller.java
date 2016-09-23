package com.benoithiller.textwave;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

/**
 * View for the scrolling text
 */
public class TextScroller extends View {

    // length of arm in inches
    private static final float ARM_LENGTH = 16;
    private static final double MAX_DEGREEWIDTH = Math.PI / 5;
    private static final int BACKGROUND_COLOR = Color.WHITE;
    private static final int FOREGROUND_COLOR = Color.BLACK;

    private static final float VIBRATOR_TICK = 0.1f;

    private Vibrator vibrator;
    private boolean vibrate = true;

    private RectF bounds;

    private TextScrollRenderer renderer;

    private double armLength = ARM_LENGTH;

    private float offset = 0;
    private float lastVibrate = 0.5f;

    private void init(Context context) {
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ((Activity) getContext()).finish();
                } catch (Throwable throwable) {
                    Log.e("finalizer", "Failed to close activity", throwable);
                }
            }
        });
        renderer = new PathTextRenderer();
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public TextScroller(Context context) {
        super(context);
        init(context);
    }

    public TextScroller(Context context, AttributeSet attributes) {
        super(context, attributes);
        init(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        requestLayout();
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());

        float width = (float) w - xpad;
        float height = (float) h - ypad;

        bounds = new RectF(0f, 0f, width, height);
        bounds.offsetTo(getPaddingLeft(), getPaddingTop());

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        renderer.setBounds(bounds);
        renderer.setMaxWidth((float) (armLength * metrics.xdpi * MAX_DEGREEWIDTH * 2));

    }

    private double sinusoidal(double angle) {
        double adjustment = 0.4;
        return Math.sin(Math.PI / 2 * angle * adjustment) / Math.sin(Math.PI / 2 * adjustment);
    }

    public void move(double angle) {
        double degreeWidth = MAX_DEGREEWIDTH * (renderer.getWidth() + bounds.width()) / renderer.getMaxWidth();
        double cappedAngle = Math.max(Math.min(degreeWidth, angle), -degreeWidth) / degreeWidth;
        double percent = sinusoidal(cappedAngle) / 2 + 0.5;
        offset = (float) (percent * renderer.getWidth() - bounds.width() / 2);

        if (vibrator.hasVibrator() && vibrate) {
            float currentTick = Math.round(percent / VIBRATOR_TICK) * VIBRATOR_TICK;
            if (lastVibrate != currentTick) {
                lastVibrate = currentTick;
                if (lastVibrate == 0 || lastVibrate == 1) {
                    vibrator.vibrate(20);
                }
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        renderer.render(canvas, offset);
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        renderer.setBackground(backgroundColor);
    }

    public void setArmLength(float armLength) {
        this.armLength = armLength;
    }

    public void setText(String text) {
        renderer.setText(text);
    }

    public void setForegroundColor(int foregroundColor) {
        renderer.setForeground(foregroundColor);
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    private abstract class TextScrollRenderer {
        protected RectF bounds;
        protected String text;
        protected int foregroundColor = FOREGROUND_COLOR;
        protected int backgroundColor = BACKGROUND_COLOR;
        protected float maxWidth = Float.NaN;

        private boolean dataChanged = false;

        protected abstract void renderImpl(Canvas canvas, float offset);

        /**
         * Get the ratio of the width of the rendered text to its height
         *
         * @return the ratio as a float.
         */
        public abstract float getWidth();

        protected abstract void updateData();

        public void render(Canvas canvas, float offset) {
            if (dataChanged) {
                if (text == null) {
                    throw new IllegalStateException("Attempted to update data before text set.");
                }
                if (bounds == null) {
                    throw new IllegalStateException("Attempted to update data before bounds set.");
                }
                if (Float.isNaN(maxWidth)) {
                    throw new IllegalStateException("Attempted to update data before bounds max width set.");
                }
                dataChanged = false;
                updateData();
            }
            renderImpl(canvas, offset);
        }

        public void setBounds(RectF bounds) {
            this.bounds = bounds;
            dataChanged = true;
        }

        public void setMaxWidth(float maxWidth) {
            this.maxWidth = maxWidth;
            dataChanged = true;
        }

        public float getMaxWidth() {
            return maxWidth;
        }

        public void setText(String text) {
            this.text = text;
            dataChanged = true;
        }

        public void setForeground(int color) {
            this.foregroundColor = color;
            dataChanged = true;
        }

        public void setBackground(int color) {
            this.backgroundColor = color;
            dataChanged = true;
        }

    }

    private class PathTextRenderer extends TextScrollRenderer {

        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path textPath = new Path();
        private final RectF pathBounds = new RectF();

        private Bitmap bitmap;
        private Canvas bitmapCanvas;

        public PathTextRenderer() {
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setTextSize(100);
            textPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            textPaint.setColor(foregroundColor);
        }

        @Override
        public void renderImpl(Canvas canvas, float offset) {
            bitmap.eraseColor(backgroundColor);
            bitmapCanvas.save();
            bitmapCanvas.translate(-pathBounds.left - offset, -pathBounds.top + (bounds.height() - pathBounds.height()) / 2);
            bitmapCanvas.drawPath(textPath, textPaint);
            bitmapCanvas.restore();

            canvas.drawColor(backgroundColor);
            canvas.drawBitmap(bitmap, null, bounds, textPaint);
        }

        @Override
        public float getWidth() {
            return pathBounds.width();
        }

        @Override
        public void updateData() {
            textPaint.setColor(foregroundColor);
            textPaint.getTextPath(text, 0, text.length(), 0, 0, textPath);
            textPath.computeBounds(pathBounds, true);

            float scaleChange = bounds.height() / pathBounds.height();

            float width = Math.min(pathBounds.width() * scaleChange, maxWidth);
            scaleChange = width / pathBounds.width();

            Matrix transformMatrix = new Matrix();
            transformMatrix.setScale(scaleChange, scaleChange);
            textPath.transform(transformMatrix);
            textPath.computeBounds(pathBounds, true);

            bitmap = Bitmap.createBitmap((int) bounds.width(), (int) bounds.height(), Bitmap.Config.ARGB_8888);
            bitmapCanvas = new Canvas(bitmap);
            bitmapCanvas.drawColor(backgroundColor);
            bitmapCanvas.save();
            bitmapCanvas.translate(-pathBounds.left, -pathBounds.top);
            bitmapCanvas.drawPath(textPath, textPaint);
            bitmapCanvas.restore();

        }
    }
}
