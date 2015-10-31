package com.nightout.idscanner;

import android.content.Context;

import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Created by behnamreyhani-masoleh on 15-10-18.
 */
public class TessUtils {

    private Context mContext;
    private TessBaseAPI mTessAPI;

    private static final String TESS_LANGUAGE_FILE = "eng.traineddata";
    private static final String TESS_DATA_SUBDIRECTORY = "tessdata";

    public TessUtils(Context context) {
        mContext = context;
        mTessAPI = new TessBaseAPI();
        configureAPIForOCR();
        // TODO: implement copyTessLanguageFile code...right now assuming language file is on users sdcard already
        /*
        if (!fileAlreadyExists()) {
            copyTessLanguageFile();
        }*/
    }

    private void configureAPIForOCR() {
        //setOCRBounds(frame);
        mTessAPI.init("/sdcard/", "eng");
        mTessAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT_OSD);
        mTessAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "`~!@#$%^&*_+=|\\}]{['\";:?>.<,");
        mTessAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,
                "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-/()");
    }

    /*
    private boolean fileAlreadyExists() {
        return (new File(getTessLanguageDirectoryPath(), TESS_LANGUAGE_FILE)).exists();
    }

    private String getTessLanguageDirectoryPath () {
        return mContext.getExternalFilesDir(null).getPath() + File.separator + TESS_DATA_SUBDIRECTORY + File.separator;
    }

    private void copyInputToOutputStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[5120];
        int length = inputStream.read(buffer);
        while (length > 0) {
            outputStream.write(buffer, 0, length);
            length = inputStream.read(buffer);
        }
    }

    public String applyOCR(Bitmap image) {
        configureAPIForOCR();
        mTessAPI.setImage(image);
        String text = mTessAPI.getUTF8Text();
        mTessAPI.end();
        return text;
    }

    public String testOCR(File file) {
        configureAPIForOCR();
        mTessAPI.setImage(file);
        String text = mTessAPI.getUTF8Text();
        mTessAPI.end();
        return text;
    }

    // Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    // Checks if external storage is available to at least read
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private void copyTessLanguageFile() {

        try {
            // Create file and directories
            File internalMemFile = new File(getTessLanguageDirectoryPath(), TESS_LANGUAGE_FILE);
            internalMemFile.mkdirs();
            internalMemFile.createNewFile();

            InputStream inputStream = mContext.getAssets().open(TESS_LANGUAGE_FILE);
            OutputStream outputStream = new FileOutputStream(internalMemFile);
            copyInputToOutputStream(inputStream, outputStream);
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

}
