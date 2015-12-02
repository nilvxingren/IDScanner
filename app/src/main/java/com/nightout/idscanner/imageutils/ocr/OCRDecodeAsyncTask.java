package com.nightout.idscanner.imageutils.ocr;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import com.nightout.idscanner.ScannerActivity;
import com.nightout.idscanner.camera.CameraManager;

import java.util.List;

/**
 * Created by behnamreyhani-masoleh on 15-11-02.
 */
// Takes care of OCR using the tesseract library
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
        List<Bitmap> bitmapList = mManager.getEnhancedBitmap(mData);
        if (bitmapList == null || bitmapList.size() < 4) {
            return null;
        }

        //TODO: decode images concurrently on seperate threads for speed
        int i = 0;
        start = System.currentTimeMillis();
        // Run OCR on each extracted textbox from preprocessing
        for (Bitmap bitmap : bitmapList) {
            long tessStart = System.currentTimeMillis();
            mTessAPI.setImage(bitmap);
            results = mTessAPI.getUTF8Text();
          //  Log.d("Faggot", "Time taken for Tess in ms for textbox #" + i + ": " + (System.currentTimeMillis() - tessStart));
            //Log.d("Faggot", "Results with rescale for textbox #" + i + ":\n" + results);
            i++;
        }
        return "\nTime Required in ms with rescale: " + (System.currentTimeMillis() - start);
    }

    protected void onPostExecute(String decodedString) {
        mDialog.dismiss();
        mActivity.showResultAlertDialog(decodedString);
    }
}
