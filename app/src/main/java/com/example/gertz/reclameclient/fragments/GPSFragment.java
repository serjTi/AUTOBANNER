package com.example.gertz.reclameclient.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.gertz.reclameclient.MainActivity;
import com.example.gertz.reclameclient.R;
import com.example.gertz.reclameclient.data.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GPSFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    private Button btnOnGps, btnReport, btnLogout;
    private TextView tvDistance, tvLocationDetected, tvMaxDst, tvCurrentDst, tvTodaysDate, tvDriverName;
    private ProgressBar pbDistance;
    private ImageButton ibSettings;
    IOnMyGPSClickListener myGPSClickListener;
    IOnMyGPSLongClickListener myGPSLongClickListener;
    int maxDistance = 100000;
    double curDst = 0;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        myGPSClickListener = (IOnMyGPSClickListener) activity;
        myGPSLongClickListener = (IOnMyGPSLongClickListener) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gps, container, false);
//        tvDistance = (TextView) view.findViewById(R.id.tvDistance);
        tvTodaysDate = (TextView) view.findViewById(R.id.tvTodaysDate);
        tvDriverName = (TextView) view.findViewById(R.id.tvDriverName);
        tvDriverName.setText(MainActivity.login);
        tvMaxDst = (TextView) view.findViewById(R.id.tvMaxDst);
        tvCurrentDst = (TextView) view.findViewById(R.id.tvCurrentDst);
//        tvLocationDetected = (TextView) view.findViewById(R.id.tvDistanceDetected);
        pbDistance = (ProgressBar) view.findViewById(R.id.pbDistance);
        pbDistance.setVisibility(View.VISIBLE);
        pbDistance.setIndeterminate(false);
        pbDistance.setMax(maxDistance);
        pbDistance.setProgress(0);
        tvMaxDst.setText(String.valueOf(maxDistance/1000) + " км");

        btnOnGps = (Button) view.findViewById(R.id.btnOnGps);
        btnOnGps.setText("В путь!");
//        btnOnGps.setBackgroundResource(R.drawable.off_gps_btn);
        btnOnGps.setOnClickListener(this);
        btnOnGps.setOnLongClickListener(this);

        ibSettings = (ImageButton) view.findViewById(R.id.ibSettings);
        ibSettings.setOnClickListener(this);

        btnReport = (Button) view.findViewById(R.id.btnReport);
        btnReport.setOnClickListener(this);

        btnLogout = (Button) view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(this);

        setDate();
        return view;
    }

    private void setDate() {
        String pattern = "dd-MM-yyyy";
        String dateInString = new SimpleDateFormat(pattern).format(new Date());
        tvTodaysDate.setText(dateInString);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.btnOnGps:
                setGPSbtn();
                myGPSClickListener.onStartGPSButtonClick();
                break;
            case R.id.ibSettings:
                myGPSClickListener.onSettingsButtonClick();
                break;
            case R.id.btnReport:
                myGPSClickListener.onReportButtonClick();
                break;
            case R.id.btnLogout:
                myGPSClickListener.onLogOutButtonClick();
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.btnOnGps:
                setGPSOffBtn();
                myGPSLongClickListener.onStopGPSButtonClick();
                return true;
        }
        return false;
    }

    public interface IOnMyGPSClickListener {
        void onStartGPSButtonClick();

        void onReportButtonClick();

        void onSettingsButtonClick();

        void onLogOutButtonClick();
    }

    public interface IOnMyGPSLongClickListener {
        void onStopGPSButtonClick();
    }

    public void setDistance(double distance) {
        curDst = round(distance/1000, 2);
        Log.d(Constants.TAG,"GPS Fragment distance = " + distance);
        pbDistance.setProgress((int) distance);
        tvCurrentDst.setText(String.valueOf(curDst) + " км");
    }

    public void setGPSbtn() {
        btnOnGps.setBackgroundResource(R.drawable.btn_auth);
        btnOnGps.setText("Остановиться");
    }

    public void setGPSOffBtn() {
        btnOnGps.setText("В путь!");
        btnOnGps.setBackgroundResource(R.drawable.off_gps_btn);
    }
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

}
