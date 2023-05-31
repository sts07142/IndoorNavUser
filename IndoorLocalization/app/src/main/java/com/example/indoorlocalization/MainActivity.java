package com.example.indoorlocalization;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;

public class MainActivity extends AppCompatActivity {
    Button btn_start;
    Spinner floor_sp, room_sp;
    TextView destination, startLoc;
    String start,dest = "";

//    String[] floorList = {"4층", "5층"};
//    Resources res = getResources();
    String[] roomList = {"강의실 선택", "401호","402호","403호","404호","405호","406호",
            "407호","408호","409호","410호","411호","412호","413호",
            "414호","415호", "416호", "417호","418호","419호", "420호",
            "421호", "422호", "423호", "424호", "425호",
            "426호", "427호", "428호", "429호", "430호", "431호", "432호", "433호", "434호", "435호",
        "501호","502호","503호","504호","505호","506호",
        "507호","508호","509호","510호","511호","512호","513호",
        "514호","515호","516호","517호","518호","519호", "520호",
        "521호", "522호", "523호", "524호", "525호",
        "526호", "527호", "528호", "529호", "530호", "531호", "532호", "533호", "534호", "535호" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startLoc = (TextView) findViewById(R.id.home_now_location_tv);
        String tmp = startLoc.getText().toString();
        start = tmp.substring(tmp.length()-4);

        //spinner
        room_sp = (Spinner) findViewById(R.id.home_destination_sp);
        destination = (TextView) findViewById(R.id.home_destination_text_tv);

//        ArrayAdapter<String> fAdapter = new ArrayAdapter<String>(
//                this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, floorList
//        );
//        floor_sp.setAdapter(fAdapter);
        ArrayAdapter<String> rAdapter = new ArrayAdapter<String>(
                this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, roomList
        );
        room_sp.setAdapter(rAdapter);

        room_sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0){
                    dest = roomList[position];
                    destination.setText(dest + " 까지");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                destination.setText("");
            }
        });



        // 시작 버튼 클릭
        btn_start = findViewById(R.id.home_start_btn_tv);
        btn_start.setOnClickListener(v -> {
            if (dest == ""){
                Toast.makeText(this, "목적지를 선택해주세요", Toast.LENGTH_SHORT).show();
            }
            else {
                /* intent 정보 보내기 */
                Intent intent = new Intent(getApplicationContext(), NavigationActivity.class);
                Bundle toNavigation = new Bundle();
                toNavigation.putString("start", start); // 출발지 위치 - 현재 위치 바꾸기
                toNavigation.putString("dest", dest); // 목적지 위치 - 선택한 위치 바꾸기

                intent.putExtras(toNavigation);
                startActivity(intent);

                finish();
            }
        });
    }
}