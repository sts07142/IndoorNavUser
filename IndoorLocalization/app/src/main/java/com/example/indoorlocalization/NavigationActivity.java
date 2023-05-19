package com.example.indoorlocalization;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class NavigationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        Toast.makeText(getApplicationContext(), "경로 안내를 시작합니다.\n인터넷 연결이 끊어지지 않게 주의하세요.", Toast.LENGTH_LONG).show();
    }
}
