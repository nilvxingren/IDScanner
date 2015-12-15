package com.nightout.idscanner.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import com.nightout.idscanner.imageutils.ImagePreProcessor;

import java.io.IOException;
import java.util.List;

/**
 * Created by behnamreyhani-masoleh on 15-10-23.
 */
public class CameraManager {
    public static final int BC_FRAME_WIDTH = 448 * 3;
    public static final int BC_FRAME_HEIGHT = 100 * 3;
    public static final int BC_LIGHT_FRAME_OFFSET_RATIO = 10 ;
    public static final int OCR_FRAME_WIDTH = 325 * 3;
    public static final int OCR_FRAME_HEIGHT = 200 * 3;

    private Context mContext;
    private CameraConfigManager mCameraConfig;
    static private Camera mCamera = null;
    private Camera.PictureCallback mCallback;
    private boolean mCameraIsInitialized;
    private Rect mFramingRect;
    private ImagePreProcessor mImagePreProcessor;
    private boolean mIsLightOn;

    public CameraManager(Context context, Camera.PictureCallback callback) {
        mContext = context;
        mCameraConfig = new CameraConfigManager(mContext);
        mCallback = callback;
        mImagePreProcessor = new ImagePreProcessor(mContext);
    }

    public synchronized void initCamera(SurfaceHolder holder, boolean turnLightOn) throws IOException {
        if (mCamera != null) {
            return;
        }

        try {
            mCamera = Camera.open();
        } catch (Exception e){
            e.printStackTrace();
        }
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
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mFramingRect = null;
        }
    }

    public boolean setLightMode(boolean turnLightOn) {
       // mCameraConfig.setSceneMode(mCamera, true, turnLightOn);
        if (mCameraConfig.setCameraLightParams(mCamera, turnLightOn)) {
            mIsLightOn = turnLightOn;
            return true;
        }
        return false;
    }

    public synchronized void adjustFramingRect(boolean isForOCR) {
        if (!mCameraIsInitialized) {
            return;
        }

        // Move the rectangle to the bottom of the screen if light is on to avoid shine
        Point screenRes = mCameraConfig.getScreenRes();
        int newWidth = isForOCR ? OCR_FRAME_WIDTH : BC_FRAME_WIDTH;
        int newHeight = isForOCR ? OCR_FRAME_HEIGHT : BC_FRAME_HEIGHT;
        int leftOffset = (screenRes.x - newWidth) / 2;
        int topOffset = mIsLightOn ? (screenRes.y - newHeight) - (screenRes.y/BC_LIGHT_FRAME_OFFSET_RATIO)
                : (screenRes.y - newHeight) / 2;

        mFramingRect = new Rect(leftOffset, topOffset,
                leftOffset + newWidth, topOffset + newHeight);
        mCameraConfig.setFocusAndMeteringArea(mCamera, mFramingRect);
       // mCameraConfig.setSceneMode(mCamera, isForOCR, mIsLightOn);
    }

    public synchronized Rect getFramingRect() {
        return mFramingRect;
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
        return mImagePreProcessor.preProcessImageForOCR(data, mFramingRect,
                mCameraConfig.getScreenRes());
    }

    public Bitmap getBarcodeRect(byte [] data) {
        return mImagePreProcessor.preProcessImageForPDF417(data, mFramingRect,
                mCameraConfig.getScreenRes());
    }

    public Camera getCamera(){
        return mCamera;
    }

    public Point getCameraRes() {
        return mCameraConfig.getCameraRes();
    }

    public Point getScreenRes() {
        return mCameraConfig.getScreenRes();
    }

    public void saveErrorImage(Bitmap bm, String fileName) {
        mImagePreProcessor.saveErrorImage(bm, fileName);
    }
}
