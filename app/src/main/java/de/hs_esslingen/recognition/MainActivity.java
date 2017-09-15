package de.hs_esslingen.recognition;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends Activity implements CvCameraViewListener2, Button.OnClickListener, TextToSpeech.OnInitListener {
    static {
        System.loadLibrary("native-lib");
    }
    CascadeClassifier cascadeClassifier;

    public static String mResFolder;
    public native String stringFromJNI();
    private static final String TAG = "Recognition ";
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private TextView mTextView = null;
    //Tmp Bitmap
    private Bitmap mBmp = null;
    public static ImageView mFound;
    //Current Image
    private ImageView mImgView = null;
    private Bitmap mImgBmp = null;
    private Mat mImgMat = null;
    //Current Frame
    private Bitmap mFrameBmp = null;
    private Mat mFrameMat = null;
    private Button takeit;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    // Copy the resource into a temp file so OpenCV can load it
                    InputStream is;
                    try {
                        is = getResources().openRawResource(R.raw.haarcascade_frontalcatface);
                        File cascadeDir = getDir("raw", Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, "haarcascade_frontalcatface.xml");
                        mResFolder = mCascadeFile.getAbsolutePath();
                        FileOutputStream os = new FileOutputStream(mCascadeFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        cascadeClassifier = new CascadeClassifier(mResFolder);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        //getResources().openRawResource(R.raw.haarcascade_frontalcatface);
        //this.mResFolder = "raw://" + R.raw.haarcascade_frontalcatface;
        Log.i(TAG,"String: "+mResFolder);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mImgView = (ImageView) findViewById(R.id.imgToFind);
        takeit = (Button) findViewById(R.id.button);
        mFound = (ImageView) findViewById(R.id.imageView);
        takeit.setOnClickListener(this);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat mRgba = inputFrame.rgba();
        /*Mat tmp = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_8UC1, new Scalar(4));
        Imgproc.pyrDown(mRgba, tmp);
        Imgproc.pyrDown(tmp, tmp);
        Bitmap bitmap = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tmp, bitmap);
        this.mFrameBmp = bitmap;
        this.mFrameMat = tmp;
        if(mImgMat != null){
            mFound.post(new CompareMats(tmp, this.mImgMat, Imgproc.TM_SQDIFF));
        }*/
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(mRgba, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);
        MatOfRect faces = new MatOfRect();
        try {
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 2, 2, Objdetect.CASCADE_DO_ROUGH_SEARCH, new Size(30, 48), new Size(300, 480));
        }catch(Exception e){
            Log.e("error tts: ",e.getMessage());
        }
        Rect[] facesArray = faces.toArray();
        Rect roi;
        Mat cropped = null;
        for (int i = 0; i <facesArray.length; i++) {
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), new Scalar(255, 255, 255, 100), 1);
            roi = new Rect((int) facesArray[i].tl().x, (int) facesArray[i].tl().y, (int) facesArray[i].width, (int) facesArray[i].height);
            cropped = new Mat(mRgba, roi);
            final Bitmap bitmapCrop = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped, bitmapCrop);
            final TextToSpeech tts = new TextToSpeech(this, this);
            tts.setLanguage(Locale.GERMAN);
            mImgView.post(new Runnable() {
                @Override
                public void run() {
                    mImgView.setImageBitmap(bitmapCrop);
                    if (!tts.isSpeaking()) {
                        tts.speak("Hii, wie geht es Dir? wer bist Duu?", TextToSpeech.QUEUE_ADD, null);
                    }
                }
            });
        }
        final Bitmap bitmap = Bitmap.createBitmap(grayscaleImage.cols(), grayscaleImage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(grayscaleImage, bitmap);
        mFound.post(new Runnable() {
            @Override
            public void run() {
                mFound.setImageBitmap(bitmap);
            }
        });
        return mRgba;
    }

    @Override
    public void onClick(View v) {
        this.mImgBmp = this.mFrameBmp;
        this.mImgMat = this.mFrameMat;
        this.mImgView.setImageBitmap(this.mImgBmp );
    }

    @Override
    public void onInit(int status) {

    }
}
