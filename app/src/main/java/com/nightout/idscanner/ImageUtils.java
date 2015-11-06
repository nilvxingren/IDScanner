package com.nightout.idscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by behnamreyhani-masoleh on 15-10-18.
 */
public class ImageUtils {


    private static final double WIDTH_BUFFER = 0.05;
    private static final int BLACK_WHITE_THRESHOLD = 50;
    private Context mContext;

    public ImageUtils(Context context) {

        mContext = context;
    }

    public Bitmap cropAndSaveBitmapTest(byte [] data, Rect frame, Point screenRes) {
        Bitmap bm = null;
        try {
            long start = System.currentTimeMillis();
            bm = getCroppedBitmapFromData(data, frame, screenRes);
            Log.d("Benji","Time for Cropping image in ms: " + (System.currentTimeMillis()-start));
            start = System.currentTimeMillis();
            bm = openCVResize(bm);
           // bm = convertToBlackAndWhite(bm);
            Log.d("Benji","Time for openCV resize in ms: " + (System.currentTimeMillis()-start));
            start = System.currentTimeMillis();
            saveBitmapToFile(bm, getExternalPicTestFile());
            Log.d("Benji", "Time for saving Image in ms:" + (System.currentTimeMillis() - start));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bm;
    }

    private Bitmap openCVResize(Bitmap org) {
        int width = org.getWidth();
        int height = org.getHeight();
        Mat mat = new Mat(width, height, CvType.CV_8UC1);
        Utils.bitmapToMat(org, mat);
        Mat outMat = new Mat();
        Imgproc.resize(mat, outMat, new Size(), 3d, 3d, Imgproc.INTER_LINEAR);
        Bitmap outBM = Bitmap.createBitmap(outMat.cols(), outMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(outMat, outBM);
        return outBM;
    }

    private Bitmap convertToBlackAndWhite(Bitmap org) {
        org = org.copy(Bitmap.Config.ARGB_8888, true);
        for (int i = 0; i < org.getWidth(); i++) {
            for (int j = 0; j < org.getHeight(); j++) {
                int pixel = org.getPixel(i,j);
                int red = Color.red(pixel);
                int blue = Color.blue(pixel);
                int green = Color.green(pixel);
                int alpha = Color.alpha(pixel);
                if ( red > BLACK_WHITE_THRESHOLD || blue > BLACK_WHITE_THRESHOLD || green > BLACK_WHITE_THRESHOLD) {
                    pixel = 255;
                } else {
                    pixel = 0;
                }

                pixel = Color.argb(alpha, pixel, pixel, pixel);
                org.setPixel(i,j,pixel);
            }

        }
        return org;
    }

    private Bitmap getCroppedBitmapFromData(byte [] data, Rect frame,Point screenRes) throws Exception {
        BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, false);
        int height = decoder.getHeight();
        int width = decoder.getWidth();

        int left = frame.left * width / screenRes.x;
        int top = frame.top * height / screenRes.y;
        int right = frame.right * width / screenRes.x;
        int bottom = frame.bottom * height / screenRes.y;

        int widthBuffer = (int)((right - left) * WIDTH_BUFFER);
        return decoder.decodeRegion(new Rect(left - widthBuffer, top,
                        right + widthBuffer, bottom), null);
    }
    // For testing purposes, checking image quality/crop

    //TODO: test with diff valeus which one is more efficient and get accuracy numbers, and speed
    private BitmapFactory.Options getHighQualityOptions() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inDensity = 290;
        opts.inTargetDensity = 300;
        opts.inScaled = true;
        return opts;
    }

     private String getImageTimeStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }
     private File getExternalAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        file.mkdirs();
        return file;
    }

    private void saveBitmapToFile(Bitmap bitmap, File destFile) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(destFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private File getExternalPicTestFile() {
        return new File(getExternalAlbumStorageDir("nightout"),"benji.png");
    }
}
