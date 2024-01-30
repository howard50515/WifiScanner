package com.example.wifiscanner;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public WifiManager wifiManager;
    public WifiRttManager wifiRttManager;
    public SensorManager sensorManager;
    public BroadcastReceiver receiver;
    public LinearLayout body;
    public TextView startScanSymbol, resultsCount, txtPermission, txtWarning, txtSDKVersion, txtOrientation, txtAccelerate, txtMagnetic;
    public TextView txtRTTSupport, txtRTTAvailable;
    public Button btScan;
    public ImageView imgCompass;
    public ArrayList<WifiInfoView> wifiInfoViews = new ArrayList<>();
    public boolean permission, rttAvailable = false, rttSupport = false;

    private Sensor accelerateSensor, magneticSensor;
    private float[] accelerateValues = new float[3], magneticValues = new float[3];

    @SuppressLint({"SetTextI18n", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        body = findViewById(R.id.body);
        startScanSymbol = findViewById(R.id.startScanSymbol);
        resultsCount = findViewById(R.id.resultsCount);
        txtPermission = findViewById(R.id.permission);
        txtWarning = findViewById(R.id.warning);
        txtOrientation = findViewById(R.id.orientation);
        txtAccelerate = findViewById(R.id.accelerate);
        txtMagnetic = findViewById(R.id.magnetic);
        txtRTTAvailable = findViewById(R.id.rttAvailable);
        txtRTTSupport = findViewById(R.id.rttSupport);
        txtSDKVersion = findViewById(R.id.sdkVersion);
        btScan = findViewById(R.id.btScan);
        imgCompass = findViewById(R.id.imgCompass);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!permission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.NEARBY_WIFI_DEVICES}, 0);
        }
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                    updateNewScan(success);
                } else if (action.equals(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        rttAvailable = wifiRttManager.isAvailable();
                        txtRTTAvailable.setText(rttAvailable ? "true" : "false");
                    }
                }
            }
        };

        btScan.setOnTouchListener(new View.OnTouchListener() {
            private boolean isOutside;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        isOutside = false;
                        btScan.setBackgroundResource(R.drawable.background_rectangle_focus);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        isOutside = motionEvent.getX() > btScan.getWidth() || motionEvent.getX() < 0 ||
                                motionEvent.getY() > btScan.getHeight() || motionEvent.getY() < 0;
                        if (!isOutside)
                            btScan.setBackgroundResource(R.drawable.background_rectangle_focus);
                        else
                            btScan.setBackgroundResource(R.drawable.background_rectangle_transparent_unfocus);
                        break;
                    case MotionEvent.ACTION_UP:
                        btScan.setBackgroundResource(R.drawable.background_rectangle_transparent_unfocus);
                        if (!isOutside) startScan();
                        break;
                }

                return true;
            }
        });

        txtPermission.setText(permission ? "true" : "false");
        txtSDKVersion.setText(String.valueOf(Build.VERSION.SDK_INT));
        startScan();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            rttSupport = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT);
            if (rttSupport) {
                wifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
                rttAvailable = wifiRttManager.isAvailable();
            }
        }

        txtRTTSupport.setText(rttSupport ? "true" : "false");
        txtRTTAvailable.setText(rttAvailable ? "true" : "false");
    }

    private void startScan() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            intentFilter.addAction(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);
        }
        registerReceiver(receiver, intentFilter);
        boolean scanSymbol = wifiManager.startScan();
        startScanSymbol.setText(scanSymbol ? "true" : "false");
        txtWarning.setVisibility(VISIBLE);
        body.setVisibility(GONE);
        txtWarning.setText(!scanSymbol ?
                "掃描失敗\n請開啟'位置'以繼續掃描附近ap" :
                "掃描中...");
    }

    @SuppressLint("SetTextI18n")
    private void updateNewScan(boolean success) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            txtWarning.setVisibility(VISIBLE);
            txtWarning.setText("無法存取位置權限\n請前往設定並允許應用程式使用位置權限");
            return;
        }
        startScanSymbol.setText(success ? "true" : "false");
        if (!success) {
            txtWarning.setVisibility(VISIBLE);
            txtWarning.setText("掃描被拒絕，可能是因為請求過於頻繁\n使用上次掃描資料");
        } else {
            txtWarning.setVisibility(GONE);
        }
        body.setVisibility(VISIBLE);
        List<ScanResult> results = wifiManager.getScanResults();
        resultsCount.setText(results.size() + "");
        results.sort((a, b) -> b.level - a.level);

        for (int i = results.size(); i < wifiInfoViews.size(); i++) {
            wifiInfoViews.get(i).setVisibility(GONE);
        }

        for (int i = 0; i < results.size(); i++) {
            if (wifiInfoViews.size() <= i) {
                WifiInfoView wifiInfoView = new WifiInfoView(MainActivity.this);
                wifiInfoView.setWifiInfo(results.get(i), null);
                wifiInfoViews.add(wifiInfoView);
                body.addView(wifiInfoView);
            } else {
                wifiInfoViews.get(i).setWifiInfo(results.get(i), null);
                wifiInfoViews.get(i).setVisibility(VISIBLE);
            }
        }
