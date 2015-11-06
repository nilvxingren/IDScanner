package com.nightout.idscanner;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.nightout.idscanner.camera.CameraManager;

/**
 * Created by behnamreyhani-masoleh on 15-11-02.
 */
public class OCRDecodeAsyncTask extends AsyncTask<Void, Void, String> {
    private ScannerActivity mActivity;
    private byte[] mData;
    private CameraManager mManager;
    private TessBaseAPI mTessAPI;
    private ProgressDialog mDialog;

    public OCRDecodeAsyncTask(ScannerActivity activity, byte [] data, CameraManager manager, TessBaseAPI api) {
        mActivity = activity;
        mData = data;
        mManager = manager;
        mTessAPI = api;
    }

    @Override
    protected void onPreExecute() {
        mDialog = mActivity.showProgressDialog("Scanning ID...");
    }

    @Override
    protected String doInBackground(Void... values) {
        long start = System.currentTimeMillis();
        String results;
        Bitmap bitmap = mManager.getCroppedBitmap(mData);
        if (bitmap == null) {
            return null;
        }
        long tessStart = System.currentTimeMillis();
        mTessAPI.setImage(bitmap);
        results = mTessAPI.getUTF8Text();
        Log.d("Benji","Time for tess shit in ms: " + (System.currentTimeMillis()-tessStart));
        long totalTime = System.currentTimeMillis() - start;
        return results + "\nTime Required in ms: " + totalTime;
    }

    protected void onPostExecute(String decodedString) {
        mDialog.dismiss();
        mActivity.showResultAlertDialog(decodedString);
    }
}
