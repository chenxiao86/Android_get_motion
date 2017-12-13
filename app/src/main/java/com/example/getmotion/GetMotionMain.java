package com.example.getmotion;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GetMotionMain extends AppCompatActivity {

    public static boolean SYS_STATE = false; //系统状态
    private SensorManager mSensorManager; //设备管理器

    //控件参数
    private CheckBox checkBox;
    private Button button;
    public TextView textView1, textView2, textView3, textView4;

    //传感器数据、监听器
    private boolean[] mAvailable = new boolean[3];
    private Sensor[] mSensors = new Sensor[3];
    public static final int TYPE_ACC = 0, TYPE_ROT = 1, TYPE_MAG = 2, TYPE_GPS = 3, TYPE_CAM = 4;
    private static final int[] SENS_TYPES = {TYPE_ACC, TYPE_ROT, TYPE_MAG, TYPE_GPS, TYPE_CAM};
    private SensorEventListenerNew[] listener = new SensorEventListenerNew[3];

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_motion_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //只许横屏

        //取得控件
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        button  = (Button) findViewById(R.id.button);
        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);

        //定义设备管理器
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);




        //设备列表，寻找设备
        for(int i = 0; i < deviceSensors.size(); i++) // 设备列表
            Log.e("SENSOR", String.valueOf(deviceSensors.get(i)));
        for (int SENS_TYPE = 0; SENS_TYPE<3; SENS_TYPE++)
            setSensor(SENS_TYPE);

    }




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

    // 按钮回调函数
    public void ctrlRecord(View view) {

        //判断系统是否在运行
        if(SYS_STATE) {
            //如果运行中，再按就停止
            stopRecord();
            checkBox.setClickable(true);
            button.setText("开始");
            SYS_STATE = false;
        }
        else{
            //如果没运行，再按就开始
            startRecord();
            checkBox.setClickable(false);
            button.setText("结束");
            SYS_STATE = true;
        }
    }

//    private class DownloadImageTask extends AsyncTask<int, Void, Void> {
//        /** The system calls this to perform work in a worker thread and
//         * delivers it the parameters given to AsyncTask.execute() */
//        protected void doInBackground(int i) {
//            return;
//        }
//
//        /** The system calls this to perform work in the UI thread and delivers
//         * the result from doInBackground() */
//        protected void onPostExecute() {
//            textView1.setText("a");
//        }
//    }

    public void startRecord(){
        for (int SENS_TYPE = 0; SENS_TYPE<3; SENS_TYPE++)
            if(mAvailable[SENS_TYPE]) {
                mSensorManager.registerListener(listener[SENS_TYPE], mSensors[SENS_TYPE], SensorManager.SENSOR_DELAY_GAME);
            }
        Log.i("start",listener[0].toString());
    }

    public void stopRecord(){
        for (int SENS_TYPE = 0; SENS_TYPE<3; SENS_TYPE++)
            if(mAvailable[SENS_TYPE]) mSensorManager.unregisterListener(listener[SENS_TYPE]);
    }


}

//传感器设备的监听，可用于所有传感器
class SensorEventListenerNew implements SensorEventListener {

    @Override
    public void onSensorChanged(SensorEvent event) {
        float X = event.values[0];
        float Y = event.values[1];
        float Z = event.values[2];
        mTextView.setText(String.format(Locale.CHINESE, textShow, X, Y, Z));
//        Log.i("数据","正常");
    }

    @Override
    public void onAccuracyChanged(Sensor mSensor, int accuracy) {

    }

    private String textShow;
    private TextView mTextView;

    public void getPara(GetMotionMain _father, int SENS_TYPE){

        father = _father;

        switch (SENS_TYPE) {
            case GetMotionMain.TYPE_ACC:
                mTextView = father.textView1;
                textShow = "加速度：%0$.2f, %0$.2f, %0$.2f";
                break;
            case GetMotionMain.TYPE_ROT:
                mTextView = father.textView2;
                textShow = "角速度：%0$.2f, %0$.2f, %0$.2f";
                break;
            case GetMotionMain.TYPE_MAG:
                mTextView = father.textView3;
                textShow = "磁场：%0$.2f, %0$.2f, %0$.2f";
                break;

        }
    }

    private GetMotionMain father;

}
