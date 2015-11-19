package com.nightout.idscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by behnamreyhani-masoleh on 15-10-18.
 */
public class ImageUtils {


    private static final double WIDTH_BUFFER = 0.05;
    private static final double IMAGE_SCALE_FACTOR = 0.50;

    private Context mContext;

    public ImageUtils(Context context) {

        mContext = context;
    }

    public Bitmap bitmapEnhancementPipeline(byte [] data, Rect frame, android.graphics.Point screenRes) {
        Bitmap bm = null;
        try {
            bm = getTestImage();
            //bm = getCroppedBitmapFromData(data, frame, screenRes);

            Mat greyscaledMat = convertMatToGrayScale(bm);

            // Use this image for OCR once text boxes are found
            Mat bw = convertToBinaryAdaptiveThreshold(greyscaledMat, 49, 30, false);
            Mat textBoxes = getTextBoxes(bw);

            bw = rescaleMat(bw, IMAGE_SCALE_FACTOR);

            Core.bitwise_not(bw, bw);

            saveIntermediateInPipelineToFile(bw, "finalBW");
            Bitmap outBM = Bitmap.createBitmap(bw.cols(), bw.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(bw, outBM);
            //getCroppedTextBoxes(greyscaledMat);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bm;
    }

    private Mat filterImage(Mat mat) {
        Mat filtered = new Mat();

        Size size = new Size(2,3);
        Mat rectKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, size);

        Imgproc.morphologyEx(mat, filtered, Imgproc.MORPH_OPEN, rectKernel);
        return filtered;
    }

    private Mat filterGreyscale(Mat grey) {
        for (int i = 3; i<= 9; i+=3) {
            Mat filteredEllipse = new Mat();
            Mat filteredRect = new Mat();
            Mat filteredCross = new Mat();

            Mat kernelElipse = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(i, i));
            Mat kernelRect = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(i, i));
            Mat kernelCross = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_CROSS, new Size(i, i));

            Imgproc.morphologyEx(grey, filteredEllipse, Imgproc.MORPH_CLOSE, kernelElipse);
            Imgproc.morphologyEx(grey, filteredRect, Imgproc.MORPH_CLOSE, kernelRect);
            Imgproc.morphologyEx(grey, filteredCross, Imgproc.MORPH_CLOSE, kernelCross);

            saveIntermediateInPipelineToFile(filteredEllipse, "FilteredEllipse" + i);
            saveIntermediateInPipelineToFile(filteredRect, "FilteredRect" + i);
            saveIntermediateInPipelineToFile(filteredCross,"FilteredCross"+i);

            Mat bw = convertToBinaryAdaptiveThreshold(filteredEllipse, 49, 30, false);
            saveIntermediateInPipelineToFile(bw, "BWElipse" + i);

            bw = convertToBinaryAdaptiveThreshold(filteredRect, 49, 30, false);
            saveIntermediateInPipelineToFile(bw, "BWRect" + i);

