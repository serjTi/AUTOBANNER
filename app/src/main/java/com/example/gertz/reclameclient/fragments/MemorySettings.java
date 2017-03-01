package com.example.gertz.reclameclient.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.example.gertz.reclameclient.MainActivity;
import com.example.gertz.reclameclient.R;
import com.example.gertz.reclameclient.data.Constants;

public class MemorySettings extends DialogFragment implements View.OnClickListener {
    CheckBox phone, sd;
    public static String checkedMemory = "";


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle("Настройки");
        View view = inflater.inflate(R.layout.memory_fragment, null);
        view.findViewById(R.id.btnCancel).setOnClickListener(this);
        view.findViewById(R.id.btnSave).setOnClickListener(this);
        phone = (CheckBox) view.findViewById(R.id.cbPhoneMemory);
        sd = (CheckBox) view.findViewById(R.id.cbSDCard);
        phone.setOnClickListener(this);
        sd.setOnClickListener(this);
        if (!MainActivity.memoryPreferences.equals("")){
            if (MainActivity.memoryPreferences.equals("phone")) {
                phone.setChecked(true);
                sd.setClickable(false);
            } else if (MainActivity.memoryPreferences.equals("sd")){
                sd.setChecked(true);
                phone.setClickable(false);
            }
        }
        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){

            case R.id.cbPhoneMemory:
                if(phone.isChecked())
                    sd.setClickable(false);
                else
                    sd.setClickable(true);
                break;

            case R.id.cbSDCard:
                if(sd.isChecked())
                    phone.setClickable(false);
                else
                    phone.setClickable(true);
                break;

            case R.id.btnSave:
                if(phone.isChecked())
                    checkedMemory = "phone";
                else if (sd.isChecked())
                    checkedMemory = "sd";
                Log.d("TAG", "stringcheckedMemory = " + checkedMemory);
                savePreferences(Constants.KEY_MEMORY_PLACE, checkedMemory);
                getDialog().cancel();
                break;
            case R.id.btnCancel:
                getDialog().cancel();
                break;
        }
    }
    private void savePreferences(String key, String value) {
        Context context = getActivity();
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }
}
