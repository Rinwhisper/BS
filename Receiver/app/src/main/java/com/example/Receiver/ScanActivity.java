package com.example.Receiver;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.king.zxing.CaptureActivity;

public class ScanActivity extends CaptureActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("ScanActivity", "ScanActivity");
        super.onCreate(savedInstanceState);
        findViewById(R.id.cancel_button).setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_scan;
    }
}