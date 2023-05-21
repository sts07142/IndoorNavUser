package com.example.indoorlocalization;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class NavigationActivity extends AppCompatActivity {

    TextView remained_distance; //남은 거리 표시
    TextView address_point; //출발지, 목적지
    ImageView direction; //안내 방향 표시

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        /* find View */
        remained_distance = findViewById(R.id.remained_distance);
        address_point = findViewById(R.id.address);
        direction = findViewById(R.id.direction);
        /* variables */
        int dist = 0;
        String start_point = "출발지 테스트", dest_point = "목적지 테스트";

        String sentence_dist = "남은 거리 : " + dist;
        remained_distance.setText(sentence_dist);
        String sentence_route = "출발지 : " + start_point + "\n목적지 : " + dest_point;
        address_point.setText(sentence_route);

        /* Rotate image view(user direction) */
        int rotate = 0; // sample - 상황에 맞게 값 변경하기
        onDirectionRotate(rotate);
        Toast.makeText(getApplicationContext(), "경로 안내를 시작합니다.\n인터넷 연결이 끊어지지 않게 주의하세요.", Toast.LENGTH_LONG).show();
    }

    // function : 경로에 따라 유저이미지의 방향 회전시키기
    protected void onDirectionRotate(int value) {
        // left, right, front, back
        direction.setRotation(value);

    }
}
