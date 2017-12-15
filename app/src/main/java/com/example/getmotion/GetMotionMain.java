package com.example.getmotion;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class GetMotionMain extends AppCompatActivity {

    ///参数部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    public static boolean SYS_STATE = false; //系统状态
    private SensorManager mSensorManager; //设备管理器

    //权限
    private static String[] PERMISSIONS_EVERY = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COURSE_LOCATION",
            "android.hardware.location.gps"
    };

    //控件参数
    private CheckBox checkBox;
    private Button button;
    public TextView textView1, textView2, textView3, textView4;

    //传感器数据、监听器
    private Sensor[] mSensors = new Sensor[3];
    public static final int TYPE_ACC = 0, TYPE_ROT = 1, TYPE_MAG = 2, TYPE_GPS = 3, TYPE_CAM = 4;
    private static final int[] SENS_TYPES = {TYPE_ACC, TYPE_ROT, TYPE_MAG, TYPE_GPS, TYPE_CAM};
    private static final List<String> SENS_NAME = Arrays.asList("Accelerometer", "Gyroscope", "Magnetometer", "GPS", "CAM");
    private SensorEventListenerNew[] listener = new SensorEventListenerNew[3];

    //GPS读取
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    //相机
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private ImageView iv_show;
    private CameraManager mCameraManager;//摄像头管理器
    private Handler childHandler, mainHandler;
    private String mCameraID;//摄像头Id 0 为后  1 为前
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private int Dbg = 0;


    //文件读写
    private File[] sensData = new File[5];
    private File rootFolder, dataRootFolder, dataRootFolderPics; //本项目文件夹，本次记录文件夹
    public FileWriter[] mFileWriter = new FileWriter[5];
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private boolean[] mAvailable = new boolean[5];

    //时间参数
    public String dayTime;
    ///参数部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//


    /**********************************************************************************/
    ///系统回调部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//

    /**********************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_motion_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //只许横屏
        Context mContext = getApplicationContext(); //得到context

        //获取屏幕信息
        int mCurrentOrientation = getResources().getConfiguration().orientation;

        if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i("info", "portrait"); // 竖屏
//            return;

        } else if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i("info", "landscape"); // 横屏
        }

        //屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.
                FLAG_KEEP_SCREEN_ON);   //应用运行时，保持屏幕高亮，不锁屏


        //取得控件
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        button = (Button) findViewById(R.id.button);
        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);


        //定义设备管理器
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        //设备列表，寻找设备
        for (int i = 0; i < deviceSensors.size(); i++) // 设备列表
            Log.e("SENSOR", String.valueOf(deviceSensors.get(i)));
        for (int SENS_TYPE = 0; SENS_TYPE < 3; SENS_TYPE++)
            setSensor(SENS_TYPE);
        Log.i("主线程", String.valueOf(Thread.currentThread().getId()));

        //GPS动态权限
        try {
            //检测是否有写的权限
            int permission3 = ActivityCompat.checkSelfPermission(this,
                    "android.permission.ACCESS_FINE_LOCATION");
            int permission4 = ActivityCompat.checkSelfPermission(this,
                    "android.permission.ACCESS_COARSE_LOCATION");
            int permission5 = ActivityCompat.checkSelfPermission(this,
                    "android.hardware.location.gps");
            if (permission3 != PackageManager.PERMISSION_GRANTED ||
                    permission4 != PackageManager.PERMISSION_GRANTED ||
                    permission5 != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(this, PERMISSIONS_EVERY, 3);
                ActivityCompat.requestPermissions(this, PERMISSIONS_EVERY, 4);
                ActivityCompat.requestPermissions(this, PERMISSIONS_EVERY, 5);
                mAvailable[TYPE_GPS] = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //GPS信号监听
        String locationProvider = LocationManager.GPS_PROVIDER;
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListenerNew();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0, 0, mLocationListener);
        //尝试收听GNSS状态。成功，还可以收听Measurement和Navigation的数据。
        //mLocationManager.registerGnssStatusCallback(new GSCnew());

        //相机设置
        initVIew();

        //查看储存器是否可用，先看外部
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            //获得外存绝对地址+我们自己的地址
            String targetDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "MotionRecord";
            rootFolder = new File(targetDir);
            //创建文件夹
            if (!rootFolder.exists())
                rootFolder.mkdirs();
            Log.e("文件夹", "外部");
        } else {
            //创建内部存储地址
            rootFolder = new File(mContext.getFilesDir(), "MotionRecord");
            if (!rootFolder.exists())
                rootFolder.mkdirs();
            Log.e("文件夹", "内部");
        }
        //权限问题
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(this,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(this, PERMISSIONS_EVERY, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("文件夹", rootFolder.getAbsolutePath());
        Toast.makeText(this, "目标将要保存于：" + rootFolder.getAbsolutePath(), Toast.LENGTH_LONG).show();

    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        Log.e("界面的变化", "会怎么样呢？");
    }

    @Override
    public void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //只许横屏
//        Log.e("重新启动","onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //只许横屏
//        Log.e("暂停程序","onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        stopAll();
    }
    ///系统回调部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//


    /**********************************************************************************/
    ///相机部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//

    /**********************************************************************************/
    private void initVIew() {
        int permission = ActivityCompat.checkSelfPermission(this,
                "android.permission.CAMERA");
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // 没有写的权限，去申请写的权限，会弹出对话框
            ActivityCompat.requestPermissions(this, PERMISSIONS_EVERY, 2);
            mAvailable[TYPE_CAM] = true;
        }
        //mSurfaceView
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        // mSurfaceView添加回调
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) { //SurfaceView创建
                // 初始化Camera
                initCamera2();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                Log.e("数据来了","？");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { //SurfaceView销毁
                // 释放Camera资源
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    GetMotionMain.this.mCameraDevice = null;
                }
            }
        });
    }

    //初始化Camera2
    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCamera2() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());
        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;//后摄像头

        mImageReader = ImageReader.newInstance(480, 640, ImageFormat.JPEG,2);//最后面的10是最大的Buffer数量。
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() { //可以在这里处理拍照得到的临时照片 例如，写入本地
            @Override
            public void onImageAvailable(ImageReader reader) {
//                 拿到拍照照片数据


                if (!SYS_STATE) return;

                Log.e("文件夹在不在", String.valueOf(dataRootFolderPics.exists()));
                if (dataRootFolderPics.exists()) {
                    Image image = reader.acquireNextImage();
                    Log.i("图像时间戳", String.valueOf(image.getTimestamp()));
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);//由缓冲区存入字节数组
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bitmap != null) {
                        File picFile = new File(dataRootFolderPics, String.valueOf(image.getTimestamp() + ".jpg"));
                        try {
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(picFile));
                            bos.write(bytes, 0, bytes.length);
                            bos.flush();
                            bos.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    image.close();
                }




            }
        }, childHandler);

        //获取摄像头管理
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //打开摄像头
            mCameraManager.openCamera(mCameraID, stateCallback, mainHandler);
