package com.nightout.idscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

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
    private static final double WHITE_PIXEL_THRESHOLD = 0.10;
    private static final double WHITE_PIXEL_THRESHOLD_TWO = 0.15;

    private Context mContext;

    public ImageUtils(Context context) {

        mContext = context;
    }

    public Bitmap bitmapEnhancementPipeline(byte [] data, Rect frame, android.graphics.Point screenRes) {
        Bitmap bm = null;
        try {
            long start = System.currentTimeMillis();
            bm = getTestImage();
            //bm = getCroppedBitmapFromData(data, frame, screenRes);
            long test = System.currentTimeMillis();
            Mat greyscaledMat = convertMatToGrayScale(bm);
            Log.d("Faggot","Time for greyscale in ms: " + (System.currentTimeMillis() - test));
            // Use this image for OCR once text boxes are found
            //Mat bw = convertToBinaryAdaptiveThreshold(greyscaledMat, 49, 30, false);

            Mat bw = new Mat();
            test = System.currentTimeMillis();
            bw  = convertToBinaryAdaptiveThreshold(greyscaledMat, 39, 30, false);
            Log.d("Faggot","Time for bw in ms: " + (System.currentTimeMillis() - test));
            test = System.currentTimeMillis();
            Mat blurred = filterImage(bw);
            Log.d("Faggot","Time for bw in ms: " + (System.currentTimeMillis() - test));
            //drawBoxes(findTextBoxes(blurred, bw), greyscaledMat, "Kir30");
            findTextBoxes(blurred, bw);
            //FOR TESTING TextBox recognition with different params
            /*for (int i = 15; i<61;i+=2) {
                bw  = convertToBinaryAdaptiveThreshold(greyscaledMat, i,30, false );
                Mat blurred = filterImage(bw);
                drawBoxes(findTextBoxes(blurred), greyscaledMat,"BoxPic-"+i+"-"+30);
            }*/


            bw = rescaleMat(bw, IMAGE_SCALE_FACTOR);

            Core.bitwise_not(bw, bw);

          //  saveIntermediateInPipelineToFile(bw, "finalBW");
            Bitmap outBM = Bitmap.createBitmap(bw.cols(), bw.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(bw, outBM);
            Log.d("Faggot","Time in ms: " + (System.currentTimeMillis() - start));
            //getCroppedTextBoxes(greyscaledMat);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bm;
    }

    private void drawBoxes(List<MatOfPoint> contours, Mat org, String name) {
        Mat temp = org;
        for (int i = 0; i<contours.size(); i++) {
            MatOfPoint point = contours.get(i);
            org.opencv.core.Rect rect = Imgproc.boundingRect(point);
            if (isTextRect(rect)) {
                // TODO: Need to filter rectangles based on white pixel density
                Imgproc.rectangle(temp, rect.br(), rect.tl(), new Scalar(0, 0,0),10);
            }
        }
    }

    //TODO : need better conditions, look at starckoverflow example regarding text pixel density within rectangle
    private boolean isTextRect(org.opencv.core.Rect rect) {
        // TODO: change height and width params to get rid of random shit noise.
        return (rect.width > rect.height) && rect.height > 10 && rect.width > 50;
    }

    private List<MatOfPoint> findTextBoxes(Mat blurredMat, Mat org) {
        long start = System.currentTimeMillis();
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(blurredMat, contours, hierarchy, Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);
        Log.d("Faggot", "Time for finding contours in ms: " + (System.currentTimeMillis() - start));
        Mat mask = Mat.zeros(blurredMat.size(), CvType.CV_8UC1);
        double firstLoop = 0;
        double secondLoop = 0;
        for (int idx = 0; idx>=0; idx =(int) hierarchy.get(0, idx)[0]) {
            org.opencv.core.Rect rect = Imgproc.boundingRect(contours.get(idx));
            if (isTextRect(rect)) {
                start = System.currentTimeMillis();
                Mat cropped = new Mat(org, rect);
                double r = Core.countNonZero(cropped)/rect.area();
                firstLoop += System.currentTimeMillis() - start;
                if (r >= WHITE_PIXEL_THRESHOLD) {
                    start = System.currentTimeMillis();
                    Mat maskROI = new Mat(mask, rect);
                    maskROI.setTo(new Scalar(0, 0, 0));
                    Imgproc.drawContours(mask, contours, idx, new Scalar(255, 255, 255), 5);
                    r = (double)Core.countNonZero(maskROI) / rect.area();
                    secondLoop += System.currentTimeMillis() - start ;
                    if (r >= WHITE_PIXEL_THRESHOLD_TWO) {
                    }
                }
            }
        }
        Log.d("Faggot", "Time for first filter in ms: " + firstLoop);
        Log.d("Faggot","Time for second filter in ms: " + secondLoop);
        return contours;
    }

    private String showArray(double [] array) {
        String out = "[";
        for (int i = 0; i<array.length; i++) {
            out+=array[i]+",";
        }
        out+="]";
        return out;
    }

    private Mat filterImage(Mat mat) {

        // First get rid of background noise pixels

        Mat filtered = new Mat();
        Mat rectKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(1,7));
        Imgproc.morphologyEx(mat, filtered, Imgproc.MORPH_OPEN, rectKernel);

        // Close white image pixels to get white boxes
        Mat closeKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(90, 10));
        Imgproc.morphologyEx(filtered, filtered, Imgproc.MORPH_CLOSE, closeKernel);
        //saveIntermediateInPipelineToFile(filtered, "ClosedInt");
        return filtered;
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
        Mat outMat = new Mat();
        Imgproc.resize(org, outMat, new Size(), scaleFactor, scaleFactor, Imgproc.INTER_LINEAR);
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
        Mat binaryMat = new Mat();
        Imgproc.adaptiveThreshold(greyscaledMat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                isBlackOnWhite ? Imgproc.THRESH_BINARY : Imgproc.THRESH_BINARY_INV, blockSize, i);
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
