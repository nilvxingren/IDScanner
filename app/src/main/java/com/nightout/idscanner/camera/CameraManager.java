package com.nightout.idscanner.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * Created by behnamreyhani-masoleh on 15-10-23.
 */
public class CameraManager {
    public static final int BC_FRAME_WIDTH = 448 * 3;
    public static final int BC_FRAME_HEIGHT = 100 * 3;
    public static final int OCR_FRAME_WIDTH = 350 * 3;
    public static final int OCR_FRAME_HEIGHT = 220 * 3;

    private Context mContext;
    private CameraConfigManager mCameraConfig;
    private Camera mCamera;
    private boolean mCameraIsInitialized;
    private Rect mFramingRect;

    public CameraManager(Context context) {
        mContext = context;
        mCameraConfig = new CameraConfigManager(mContext);
    }

    public synchronized void initCamera(SurfaceHolder holder, boolean turnLightOn) throws IOException {
        mCamera = Camera.open();
        if (mCamera == null) {
            throw new IOException();
        }
        mCamera.setPreviewDisplay(holder);
        if (!mCameraIsInitialized) {
            mCameraConfig.initFromCameraParams(mCamera);
            mCameraIsInitialized = true;
        }
        mCameraConfig.setCameraDefaultParams(mCamera);
        setLightMode(turnLightOn);
        mCamera.startPreview();
    }

    public synchronized void deInitCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mFramingRect = null;
        }
    }

    public void setLightMode(boolean turnLightOn) {
        mCameraConfig.setCameraLightParams(mCamera, turnLightOn);
    }

    public synchronized void adjustFramingRect(boolean isForOCR) {
        if (!mCameraIsInitialized) {
            return;
        }

        Point screenRes = mCameraConfig.getScreenRes();
        int newWidth = isForOCR ? OCR_FRAME_WIDTH : BC_FRAME_WIDTH;
        int newHeight = isForOCR ? OCR_FRAME_HEIGHT : BC_FRAME_HEIGHT;
        int leftOffset = (screenRes.x - newWidth) / 2;
        int topOffset = (screenRes.y - newHeight) / 2;

        mFramingRect = new Rect(leftOffset, topOffset,
                leftOffset + newWidth, topOffset + newHeight);
        mCameraConfig.setFocusArea(mCamera, mFramingRect);
    }

    public synchronized Rect getFramingRect() {
        return mFramingRect;
    }

}
