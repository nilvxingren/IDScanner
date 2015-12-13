package com.nightout.idscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.nightout.idscanner.imageutils.pdf417.PDF417DecodeAsyncTask;

/**
 * Created by behnamreyhani-masoleh on 15-12-03.
 */
public class TestStats extends BroadcastReceiver {
    private static final String DUMP_ACTION = "DUMP_XZING_STATS";
    private static final String RESET_ACTION = "RESET_XZING_STATS";

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
        String action = intent.getAction();
        Log.d("Faggot","Broadcast received w action: " + action);
        switch (action){
            case DUMP_ACTION:
                displayXzingTestResults();
                break;
            case RESET_ACTION:
                resetStats();
                break;
        }
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

    private static void displayXzingTestResults(){
        String testResults = "Type                        |     Success Rate     |    Avg Success Decode Time    |    Avg Fail Decode Time    |\n\n ";
        for (int i = 0; i<SUCCESSFUL_TRIAL_COUNT.length; i++) {
            String name = getTestTitle(i);
            if (i!=3) {
                name+="              ";
            }
            double accuracyPercentage = ((double) SUCCESSFUL_TRIAL_COUNT[i] / TOTAL_TRIALS)*100;
            double avgSuccessTime = ((double)SUCCESS_TIME_TOTAL[i]/TOTAL_TRIALS);
            double avgFailTime = ((double)FAIL_TIME_TOTAL[i]/TOTAL_TRIALS);

            testResults+= name + "   |      " + SUCCESSFUL_TRIAL_COUNT[i] + "/" + TOTAL_TRIALS + ", "
                    + accuracyPercentage + "%      |       " + "        " + avgSuccessTime
                         + " ms                |        "  + avgFailTime + " ms      |\n\n";
        }
        Log.d("Xzing Test Results:", testResults);
    }

    public static String getTestTitle(int paramType) {
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

    private static void resetStats(){
        TOTAL_TRIALS = 0;

        for (int i = 0; i < SUCCESSFUL_TRIAL_COUNT.length; i++) {
            SUCCESSFUL_TRIAL_COUNT[i] = 0;
            SUCCESS_TIME_TOTAL[i] = 0;
            FAIL_TIME_TOTAL[i] = 0;
        }
    }

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
