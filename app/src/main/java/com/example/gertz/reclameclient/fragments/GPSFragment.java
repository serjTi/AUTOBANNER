package com.example.gertz.reclameclient.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.gertz.reclameclient.R;

public class GPSFragment extends Fragment implements View.OnClickListener {

    private Button btnOnGps, btnReport, btnOffGps;
    private TextView tvDistance, tvLocationDetected;
    private ProgressBar pbDistance;

    IOnMyGPSClickListener myGPSClickListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        myGPSClickListener = (IOnMyGPSClickListener) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gps, container, false);

        tvDistance = (TextView) view.findViewById(R.id.tvDistance);
        tvLocationDetected = (TextView) view.findViewById(R.id.tvDistanceDetected);
        pbDistance = (ProgressBar) view.findViewById(R.id.pbDistance);

        btnOnGps = (Button) view.findViewById(R.id.btnOnGps);
        btnOnGps.setOnClickListener(this);

        btnOffGps = (Button) view.findViewById(R.id.btnOffGps);
        btnOffGps.setOnClickListener(this);

        btnReport = (Button) view.findViewById(R.id.btnReport);
        btnReport.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.btnOnGps:
                myGPSClickListener.onStartGPSButtonClick();
                break;

            case R.id.btnOffGps:
                myGPSClickListener.onStopGPSButtonClick();
                break;

            case R.id.btnReport:
                myGPSClickListener.onReportButtonClick();
                break;
        }
    }


    public interface IOnMyGPSClickListener {
        void onStartGPSButtonClick();
        void onStopGPSButtonClick();
        void onReportButtonClick();
    }
    public void setDistance(int distance){
        tvDistance.setText( "Вы проехали:" + String.valueOf(distance) + "метров");
    }

    public void setLocality(String locality){
        tvLocationDetected.setText(locality);
    }



}
