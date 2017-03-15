package com.example.gertz.reclameclient.fragments;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.gertz.reclameclient.R;
import com.example.gertz.reclameclient.data.Constants;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthFragment extends Fragment implements View.OnClickListener, TextView.OnEditorActionListener {
    Button btnAuth;
    IOnMyAuthClickListener authClickListener;
    EditText etLogin, etPassword;
    public String login = "";
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        authClickListener = (IOnMyAuthClickListener) activity;
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth, container, false);
        etLogin = (EditText)view.findViewById(R.id.etLogin);
        etPassword = (EditText) view.findViewById(R.id.etPassword);
        etPassword.setOnEditorActionListener(this);
        btnAuth = (Button) view.findViewById(R.id.btnAuth);
        btnAuth.setOnClickListener(this);
        return view;
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnAuth:
                String login = etLogin.getText().toString();
                String password = etPassword.getText().toString();
                String md5pass = md5(password + "pass100lead" + password + "okglass");
                authClickListener.onAuthButtonClick(login,md5pass);
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {

        if (i == EditorInfo.IME_ACTION_GO) {
            // обрабатываем нажатие кнопки GO
            login = etLogin.getText().toString();
            String password = etPassword.getText().toString();
            String md5pass = md5(password + "pass100lead" + password + "okglass");
            authClickListener.onAuthButtonClick(login,md5pass);
            Log.d(Constants.TAG, "AF login = " + login);
            return true;
        }
        return false;
    }

    public interface IOnMyAuthClickListener {
        void onAuthButtonClick(String login, String password);
    }


    public String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(Constants.MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
