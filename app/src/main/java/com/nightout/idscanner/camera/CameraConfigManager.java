package com.nightout.idscanner.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.Image;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.nightout.idscanner.imageutils.ImagePreProcessor;

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
    private static final float MAX_EXPOSURE_COMPENSATION = 1.5f;
    private static final float MIN_EXPOSURE_COMPENSATION = 0.0f;

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

        if (params.isVideoStabilizationSupported()) {
            params.setVideoStabilization(true);
            camera.setParameters(params);
        }
    }

    void setCameraDefaultParams(Camera camera) {
        Camera.Parameters params = camera.getParameters();
        String focusMode = findSettableValue(params.getSupportedFocusModes(),
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
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

    void setSceneMode(Camera camera, boolean isForOCR, boolean isLightOn) {
        Camera.Parameters params = camera.getParameters();
        String desiredSceneMode = null;

        // STEADYPHOTO mode and others may turn off light, therefore don't set scene when light is on
        if (isLightOn) {
            desiredSceneMode = findSettableValue(params.getSupportedSceneModes(),
                    Camera.Parameters.SCENE_MODE_AUTO);
        } else {
            if (isForOCR) {
                desiredSceneMode = findSettableValue(params.getSupportedSceneModes(),
                        Camera.Parameters.SCENE_MODE_STEADYPHOTO,
                        Camera.Parameters.SCENE_MODE_BARCODE,
                        Camera.Parameters.SCENE_MODE_AUTO,
                        Camera.Parameters.SCENE_MODE_LANDSCAPE
                );
            } else {
                desiredSceneMode = findSettableValue(params.getSupportedSceneModes(),
                        Camera.Parameters.SCENE_MODE_BARCODE,
                        Camera.Parameters.SCENE_MODE_STEADYPHOTO,
                        Camera.Parameters.SCENE_MODE_AUTO,
                        Camera.Parameters.SCENE_MODE_LANDSCAPE
                );
            }
        }

        if (desiredSceneMode != null) {
            params.setSceneMode(desiredSceneMode);
            camera.setParameters(params);
        }
    }

    void setFocusAndMeteringArea(Camera camera, Rect focusAreaRect) {
        Camera.Parameters params = camera.getParameters();
        List<Camera.Area> convertedCameraFocusArea = convertToCameraParamsFocusArea(focusAreaRect);

        if (params.getMaxNumFocusAreas() > 0) {
            params.setFocusAreas(convertedCameraFocusArea);
        }

        if (params.getMaxNumMeteringAreas() > 0) {
            params.setMeteringAreas(convertedCameraFocusArea);

        }
        camera.setParameters(params);
    }



    boolean setCameraLightParams(Camera camera, boolean turnLightOn) {
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
            // TODO: Might be better compensated outdoors
            //setBestExposure(params, turnLightOn);
            camera.setParameters(params);
            return true;
        }
        return false;
    }

    void setBestExposure(Camera.Parameters parameters, boolean lightOn) {
        int minExposure = parameters.getMinExposureCompensation();
        int maxExposure = parameters.getMaxExposureCompensation();

        float step = parameters.getExposureCompensationStep();
        if ((minExposure != 0 || maxExposure != 0) && step > 0.0f) {
            // Set low when light is on
            float targetCompensation = lightOn ? MIN_EXPOSURE_COMPENSATION : MAX_EXPOSURE_COMPENSATION;
            int compensationSteps = Math.round(targetCompensation / step);
            // Clamp value:
            compensationSteps = Math.max(Math.min(compensationSteps, maxExposure), minExposure);

            if (parameters.getExposureCompensation() != compensationSteps) {
                parameters.setExposureCompensation(compensationSteps);
            }
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

    private List<Camera.Area> convertToCameraParamsFocusArea(Rect focusArea) {
        List<Camera.Area>list = new ArrayList<>();

        double heightBuffer = (double) mScreenRes.y/ ImagePreProcessor.HEIGHT_BUFFER_RATIO;
        double widthBuffer = (double) mScreenRes.x/ImagePreProcessor.WIDTH_BUFFER_RATIO;

        Double left = ((double)focusArea.left/mScreenRes.x)*2000 - widthBuffer;
        Double top =  ((double)focusArea.top/mScreenRes.y)*2000 - heightBuffer;
        Double right = ((double)focusArea.right/mScreenRes.x)*2000 + widthBuffer;
        Double bottom = ((double)focusArea.bottom/mScreenRes.y)*2000 + heightBuffer;
        Camera.Area area = new Camera.Area(new Rect( left.intValue() - 1000, top.intValue() - 1000,
                right.intValue() - 1000, bottom.intValue() - 1000),1);
        list.add(area);
        return list;
    }

    public Point getScreenRes() {
        return mScreenRes;
    }

    public Point getCameraRes() { return mCameraRes; }
}
