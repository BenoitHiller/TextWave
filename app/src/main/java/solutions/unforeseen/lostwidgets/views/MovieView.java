package solutions.unforeseen.lostwidgets.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.benoithiller.textwave.R;

/**
 * View to display a looped movie. It may only support GIF.
 */
public class MovieView extends View {
    private Movie movie;
    private long movieStart;
    private Rect movieBounds;
    private Bitmap movieBitmap;
    private Canvas movieCanvas;

    private float aspectRatio = -1;

    private Rect bounds;

    public MovieView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedAttributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MovieView,
                0, 0);

        try {
            int videoId = typedAttributes.getResourceId(R.styleable.MovieView_source, -1);
            if (videoId < 0) {
                throw new NullPointerException("Source attribute missing.");
            }
            aspectRatio = typedAttributes.getFloat(R.styleable.MovieView_aspectRatio, -1);
            new AsyncTask<Integer,Void,Movie>() {
                @Override
                protected Movie doInBackground(Integer... params) {
                    int videoId = params[0];
                    return Movie.decodeStream(context.getResources().openRawResource(videoId));
                }

                @Override
                protected void onPostExecute(Movie movie) {
                    setMovie(movie);
                }
            }.execute(videoId);
        } finally {
            typedAttributes.recycle();
        }
    }

    private void setMovie(Movie movie) {
        Log.d("movie", "Movie loaded");
        movieStart = android.os.SystemClock.uptimeMillis();
        movieBounds = new Rect(0, 0, movie.width(), movie.height());
        movieBitmap =
                Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888);
        movieCanvas = new Canvas(movieBitmap);
        this.movie = movie;

        //TODO: remove unnecessary calls
        requestLayout();
        invalidate();
        if (getParent() instanceof View) {
            View parent = (View) getParent();
            parent.requestLayout();
            parent.invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (movie != null) {
            int desiredWidth = movie.width();
            int desiredHeight = movie.height();
            float aspectRatio = (float) desiredWidth / desiredHeight;

            if (widthMode == MeasureSpec.EXACTLY
                    || (widthMode == MeasureSpec.AT_MOST && widthSize < desiredWidth)) {
                desiredWidth = widthSize;
                desiredHeight = (int) (desiredWidth / aspectRatio);
                if (heightMode == MeasureSpec.EXACTLY) {
                    desiredHeight = heightSize;
                } else if (heightMode == MeasureSpec.AT_MOST && heightSize < desiredHeight) {
                    desiredHeight = heightSize;
                }
            } else if (heightMode == MeasureSpec.EXACTLY
                    || (heightMode == MeasureSpec.AT_MOST && heightSize < desiredHeight)) {
                desiredHeight = heightSize;
                desiredWidth = (int) (desiredHeight * aspectRatio);
            }

            setMeasuredDimension(desiredWidth, desiredHeight);
        } else if(aspectRatio > 0) {
            int desiredHeight = 0;
            int desiredWidth = 0;

            if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
                desiredHeight = (int) (widthSize / aspectRatio);
                if (heightMode == MeasureSpec.EXACTLY) {
                    desiredHeight = heightSize;
                } else if (heightMode == MeasureSpec.AT_MOST && heightSize < desiredHeight) {
                    desiredHeight = heightSize;
                }
            } else if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
                desiredHeight = heightSize;
                desiredWidth = (int) (desiredHeight * aspectRatio);
            }

            setMeasuredDimension(desiredWidth, desiredHeight);
        } else {
            setMeasuredDimension(0, 0);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int xpad = getPaddingLeft() + getPaddingRight();
        int ypad = getPaddingTop() + getPaddingBottom();

        int width = w - xpad;
        int height = h - ypad;

        bounds = new Rect(0, 0, width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (movie != null && movie.duration() != 0) {
            int runtime =
                    (int) ((android.os.SystemClock.uptimeMillis() - movieStart) % movie.duration());
            movie.setTime(runtime);

            movieBitmap.eraseColor(Color.TRANSPARENT);
            movie.draw(movieCanvas, 0, 0);

            canvas.drawBitmap(movieBitmap, movieBounds, bounds, null);

            invalidate();
        }


    }
}
