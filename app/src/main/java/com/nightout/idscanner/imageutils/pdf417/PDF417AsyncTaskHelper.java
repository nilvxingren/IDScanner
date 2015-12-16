package com.nightout.idscanner.imageutils.pdf417;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.nightout.idscanner.ScannerActivity;
import com.nightout.idscanner.camera.CameraManager;

/**
 * Created by behnamreyhani-masoleh on 15-12-15.
 */
public class PDF417AsyncTaskHelper {
    private static final int INITIAL_PICTURE_TAKE_DELAY = 50;
    private static final int PICTURE_TAKING_INTERVAL = 25;
    private static final int MAX_THREAD_COUNT = 4;

    private int mStartedThreadCount;
    private int mFinishedThreadCount;

    private boolean mScanningsCurrentlyInSession;
    private boolean mSuccessfullyDecoded;
    private String mDecodedResponse;

    private ScannerActivity mScannerActivity;
    private CameraManager mCameraManager;

    Camera.PictureCallback mCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mCameraManager.restartCamera();
            mStartedThreadCount++;
            startAsyncTaskInParallel(new PDF417DecodeAsyncTask(data, mCameraManager,
                    PDF417AsyncTaskHelper.this));
            if (mStartedThreadCount < MAX_THREAD_COUNT) {
                takePictureWithDelay(PICTURE_TAKING_INTERVAL);
            }
        }
    };

    public PDF417AsyncTaskHelper(ScannerActivity activity) {
        mScannerActivity = activity;
        mCameraManager = mScannerActivity.getCameraManager();
    }

    public boolean currentlyScanning(){
        return mScanningsCurrentlyInSession;
    }

    public void decodeBatch(){
        mScanningsCurrentlyInSession = true;
        takePictureWithDelay(INITIAL_PICTURE_TAKE_DELAY);
    }

    private void takePictureWithDelay(long delay){
        Handler h = new Handler();
        Runnable delayed = new Runnable() {
            @Override
            public void run() {
                mCameraManager.takePicture(mCallback);
            }
        };
        h.postDelayed(delayed, delay);
    }

    private void startAsyncTaskInParallel(PDF417DecodeAsyncTask task) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            task.execute();
    }

    public synchronized void reportResult(boolean successful, String result) {
        mFinishedThreadCount++;
        if (successful && !mSuccessfullyDecoded) {
            mSuccessfullyDecoded = true;
            mDecodedResponse = result;
            mScannerActivity.reportScannerBatchResponse(mSuccessfullyDecoded, mDecodedResponse);
        }

        if (mFinishedThreadCount == MAX_THREAD_COUNT) {
            if (!mSuccessfullyDecoded) {
                mScannerActivity.reportScannerBatchResponse(false, null);
            }
            cleanup();
        }
    }

    private void cleanup(){
        mScanningsCurrentlyInSession = false;
        mSuccessfullyDecoded = false;
        mDecodedResponse = null;
        mFinishedThreadCount = 0;
        mStartedThreadCount = 0;
    }
}
