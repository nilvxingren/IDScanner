package com.nightout.idscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.nightout.idscanner.camera.CameraManager;

/**
 * Created by behnamreyhani-masoleh on 15-10-23.
 */
public final class ViewFinderView extends View {

    private static final String VIEWFINDER_EXTERIOR_COLOR = "#60000000";
    private static final String VIEWFINDER_INTERIOR_COLOR = "#ffd6d6d6";

    private Context mContext;
    private CameraManager mCameraManager;

    public ViewFinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @SuppressWarnings("unused")
    @Override
    public void onDraw(Canvas canvas) {
        try {
            Rect frame = mCameraManager.getFramingRect();
            if (frame == null ) {
                return;
            }

            int width = canvas.getWidth();
            int height = canvas.getHeight();

            Paint paint = new Paint();

            // Draw the framing rect exterior UI shaded elements
            paint.setColor(Color.parseColor(VIEWFINDER_EXTERIOR_COLOR));
            canvas.drawRect(0, 0, width, frame.top, paint);
            canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
            canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
            canvas.drawRect(0, frame.bottom + 1, width, height, paint);

            // Draw the framing rect interior UI elements
            paint.setAlpha(0);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.parseColor(VIEWFINDER_INTERIOR_COLOR));
            canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
            canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
            canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
            canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);
        } catch (NullPointerException e) {
            Log.e("Ben", "exception", e);
        }
    }

    public void setCameraManager(CameraManager manager) {
        mCameraManager = manager;
    }

}
