package com.example.wifiscanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.net.wifi.ScanResult;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class WifiInfoView extends LinearLayout {
    public Context context;
    public String txtSSID, txtLevel, txtFrequency;
    public TextView txtViewSSID, txtViewBSSID, txtViewLevel, txtViewFrequency, txtRangingStatus;

    public WifiInfoView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    public WifiInfoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WifiInfoView);
        getValues(typedArray);
        initView();
    }

    private void getValues(TypedArray typedArray){
        txtSSID = typedArray.getString(R.styleable.WifiInfoView_txtSSID);
        txtLevel = typedArray.getString(R.styleable.WifiInfoView_txtLevel);
        txtFrequency = typedArray.getString(R.styleable.WifiInfoView_txtFrequency);
        typedArray.recycle();
    }

    @SuppressLint("SetTextI18n")
    private void initView(){
        inflate(context, R.layout.component_wifiinfoview, this);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        txtViewSSID = findViewById(R.id.txtSSID);
        txtViewBSSID = findViewById(R.id.txtBSSID);
        txtViewLevel = findViewById(R.id.txtLevel);
        txtViewFrequency = findViewById(R.id.txtFrequency);
        txtRangingStatus = findViewById(R.id.rangingStatus);
        txtViewSSID.setText(txtSSID);
        txtViewLevel.setText(txtLevel);
        txtViewFrequency.setText(txtFrequency);
        txtRangingStatus.setText("false");
    }

    @SuppressLint("SetTextI18n")
    public void setWifiInfo(ScanResult result, RangingResult rangingResult){
        txtViewSSID.setText(result.SSID);
        txtViewBSSID.setText(result.BSSID);
        txtViewLevel.setText(result.level + " dBm");
        txtViewFrequency.setText(result.frequency + "");
        txtRangingStatus.setText(String.valueOf(result.is80211mcResponder()));

//        if (!result.is80211mcResponder()){
//            txtRangingStatus.setText("UnSupported");
//            return;
//        }
//        if (rangingResult == null){
//            txtRangingStatus.setText("no match ranging result");
//            return;
//        }
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
//            txtRangingStatus.setText("VERSION TOO LOW");
//            return;
//        }

        //txtRangingStatus.setText(rangingResult.getStatus() == RangingResult.STATUS_SUCCESS ? "true" : "false");
    }
}
