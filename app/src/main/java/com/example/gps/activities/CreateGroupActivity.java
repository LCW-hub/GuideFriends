package com.example.gps.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.activities.Register_Login.LoginActivity;
import com.example.gps.adapters.FriendSelectAdapter;
import com.example.gps.api.ApiClient;
import com.example.gps.api.FriendApiService;
import com.example.gps.api.GroupApiService;
import com.example.gps.dto.CreateGroupRequest;
import com.example.gps.model.User; // ⭐️ [기준 변경] FriendResponse 대신 User 모델 사용
import com.example.gps.utils.TokenManager; // ⭐ [기능 추가] TokenManager import

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateGroupActivity extends AppCompatActivity {

    // --- 변수 선언 (두 번째 코드 기준) ---
    private EditText etGroupName;
    private Button btnDestination, btnStartTime, btnEndTime; // 목적지/시간 선택은 Button UI 유지
    private RecyclerView rvFriends;
    private Button btnCreate;
    private FriendSelectAdapter adapter;
    private List<User> friendList = new ArrayList<>(); // ⭐️ 데이터 모델 'User'로 변경

    // 목적지 정보 저장 변수
    private String destinationName;
    private double destinationLat = 0.0;
    private double destinationLng = 0.0;

    // 시간 정보 저장 변수
    private Calendar startTimeCalendar = Calendar.getInstance();
    private Calendar endTimeCalendar = Calendar.getInstance();
    private SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA);

    // --- 기능 구현 (첫 번째 코드 기준) ---
    private ActivityResultLauncher<Intent> destinationSelectorLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // --- UI 요소 초기화 (ID는 activity_create_group.xml에 맞춰야 함) ---
        etGroupName = findViewById(R.id.etGroupName);
        btnDestination = findViewById(R.id.btnSelectDestination); // XML의 ID는 btnSelectDestination 가정
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        rvFriends = findViewById(R.id.rvFriends);
        btnCreate = findViewById(R.id.btnCreate);

        // --- 기능 설정 ---
        setupDestinationSelectorLauncher(); // 최신 결과 처리 방식 설정 (첫 번째 코드 기능)
        rvFriends.setLayoutManager(new LinearLayoutManager(this));

        // 어댑터 초기화 (User 모델 사용)
        adapter = new FriendSelectAdapter(friendList);
        rvFriends.setAdapter(adapter);

        // 클릭 리스너 설정
        btnDestination.setOnClickListener(v -> launchDestinationSearch());
        btnStartTime.setOnClickListener(v -> showDateTimePicker(true)); // 날짜/시간 선택 UI (첫 번째 코드 기능)
        btnEndTime.setOnClickListener(v -> showDateTimePicker(false));  // 날짜/시간 선택 UI (첫 번째 코드 기능)
        btnCreate.setOnClickListener(v -> createGroup());

        // 데이터 로드
        fetchGroupSelectableMembers(); // API 호출 (두 번째 코드 기능)
    }

    /**
     * [첫 번째 코드 기능]
     * MapsActivity를 실행하고 그 결과를 처리하는 ActivityResultLauncher 설정
     */
    private void setupDestinationSelectorLauncher() {
        destinationSelectorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        destinationName = data.getStringExtra("PLACE_NAME");
                        destinationLat = data.getDoubleExtra("PLACE_LAT", 0.0);
                        destinationLng = data.getDoubleExtra("PLACE_LNG", 0.0);

                        if (destinationName != null && !destinationName.isEmpty()) {
                            btnDestination.setText(destinationName); // 버튼 텍스트를 장소 이름으로 변경
                        }
                    }
                }
        );
    }

    /**
     * [첫 번째 코드 기능]
     * 지도(MapsActivity)를 목적지 선택 모드로 실행
     */
    private void launchDestinationSearch() {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("PURPOSE", "SELECT_DESTINATION");
        destinationSelectorLauncher.launch(intent);
    }

    /**
     * [첫 번째 코드 기능]
     * 날짜와 시간을 선택할 수 있는 다이얼로그 표시
     * @param isStart 시작 시간(true)인지 종료 시간(false)인지 구분
     */
    private void showDateTimePicker(final boolean isStart) {
        final Calendar currentCalendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth, hourOfDay, minute);

                SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);
                if (isStart) {
                    startTimeCalendar = selectedCalendar;
                    btnStartTime.setText(displayFormat.format(startTimeCalendar.getTime()));
                } else {
                    endTimeCalendar = selectedCalendar;
                    btnEndTime.setText(displayFormat.format(endTimeCalendar.getTime()));
                }
            }, currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE), true).show();
        }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * [두 번째 코드 기능]
     * 서버에서 그룹에 초대할 수 있는 멤버 목록을 가져옴
     */
    private void fetchGroupSelectableMembers() {
        FriendApiService api