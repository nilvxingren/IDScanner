package com.nightout.idscanner.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import com.nightout.idscanner.imageutils.ocr.OCRPreProcessor;

import java.io.IOException;
import java.util.List;

/**
 * Created by behnamreyhani-masoleh on 15-10-23.
 */
public class CameraManager {
    public static final int BC_FRAME_WIDTH = 448 * 3;
    public static final int BC_FRAME_HEIGHT = 100 * 3;
    public static final int OCR_FRAME_WIDTH = 325 * 3;
    public static final int OCR_FRAME_HEIGHT = 200 * 3;

    private Context mContext;
    private CameraConfigManager mCameraConfig;
    private Camera mCamera;
    private Camera.PictureCallback mCallback;
    private boolean mCameraIsInitialized;
    private Rect mFramingRect;
    private OCRPreProcessor mOCRPreProcessor;

    public CameraManager(Context context, Camera.PictureCallback callback) {
        mContext = context;
        mCameraConfig = new CameraConfigManager(mContext);
        mCallback = callback;
        mOCRPreProcessor = new OCRPreProcessor(mContext);
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

    public synchronized Rect getFramingRectInPreview() {
        Rect rect = new Rect(getFramingRect());
        Point cameraResolution = mCameraConfig.getCameraRes();
        Point screenResolution = mCameraConfig.getScreenRes();
        if (cameraResolution == null || screenResolution == null) {
            return null;
        }
        rect.left = rect.left * cameraResolution.x / screenResolution.x;
        rect.right = rect.right * cameraResolution.x / screenResolution.x;
        rect.top = rect.top * cameraResolution.y / screenResolution.y;
        rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
        return rect;
    }

    public void takePicture() {
        if (mCamera != null) {
            mCamera.takePicture(null, null, mCallback);
        }
    }

    public void restartCamera(){
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    public List<Bitmap> getEnhancedBitmap(byte [] data) {
        return mOCRPreProcessor.preProcessImage(data, mFramingRect, mCameraConfig.getScreenRes());
    }
}
