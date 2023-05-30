package com.example.indoorlocalization;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
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
    }
}