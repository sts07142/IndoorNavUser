package com.example.indoorlocalization;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    Button btn_start;
    TextView selected_point;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selected_point = findViewById(R.id.selected_destination);
        btn_start = findViewById(R.id.home_start_btn_tv);
        btn_start.setOnClickListener(v -> {
            /* intent 정보 보내기 */
            Intent intent = new Intent(getApplicationContext(), NavigationActivity.class);
            Bundle toNavigation = new Bundle();
            toNavigation.putString("start", "start address"); // 출발지 위치 - 현재 위치 바꾸기
            toNavigation.putString("dest", "dest address"); // 목적지 위치 - 선택한 위치 바꾸기

            intent.putExtras(toNavigation);
            startActivity(intent);

            finish();
        });
        
        selected_point.setText("선택한 목적지" + "");
// Inside onCreate() or a relevant initialization method
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            // Continue with camera setup
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, continue with camera setup
            } else {
                // Permission denied, handle accordingly (e.g., show a message or disable camera functionality)
            }
        }
    }
}