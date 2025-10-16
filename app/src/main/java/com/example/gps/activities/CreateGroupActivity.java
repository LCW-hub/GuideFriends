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
    private Button etDestination, etStartTime, etEndTime; // 목적지/시간 선택은 Button UI 유지
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
        etDestination = findViewById(R.id.etDestination); // XML의 ID는 btnSelectDestination 가정
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        rvFriends = findViewById(R.id.rvFriends);
        btnCreate = findViewById(R.id.btnCreate);

        // --- 기능 설정 ---
        setupDestinationSelectorLauncher(); // 최신 결과 처리 방식 설정 (첫 번째 코드 기능)
        rvFriends.setLayoutManager(new LinearLayoutManager(this));

        // 어댑터 초기화 (User 모델 사용)
        adapter = new FriendSelectAdapter(friendList);
        rvFriends.setAdapter(adapter);

        // 클릭 리스너 설정
        etDestination.setOnClickListener(v -> launchDestinationSearch());
        etStartTime.setOnClickListener(v -> showDateTimePicker(true)); // 날짜/시간 선택 UI (첫 번째 코드 기능)
        etEndTime.setOnClickListener(v -> showDateTimePicker(false));  // 날짜/시간 선택 UI (첫 번째 코드 기능)
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
                            etDestination.setText(destinationName); // 버튼 텍스트를 장소 이름으로 변경
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
                    etStartTime.setText(displayFormat.format(startTimeCalendar.getTime()));
                } else {
                    endTimeCalendar = selectedCalendar;
                    etEndTime.setText(displayFormat.format(endTimeCalendar.getTime()));
                }
            }, currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE), true).show();
        }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * [두 번째 코드 기능]
     * 서버에서 그룹에 초대할 수 있는 멤버 목록을 가져옴
     */
    private void fetchGroupSelectableMembers() {
        FriendApiService apiService = ApiClient.getClient(this).create(FriendApiService.class);
        Call<List<User>> call = apiService.getGroupSelectableMembers(); // ⭐️ API 호출 변경

        call.enqueue(new Callback<List<User>>() { // ⭐️ User 모델로 변경
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    friendList.clear();
                    friendList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    Log.d("CreateGroupActivity", "초대 가능 멤버 로드 성공. 수: " + response.body().size());
                } else {
                    Toast.makeText(CreateGroupActivity.this, "멤버 목록을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("CreateGroupActivity", "멤버 로드 실패. 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(CreateGroupActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("CreateGroupActivity", "네트워크 오류", t);
            }
        });
    }

    /**
     * [기능 통합]
     * 입력된 정보로 그룹 생성을 서버에 요청 (오류 처리 포함)
     */
    private void createGroup() {
        String groupName = etGroupName.getText().toString().trim();
        List<Long> selectedMemberIds = adapter.getSelectedFriendIds();

        // [두 번째 코드 기준] 입력 유효성 검사
        if (groupName.isEmpty() || destinationName == null || destinationLat == 0.0 || selectedMemberIds.isEmpty()) {
            Toast.makeText(this, "그룹 이름, 목적지, 최소 한 명의 친구를 선택해야 합니다.", Toast.LENGTH_LONG).show();
            return;
        }

        // [첫 번째 코드 기준] 시간 포맷팅
        String startTimeStr = serverFormat.format(startTimeCalendar.getTime());
        String endTimeStr = serverFormat.format(endTimeCalendar.getTime());

        CreateGroupRequest request = new CreateGroupRequest();
        request.setName(groupName);
        request.setDestinationName(destinationName);
        request.setDestinationLat(destinationLat);
        request.setDestinationLng(destinationLng);
        request.setStartTime(startTimeStr);
        request.setEndTime(endTimeStr);
        request.setMemberIds(selectedMemberIds);

        GroupApiService groupApiService = ApiClient.getGroupApiService(this);
        Call<Map<String, String>> call = groupApiService.createGroup(request);

        // [두 번째 코드 기능] 상세 오류 처리 및 인증 실패 시 리디렉션
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateGroupActivity.this, "그룹이 생성되었습니다!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorBody = "N/A";
                    try {
                        if (response.errorBody() != null) errorBody = response.errorBody().string();
                    } catch (Exception e) {
                        Log.e("CreateGroupActivity", "Error body parsing failed", e);
                    }
                    Log.e("CreateGroupActivity", "그룹 생성 실패. 코드: " + response.code() + ", 본문: " + errorBody);

                    if (response.code() == 403 || response.code() == 401) {
                        handleAuthErrorAndRedirect();
                    } else {
                        Toast.makeText(CreateGroupActivity.this, "그룹 생성 실패 (코드: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(CreateGroupActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * [두 번째 코드 기능]
     * 인증 오류(401/403) 발생 시 토큰 삭제 및 로그인 화면으로 이동
     */
    private void handleAuthErrorAndRedirect() {
        Toast.makeText(this, "세션이 만료되었습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show();
        TokenManager tokenManager = new TokenManager(getApplicationContext());
        tokenManager.deleteToken();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}