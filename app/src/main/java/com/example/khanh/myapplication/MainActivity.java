package com.example.khanh.myapplication;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier faceDetect, humanDetect;
    private Mat grayscaleImage;
    private int absoluteFaceSize;

    private ImageView imgCrop;
    private Button btnScan, btnRgb, btnGray;
    private boolean bClick = false, bRgb = true, bGray = false;
    private static int indexImg = 0;
    private Spinner spDetect, spColor;
    private ArrayList<String> dsDetect, dsColor;
    private ArrayAdapter<String> adapterDetect, adapterColor;
    private int indexDetect = 0, indexColor = 0;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }

        }
    };

    private void initializeOpenCVDependencies() {
        try{
            // Load file xml FaceDetect
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os  = new FileOutputStream(mCascadeFile);

            byte []buffer = new byte[4096];
            int bytesRead;
            while((bytesRead = is.read(buffer)) != -1){
                os.write(buffer, 0, bytesRead);
            }

            faceDetect = new CascadeClassifier(mCascadeFile.getAbsolutePath());


            // Load file xml HumanDetect
            is = getResources().openRawResource(R.raw.haarcascade_fullbody);
            cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "haarcascade_fullbody.xml");
            os  = new FileOutputStream(mCascadeFile);

            while((bytesRead = is.read(buffer)) != -1){
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();

            humanDetect = new CascadeClassifier(mCascadeFile.getAbsolutePath());

        }
        catch (Exception e){
            Log.e("OpenCvActivity", "Error loading cascade", e);
        }

        openCvCameraView.enableView();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        setContentView(openCvCameraView)

        addControls();

        addEvents();

    }

    private void addControls() {
        openCvCameraView = new JavaCameraView(this, -1);
        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);

        imgCrop = (ImageView) findViewById(R.id.imgCrop);
        btnScan = (Button) findViewById(R.id.btnCrop);
//        btnGray = (Button) findViewById(R.id.btnGray);
//        btnRgb = (Button) findViewById(R.id.btnRgb);

        // spinner detect: cac che do detect
        spDetect = (Spinner) findViewById(R.id.spDetect);
        dsDetect = new ArrayList<>();
        dsDetect.add("Faces");
        dsDetect.add("Humans");
        adapterDetect = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, dsDetect);
        adapterDetect.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDetect.setAdapter(adapterDetect);

        // spinner color: cac che do mau
        spColor = (Spinner) findViewById(R.id.spColor);
        dsColor = new ArrayList<>();
        dsColor.add("RGB");
        dsColor.add("Gray");
        dsColor.add("Canny");
        adapterColor = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, dsColor);
        adapterColor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spColor.setAdapter(adapterColor);

    }

    private void addEvents(){

        openCvCameraView.setCvCameraViewListener(this);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bClick = true;
            }
        });

        spDetect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                indexDetect = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                indexColor = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

//        btnRgb.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                bRgb = true;
//                bGray = false;
//            }
//        });
//
//        btnGray.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                bGray = true;
//                bRgb = false;
//            }
//        });
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);
        absoluteFaceSize = (int) (height * 0.2);

    }

    @Override
    public void onCameraViewStopped() {
        grayscaleImage.release();
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {

        Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.CV_RGBA2mRGBA);

//        if(bRgb == true) {
//            Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.CV_RGBA2mRGBA);
//        }
//
//        if(bGray == true) {
//            Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.COLOR_RGB2GRAY);
//            inputFrame = grayscaleImage;
//        }

        switch (indexColor){
            case 0:{
                Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.CV_RGBA2mRGBA);
                break;
            }
            case 1:{
                Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.COLOR_RGB2GRAY);
                inputFrame = grayscaleImage;
                break;
            }
            case 2:{
                Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.COLOR_RGB2GRAY);
//              // nguong
                Imgproc.Canny(grayscaleImage, inputFrame, 50, 150);
                break;
            }
            default: {
                Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.CV_RGBA2mRGBA);
                break;
            }
        }

        MatOfRect faceshumans = new MatOfRect();

        switch (indexDetect){
            case 0:{
                if(faceDetect != null){
                    faceDetect.detectMultiScale(grayscaleImage, faceshumans, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
                }
                break;
            }
            case 1:{
                if(faceDetect != null){
                    humanDetect.detectMultiScale(grayscaleImage, faceshumans, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
                }
                break;
            }
            default:{
                if(faceDetect != null){
                    faceDetect.detectMultiScale(grayscaleImage, faceshumans, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
                }
                break;
            }
        }

        // Nhan dien khuon mat
//        if(faceDetect != null){
//            faceDetect.detectMultiScale(grayscaleImage, faceshumans, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
//        }

//        if(humanDetect != null){
//            humanDetect.detectMultiScale(grayscaleImage, faceshumans, 1.1, 2, 3, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
//        }


        // Lay duong dan thu muc de luu anh
        String filename = Environment.getExternalStorageDirectory().getPath();

        Rect [] faceshumansArray = faceshumans.toArray();
        final int l = faceshumansArray.length;
        final Mat tmpInputFrame = inputFrame;

        if(l == 0)
            bClick = false;

        for(int i = 0; i < faceshumansArray.length; i++){
            switch (indexDetect){
                case 0:{
                    Core.rectangle(inputFrame, faceshumansArray[i].tl(), faceshumansArray[i].br(), new Scalar(0, 255, 0, 255),1);
                    break;
                }
                case 1:{
                    Core.rectangle(inputFrame, new  Point(faceshumansArray[i].x, faceshumansArray[i].y), new  Point(faceshumansArray[i].x + faceshumansArray[i].width, faceshumansArray[i].y + faceshumansArray[i].height), new Scalar(0, 255, 0));
                    break;
                }
                default:{
                    Core.rectangle(inputFrame, faceshumansArray[i].tl(), faceshumansArray[i].br(), new Scalar(0, 255, 0, 255),1);
                    break;
                }
            }
//            Core.rectangle(inputFrame, faceshumansArray[i].tl(), faceshumansArray[i].br(), new Scalar(0, 255, 0, 255),1);
//            Core.rectangle(inputFrame, new  Point(faceshumansArray[i].x, faceshumansArray[i].y), new  Point(faceshumansArray[i].x + faceshumansArray[i].width, faceshumansArray[i].y + faceshumansArray[i].height), new Scalar(0, 255, 0));

            if(bClick == true) {

                // Luu anh
                Highgui.imwrite(filename + "/Test/" + Integer.toString(indexImg) + ".jpg", inputFrame.submat(faceshumansArray[i]));
                indexImg++;

            }
        }

        if(bClick == true){
            // Hien thi anh cat (anh cuoi cung)
            final Rect tmpFacesArray = faceshumansArray[l - 1];
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Mat tmp = tmpInputFrame.submat(tmpFacesArray);

                    Bitmap bmp = null;
                    try {

                        bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);

                        Utils.matToBitmap(tmp, bmp);

                        imgCrop.setImageBitmap(bmp);
                    }
                    catch (Exception e){
                        Log.d("MainError", "Loi");
                    }

                    Toast.makeText(MainActivity.this, "Scan " + l + " ảnh", Toast.LENGTH_SHORT).show();
                }
            });
        }

        bClick = false;


        return inputFrame;
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, baseLoaderCallback);
    }

}
