package de.hs_esslingen.recognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.support.constraint.solver.widgets.Snapshot;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Steven on 14.09.2017.
 */

public class CompareMats implements Runnable {
    Mat matimg, mattempl; int match_method;


    CompareMats(Mat newImage, Mat oldImage, int match_method) {
        this.matimg = newImage;
        this.mattempl = oldImage;
        this.match_method = match_method;
    }

    public void run() {
        Rect roi = new Rect(50, 10, 70, 100);
        Mat cropped = new Mat(mattempl, roi);
        int result_cols = this.matimg.cols() - cropped.cols() + 1;
        int result_rows = this.matimg.rows() - cropped.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
        Imgproc.matchTemplate(matimg, cropped, result, match_method);
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        Log.e("comeon: ",result.toString());
        Point matchLoc;
        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = mmr.minLoc;
        } else {
            matchLoc = mmr.maxLoc;
        }
        Mat temp = matimg;
        Imgproc.rectangle(matimg, matchLoc, new Point(matchLoc.x + cropped.cols(),
                matchLoc.y + cropped.rows()), new Scalar(0, 255, 0));
        Bitmap bitmap = Bitmap.createBitmap(temp.cols(), temp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(temp, bitmap);
        MainActivity.mFound.setImageBitmap(bitmap);
        Log.i("test", String.valueOf(mmr.maxLoc));
        Log.i("test", String.valueOf(mmr.minLoc));
    }
}

