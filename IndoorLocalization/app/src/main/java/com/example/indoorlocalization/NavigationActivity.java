package com.example.indoorlocalization;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.appcompat.app.AppCompatActivity;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class NavigationActivity extends AppCompatActivity {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private WifiManager wifiManager;
    private MediaPlayer mediaPlayer;
    TextView test;
    /* component variables */
    TextView remained_distance; //남은 거리 표시
    TextView address_point; //출발지, 목적지
    TextView current_position; // 현재 위치
    ImageView direction; //안내 방향 표시 화살표
    ImageView img_popup; //안내 팝업 이미지

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    PreviewView previewView;
    SensorManager manager;
    SensorListener listener;
    private float mCurrentDegree = 0f;
    private static final int INFINITY = 99999;
    double[][] linkBox;
    int cntLink,cntNode,startNodeIndex,endNodeIndex,prevNode,currentNode,cursor,isPoint[];
    double graph[][][],route[],answerNode[];
    long startTime, prevTime,finishTime,totalTime;

    String start_point = "", end_point = "";
    int starter=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        Intent intent = getIntent();
        Bundle fromMain = intent.getExtras();
        /* find View */
        test = findViewById(R.id.testview);
        remained_distance = findViewById(R.id.remained_distance);
        address_point = findViewById(R.id.navigation_textview_destination);
        current_position = findViewById(R.id.current_position);
        direction = findViewById(R.id.direction);
        img_popup = findViewById(R.id.view_popup); //for stair_image
        // 현재 위치 변수 만들기
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);//와이파이 신호를 받아오기위한 wifi메니저 인스턴스

        /* temp */
        previewView = findViewById(R.id.preview);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

        /* variables */
        int dist = 0;
        /* intent에서 받아오기(출발위치, 목적위치) */
        start_point = fromMain.getString("start");
        Log.d("StartPoint",  start_point);
        end_point = fromMain.getString("dest");
        Log.d("EndPoint",  end_point);

        String sentence_dist = "남은 거리 : " + dist;
        remained_distance.setText(sentence_dist);
        String sentence_route = "출발지 : " + changeToRoom(Integer.parseInt(start_point)) + "\n목적지 : " + end_point;
        address_point.setText(sentence_route);

        /* Rotate image view(user direction) */
        int rotate = 0; // sample - 상황에 맞게 값 변경하기
        //onDirectionRotate(rotate);
        Toast.makeText(getApplicationContext(), "경로 안내를 시작합니다.", Toast.LENGTH_SHORT).show();

        //화살표 회전
        manager = (SensorManager)getSystemService(SENSOR_SERVICE); //각 객체설정
        listener = new SensorListener();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        scanWifiData(); // 원하는 작업을 수행

                        // 1초 동안 스레드를 일시 정지
                        Thread.sleep(3000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        //내 위치 계산하기
        currentNode= Integer.parseInt(start_point);
        prevNode=currentNode;
        startNodeIndex=currentNode;
        endNodeIndex=changeToNode(end_point);

        fillNode();
        fillPath();
        findPath();
        startSensor();

        img_popup.setVisibility(View.INVISIBLE);

        startTime = System.currentTimeMillis();
        prevTime = startTime;
        new Thread(new Runnable() {
            @Override
            public void run() {
                cycle();
            }
        }).start();

    }


    void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview);
    }

    // scan wifi data in here!
    private void scanWifiData() throws JSONException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 거부되었을 경우 종료
            return;
        }
        JSONObject jsonData = new JSONObject();
        List<ScanResult> results = wifiManager.getScanResults();

        JSONArray wifiArray = new JSONArray();
        for (ScanResult result : results){
            String bssid = result.BSSID;
            int signalStrength = result.level;

            JSONObject wifi= new JSONObject();
            try{
                wifi.put("BSSID",bssid);
                wifi.put("RSSI",signalStrength);
                wifiArray.put(wifi);
            }catch(JSONException e){
                e.printStackTrace();
            }
        }
        try{
            jsonData.put("wifi",wifiArray);
            runOnUiThread(() -> {
                test.setText(jsonData.toString());
            });
            sendJsonData(jsonData);

        }catch(JSONException e){
            e.printStackTrace();
        }

    }
    // JSON 데이터 전송 메서드 정의 -> 수집한 와이파이 정보 보내기
    private void sendJsonData(JSONObject jsonData) {
        /* for connecting Flask server */
        String serverUrl = "http://aeong.pythonanywhere.com";
        String endpoint = serverUrl + "/location";

        // OkHttp 클라이언트 인스턴스 생성
        OkHttpClient client = new OkHttpClient();

        //JSONObject msg = new JSONObject();

        // JSON 요청 본문 생성
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonData.toString()
        );

        // OkHttp Request 객체 생성
        Request request = new Request.Builder()
                .url(endpoint)
                .post(requestBody) // POST 요청으로 설정
                .build();

        // 비동기적으로 요청을 보내고 응답 처리를 위한 콜백 등록
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String location;
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    JSONObject jObject = null;
                    try {
                        jObject = new JSONObject(responseData);
                        location = jObject.getString("msg");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    // 응답 데이터 처리
                    // 출발 위치 응답받아 넣기, 출발 위치 설정하기
                    // TextView의 텍스트 변경
                    prevNode=currentNode;
                    currentNode=changeToNode(location);

                    location=changeToRoom(changeToNode(location));
                    String tmp = "현재위치 : " + location;
                    runOnUiThread(() -> {
                        current_position.setText(tmp);
                    });
                    // start = responseData;
                    Log.d("API2 success ", responseData);
                } else {
                    // 응답이 실패한 경우 처리
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // 요청 실패 처리
            }
        });
    }
    void cycle(){
        while(currentNode!=endNodeIndex){
            //이전위치와 변화가 생겼을 때
            if(prevNode!=currentNode || starter==0){
                starter++;
                Log.d("path","위치 변화");
                //경로이탈
                if(inPath()==false){
                    Log.d("path","경로 이탈");
                    playSound_wrong(); //효과음 재생
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "경로를 이탈하였습니다.\n경로를 재탐색합니다.", Toast.LENGTH_LONG).show();
                            address_point.setText("출발지 : " + changeToRoom(Integer.parseInt(start_point)) + "\n목적지 : " + end_point);
                        }
                    });
                    startNodeIndex=currentNode;
                    findPath();
                }
                //정보 갱신
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        remained_distance.setText("다음 안내 : "+String.format("%.2f",findPartDist())+" M\n남은 거리 : "+String.format("%.2f",findFullDist())+" M");
                        Log.d("path","남은 거리 갱신");
                        playSound_next(); //sound 효과음 재생
                        //남은 길 정보에 의해, 사용해야 할 화살표 0:일반화살표 1:왼쪽으로 꺽인 화살표 2:오른쪽으로 꺽인 화살표
                        switch (arrowShape()){
                            case -2:
                                Log.d("path","층간 이동 필요 5-4");
                                //이미지뷰의 소스를 일반 화살표로 설정한다
                                Toast.makeText(getApplicationContext(), "5층 -> 4층으로 이동이 필요합니다.\n계단/엘레베이터를 이용해주세요.", Toast.LENGTH_LONG).show();
                                direction.setImageResource(R.drawable.ic_nav);
                                img_popup.setVisibility(View.VISIBLE);
                                onDirectionRotate(0);
                                break;
                            case -1:
                                Log.d("path","층간 이동 필요 4-5");
                                //이미지뷰의 소스를 일반 화살표로 설정한다
                                Toast.makeText(getApplicationContext(), "4층 -> 5층으로 이동이 필요합니다.\n계단/엘레베이터를 이용해주세요.", Toast.LENGTH_LONG).show();
                                onDirectionRotate(0);
                                img_popup.setVisibility(View.VISIBLE);
//                                /* sample - 3초 뒤 img popup 안 보이게 하기 */
//                                Handler handler = new Handler();
//                                handler.postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        img_popup.setVisibility(View.INVISIBLE);
//
//                                    }
//                                },3000);	//3초 동안 딜레이
                                break;
                            case 0:
                                Log.d("path","직진");
                                //이미지뷰의 소스를 일반 화살표로 설정한다
                                direction.setImageResource(R.drawable.ic_nav);
                                img_popup.setVisibility(View.INVISIBLE);
                                break;
                            case 1:
                                Log.d("path","좌회전");
                                //이미지뷰의 소스를 왼쪽으로 꺽인 화살표로 설정한다
                                direction.setImageResource(R.drawable.ic_arrow_turn_left);
                                img_popup.setVisibility(View.INVISIBLE);
                                break;
                            case 2:
                                Log.d("path","우회전");
                                //이미지뷰의 소스를 오른쪽으로 꺽인 화살표로 설정한다
                                direction.setImageResource(R.drawable.ic_arrow_turn_right);
                                img_popup.setVisibility(View.INVISIBLE);
                                break;
                        }
                    }
                });
            }
            //위치 계산이 실행된지 1초가 경과 했다면, 다시 계산
            if(System.currentTimeMillis()-prevTime>=1000){
                //내 위치 계산하기
                Log.d("path","위치 갱신");
                prevTime=System.currentTimeMillis();
                try {
                    scanWifiData();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        //while문이 끝났다는 것은 도착했다는 뜻
        finishTime=System.currentTimeMillis();
        totalTime=finishTime-startTime;
        //경과 시간을 소수점 아래 2번째 자리까지 표시
        Log.d("finish",String.format("%.2f",(double)totalTime/1000));
        playSound_success(); //효과음 재생
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "목적지에 도착하였습니다.\n소요시간 : "+String.format("%.2f",(double)totalTime/1000)+"초", Toast.LENGTH_LONG).show();
            }
        });
    }

    //fill node info
    void fillNode(){
        // 파일 읽기
        InputStream scanner= getResources().openRawResource(R.raw.node);
        byte[] txt = new byte[0];
        try {
            txt = new byte[scanner.available()];
            scanner.read(txt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String strTxt=new String(txt);

        // 각 행을 노드와 두 노드 사이의 거리의 튜플로 구문 분석
        String[] line=strTxt.split("\n");
        cntNode=line.length;

        isPoint=new int[cntNode];

        for(int i=0; i<cntNode; i++){
            String g= (line[i]).toString().trim();
            if(i==0){
                isPoint[i] = 0;
            }
            else if(g.equals("0")){
                isPoint[i] = 0;
            }else{
                isPoint[i] = 1;
            }
        }



        isPoint[endNodeIndex]=1;
    }

    //fill path info
    void fillPath(){
        graph=new double[cntNode][cntNode][2];
        // 파일 읽기
        InputStream scanner= getResources().openRawResource(R.raw.path);
        byte[] txt = new byte[0];
        try {
            txt = new byte[scanner.available()];
            scanner.read(txt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String strTxt=new String(txt);

        // 각 행을 노드와 두 노드 사이의 거리의 튜플로 구문 분석
        String[] line=strTxt.split("\n");
        cntLink=line.length;
        linkBox=new double[cntLink][4]; //from to dist dire
        int temp=0;
        while (temp<line.length) {
            String[] tokens = line[temp].split(",");

            //Log.d("token", Arrays.toString(tokens));
            try {
                linkBox[temp][0] = Double.parseDouble(tokens[0].trim());
            }catch (NumberFormatException e){
                linkBox[temp][0] = 0;
            }
            linkBox[temp][1] = Double.parseDouble(tokens[1].trim());
            linkBox[temp][2] = Double.parseDouble(tokens[2].trim());
            linkBox[temp][3] = Double.parseDouble(tokens[3].trim());
            temp++;
        }

        for(int i=0; i<cntNode; i++) {
            for(int j=0; j<cntNode; j++) {
                if(i==j) {
                    graph[i][j][0]=0;
                    continue;
                }
                for(int k=0; k<cntLink; k++) {
                    if( (i==linkBox[k][0]) && (j==linkBox[k][1]) ) {
                        graph[i][j][0]=linkBox[k][2];
                        graph[i][j][1]=linkBox[k][3];
                        break;
                    }
                    else
                        graph[i][j][0]=INFINITY;
                    graph[i][j][1]=-1;
                }
            }
        }
    }

    //return index that has minimum distance
    int minDistance(double dist[], Boolean sptSet[])    {
        double min = INFINITY;
        int min_index = 0;

        for (int v = 0; v < cntNode; v++)
        {
            if (!sptSet[v] && min > dist[v])
            {
                min_index = v;
                min = dist[v];
            }
        }

        return min_index;
    }

    //algorithm that find path (minimum distance)
    void findPath()    {
        route=new double[cntNode];

        int src=startNodeIndex;
        double dist[]=new double[cntNode]; // 최단 거리를 파악하는 배열
        Boolean sptSet[]=new Boolean[cntNode]; // 방문 했는지 체크 하는 bool형 배열

        for (int i = 0; i<cntNode; i++) {
            dist[i] = INFINITY;
            sptSet[i] = false;
        }

        // 초기 조건 설정.
        dist[src] = 0;
        // cntNode-1번 루프를 수행한다는 것은 첫 src노드를 제외한 모든 노드들에 접근을 해 계산을 한다는 의미.
        for (int count = 0; count < cntNode - 1; count++)
        {
            // 최단거리 정보를 알고 있는 노드들 중 가장 거리가 짧은 노드의 인덱스를 가져온다.
            int u = minDistance(dist, sptSet);
            // 그래프 상의 모든 노드들을 탐색하며 u 노드의 주변 정보를 갱신한다.
            for (int v = 0; v < cntNode; v++)
            {
                // 1. 아직 처리가 되지 않은 노드이어야 하며 (무한루프 방지)
                // 2. u-v 간에 edge가 존재하고
                // 3. src부터 u까지의 경로가 존재하고
                // 4. 기존의 v노드까지의 최단거리 값보다 새로 계산되는 최단거리가 더 짧을 경우
                if ( (!sptSet[v])  && (dist[u] != INFINITY) && (dist[v] > dist[u] + graph[u][v][0]) )
                {
                    // 최단거리를 갱신해준다.
                    dist[v] = dist[u] + graph[u][v][0];
                    route[v]=u;
                }
            }

            // 이제 이 노드(u)는 접근할 일이 없다. 플래그를 true로 설정.
            sptSet[u] = true;

            // 현재까지의 최단 거리를 출력해준다.
            //printSolution(dist, cntNode);
        }

        double[]visitNode = new double[cntNode];
        //find visited node
        int idx=endNodeIndex;
        int i=0;
        while(idx!=startNodeIndex){
            visitNode[i]=idx;
            idx=(int)route[idx];
            i++;
        }
        visitNode[i]=idx;

        answerNode=new double[i+1];

        int visitNodeTemp=0;
        int visitPathTemp=0;

        Log.d("path","visited route(node)");
        for(int j=i; j>=0; j--){
            Log.d("path",""+(int)visitNode[j]);
            answerNode[visitNodeTemp++]=visitNode[j];
        }

        Log.d("path","Distance "+startNodeIndex+" to "+endNodeIndex+" = "+dist[endNodeIndex]);
    }

    //return currentNdoe is finish
    boolean isFinish(){
        return currentNode==endNodeIndex;
    }

    //return boolean currentNode is in Path
    boolean inPath(){
        for(int i=0; i<answerNode.length; i++){
            if(answerNode[i]==currentNode){
                cursor=i;
                return true;
            }
        }
        return false;
    }

    //return remain Full distance
    double findFullDist(){
        double distance=0;
        for(int i=cursor; i<answerNode.length-1; i++){
            distance+=graph[(int) answerNode[i]][(int) answerNode[i+1]][0];
        }
        Log.d("path","남은 전체 거리 "+distance);
        return distance;
    }

    //return remain part distance(next turning point)
    double findPartDist(){
        double distance=0;
        for(int i=cursor; i<answerNode.length-1; i++){
            distance+=graph[(int) answerNode[i]][(int) answerNode[i+1]][0];
            if(1==isPoint[i+1]){
                Log.d("path","Point "+(i+1));
                Log.d("path","남은 부분 거리 "+distance);
                return distance;
            }
        }
        Log.d("path","남은 부분 거리 "+distance);
        return distance;
    }

    //return direction that need to ahead
    int aheadDirection(){
        return (int) graph[(int) answerNode[cursor]][(int) answerNode[cursor+1]][1];
    }

    //return arrow shape 0 : straight 1 : left 2 : right
    int arrowShape(){
        int dire1 = 0,dire2=0;
        Log.d("path","화살표 설정");
        if((int) graph[(int) answerNode[cursor]][(int) answerNode[cursor+1]][1]==-2){
            return -2;
        }else if((int) graph[(int) answerNode[cursor]][(int) answerNode[cursor+1]][1]==-1){
            return -1;
        }
        else
            for(int i=cursor; i<answerNode.length-1; i++) {
                if (i != cursor && 1 == isPoint[i] && i != answerNode.length) {
                    dire1 = (int) graph[(int) answerNode[i-1]][(int) answerNode[i]][1];
                    dire2 = (int) graph[(int) answerNode[i]][(int) answerNode[i+1]][1];
                    Log.d("path","dire1:"+dire1);
                    Log.d("path","dire2:"+dire2);
                    if(dire1<0 || dire2<0){
                            return 0;
                    }
                    else if (dire1 == dire2)
                        return 0;
                    else if (dire1 > 180) {
                        if ((dire1 - 180) < dire2 && dire2 < dire1)
                            return 1;
                        else
                            return 2;
                    } else {
                        if (dire1 < dire2 && dire2 < dire1 + 180)
                            return 2;
                        else
                            return 1;
                    }
                }

            }
        return 0;
    }

    @Override
    public void onResume(){
        super.onResume();
        startSensor();
    }

    @Override
    public void onPause(){
        super.onPause();
        stopSensor();
    }

    //화살표 회전 (ahead North)
    public void startSensor(){
        Sensor sensor1 = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);   //가속도 센서
        Sensor sensor2 = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);   //Magnetic센서
        //아래 만들었던 리스너 연결
        manager.registerListener(listener, sensor1, SensorManager.SENSOR_DELAY_UI);
        manager.registerListener(listener, sensor2, SensorManager.SENSOR_DELAY_UI);

    }

    public void stopSensor(){
        manager.unregisterListener(listener);
    }

    class SensorListener implements SensorEventListener {
        //각 센터 측정된 값 담을 배열
        float [] accValue = new float[3];
        float [] magValue = new float[3];

        boolean isGetAcc = false;
        boolean isGetMag = false;

        @Override
        public void onSensorChanged(SensorEvent event) {
            //센서변화
            //sensor 타입 구분
            int type = event.sensor.getType();

            switch (type){
                case Sensor.TYPE_ACCELEROMETER : //가속센서 라면
                    System.arraycopy(event.values,0, accValue, 0, event.values.length);
                    isGetAcc = true;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD : //마그네틱 필드 라면
                    System.arraycopy(event.values,0, magValue, 0, event.values.length);
                    isGetMag = true;
                    break;
            }
            //두가지 data 받는다면 -> 방위값
            if(isGetAcc == true && isGetMag == true){
                //행렬 계산위해 사용할 배열
                float [] R = new float[9];
                float [] I = new float[9];
                //행렬계산
                SensorManager.getRotationMatrix(R,null, accValue,magValue);
                //계산된 결과(R) -> 방위값으로 환산
                float [] values = new float[3];
                SensorManager.getOrientation(R, values);
                float azimuth  = (int) ( Math.toDegrees( SensorManager.getOrientation( R, I)[0] ) + 360 ) % 360;
                //같은 층간 이동시, 1층을 올라가야 할때, 1층을 내려가야 할때
                if(aheadDirection()>=0){
                    azimuth=(azimuth-aheadDirection())%360;
                    RotateAnimation ra = new RotateAnimation(
                            mCurrentDegree,
                            -azimuth,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f
                    );
                    ra.setDuration(100);
                    ra.setFillAfter(true);
                    direction.startAnimation(ra);
                    mCurrentDegree = -azimuth;
                }else if(aheadDirection()==-1){
                    RotateAnimation ra = new RotateAnimation(
                            0,
                            0,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f
                    );
                    ra.setDuration(100);
                    ra.setFillAfter(true);
                    direction.startAnimation(ra);
                    mCurrentDegree = 0;
                }else if(aheadDirection()==-2){
                    RotateAnimation ra = new RotateAnimation(
                            180,
                            180,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f
                    );
                    ra.setDuration(100);
                    ra.setFillAfter(true);
                    direction.startAnimation(ra);
                    mCurrentDegree = 180;
                }


            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { //감도변화

        }
    }

    // function : 경로에 따라 유저 이미지의 방향 회전시키기
    protected void onDirectionRotate(int value) {
        // left, right, front, back
        direction.setRotation(value);

    }
    int changeToNode(String dest_point){
        dest_point=dest_point.trim();
        dest_point=dest_point.replaceAll("[층,/,호, ]","");
        if(dest_point.equals("NOINFO") || dest_point.equals("")|| dest_point.equals(" ")){
//            return changeToNode(start_point);
            return 0;
        }else{
            String stair= String.valueOf(dest_point.charAt(0));
            int room= Integer.parseInt(dest_point.substring(dest_point.length()-2,dest_point.length()));
            int node=0;

            if(Integer.parseInt(dest_point)<100){
                stair="t";
            }

            if(stair.equals("4")){
                node=(room-1);
            }else if(stair.equals("5")){
                node=(room-1);
                node+=50;
            }else if(stair.equals("t")){
                if(room==1){
                    node=40;
                }else if(room==2){
                    node=42;
                }else if(room==3){
                    node=41;
                }else if(room==4){
                    node=43;
                }else if(room==5){
                    node=35;
                }else if(room==6){
                    node=39;
                }else if(room==7){
                    node=36;
                }else if(room==8){
                    node=38;
                }else if(room==9){
                    node=37;
                }else if(room==10){
                    node=88;
                }else if(room==11){
                    node=86;
                }else if(room==12){
                    node=87;
                }else if(room==13){
                    node=89;
                }else if(room==14){
                    node=85;
                }else if(room==15){
                    node=92;
                }else if(room==16){
                    node=90;
                }else if(room==17){
                    node=91;
                }else if(room==18){
                    node=93;
                }else if(room==19){
                    node=44;
                }else if(room==20){
                    node=47;
                }else if(room==21){
                    node=45;
                }else if(room==22){
                    node=48;
                }else if(room==23){
                    node=46;
                }else if(room==24){
                    node=94;
                }else if(room==25){
                    node=97;
                }else if(room==26){
                    node=95;
                }else if(room==27){
                    node=98;
                }else if(room==28){
                    node=96;
                }else if(room==29){
                    //존재하지 않음
                }else if(room==30){
                    node=49;
                }else if(room==31){
                    node=99;
                }
            }

            return node;
        }

    }

    String changeToRoom(int node){
        String floor="";
        int flag=0;
        String info = "";
        if(node<50){//4th fllor
            floor="4";
        }else if(node<100){//5th floor
            floor="5";
            node-=50;
        }

        if(node<35){//room
            node+=1;
            if(node<10)
                info="0"+node+"호";
            else
                info= node+"호";

            if(node==10)
                info="11호 & 계단";
        }else if(node<50){//not room
            flag=1;
            if(node==35)
                info="우측 엘레베이터 복도5";
            else if(node==36)
                info="우측 엘레베이터 복도1";
            else if(node==37)
                info="우측 엘레베이터 복도2";
            else if(node==38)
                info="우측 엘레베이터 복도3";
            else if(node==39)
                info="우측 엘레베이터 복도4";
            else if(node==40)
                info="중간 엘레베이터 복도1";
            else if(node==41)
                info="중간 엘레베이터 복도2";
            else if(node==42)
                info="중간 엘레베이터 복도3";
            else if(node==43)
                info="좌측 엘레베이터 공간";
            else if(node==44)
                info="좌측 계단";
            else if(node==45)
                info="중간 엘레베이터 앞";
            else if(node==46)
                info="중간 계단 위쪽";
            else if(node==47)
                info="중간 계단 아래쪽";
            else if(node==48)
                info="우측 엘레베이터 앞";
            else if(node==49)
                info="08호 앞 계단";


        }

        if(flag==0){
            return floor+info;
        }else if(flag==1){
            return floor+"층 "+info;
        }

        return node+"";
    }
    private void playSound_next() {
        mediaPlayer = MediaPlayer.create(this, R.raw.sfx_next);
        mediaPlayer.start();

        // 효과음 재생이 끝나면 MediaPlayer를 해제합니다.
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        });
    }
    private void playSound_wrong() {
        mediaPlayer = MediaPlayer.create(this, R.raw.sfx_caution);
        mediaPlayer.start();

        // 효과음 재생이 끝나면 MediaPlayer를 해제합니다.
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        });
    }
    private void playSound_success() {
        mediaPlayer = MediaPlayer.create(this, R.raw.sfx_success);
        mediaPlayer.start();

        // 효과음 재생이 끝나면 MediaPlayer를 해제합니다.
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        });
    }
}
