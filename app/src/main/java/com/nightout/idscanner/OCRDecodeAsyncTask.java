package com.nightout.idscanner;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

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
        Bitmap bitmap = mManager.getEnhancedBitmap(mData);
        // Testing algo:
        //Bitmap bitmap = mManager.testAlgo();
        Log.d("Benji", "Pipeline time in ms: " + (System.currentTimeMillis() - start));
        if (bitmap == null) {
            return null;
        }
        mTessAPI.setImage(bitmap);
        results = mTessAPI.getUTF8Text();

        long tessStart = System.currentTimeMillis();

        Log.d("Benji", "Time taken for Tess in ms: " + (System.currentTimeMillis() - tessStart));
        Log.d("Benji", "Results with rescale:\n" + results);
        return "\nTime Required in ms with rescale: " + (System.currentTimeMillis() - start);
    }

    protected void onPostExecute(String decodedString) {
        mDialog.dismiss();
        mActivity.showResultAlertDialog(decodedString);
    }
}
