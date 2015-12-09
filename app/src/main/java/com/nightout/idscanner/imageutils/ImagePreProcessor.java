package com.nightout.idscanner.imageutils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by behnamreyhani-masoleh on 15-10-18.
 */
// Takes care of OCR image pre-processing using the opencv library
public class ImagePreProcessor {


    private static final double IMAGE_SCALE_FACTOR = 0.50;
    public static final int WIDTH_BUFFER_RATIO = 14;
    public static final int HEIGHT_BUFFER_RATIO = 14;
    private static final int TEXT_BOX_CROP_PIXEL_BUFFER = 20;
    private static final double WHITE_PIXEL_THRESHOLD = 0.15;
    private static final double WHITE_PIXEL_THRESHOLD_TWO = 0.15;

    private Context mContext;

    public ImagePreProcessor(Context context) {
        mContext = context;
    }

    public Bitmap preProcessImageForPDF417(byte [] data, Rect frame, Point screenRes) {
        Bitmap bm = null;
        try {
            //TODO: have to add anti-blurring/noise
            bm = getCroppedBitmapFromData(data, frame, screenRes);
//            bm = getTestImage(false);
            saveIntermediateInPipelineToFile(bm,"Test");
            Mat greyscaledMat = convertMatToGrayScale(bm);

            Mat blurredAdaptive = getBlurredBWUsingAdaptive(greyscaledMat);
            Bitmap tmp = bm;

            bm = cropForPDF417(blurredAdaptive, bm);

            if (bm == null) {
                saveIntermediateInPipelineToFile(bm, "Error");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bm;
    }


    public List<Bitmap> preProcessImageForOCR(byte [] data, Rect frame, Point screenRes) {
        Bitmap bm;
        List<Bitmap> textBoxList = null;
        try {
            bm = getCroppedBitmapFromData(data, frame, screenRes);
          //  bm = getTestImage(true);

            Mat greyscaledMat = convertMatToGrayScale(bm);

            Mat bwForOCR  = convertToBinaryAdaptiveThreshold(greyscaledMat, 21, 20, true);

            // Pre-processing for text box recognition
            Mat bwForTextBoxRecognition = getBlurredBWUsingAdaptive(greyscaledMat);
            //Mat bwForTextBoxRecognition = getBlurredBWUsingCannyEdge(greyscaledMat);

            textBoxList = findTextBoxes(bwForTextBoxRecognition, bwForOCR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textBoxList;
    }

    private boolean isTextRect(org.opencv.core.Rect rect, Mat org) {
        return (rect.width > rect.height) && rect.height > 10 && rect.width > 50
                && rect.height < org.height()/1.5 && rect.width < org.width()/1.5;
    }

    private boolean canBePDF417Rect(org.opencv.core.Rect rect, Mat org) {
        return (rect.width > 3*rect.height) && (rect.width >= 0.6*org.width());
    }

    private Mat getBlurredBWUsingAdaptive(Mat grey) {
        Mat bw = convertToBinaryAdaptiveThreshold(grey, 13, 10, false);
        return blurImageForTextBoxRecognition(bw);
    }

    private Mat getBlurredBWUsingCannyEdge(Mat grey) {
        Mat filtered = new Mat();
        //Imgproc.blur(grey, filtered, new Size(3, 3));
        Imgproc.Canny(grey, filtered, 100, 300);
        saveIntermediateInPipelineToFile(filtered, "Canny");
        return blurImageForTextBoxRecognition(filtered);
    }

    private Bitmap cropForPDF417(Mat blurredMat, Bitmap bm) {
        Mat org = new Mat();
        Utils.bitmapToMat(bm, org);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(blurredMat, contours, hierarchy, Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);

        // Indicates that no barcode was found in pre-processing, send message of error back early
        if (hierarchy.empty()) {
            return null;
        }

        org.opencv.core.Rect pdfRect = null;
        for (int idx = 0; idx>=0; idx = (int) hierarchy.get(0,idx)[0]) {
            org.opencv.core.Rect rect = Imgproc.boundingRect(contours.get(idx));
            // assuming the biggest rect in the image is the pdf417 one and getting it
            if (canBePDF417Rect(rect,org)) {
                // for testing only
                if (pdfRect == null || pdfRect.area() < rect.area()) {
                    pdfRect = rect;
                }
            }
        }

        if (pdfRect == null) {
            Log.d("Faggot","No rectangles found for pdf");
            return null;
        }

        return getBitmapFromOpenCVRect(org, pdfRect);
    }

    private Bitmap getBitmapFromOpenCVRect(Mat org, org.opencv.core.Rect rect) {
        Mat buffered = addBufferToTextBoxMat(org, rect);
        Bitmap outBM = Bitmap.createBitmap(buffered.cols(), buffered.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(buffered, outBM);
        return outBM;
    }

    private List<Bitmap> findTextBoxes(Mat blurredMat, Mat org) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(blurredMat, contours, hierarchy, Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);
        Mat mask = Mat.zeros(blurredMat.size(), CvType.CV_8UC1);

        List<Bitmap> textBoxes = new ArrayList<>();
        // For testing boxes found
        Mat tmp = new Mat();
        org.copyTo(tmp);
        int count = 0;
        int countH = 0;
        for (int idx = 0; idx>=0; idx =(int) hierarchy.get(0, idx)[0]) {
            countH++;
            org.opencv.core.Rect rect = Imgproc.boundingRect(contours.get(idx));
            if (isTextRect(rect, org)) {
                Mat cropped = new Mat(org, rect);
                double r1 = Core.countNonZero(cropped)/rect.area();
                Log.d("Faggot", "first ratio for index #" + idx + ": " + r1);
                if (r1 >= WHITE_PIXEL_THRESHOLD) {
                    Mat maskROI = new Mat(mask, rect);
                    maskROI.setTo(new Scalar(0, 0, 0));
                    Imgproc.drawContours(mask, contours, idx, new Scalar(255, 255, 255), 5);
                    double r = (double)Core.countNonZero(maskROI) / rect.area();
                    Log.d("Faggot", "second ratio for index #" + idx + ": " + r+"\n");
                    if (r >= WHITE_PIXEL_THRESHOLD_TWO) {
                        Mat bufferedMat = addBufferToTextBoxMat(org, rect);

                        // TODO: see which order is faster
                        saveIntermediateInPipelineToFile(bufferedMat, "TextBox-" + idx);
                        //  rescaleMat(bufferedMat, IMAGE_SCALE_FACTOR);
                        Imgproc.rectangle(tmp, rect.br(), rect.tl(), new Scalar(0, 0, 0), 10);

                        Bitmap outBM = Bitmap.createBitmap(bufferedMat.cols(), bufferedMat.rows(),
                                Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(bufferedMat, outBM);
                        textBoxes.add(outBM);
                        count++;
                    }
                }
            }
        }
        saveIntermediateInPipelineToFile(tmp, "Boxed");
        Log.d("Faggot", "The number of text boxes detected: " + count);
        Log.d("Faggot", "The number of boxes detected: " + countH);

        return textBoxes;
    }

    private Mat addBufferToTextBoxMat(Mat org, org.opencv.core.Rect rect) {
        Mat shallowCopy = new Mat();
        org.copyTo(shallowCopy);

        Mat originalBox = new Mat(shallowCopy, rect);
        try {
            rect.x-=TEXT_BOX_CROP_PIXEL_BUFFER;
            rect.y-=TEXT_BOX_CROP_PIXEL_BUFFER;
            rect.width+=2*TEXT_BOX_CROP_PIXEL_BUFFER;
            rect.height+=2*TEXT_BOX_CROP_PIXEL_BUFFER;
            return new Mat(shallowCopy, rect);
        } catch (Exception e) {
            return originalBox;
        }
    }

    private Mat blurImageForTextBoxRecognition(Mat mat) {
        Mat blurred = new Mat();
        mat.copyTo(blurred);

        //TODO: Play around with the params for opening and closing, only for recognizing text boxes **
        // First get rid of background noise pixels
        Mat rectKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(1, 11));
        Imgproc.morphologyEx(blurred, blurred, Imgproc.MORPH_OPEN, rectKernel);
//        saveIntermediateInPipelineToFile(blurred,"Opened");

        // Close white image pixels to get white boxes
        Mat closeKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(80, 1));
        Imgproc.morphologyEx(blurred, blurred, Imgproc.MORPH_CLOSE, closeKernel);
       // saveIntermediateInPipelineToFile(blurred, "Closed");
        return blurred;
    }

    private Bitmap getTestImage(boolean isForOCR) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        File photoPath = new File(getExternalAlbumStorageDir("nightout"), isForOCR ? "Test.png" : "PDFTest.png");
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath.getPath(), options);
        return bitmap;
    }

    private double calculateSkewAngle(Mat bw) {
        Mat lines = new Mat();
        double angle = 0;
        Imgproc.HoughLinesP(bw, lines, 1, Math.PI / 180, 100, bw.width() / 2.5, 60);
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
    }


    private Mat convertMatToGrayScale(Bitmap org) {
        Mat grayScaled = new Mat();
        Mat orgMat = new Mat();
        Utils.bitmapToMat(org, orgMat);
        Imgproc.cvtColor(orgMat, grayScaled, Imgproc.COLOR_BGR2GRAY);
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

    private Bitmap getCroppedBitmapFromData(byte [] data, Rect frame, Point screenRes) throws Exception {
        BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, false);
        int height = decoder.getHeight();
        int width = decoder.getWidth();

        double heightBuffer = (double) screenRes.y/HEIGHT_BUFFER_RATIO;
        double widthBuffer = (double) screenRes.x/WIDTH_BUFFER_RATIO;

        Double left = ((double)frame.left/screenRes.x)*width - widthBuffer;
        Double top =  ((double)frame.top/screenRes.y)*height - heightBuffer;
        Double right = ((double)frame.right/screenRes.x)*width + widthBuffer;
        Double bottom = ((double)frame.bottom/screenRes.y)*height + heightBuffer;

        return decoder.decodeRegion(new Rect(left.intValue(), top.intValue(),
                right.intValue(), bottom.intValue()), null);
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

    private void saveIntermediateInPipelineToFile(Bitmap intermediate, String fileName) {
        saveBitmapToFile(intermediate, new File(getExternalAlbumStorageDir("nightout"), fileName + ".png"));
    }

    private void saveIntermediateInPipelineToFile(Mat intermediate, String fileName) {
        Bitmap outBM = Bitmap.createBitmap(intermediate.cols(), intermediate.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(intermediate, outBM);
        saveIntermediateInPipelineToFile(outBM, fileName);

    }

    public void saveErrorImage(Bitmap scannedBarcode, String fileName) {
        saveBitmapToFile(scannedBarcode, new File(getExternalAlbumStorageDir("nightout/Error/"
                + getCorrectDirectoryFromFileName(fileName)), fileName + ".png"));
    }

    private String getCorrectDirectoryFromFileName(String fileName) {
        String subDir = "Normal";
        if (fileName.contains("CheckSum")) {
            subDir = "CheckSum";
        } else if (fileName.contains("Format")) {
            subDir = "Format";
        } else if (fileName.contains("NotFound")) {
            subDir = "NotFound";
        }
        return subDir;
    }

}
