package com.nightout.idscanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.nightout.idscanner.camera.CameraManager;
import com.nightout.idscanner.imageutils.ocr.OCRHelper;
import com.nightout.idscanner.imageutils.pdf417.PDF417Helper;

import org.opencv.android.OpenCVLoader;

public class ScannerActivity extends Activity implements SurfaceHolder.Callback {
    private ToggleButton mCardSideButton;
    private ToggleButton mLightButton;
    private View mDecodeSpinner;
    private ViewFinderView mViewFinder;

    private CameraManager mCameraManager;
    private PDF417Helper mBarcodeScannerHelper;
    private OCRHelper mOCRHelper;

    private boolean mHasSurface;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("Ben", "Unable to use opencv");
        } else {
            Log.d("Ben","opencv started");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraManager = new CameraManager();

        FileManager fileManager = new FileManager(this);
        mOCRHelper = new OCRHelper(this, fileManager);

        mBarcodeScannerHelper = new PDF417Helper(this);

        mOCRHelper.setupOCRLibrary();

        initializeViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SurfaceHolder holder = ((SurfaceView)findViewById(R.id.surface_view)).getHolder();
        if (!mHasSurface) {
            holder.addCallback(this);
        }
    }

    @Override
    protected void onPause() {
        mCameraManager.deInitCamera();
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        mOCRHelper.deInitOCRLibrary();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!mHasSurface) {
            try {
                startCamera(holder, mLightButton.isChecked());
                mCameraManager.adjustFramingRect(mCardSideButton.isChecked());
                mViewFinder.invalidate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mHasSurface = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mHasSurface) {
            SurfaceHolder surfaceHolder = ((SurfaceView) findViewById(R.id.surface_view))
                    .getHolder();
            surfaceHolder.removeCallback(this);
            mHasSurface = false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (mHasSurface && mCameraManager != null && mCameraManager.isPictureTakingReady()
                    && !mBarcodeScannerHelper.currentlyScanning()) {
                try {
                    mDecodeSpinner.setVisibility(View.VISIBLE);
                    mBarcodeScannerHelper.decodeBatch();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initializeViews() {
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
                    e.printStackTrace();
                }
            }
        });

        mLightButton = (ToggleButton)findViewById(R.id.toggleFlash);
        mLightButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (mCameraManager.setLightMode(isChecked)) {
                        mCameraManager.adjustFramingRect((
                                ((ToggleButton) findViewById(R.id.toggleSide)).isChecked()));
                        mViewFinder.invalidate();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mDecodeSpinner = findViewById(R.id.decode_spinner);

        /* Hardware volume up/down used for starting barcode decoding for now
        (findViewById(R.id.button_capture)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHasSurface) {
                    try {
                        mCameraManager.takePicture();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorMessage("Camera Hardware Error", "There was a problem with the camera hardware.");
                    }
            }
            }
        });
         */
    }

    private void startCamera(SurfaceHolder holder, boolean turnLightOn){
        try {
            mCameraManager.initCamera(holder, turnLightOn, this);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Camera Hardware Error", "There was a problem with the camera hardware. Please re-start you're phone.");
        }
    }

    public void reportScannerBatchResponse(boolean successful, String decodedResult) {
        if (successful) {
            mDecodeSpinner.setVisibility(View.GONE);
            showAlertDialog("Decode Response:", decodedResult);
        } else if (!mCameraManager.isLightOn()){
            mDecodeSpinner.setVisibility(View.GONE);
            showAlertDialog("Inadequate Lighting",
                    "Please turn on the scanner light and try again for improved accuracy.");
        } else {
            mBarcodeScannerHelper.decodeBatch();
        }
    }

    public void showErrorMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setOnCancelListener(new FinishListener(this))
                .setPositiveButton( "Ok", new FinishListener(this))
                .show();
    }

    public ProgressDialog showProgressDialog(String msg) {
        final ProgressDialog progressDialog = ProgressDialog.show(this, "Please wait ...", msg, true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    //only for testing purposes
    public void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mCameraManager.restartCamera();
                    }
                }).create().show();
    }

    private boolean isForOCR() {
        return mCardSideButton != null && mCardSideButton.isChecked();
    }

    public CameraManager getCameraManager(){
        return mCameraManager;
    }
}
