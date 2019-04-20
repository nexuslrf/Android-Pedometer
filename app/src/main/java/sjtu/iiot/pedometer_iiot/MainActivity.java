package sjtu.iiot.pedometer_iiot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.ContentValues;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.SeekBar;
//import android.database.sqlite.SQLiteDatabase;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends Activity implements SensorEventListener,
        OnClickListener {
    /** Called when the activity is first created. */
    //Create a LOG label
    private Button mWriteButton, mStopButton;
    private boolean doWrite = false;
    private int show_loop = 0;
    private SensorManager sm;
    private float lowX = 0, lowY = 0, lowZ = 0;
    private float smoothA = 0;
    private float FILTERING_VALAUE = 0.1f;
    private TextView AT,ACT,PG,BIST,OST, PS, ST;
    private SeekBar SKB;
    private int built_in_step = 0;
    private GraphView graph;
    private LineGraphSeries<DataPoint> series;
    private int timeStamp = 0;
    private LocationManager lm;
    private Location location_now;
    //        // 创建SQLiteOpenHelper子类对象
//  private MySQLiteOpenHelper dbHelper = new MySQLiteOpenHelper(this,"test_carson");
//        //数据库实际上是没有被创建或者打开的，直到getWritableDatabase() 或者 getReadableDatabase() 方法中的一个被调用时才会进行创建或者打开
//    private SQLiteDatabase  sqliteDatabase = dbHelper.getWritableDatabase();
//        // SQLiteDatabase  sqliteDatabase = dbHelper.getReadbleDatabase();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AT = (TextView)findViewById(R.id.AT);
        ACT = (TextView)findViewById(R.id.onAccuracyChanged);
        SKB = (SeekBar) findViewById(R.id.SeekBar);
        PG = (TextView) findViewById(R.id.filters);
        BIST = (TextView) findViewById(R.id.step_built_in);
        OST = (TextView) findViewById(R.id.step_sm);
        PS = (TextView) findViewById(R.id.location);
        ST = (TextView) findViewById(R.id.state);
        PG.setText("FilterWindow: "+Float.toString(FILTERING_VALAUE));
        verifyStoragePermissions(this);
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        location_now = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        updateShow();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 8, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // 当GPS定位信息发生改变时，更新定位
                location_now = location;
                updateShow();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                // 当GPS LocationProvider可用时，更新定位
                location_now = lm.getLastKnownLocation(provider);
                updateShow();
            }

            @Override
            public void onProviderDisabled(String provider) {
                location_now = null;
                updateShow();
            }
        });
        //Create a SensorManager to get the system’s sensor service
        sm =
                (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        /* **********************************************************************
        * Using the most common method to register an event
        * Parameter1 ：SensorEventListener detectophone
        * Parameter2 ：Sensor one service could have several Sensor
        realizations. Here,We use getDefaultSensor to get the defaulted Sensor
        * Parameter3 ：Mode We can choose the refresh frequency of the
        data change
        * **********************************************************************/
        // Register the acceleration sensor
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_FASTEST);//High sampling rate；.SENSOR_DELAY_NORMAL means a lower sampling rate
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);//High sampling rate；.SENSOR_DELAY_NORMAL means a lower sampling rate
        try {
            FileOutputStream fout = openFileOutput("acc.txt",
                    Context.MODE_PRIVATE);
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        SKB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // When the progress value has changed
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                FILTERING_VALAUE = progress * 1.0f / 100;
                PG.setText("FilterWindow: "+Float.toString(FILTERING_VALAUE));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
        });
        mWriteButton = (Button) findViewById(R.id.Button_Write);
        mWriteButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.Button_Stop);
        mStopButton.setOnClickListener(this);

        graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<>();

        graph.addSeries(series);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10000);
    }
    public void onPause(){
        super.onPause();
    }
    public void onClick(View v) {
        if (v.getId() == R.id.Button_Write) {
            doWrite = true;
        }
        if (v.getId() == R.id.Button_Stop) {
            doWrite = false;
        }
    }
    private void updateShow() {
        if (location_now != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("GPS Info：\n");
            sb.append("Longitude：" + location_now.getLongitude() + "\n");
            sb.append("Latitude：" + location_now.getLatitude() + "\n");
            sb.append("Altitude：" + location_now.getAltitude() + "\n");
            sb.append("Velocity：" + location_now.getSpeed() + "\n");
            sb.append("Direction：" + location_now.getBearing() + "\n");
            sb.append("Precision：" + location_now.getAccuracy() + "\n");
            PS.setText(sb.toString());
        } else PS.setText("");
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        ACT.setText(String.format("AccuracyChanged: accuracy: %d", accuracy));
    }
    public void onSensorChanged(SensorEvent event) {
        String message = new String();
        String message_2 = new String();
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float X = event.values[0];
            float Y = event.values[1];
            float Z = event.values[2];
            float A = (float) Math.sqrt(X*X + Y*Y + Z*Z);

            if (timeStamp++ % 100 == 0)
                series.appendData(new DataPoint(timeStamp, A), true, 50, false);
/*
            //Low-Pass Filter
            lowX = X * FILTERING_VALAUE + lowX * (1.0f -
                    FILTERING_VALAUE);
            lowY = Y * FILTERING_VALAUE + lowY * (1.0f -
                    FILTERING_VALAUE);
            lowZ = Z * FILTERING_VALAUE + lowZ * (1.0f -
                    FILTERING_VALAUE);
            //High-pass filter
            float highX = X - lowX;
            float highY = Y - lowY;
            float highZ = Z - lowZ;
            float highA = (float) Math.sqrt(highX * highX + highY * highY + highZ
                    * highZ);
*/
            DecimalFormat df = new DecimalFormat("#,##0.000");
/*            message = df.format(highX) + " ";
            message += df.format(highY) + " ";
            message += df.format(highZ) + " ";
            message += df.format(highA) + " ";
            message += Integer.toString(built_in_step);
            */

            message_2 = df.format(X) + " ";
            message_2 += df.format(Y) + " ";
            message_2 += df.format(Z) + " ";
            message_2 += df.format(A) + " ";
            message_2 += Integer.toString(built_in_step);

//            detectNewStep(highA);

            detectNewStep(A);

            if (show_loop == 0) {
//                AT.setText("pre: " + message + "\nraw: " + message_2);
                AT.setText("linear acc: " + message_2);
                OST.setText(String.format("Our Step: %d",count_step));
            }

            show_loop = (show_loop+1)%200;
            if (doWrite) {
//                write2file(message+"\n", String.format("/sdcard/acc_preprocess_%.2f.txt",FILTERING_VALAUE));
                write2file(message_2+"\n", String.format("/sdcard/acc_raw_%.2f.txt",FILTERING_VALAUE));
            }
        }
        if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR)
        {
            built_in_step++;
            BIST.setText(String.format("Built-in Step: %d",built_in_step));
        }

    }
    private float accOld = 0, accNew = 0, peak = 0, valley = 0;
    private boolean lastStatus = false, isUp = false;
    private int Upcount = 0, Upcount_prev= 0, tmpcount = 0;
    final private int valueNum = 4;
    float[] tmpVal = new float[valueNum];
    private int pretend_step=0, count_step = 0;
    private long currentTime, lastTimePeak=0, lastTime=0,thisTimePeak = 0,

            LastTimeValley = 0, timeInterval = 400, stopInterval = 10000; // 0.3s
    private float threshold = 1.5f, initialgap = 1.0f;
    private boolean walk = false, high = false;

    private void detectNewStep(float acc){
        if(accOld == 0)
            accOld = acc;
        else
        {
            if (detectPeak(acc, accOld))
            {
                lastTimePeak = thisTimePeak;
                currentTime = System.currentTimeMillis();
                if(currentTime - lastTimePeak >= timeInterval) {
                    if (peak - valley >= threshold) {
                        thisTimePeak = currentTime;
                        count_step++;
                    }
                    if (peak - valley >= initialgap)
                    {
                        thisTimePeak = currentTime;
                        threshold = peakValleyThreshold(peak-valley);
                    }
                }
            }
        }
        accOld = acc;
    }
    private boolean detectPeak(float val, float oldval)
    {
        lastStatus = isUp;
        if(val > oldval)
        {
            isUp = true;
            Upcount++;
        }
        else {
            Upcount_prev = Upcount;
            Upcount = 0;
            isUp = false;
        }
        if(!isUp && lastStatus && (Upcount_prev >= 2 || oldval >= 3))
        {
            peak = oldval;
            return true;
        }
        else if(!lastStatus && isUp){
            valley = oldval;
            return false;
        }
        else
            return false;
    }
    public float peakValleyThreshold(float value) {
        float tmpThreshold = threshold;
        if (tmpcount < valueNum) {
            tmpVal[tmpcount] = value;
            tmpcount++;
        } else {
            tmpThreshold = averageValue(tmpVal, valueNum);
            for (int i = 1; i < valueNum; i++) {
                tmpVal[i - 1] = tmpVal[i];
            }
            tmpVal[valueNum - 1] = value;
        }
        return tmpThreshold;

    }
    public float averageValue(float value[], int n) {
        float avg = 0;
        for (int i = 0; i < n; i++) {
            avg += value[i];
        }
        avg = avg / valueNum;
        if (avg >= 7)
            avg = (float) 3.0;
        else if (avg >= 5 && avg < 7)
            avg = (float) 2.4;
        else if (avg >= 3 && avg < 5)
            avg = (float) 1.8;
        else if (avg >= 1 && avg < 3)
            avg = (float) 1.5;
        else {
            avg = (float) 1.0;
        }
        return avg;
    }

    private void write2file(String a,String pathname){
        try {
            File file = new File(pathname);//write the result into/sdcard/acc.txt
            if (!file.exists()){
                file.createNewFile();}
            // Open a random access file stream for reading and writing
            RandomAccessFile randomFile = new RandomAccessFile(pathname, "rw");
            // The length of the file (the number of bytes)
            long fileLength = randomFile.length();
            // Move the file pointer to the end of the file
            randomFile.seek(fileLength);
            randomFile.writeBytes(a);
            randomFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_APP = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION};

//    private void insertSQL(float acc, float lgt,float ltt, float speed)
//    {
//        System.out.println("插入数据");
//
//        // 创建ContentValues对象
//        ContentValues values1 = new ContentValues();
//
//        // 向该对象中插入键值对
//        values1.put("id", 0);
//        values1.put("acc", acc);
//        values1.put("lgt", lgt);
//        values1.put("ltt", ltt);
//        values1.put("speed", speed);
//
//        // 调用insert()方法将数据插入到数据库当中
//        sqliteDatabase1.insert("user", null, values1);
//
//        // sqliteDatabase.execSQL("insert into user (id,name) values (1,'carson')");
//
//        //关闭数据库
//        sqliteDatabase1.close();
//    }
    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to
     * grant permissions
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
// Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
// We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_APP,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }



}