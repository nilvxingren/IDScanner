package com.nightout.idscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by behnamreyhani-masoleh on 15-10-18.
 */
public class ImageUtils {

    private static String NO_IMG_IDENTIFIER = "NIGHTOUT_IMG";

    //TODO: test with diff valeus which one is more efficient and get accuracy numbers, and speed
    /*private BitmapFactory.Options getHighQualityOptions() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inDensity = 290;
        opts.inTargetDensity = 300;
        opts.inScaled = true;
        return opts;
    }*/

    private Context mContext;

    public ImageUtils(Context context) {
        mContext = context;
    }

    private File getExternalAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        file.mkdirs();
        return file;
    }

    private String getImageTimeStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
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

    public void test(byte [] data, View frame) {
        getCroppedBM(data, frame);
       //saveBitmapToFile(byteDataToBitmap(data), getExternalPicTestFile());
    }

    private Rect createRectFrame(View frame, int widthScale, int heightScale) {
        return new Rect(frame.getLeft() * widthScale, frame.getTop()*heightScale, frame.getRight()*widthScale, frame.getBottom()*heightScale);
    }

    private boolean getCroppedBM(byte [] data, View frame) {
        boolean value = false;
        try {
           BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, false);

            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;

            int widthScale = decoder.getWidth() / width;
            int heightScale = decoder.getHeight() / height;
           Bitmap bm = decoder.decodeRegion(createRectFrame(frame, widthScale, heightScale), null);
            saveBitmapToFile(bm, getExternalPicTestFile());
            value = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
}
