package com.nightout.idscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;

/**
 * Created by behnamreyhani-masoleh on 15-12-03.
 */
public class TestStats extends BroadcastReceiver {
    private static int TOTAL_TRIALS;

    private static int [] SUCCESSFUL_TRIAL_COUNT = new int [4];
    private static long [] SUCCESS_TIME_TOTAL = new long [4];
    private static long [] FAIL_TIME_TOTAL = new long [4];

    /*
    private static int [] CHECKSUM_ERROR_COUNT = new int [4];
    private static int [] FORMAT_ERROR_COUNT = new int [4];
    private static int [] NOT_FOUND_ERROR_COUNT = new int [4];
    private static int [] PREPROCESSOR_ERROR_COUNT = new int [4];*/


    public static final int NO_PARAMS = 0;
    public static final int MONO_PARAMS = 1;
    public static final int TRY_HARDER_PARAMS = 2;
    public static final int MONO_TRY_HARDER_PARAMS = 3;

    @Override
    public void onReceive(Context context, Intent intent) {

    }

   /* public static String incrementExceptionCountAndGetFileName(Exception e) {
        String fileName = "NotZXingException-" + NOT_ZXING_EXCEPTION_COUNT;
        if (e instanceof ChecksumException) {
            fileName = "CheckSumException-" + CHECKSUM_EXCEPTION_COUNT;
            CHECKSUM_EXCEPTION_COUNT++;
        } else if (e instanceof FormatException) {
            fileName = "FormatException-" + FORMAT_EXCEPTION_COUNT;
            FORMAT_EXCEPTION_COUNT++;
        } else if (e instanceof NotFoundException) {
            fileName = "NotFoundException-" + NOT_FOUND_EXCEPTION_COUNT;
            NOT_FOUND_EXCEPTION_COUNT++;
        } else {
            NOT_ZXING_EXCEPTION_COUNT++;
        }
        return fileName;
    }

    public static void incrementErrorCount(Exception e, int barcodeScannerType) {
        if (e instanceof ChecksumException) {
            CHECKSUM_ERROR_COUNT[barcodeScannerType]++;
        } else if (e instanceof FormatException) {
            FORMAT_ERROR_COUNT[barcodeScannerType]++;
        } else if (e instanceof NotFoundException) {
            NOT_FOUND_ERROR_COUNT[barcodeScannerType]++;
        } else {
            PREPROCESSOR_ERROR_COUNT[barcodeScannerType]++;
        }
    }*/

    public static void increaseErrorTime(int barcodeScannerType, long time) {
        FAIL_TIME_TOTAL[barcodeScannerType]+=time;
    }

    public static void increaseSuccessTime(int barcodeScannerType, long time) {
        SUCCESS_TIME_TOTAL[barcodeScannerType]+=time;
        SUCCESSFUL_TRIAL_COUNT[barcodeScannerType]++;
    }

    public static void incrementRunCount(){
        TOTAL_TRIALS++;
    }

}
