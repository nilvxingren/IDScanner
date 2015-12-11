package com.nightout.idscanner.imageutils.pdf417;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.pdf417.PDF417Reader;
import com.nightout.idscanner.TestStats;
import com.nightout.idscanner.ScannerActivity;
import com.nightout.idscanner.camera.CameraManager;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    private static final String ERROR_RESPONSE = "Image Pre-Processing Error in ms: ";

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
        return runZxingTests();
      /*  long start = System.currentTimeMillis();
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
            mManager.saveErrorImage(pdf417Barcode,
                    TestStats.incrementExceptionCountAndGetFileName(e));
            e.printStackTrace();
            return ERROR_RESPONSE;
        }
        Log.d("Faggot","Results from barcode scanning:\n" + results);
        Log.d("Faggot", "Time taken for scanning in ms: " + (System.currentTimeMillis() - start));
        results += "\nTime taken for scanning in ms: " + (System.currentTimeMillis() - start);
        return results;*/
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

    private String runZxingTests(){
        TestStats.incrementRunCount();
        long start = System.currentTimeMillis();
        Bitmap pdf417Barcode = mManager.getBarcodeRect(mData);
        long preProcessTime = System.currentTimeMillis() - start;

        if (pdf417Barcode == null) {
            for (int i = 0; i < 4; i ++) {
                TestStats.increaseErrorTime(i, preProcessTime);
            }
            return ERROR_RESPONSE + preProcessTime;
        }

        boolean [] successful = {true,true,true,true};
        long [] decodeTimes = new long [4];

        for (int i = 0; i < 4; i++) {
            long decodeStart = System.currentTimeMillis();
            try {
                Result result = mBarcodeReader.decode(bitmapToBinaryBitmap(pdf417Barcode), getDecoderParams(i));
                if (result != null && result.getText() != null && !result.getText().equals("")) {
                    // Represents successful barcode decode
                    Log.d("BenResults","Param type: " + i + "\n" + result.getText());
                    long decodeTime = System.currentTimeMillis() - decodeStart;
                    TestStats.increaseSuccessTime(i, decodeTime);
                    decodeTimes[i] = decodeTime;
                } else {
                    // indicate that the barcode decode was not successful barcode decode
                    throw new Exception();
                }
            } catch (Exception e) {
                // Represents unsuccessful barcode decode
                long decodeFail = System.currentTimeMillis() - decodeStart;
                TestStats.increaseErrorTime(i, decodeFail);
                decodeTimes[i] = decodeFail;
                successful[i] = false;
            }
        }
        return visualizeTests(successful, decodeTimes);
    }

    private Map<DecodeHintType,Object> getDecoderParams(int type) {
        Map<DecodeHintType,Object> paramMap = new EnumMap<>(DecodeHintType.class);

        List<BarcodeFormat> supportedBarcodes = new LinkedList<>();
        supportedBarcodes.add(BarcodeFormat.PDF_417);
        paramMap.put(DecodeHintType.POSSIBLE_FORMATS, supportedBarcodes);
        switch(type) {
            case TestStats.MONO_PARAMS:
                paramMap.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
                break;
            case TestStats.TRY_HARDER_PARAMS:
                paramMap.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);
                break;
            case TestStats.MONO_TRY_HARDER_PARAMS:
                paramMap.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
                paramMap.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);
                break;
        }
        return paramMap;
    }

    private String visualizeTests(boolean [] successful, long [] decodeTimes) {
        String results = "";
        for (int i = 0; i<successful.length; i++) {

            results+= (successful[i] ? "Success" : "Fail") + " - " + decodeTimes[i] + " ms - " +
                    getTestTitle(i) + "\n";
        }
        return results;
    }

    private String getTestTitle(int paramType) {
        String type = "No Params";
        switch (paramType) {
            case TestStats.NO_PARAMS:
                break;
            case TestStats.MONO_PARAMS:
                type = "Pure Barcode";
                break;
            case TestStats.TRY_HARDER_PARAMS:
                type = "Try Harder";
                break;
            case TestStats.MONO_TRY_HARDER_PARAMS:
                type = "Pure Barcode & Try Harder";
                break;
        }
        return type;
    }

}
