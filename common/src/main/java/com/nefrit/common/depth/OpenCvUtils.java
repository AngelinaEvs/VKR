package com.nefrit.common.depth;

import android.graphics.Bitmap;
import android.media.Image;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class OpenCvUtils {

    public static Image findContour(Image image) {
        Mat img = Imgcodecs.imread(image);
        Mat hsvImg = new Mat();
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV);

        Scalar lower = new Scalar(170, 255, 255);
        Scalar upper = new Scalar(10, 0, 0);

        Core.inRange(img, lower, upper, hsvImg);

        return Bitmap(hsvImg);
    }
}