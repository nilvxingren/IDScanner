package com.nightout.idscanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.google.zxing.pdf417.PDF417Reader;
import com.nightout.idscanner.camera.CameraManager;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.nightout.idscanner.imageutils.ocr.OCRDecodeAsyncTask;
import com.nightout.idscanner.imageutils.pdf417.PDF417DecodeAsyncTask;

import org.opencv.android.OpenCVLoader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ScannerActivity extends Activity implements SurfaceHolder.Callback {

    private ToggleButton mCardSideButton;
    private ToggleButton mLightButton;
    private ViewFinderView mViewFinder;
    private CameraManager mCameraManager;
    private boolean mHasSurface;
    private TessBaseAPI mTessAPI;
    private PDF417Reader mBarcodeReader;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("Ben","Unable to use opencv");
        } else {
            Log.d("Ben","opencv started");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTessEnglishFileOnDevice()) {

            final ProgressDialog progressDialog = showProgressDialog("Downloading Image ...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getLanguageFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorMessage("Error", "There was a problem getting the required files.");
                    }
                    progressDialog.dismiss();
                }
            }).start();
        }

        mBarcodeReader = new PDF417Reader();

        //TODO: only support barcode scanning, show error msg for now and exit app
        if (!initOCR()) {
            showErrorMessage("Error", "There was a problem with the ID scanner.");
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraManager = new CameraManager(this, new Camera.PictureCallback(){

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                if (isForOCR()) {
                    new OCRDecodeAsyncTask(ScannerActivity.this, data, mCameraManager, mTessAPI).execute();
		        } else {
                    new PDF417DecodeAsyncTask(ScannerActivity.this, data, mCameraManager, mBarcodeReader).execute();
                }
            }
        });

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
                    if (mCameraManager.setLightMode(isChecked)){
                        mCameraManager.adjustFramingRect((
                                ((ToggleButton) findViewById(R.id.toggleSide)).isChecked()));
                        mViewFinder.invalidate();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

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
    protected void onDestroy() {
        if (mTessAPI != null) {
            mTessAPI.end();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mCameraManager.deInitCamera();
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
                e.printStackTrace();
            }
        }
        mHasSurface = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mHasSurface) {
            SurfaceHolder surfaceHolder = ((SurfaceView) findViewById(R.id.surface_view))
                    .getHolder();
            surfaceHolder.removeCallback(this);
            mHasSurface = false;
        }
    }

    private void startCamera(SurfaceHolder holder, boolean turnLightOn){
        try {
            mCameraManager.initCamera(holder, turnLightOn);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Camera Hardware Error", "There was a problem with the camera hardware. Please re-start you're phone.");
        }
    }

    private File getStorageDirectory() {
        String state = null;
        File temp = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (RuntimeException e) {
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            try {
                temp = getExternalFilesDir(Environment.MEDIA_MOUNTED);
            } catch (NullPointerException e) {
                // We get an error here if the SD card is visible, but full
                showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
            }
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            // to know is we can neither read nor write
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable or corrupted.");
        }
        return temp;
    }

    private boolean isTessEnglishFileOnDevice() {
        return (new File(getStorageDirectory().toString() + File.separator + "tessdata"
                + File.separator + "eng.traineddata")).exists();
    }

    void showErrorMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setOnCancelListener(new FinishListener(this))
                .setPositiveButton( "Ok", new FinishListener(this))
                .show();
    }

    // TODO: improve accuracy by setting other tessbaseAPI params
    private boolean initOCR(){
        mTessAPI = new TessBaseAPI();
        if (!mTessAPI.init(getStorageDirectory().toString(), "eng")) {
            return false;
        }
        mTessAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        mTessAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "`~!@#$%^&*_+=|\\}]{['\";:?>.<,");
        mTessAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,
                "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-/()");
        return true;
    }

    private void getLanguageFile(){
        File tessDir = new File(getStorageDirectory().toString()
                + File.separator + "tessdata");
        // directory exists or is created, now unzip from assets zip file
        boolean installedFromAssets;
        if (tessDir.mkdir() || tessDir.isDirectory()) {
            installedFromAssets = unzipLanguageFileFromAssets();
            //TODO: if downloading language file fails from assets, get from the internet
            if (!installedFromAssets) {

            }
        } else {
            showErrorMessage("Error", "Unable to install required files.");
        }
    }

    private boolean unzipLanguageFileFromAssets() {
        try {
            ZipInputStream inputStream = new ZipInputStream(getAssets().open("eng.traineddata.zip"));
            ZipEntry entry = inputStream.getNextEntry();
            File destinationFile = new File(getStorageDirectory().toString()
                    + File.separator + "tessdata", "eng.traineddata");
            long zippedFileSize = entry.getSize();

            // Create a file output stream
            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            final int BUFFER = 8192;

            // Buffer the output to the file
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, BUFFER);

            // Write the contents
            int count = 0;
            byte[] data = new byte[BUFFER];
            while ((count = inputStream.read(data, 0, BUFFER)) != -1) {
                bufferedOutputStream.write(data, 0, count);
            }
            bufferedOutputStream.close();
            inputStream.closeEntry();
            inputStream.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public ProgressDialog showProgressDialog(String msg) {
        final ProgressDialog progressDialog = ProgressDialog.show(this, "Please wait ...", msg, true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    //only for testing purposes
    public void showResultAlertDialog(String decodedString) {
        new AlertDialog.Builder(this)
                .setTitle("Decoded Data:")
                .setMessage(decodedString)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mCameraManager.restartCamera();
                    }
                }).create().show();
    }

    private boolean isForOCR() {
	if (mCardSideButton != null) {
        return mCardSideButton.isChecked();
	}
        return false;
    }
}
