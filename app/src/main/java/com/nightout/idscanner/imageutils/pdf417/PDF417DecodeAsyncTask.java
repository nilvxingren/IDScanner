package com.nightout.idscanner.imageutils.pdf417;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.pdf417.PDF417Reader;
import com.nightout.idscanner.ScannerActivity;
import com.nightout.idscanner.camera.CameraManager;

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

    // TODO: need to change error handling, just for simplification during testing phase purposes
    private static final String ERROR_RESPONSE = "ERROR";

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
        long start = System.currentTimeMillis();
        String results = "";
        Bitmap pdf417Barcode = mManager.getBarcodeRect(mData);

        if (pdf417Barcode == null) {
            return ERROR_RESPONSE;
        }

        try {
            Result result = mBarcodeReader.decode(bitmapToBinaryBitmap(pdf417Barcode));
            if (result.getText() != null) {
                results = result.getText();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR_RESPONSE;
        }
        Log.d("Faggot","Results from barcode scanning:\n" + results);
        Log.d("Faggot", "Time taken for scanning in ms: " + (System.currentTimeMillis() - start));
        results += "\nTime taken for scanning in ms: " + (System.currentTimeMillis() - start);
        return results;
    }

    protected void onPostExecute(String decodedString) {
        mDialog.dismiss();
        mActivity.showResultAlertDialog(decodedString);
    }

    private BinaryBitmap bitmapToBinaryBitmap(Bitmap pdf417Bitmap) {
        int [] pixels = new int[pdf417Bitmap.getWidth()*pdf417Bitmap.getHeight()];
        pdf417Bitmap.getPixels(pixels, 0, pdf417Bitmap.getWidth(), 0, 0, pdf417Bitmap.getWidth(),
                pdf417Bitmap.getHeight());
        LuminanceSource source = new RGBLuminanceSource(pdf417Bitmap.getWidth(), pdf417Bitmap.getHeight(), pixels);
        return new BinaryBitmap(new HybridBinarizer(source));
    }
}
