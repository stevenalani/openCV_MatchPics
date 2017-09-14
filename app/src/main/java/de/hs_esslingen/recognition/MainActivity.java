package de.hs_esslingen.recognition;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements CvCameraViewListener2, Button.OnClickListener {
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
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
        Mat tmp = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_8UC1, new Scalar(4));
        Imgproc.pyrDown(mRgba, tmp);
        Imgproc.pyrDown(tmp, tmp);
        Bitmap bitmap = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tmp, bitmap);
        this.mFrameBmp = bitmap;
        this.mFrameMat = tmp;
        if(mImgMat != null){
            mFound.post(new CompareMats(tmp, this.mImgMat, Imgproc.TM_SQDIFF));
        }
        return mRgba;
    }

    @Override
    public void onClick(View v) {
        this.mImgBmp = this.mFrameBmp;
        this.mImgMat = this.mFrameMat;
        this.mImgView.setImageBitmap(this.mImgBmp );
    }
}
