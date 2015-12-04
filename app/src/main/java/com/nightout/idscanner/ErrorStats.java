package com.nightout.idscanner;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;

/**
 * Created by behnamreyhani-masoleh on 15-12-03.
 */
public class ErrorStats {
    private static int CHECKSUM_EXCEPTION_COUNT;
    private static int FORMAT_EXCEPTION_COUNT;
    private static int NOT_FOUND_EXCEPTION_COUNT;
    private static int NOT_ZXING_EXCEPTION_COUNT;

    public static String incrementExceptionCountAndGetFileName(Exception e) {
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
}