//        boolean ranging = false;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            rttSupport = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT);
//            if (rttSupport) {
//                wifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
//                rttAvailable = wifiRttManager.isAvailable();
//            }
//            RangingRequest.Builder builder = new RangingRequest.Builder();
//            builder.addAccessPoints(results);
//            if (wifiRttManager != null) {
//                ranging = true;
//                txtWarning.setVisibility(VISIBLE);
//                wifiRttManager.startRanging(builder.build(), getMainExecutor(), new RangingResultCallback() {
//                    @Override
//                    public void onRangingFailure(int i) {
//                        txtWarning.setText("Ranging Fail " + wifiRttManager.isAvailable() + Build.VERSION.SDK_INT);
//                    }
//
//                    @Override
//                    public void onRangingResults(@NonNull List<RangingResult> list) {
//                    }
//                });
//            }
//        }
//        if (!ranging) {
//            for (int i = 0; i < results.size(); i++) {
//                if (wifiInfoViews.size() <= i) {
//                    WifiInfoView wifiInfoView = new WifiInfoView(MainActivity.this);
//                    wifiInfoView.setWifiInfo(results.get(i), null);
//                    wifiInfoViews.add(wifiInfoView);
//                    body.addView(wifiInfoView);
//                } else {
//                    wifiInfoViews.get(i).setWifiInfo(results.get(i), null);
//                    wifiInfoViews.get(i).setVisibility(VISIBLE);
//                }
//            }
//        }

        txtRTTSupport.setText(rttSupport ? "true" : "false");
        txtRTTAvailable.setText(rttAvailable ? "true" : "false");
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, accelerateSensor);
        sensorManager.unregisterListener(this, magneticSensor);
    }

    private float[] r = new float[9];
    private float[] values = new float[3];
    @Override
    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerateValues = event.values.clone();
            txtAccelerate.setText(String.format("(%.2f, %.2f, %.2f)", accelerateValues[0], accelerateValues[1], accelerateValues[2]));
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values.clone();
            txtMagnetic.setText(String.format("(%.2f, %.2f, %.2f)", magneticValues[0], magneticValues[1], magneticValues[2]));
        }

        SensorManager.getRotationMatrix(r, null, accelerateValues, magneticValues);
        SensorManager.getOrientation(r, values);
        float degree = (float)Math.toDegrees(values[0]);
        imgCompass.setRotation(degree);
        txtOrientation.setText(String.format("%.2f (%s)", degree, getDirection(degree)));
    }

    public String getDirection(float degree){
        float range = Math.abs(degree);
        if (range < 22.5){
            return "N";
        }
        else if (range < 67.5){
            return  (degree < 0) ? "NW" : "NE";
        }
        else if (range < 112.5){
            return  (degree < 0) ? "W" : "E";
        }
        else if (range < 135){
            return  (degree < 0) ? "W" : "E";
        }
        else if (range < 157.5){
            return  (degree < 0) ? "SW" : "SE";
        }

        return "S";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}