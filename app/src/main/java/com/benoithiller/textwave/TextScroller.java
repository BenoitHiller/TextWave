package com.benoithiller.textwave;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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

    private Vibrator vibrator;
    private boolean vibrate = true;

    private RectF bounds;

    private TextScrollRenderer renderer;

    private double armLength = ARM_LENGTH;

    private float offset = 0;
    private double lastVibrate = 0;

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
        double maxAngle = MAX_DEGREEWIDTH * (renderer.getWidth() + bounds.width()) / renderer.getMaxWidth();
        double maxTextAngle = (maxAngle - (MAX_DEGREEWIDTH * bounds.width() / renderer.getMaxWidth())) / maxAngle;
        double cappedAngle = Math.max(Math.min(maxAngle, angle), -maxAngle) / maxAngle;
        double percent = sinusoidal(cappedAngle) / 2 + 0.5;
        offset = (float) (percent * renderer.getWidth() - bounds.width() / 2);

        if (vibrator.hasVibrator() && vibrate) {
            double direction = Math.signum(cappedAngle);
            if (Math.abs(cappedAngle) < maxTextAngle) {
                direction = 0;
            }
            if (lastVibrate != direction && direction != 0) {
                vibrator.vibrate(20);
            }
            lastVibrate = direction;
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
        /**
         * The maximum width of the displayable text. Currently this is just set externally as the
         * length of the arc of 2 * DEGREEWIDTH for an arm of the specified length in the device dpi.
         *
         * So a better way of doing this would be to require the renderer to acquire its own info
         * to determine the maximum width of renderable text. That is assuming this measurement
         * shouldn't just be pulled up into the spec.
         */
        protected float maxWidth = Float.NaN;

        private boolean dataChanged = false;

        protected abstract void renderImpl(Canvas canvas, float offset);

        /**
         * Get the full width of the scrollable text
         *
         * @return the full width of the text in dpi
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
        private final Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path textPath = new Path();
        private final RectF pathBounds = new RectF();
        private final Rect textBounds = new Rect();
        private final Rect scaledBounds = new Rect();

        private Bitmap bitmap;
        private Canvas bitmapCanvas;
        private Bitmap emojiBitmap;
        private Canvas emojiCanvas;
        private float scaleChange;

        public PathTextRenderer() {
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setTextSize(100);
            textPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            textPaint.setColor(foregroundColor);

            emojiPaint.setStyle(Paint.Style.FILL);
            emojiPaint.setTextSize(100);
            emojiPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            emojiPaint.setColor(backgroundColor);
        }

        @Override
        public void renderImpl(Canvas canvas, float offset) {
            bitmap.eraseColor(Color.TRANSPARENT);
            bitmapCanvas.save();
            bitmapCanvas.translate(-pathBounds.left - offset, -pathBounds.top + (bounds.height() - pathBounds.height()) / 2);
            bitmapCanvas.drawPath(textPath, textPaint);
            bitmapCanvas.restore();

            emojiBitmap.eraseColor(Color.TRANSPARENT);
            emojiCanvas.save();
            emojiCanvas.translate(-textBounds.left - offset / scaleChange, -textBounds.top + (scaledBounds.height() - textBounds.height()) / 2);
            emojiCanvas.drawText(text, 0, 0, emojiPaint);
            emojiCanvas.restore();

            canvas.drawColor(backgroundColor);
            canvas.drawBitmap(emojiBitmap, null, bounds, emojiPaint);
            canvas.drawBitmap(bitmap, null, bounds, textPaint);
        }

        @Override
        public float getWidth() {
            return pathBounds.width();
        }

        @Override
        public void updateData() {
            textPaint.setColor(foregroundColor);
            emojiPaint.setColor(backgroundColor);
            textPaint.getTextPath(text, 0, text.length(), 0, 0, textPath);
            textPaint.getTextBounds(text, 0, text.length(), textBounds);

            pathBounds.set(textBounds);

            scaleChange = bounds.height() / pathBounds.height();

            float width = Math.min(pathBounds.width() * scaleChange, maxWidth);
            scaleChange = width / pathBounds.width();

            Matrix transformMatrix = new Matrix();
            transformMatrix.setScale(scaleChange, scaleChange);

            textPath.transform(transformMatrix);
            transformMatrix.mapRect(pathBounds);

            transformMatrix.setScale(1 / scaleChange, 1 / scaleChange);
            RectF tempRect = new RectF(bounds);
            transformMatrix.mapRect(tempRect);
            tempRect.round(scaledBounds);

            emojiBitmap = Bitmap.createBitmap(scaledBounds.width(), scaledBounds.height(), Bitmap.Config.ARGB_8888);
            emojiCanvas = new Canvas(emojiBitmap);
            emojiCanvas.save();
            emojiCanvas.translate(-textBounds.left, -textBounds.top);
            emojiCanvas.drawText(text, 0, 0, emojiPaint);
            emojiCanvas.restore();

            bitmap = Bitmap.createBitmap((int) bounds.width(), (int) bounds.height(), Bitmap.Config.ARGB_8888);
            bitmapCanvas = new Canvas(bitmap);
            bitmapCanvas.save();
            bitmapCanvas.translate(-pathBounds.left, -pathBounds.top);
            bitmapCanvas.drawPath(textPath, textPaint);
            bitmapCanvas.restore();

        }
    }
}
