package com.example.behnamreyhani_masoleh.idscannertest;

import android.app.ActionBar;
import android.hardware.Camera;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScannerActivity extends Activity implements Camera.PictureCallback {

    private static final int BACK_FRAME_WIDTH = 448*3;
    private static final int BACK_FRAME_HEIGHT = 100*3;
    private static final int FRONT_FRAME_WIDTH = 350*3;
    private static final int FRONT_FRAME_HEGIHT = 220*3;

    private FrameLayout.LayoutParams backOfCardParams =
            new FrameLayout.LayoutParams(BACK_FRAME_WIDTH, BACK_FRAME_HEIGHT, Gravity.CENTER);

    private FrameLayout.LayoutParams frontOfCardParams =
            new FrameLayout.LayoutParams(FRONT_FRAME_WIDTH, FRONT_FRAME_HEGIHT, Gravity.CENTER);

    private Camera mCamera;
    private ScannerView mPreview;
    private ToggleButton mCardSideButton;
    private View mCardFrame;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        mCardSideButton = (ToggleButton) findViewById(R.id.toggleSide);
        mCardFrame = findViewById(R.id.frame);
        mCardSideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCardSideButton.isChecked()) {
                    mCardFrame.setLayoutParams(frontOfCardParams);
                } else {
                    mCardFrame.setLayoutParams(backOfCardParams);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Create an instance of Camera
        mCamera = getCameraInstance();
        // Create our Preview view and set it as the content of our activity.
        mPreview = new ScannerView(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, ScannerActivity.this);
                    }
                }
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.setPreviewCallback(null);
        mPreview.getHolder().removeCallback(mPreview);
        releaseCamera();              // release the camera immediately on pause event
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null){
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (Exception e) {
        }
        // TODO: Need to store Language files for tesseract onto app device
        if (pictureIsForOCR()) {
            // TODO: Pass in picture to OCR Library
        } else {
            // TODO: Pass in picture to barcode scanner
        }
        // Delete temp file once OCR/scan is done
        pictureFile.delete();
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        try {
            return File.createTempFile("NIGHTOUT_IMG", timeStamp);
        } catch (Exception e) {
            return null;
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

    private boolean pictureIsForOCR() {
        return mCardSideButton.isChecked();
    }
}