            bw = convertToBinaryAdaptiveThreshold(filteredCross, 49, 30, false);
            saveIntermediateInPipelineToFile(bw, "BWCross" + i);

        }
        return grey;
    }

    private Mat getTextBoxes(Mat bwMat) {
        long first = System.currentTimeMillis();

        // First filter image to some extent to get rid of noise

        Mat bwFiltered = filterImage(bwMat);

        // Run Stackoverflow algorithm to find text boxes by 'bleaching' white pixels
        Mat sobel = new Mat();
        Imgproc.Sobel(bwFiltered, sobel, CvType.CV_8U, 1, 0, 3, 1, 0, Core.BORDER_DEFAULT);
        saveIntermediateInPipelineToFile(sobel, "SobelInt");
        Mat threshold = new Mat();
        Imgproc.threshold(sobel, threshold, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
        saveIntermediateInPipelineToFile(threshold, "ThresholdInt");
        Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(17, 3));
        Imgproc.morphologyEx(threshold, threshold, Imgproc.MORPH_CLOSE, element);
        saveIntermediateInPipelineToFile(threshold, "ClosedInt");
        Log.d("Benji", "Time for getting text boxes in ms: " + (System.currentTimeMillis() - first));
        return sobel;

    }

    private Bitmap getTestImage() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        File photoPath = new File(getExternalAlbumStorageDir("nightout"), "TestPic.png");
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath.getPath(), options);
        return bitmap;
    }

    private double calculateSkewAngle(Mat greyScaledMat) {
        Mat whiteOnBlackMat = convertToBinaryAdaptiveThreshold(greyScaledMat, 161, 38, false);
        Mat lines = new Mat();
        double angle = 0;
        Imgproc.HoughLinesP(whiteOnBlackMat, lines, 1, Math.PI / 180, 100, whiteOnBlackMat.width() / 2.5, 60);
        if (!lines.empty()) {
            for (int i = 0; i < lines.rows(); i++) {
                double [] linePoints = lines.get(i,0);
                angle += Math.atan2(linePoints[3] - linePoints[1], linePoints[2] - linePoints[0]);
            }
        }
        return angle/lines.rows();
    }

    private void testBWGaussianParams(final Mat org, boolean blackOnWhite) {
        for (int mean = 34; mean<=44; mean+=2) {
            for (int blockSize = 151; blockSize <= 171; blockSize += 2) {
                Mat blackAndWhiteMat = convertToBinaryAdaptiveThreshold(org, blockSize, mean, blackOnWhite);
                saveIntermediateInPipelineToFile(blackAndWhiteMat, "bw-" + blockSize + ":" + mean);
            }
        }
        Log.d("Benji", "Finished Gaussian adaptive threshold");
    }


    private Mat convertMatToGrayScale(Bitmap org) {
        long first = System.currentTimeMillis();
        Mat grayScaled = new Mat();
        Mat orgMat = new Mat();
        Utils.bitmapToMat(org, orgMat);
        Imgproc.cvtColor(orgMat, grayScaled, Imgproc.COLOR_BGR2GRAY);
        Log.d("Benji", "Time for grayscaling in ms: " + (System.currentTimeMillis() - first));
        return grayScaled;
    }

    private Mat rescaleMat(Mat org, double scaleFactor) {
        long first = System.currentTimeMillis();
        Mat outMat = new Mat();
        Imgproc.resize(org, outMat, new Size(), scaleFactor, scaleFactor, Imgproc.INTER_LINEAR);
        Log.d("Benji", "Time for rescaling in ms: " + (System.currentTimeMillis() - first));
        return outMat;
    }

    private Bitmap rescaleBitmap(Bitmap org, double scaleFactor) {
        long start = System.currentTimeMillis();
        Mat mat = new Mat(org.getWidth(), org.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(org, mat);
        Log.d("Benji", "Time for bitmap to Mat conversion in ms: " + (System.currentTimeMillis() - start));
        Mat outMat = new Mat();
        start = System.currentTimeMillis();
        Imgproc.resize(mat, outMat, new Size(), scaleFactor, scaleFactor, Imgproc.INTER_LINEAR);
        Log.d("Benji", "Time for opencv rescale in ms: " + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        Bitmap outBM = Bitmap.createBitmap(outMat.cols(), outMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(outMat, outBM);
        Log.d("Benji", "Time for mat to bitmap conversion in ms: " + (System.currentTimeMillis() - start));
        return outBM;
    }

    private Mat convertToBinaryAdaptiveThreshold(Mat greyscaledMat, int blockSize, int i, boolean isBlackOnWhite) {
        long first = System.currentTimeMillis();
        Mat binaryMat = new Mat();
        Imgproc.adaptiveThreshold(greyscaledMat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                isBlackOnWhite ? Imgproc.THRESH_BINARY : Imgproc.THRESH_BINARY_INV, blockSize, i);
        Log.d("Benji", "Time for bwing in ms: " + (System.currentTimeMillis() - first));
        return binaryMat;
    }

    private Bitmap getCroppedBitmapFromData(byte [] data, Rect frame, android.graphics.Point screenRes) throws Exception {
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

    private void saveIntermediateInPipelineToFile(Bitmap intermediate, String intermediateType) {
        saveBitmapToFile(intermediate, new File(getExternalAlbumStorageDir("nightout"),intermediateType + ".png"));
    }

    private void saveIntermediateInPipelineToFile(Mat intermediate, String intermediateType) {
        Bitmap outBM = Bitmap.createBitmap(intermediate.cols(), intermediate.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(intermediate, outBM);
        saveIntermediateInPipelineToFile(outBM, intermediateType);

    }

    private File getExternalPicTestFile() {
        return new File(getExternalAlbumStorageDir("nightout"),"openCVGrayScale.png");
    }

}
