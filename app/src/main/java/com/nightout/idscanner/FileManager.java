package com.nightout.idscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by behnamreyhani-masoleh on 15-12-17.
 */
public class FileManager {
    private Context mContext;

    public FileManager(Context context) {
        mContext = context;
    }

    private static File getExternalAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        file.mkdirs();
        return file;
    }

    private static void saveBitmapToFile(Bitmap bitmap, File destFile) {
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

    //Used to save images that we're modified by image processing techniques to file; mostly for testing
    public static void savePicToExternalDirectory(Bitmap bitmap, String fileName) {
        saveBitmapToFile(bitmap, new File(getExternalAlbumStorageDir("nightout"), fileName + ".png"));
    }

    public static void savePicToExternalDirectory(Mat mat, String fileName) {
        Bitmap outBM = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, outBM);
        savePicToExternalDirectory(outBM, fileName);
    }

    public File getStorageDirectory() throws Exception {
        File temp = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            temp = mContext.getExternalFilesDir(Environment.MEDIA_MOUNTED);
        }

        if (temp == null) {
            throw new Exception();
        }

        return temp;
    }

    /* With specific feedback to the user, not using
    private File getStorageDirectory() {
        String state = null;
        File temp = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (RuntimeException e) {
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            try {
                temp = mContext.getExternalFilesDir(Environment.MEDIA_MOUNTED);
            } catch (NullPointerException e) {
                // We get an error here if the SD card is visible, but full
                showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
            }
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            // to know is we can neither read nor write
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable or corrupted.");
        }
        return temp;
    }
    */
}
