package com.nightout.idscanner.imageutils.pdf417;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.zxing.Result;
import com.google.zxing.pdf417.PDF417Reader;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.nightout.idscanner.ScannerActivity;
import com.nightout.idscanner.camera.CameraManager;

import java.util.List;

/**
 * Created by behnamreyhani-masoleh on 15-11-02.
 */
// Takes care of PDF417 decode using the xzing library
public class PDF417DecodeAsyncTask extends AsyncTask<Void, Void, String> {
    private ScannerActivity mActivity;
    private byte[] mData;
    private CameraManager mManager;
    private ProgressDialog mDialog;
    private PDF417Reader mBarcodeReader;

    public PDF417DecodeAsyncTask(ScannerActivity activity, byte[] data, CameraManager manager, PDF417Reader scanner ) {
        mActivity = activity;
        mData = data;
        mManager = manager;
        mBarcodeReader = scanner;
    }

    @Override
    protected void onPreExecute() {
        mDialog = mActivity.showProgressDialog("Scanning ID...");
    }

    @Override
    protected String doInBackground(Void... values) {
        String results;
        Log.d("Faggot", "Inside async task background work");
        Bitmap pdf417Barcode = mManager.getBarcodeRect(mData);
        if (pdf417Barcode == null) {
            return null;
        }

       // Result result = mBarcodeReader.decode(pdf417Barcode);

        //TODO: run Xzing onto pdf417Barcode instance
        return "";

    }

    protected void onPostExecute(String decodedString) {
        mDialog.dismiss();
        mActivity.showResultAlertDialog(decodedString);
    }
}
