package com.example.behnamreyhani_masoleh.idscannertest;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.example.behnamreyhani_masoleh.idscannertest.camera.CameraManager;

import java.io.IOException;

public class ScannerActivity extends Activity implements SurfaceHolder.Callback {

    private ToggleButton mCardSideButton;
    private ToggleButton mLightButton;
    private ViewFinderView mViewFinder;
    private CameraManager mCameraManager;
    private boolean mHasSurface;
    private TessUtils mTessUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        mCameraManager = new CameraManager(this);
        mTessUtils = new TessUtils(this);

        setContentView(R.layout.activity_scanner);
        mViewFinder = (ViewFinderView) findViewById(R.id.view_finder_view);
        mViewFinder.setCameraManager(mCameraManager);

        mCardSideButton = (ToggleButton) findViewById(R.id.toggleSide);
        mCardSideButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    mCameraManager.adjustFramingRect(isChecked);
                    mViewFinder.invalidate();
                } catch (Exception e){
                    Log.e("Ben", "exception", e);
                }
            }
        });

        mLightButton = (ToggleButton)findViewById(R.id.toggleFlash);
        mLightButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        try {
                            mCameraManager.setLightMode(isChecked);
                        } catch (Exception e){
                            Log.e("Ben", "exception", e);
                        }
                    }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SurfaceHolder holder = ((SurfaceView)findViewById(R.id.surface_view)).getHolder();
        if (mHasSurface) {
            try {
                startCamera(holder, mLightButton.isChecked());
                mCameraManager.adjustFramingRect(mCardSideButton.isChecked());
            } catch (Exception e) {
                Log.e("Ben", "exception", e);
            }
        } else {
            holder.addCallback(this);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mCameraManager.deInitCamera();
        if (!mHasSurface) {
            SurfaceHolder surfaceHolder = ((SurfaceView) findViewById(R.id.surface_view))
                    .getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!mHasSurface) {
            try {
                startCamera(holder, mLightButton.isChecked());
                mCameraManager.adjustFramingRect(mCardSideButton.isChecked());
                mViewFinder.invalidate();
            } catch (Exception e) {
                Log.e("Ben", "exception", e);
            }
        }
        mHasSurface = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    private void startCamera(SurfaceHolder holder, boolean turnLightOn){
        try {
            mCameraManager.initCamera(holder, turnLightOn);
        } catch (IOException e) {
            Log.e("Ben", "exception", e);
        }
    }

/*
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if (pictureIsForOCR()) {
          mImageHelper.test(data,mCardFrame);
           //String text = mTessHelper.applyOCR(mImageHelper.convertRawDataToBWBitmap(data));
           //Log.d("Ben", text);
        } else {
            // TODO: Pass in picture to barcode scanner
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private boolean pictureIsForOCR() {
        return mCardSideButton.isChecked();
    }

      private void testOCRAlgo(){
        File testImage = new File(Environment.getExternalStorageDirectory(), "nightout/benji.png");
        String text = mTessHelper.testOCR(testImage);
        new AlertDialog.Builder(this).setTitle("Test").setMessage(text).create().show();
    }*/
}