//            CameraCharacteristics mCC = mCameraManager.getCameraCharacteristics(mCameraID);
//            float[] a = mCC.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    //摄像头创建监听
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {//打开摄像头
            mCameraDevice = camera;
            //开启预览(自己写的函数)
            takePreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {//关闭摄像头
            if (null != mCameraDevice) {
                mCameraDevice.close();
                GetMotionMain.this.mCameraDevice = null;
            }
        }
        @Override
        public void onError(CameraDevice camera, int error) {//发生错误
            Toast.makeText(GetMotionMain.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
        }
    };


    //开始预览
    private void takePreview() {
        try {
            // 创建预览需要的CaptureRequest.Builder
            final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            previewRequestBuilder.addTarget(mSurfaceHolder.getSurface());
            previewRequestBuilder.addTarget(mImageReader.getSurface());
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface(),mImageReader.getSurface()), new CameraCaptureSession.StateCallback() // ③
            {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) return;
                    // 当摄像头已经准备好时，开始显示预览
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        //previewRequestBuilder.get();
                        // 对焦无穷远
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                        previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.05f);
                        //关闭自动稳像
                        previewRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
                        // 关闭闪光灯
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                        // 帧频
                        Range<Integer> fpsRange = previewRequestBuilder.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
                        Log.i("帧频",fpsRange.toString());

                        // 显示预览
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null /*new mCaptureCallback()*/, childHandler);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(GetMotionMain.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
//
//    class mCaptureCallback extends CameraCaptureSession.CaptureCallback {
//        @Override
//        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result){
////            Log.i("接收的信息", String.valueOf(result.getKeys()));
//            Log.i("时间", String.valueOf(result.get(TotalCaptureResult.SENSOR_TIMESTAMP)));
////            Log.i("RS时间", String.valueOf(result.get(TotalCaptureResult.SENSOR_ROLLING_SHUTTER_SKEW)));
//        }
//    }

    ///相机部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//



    /**********************************************************************************/
    ///GPS部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    /**********************************************************************************/
    class LocationListenerNew implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            int SENS_TYPE = TYPE_GPS;

            double lon = location.getLongitude();
            double lat = location.getLatitude();
            double alt = location.getAltitude();
            float spd = location.getSpeed();
            float brg = location.getBearing();
            float acc = location.getAccuracy();//水平精度
//            float acc_ver = location.getVerticalAccuracyMeters();
            long time = location.getTime();

            Log.i("GPS经度", String.valueOf(lon));
            Log.i("GPS纬度", String.valueOf(lat));
            Log.i("GPS高度", String.valueOf(alt));
            Log.i("GPS速度", String.valueOf(spd));
            Log.i("GPS方向", String.valueOf(brg));
            Log.i("GPS精度", String.valueOf(acc));//水平精度
            Log.i("GPS时间", String.valueOf(time));
            textView4.setText(String.format("%0$.2f, %0$.2f, %0$.2f, %0$.2f",
                    lon, lat, alt, acc));

            //只有系统运行，以及文件存在的情况下才记录
            if ((sensData[SENS_TYPE] == null) || !SYS_STATE) return;
            if (sensData[SENS_TYPE].exists()) {
                try {
                    mFileWriter[SENS_TYPE].write(String.format("%18d, %0$.9f, %0$.9f, %0$.9f, %0$.3f, %0$.3f, %0$.3f\n",
                            time, lon, lat, alt, acc, spd, brg));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status,
        Bundle extras) {
        }
    }

    //GNSS监听器
    @RequiresApi(api = Build.VERSION_CODES.N)
    class GSCnew extends GnssStatus.Callback {
        @Override
        public void onSatelliteStatusChanged(GnssStatus status){
            int satNum = status.getSatelliteCount();
            textView4.setText(String.format("卫星数量%d", satNum));
        }

    }

    ///GPS部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//


    /**********************************************************************************/
    ///传感器部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    /**********************************************************************************/
    //判断并且设置传感器
    private void setSensor(int SENSOR_NUM)
    {
        if(SENSOR_NUM < 0 || SENSOR_NUM > 2) return; //只能是0,1,2

        //设置传感器可能的参数
        List<String> mSensNames = Arrays.asList("加速度计", "陀螺仪","磁场计");
        List<TextView> mTextHandles = Arrays.asList(textView1, textView2, textView3);
        int[] mTypes_u = {0x00000023/*未标定的加速度计*/, Sensor.TYPE_GYROSCOPE_UNCALIBRATED, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED};
        int[] mTypes = { Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD};

        //初始化监听器
        listener[SENSOR_NUM] = new SensorEventListenerNew();
        listener[SENSOR_NUM].getPara(this, SENSOR_NUM);

        //设置控件
        TextView Obj_t = mTextHandles.get(SENSOR_NUM);

        //开始配置，首先从未标定的传感器开始
        if (mSensorManager.getDefaultSensor( mTypes_u[SENSOR_NUM] ) != null){

            Obj_t.setText("成功获得"+mSensNames.get(SENSOR_NUM));
            Obj_t.setTextColor(Color.rgb(0, 255, 0));
            Log.i("收到：",mSensNames.get(SENSOR_NUM));
            mAvailable[SENSOR_NUM] = true;
            mSensors[SENSOR_NUM] = mSensorManager.getDefaultSensor(mTypes_u[SENSOR_NUM]);


        }
        else if (mSensorManager.getDefaultSensor(mTypes[SENSOR_NUM]) != null){

            Obj_t.setText("成功获取"+mSensNames.get(SENSOR_NUM));
            Obj_t.setTextColor(Color.rgb(0, 0, 255));
            Log.i("收到：","（标定的）"+mSensNames.get(SENSOR_NUM));
            mAvailable[SENSOR_NUM] = true;
            mSensors[SENSOR_NUM] = mSensorManager.getDefaultSensor(mTypes[SENSOR_NUM]);

        }
        else {

            Obj_t.setText("未获取"+mSensNames.get(SENSOR_NUM));
            Obj_t.setTextColor(Color.rgb(255, 0, 0));
            Log.e("错误：", mSensNames.get(SENSOR_NUM)+"获取失败");
            mAvailable[SENSOR_NUM] = false;

        }

    }

    //传感器设备的监听，可用于所有传感器
    class SensorEventListenerNew implements SensorEventListener {

        @Override
        public void onSensorChanged(final SensorEvent event) {
            float X = event.values[0];
            float Y = event.values[1];
            float Z = event.values[2];
            long T = event.timestamp;
            mTextView.setText(String.format(Locale.CHINESE, textShow, X, Y, Z));
            try {
                //在低版本的系统上，这里会出错，可能要分开写进去才行
                mFileWriter[SENS_TYPE].write(String.format("%18d, %0$.5f, %0$.5f, %0$.5f\n", T, X, Y, Z));
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("记录", "出错");
            }
            //Log.i("记录", String.valueOf(SENS_TYPE));
        }

        @Override
        public void onAccuracyChanged(Sensor mSensor, int accuracy) {

        }

        private String textShow;
        private TextView mTextView;
        private int SENS_TYPE;

        // 初始设置这个监听器属于哪个设备
        public void getPara(GetMotionMain _father, int _SENS_TYPE){

            SENS_TYPE = _SENS_TYPE;

            switch (SENS_TYPE) {
                case GetMotionMain.TYPE_ACC:
                    mTextView = textView1;
                    textShow = "加速度：%0$.2f, %0$.2f, %0$.2f";
                    break;
                case GetMotionMain.TYPE_ROT:
                    mTextView = textView2;
                    textShow = "角速度：%0$.2f, %0$.2f, %0$.2f";
                    break;
                case GetMotionMain.TYPE_MAG:
                    mTextView = textView3;
                    textShow = "磁场：%0$.2f, %0$.2f, %0$.2f";
                    break;

            }
        }
    }
    ///传感器部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//


    /**********************************************************************************/
    ///控件部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    /**********************************************************************************/
    // 按钮回调函数
    public void ctrlRecord(View view) {

        //获得系统时间
        SimpleDateFormat sDateFormat=new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        dayTime = sDateFormat.format(new Date());
        Log.e("时间", dayTime);

        //判断系统是否在运行
        if(SYS_STATE) {
            //如果运行中，再按就停止
            stopRecord();
            checkBox.setClickable(true);
            button.setText("开始");
            SYS_STATE = false;
        }
        else{
            //如果没运行，再按就开始，先建立文件夹
            dataRootFolder = new File(rootFolder, dayTime);
            dataRootFolderPics = new File(dataRootFolder, "Cam");

            if (dataRootFolder.mkdirs()) {
                if (dataRootFolderPics.mkdirs()) {
                    Log.i("文件夹", "成功创建");
                    startRecord();
                }
                else
                {
                    Toast.makeText(this, "创建文件夹失败", Toast.LENGTH_LONG).show();
                }
            }
            else {
                Log.e("文件夹", "创建失败");
                Toast.makeText(this, "创建文件夹失败", Toast.LENGTH_LONG).show();
            }

            checkBox.setClickable(false);
            button.setText("结束");
            SYS_STATE = true;
        }
    }


    public void startRecord(){
        Log.i("启动记录程序中的线程", String.valueOf(Thread.currentThread().getId()));
        //对每一个传感器，注册监听器
        for (int SENS_TYPE = 0; SENS_TYPE<3; SENS_TYPE++)
            if(mAvailable[SENS_TYPE]) {
                //注册sensor监听器
                mSensorManager.registerListener(listener[SENS_TYPE], mSensors[SENS_TYPE], SensorManager.SENSOR_DELAY_GAME);
            }

        //G文件
        for (int SENS_TYPE = 0; SENS_TYPE<5; SENS_TYPE++)
            if(mAvailable[SENS_TYPE]) {
                //创建文件
                sensData[SENS_TYPE] = new File(dataRootFolder, SENS_NAME.get(SENS_TYPE) + ".txt");
                try {
                    sensData[SENS_TYPE].createNewFile();
                    mFileWriter[SENS_TYPE] = new FileWriter(sensData[SENS_TYPE]);
                } catch (IOException e) {
                    Toast.makeText(this, "创建文件失败", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }

        //Log.i("start",listener[0].toString());
    }

    public void stopRecord(){
        stopAll();
    }
    ///控件部分++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//


    private void stopAll(){
        //关闭监听器，关闭文件
        mLocationManager.removeUpdates(mLocationListener);
        for (int SENS_TYPE = 0; SENS_TYPE<3; SENS_TYPE++) {
            if (mAvailable[SENS_TYPE]) mSensorManager.unregisterListener(listener[SENS_TYPE]);
        }
        for (int SENS_TYPE = 0; SENS_TYPE<5; SENS_TYPE++) {
            if (mAvailable[SENS_TYPE])
                try {
                    mFileWriter[SENS_TYPE].close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }




}

//    // 新线程的函数，暂时不用，还要再改
////    public class readSensorThread extends Thread {
////        public void run() {
////            float X = event.values[0];
////            float Y = event.values[1];
////            float Z = event.values[2];
//////                mTextView.setText(String.format(Locale.CHINESE, textShow, X, Y, Z));
////            Log.i("数据", String.valueOf(X));
////            Log.i("监听器回调函数线程", String.valueOf(Thread.currentThread().getId()));
////            this.yield();
////        }
////        public void getEvent(SensorEvent _event){
////            event = _event;
////        }
////        private SensorEvent event;
////    }
////    readSensorThread readThread = new readSensorThread();
//}
