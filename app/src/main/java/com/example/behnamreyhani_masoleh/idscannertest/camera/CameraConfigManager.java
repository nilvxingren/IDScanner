package com.example.behnamreyhani_masoleh.idscannertest.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by behnamreyhani-masoleh on 15-10-23.
 */
public class CameraConfigManager {
    private static final int MIN_PREVIEW_PIXELS = 470 * 320; // normal screen
    private static final int MAX_PREVIEW_PIXELS = 800 * 600; // more than large/HD screen

    private Context mContext;
    private Point mScreenRes;
    private Point mCameraRes;

    public CameraConfigManager(Context context) {
        mContext = context;
    }

    void initFromCameraParams(Camera camera) {
        Camera.Parameters params = camera.getParameters();
        WindowManager windowManager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        // In case it thinks its in portrait
        if (width < height) {
            int temp = width;
            width = height;
            height = temp;
        }
        mScreenRes = new Point(width, height);
        mCameraRes = findBestPreviewSizeValue(params);
    }

    void setCameraDefaultParams(Camera camera) {
        if (camera == null) {
            return;
        }

        Camera.Parameters params = camera.getParameters();
        if (params == null) {
            return;
        }

        String effectMode = findSettableValue(params.getSupportedColorEffects(),
                Camera.Parameters.EFFECT_MONO);
        if (effectMode != null) {
            params.setColorEffect(effectMode);
        }

        String focusMode = findSettableValue(params.getSupportedFocusModes(),
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
                Camera.Parameters.FOCUS_MODE_AUTO,
                Camera.Parameters.FOCUS_MODE_MACRO,
                Camera.Parameters.FOCUS_MODE_EDOF);
        if (focusMode != null) {
            params.setFocusMode(focusMode);
        }

        params.set("orientation", "landscape");
        params.setPreviewSize(mCameraRes.x, mCameraRes.y);
        camera.setParameters(params);
    }

    void setCameraLightParams(Camera camera, boolean turnLightOn) {
        if (camera == null) {
            return;
        }

        Camera.Parameters params = camera.getParameters();

        String torch = null;
        if (turnLightOn) {
            torch = findSettableValue(params.getSupportedFlashModes(),
                    Camera.Parameters.FLASH_MODE_TORCH,
                    Camera.Parameters.FLASH_MODE_ON);
        } else {
            torch = findSettableValue(params.getSupportedFlashModes(),
                    Camera.Parameters.FLASH_MODE_OFF);
        }

        if (torch != null) {
            params.setFlashMode(torch);
            camera.setParameters(params);
        }
    }

    private Point findBestPreviewSizeValue(Camera.Parameters parameters) {

        // Sort by size, descending
        List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(parameters.getSupportedPreviewSizes());
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

            StringBuilder previewSizesString = new StringBuilder();
            for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
                previewSizesString.append(supportedPreviewSize.width).append('x')
                        .append(supportedPreviewSize.height).append(' ');
            }

        Point bestSize = null;
        float screenAspectRatio = (float) mScreenRes.x / (float) mScreenRes.y;

        float diff = Float.POSITIVE_INFINITY;
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            int pixels = realWidth * realHeight;
            if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS) {
                continue;
            }
            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            if (maybeFlippedWidth == mScreenRes.x && maybeFlippedHeight == mScreenRes.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                return exactPoint;
            }
            float aspectRatio = (float) maybeFlippedWidth / (float) maybeFlippedHeight;
            float newDiff = Math.abs(aspectRatio - screenAspectRatio);
            if (newDiff < diff) {
                bestSize = new Point(realWidth, realHeight);
                diff = newDiff;
            }
        }

        if (bestSize == null) {
            Camera.Size defaultSize = parameters.getPreviewSize();
            bestSize = new Point(defaultSize.width, defaultSize.height);
        }

        return bestSize;
    }

    private static String findSettableValue(Collection<String> supportedValues,
                                            String... desiredValues) {
        String result = null;
        if (supportedValues != null) {
            for (String desiredValue : desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    result = desiredValue;
                    break;
                }
            }
        }
        return result;
    }

    public Point getScreenRes() {
        return mScreenRes;
    }

}
