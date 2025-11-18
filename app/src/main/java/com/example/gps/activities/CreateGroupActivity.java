package com.example.gps.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

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
import com.example.gps.model.User;
import com.example.gps.utils.TokenManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone; // TimeZone import 추가

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class CreateGroupActivity extends AppCompatActivity {

    // --- UI/Data Variables ---
    private EditText etGroupName;
    private Button etDestination, etStartTime, etEndTime;
    private RecyclerView rvFriends;
    private Button btnCreate;
    private FriendSelectAdapter adapter;
    private List<User> friendList = new ArrayList<>();

    // 사용자 정보
    private String loggedInUsername;

    // 목적지 정보 저장 변수
    private String destinationName;
    private double destinationLat = 0.0;
    private double destinationLng = 0.0;

    // 시간 정보 저장 변수
    // Calendar 객체는 onCreate에서 KST TimeZone을 설정하여 초기화됩니다.
    private Calendar startTimeCalendar;
    private Calendar endTimeCalendar;
    private SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA);

    // --- 기능 구현 ---
    private ActivityResultLauncher<Intent> destinationSelectorLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // ⭐ [추가/수정] TimeZone 설정 및 Calendar 객체 초기화 (KST 강제)
        TimeZone kstZone = TimeZone.getTimeZone("Asia/Seoul");

        // Calendar 객체를 KST TimeZone으로 초기화
        startTimeCalendar = Calendar.getInstance(kstZone);
        endTimeCalendar = Calendar.getInstance(kstZone);

        // SimpleDateFormat에도 KST TimeZone 강제 설정
        serverFormat.setTimeZone(kstZone);

        loggedInUsername = getIntent().getStringExtra("username");

        // --- UI 요소 초기화 ---
        etGroupName = findViewById(R.id.etGroupName);
        etDestination = findViewById(R.id.etDestination);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        rvFriends = findViewById(R.id.rvFriends);
        btnCreate = findViewById(R.id.btnCreate);

        // --- 기능 설정 ---
        setupDestinationSelectorLauncher();
        rvFriends.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FriendSelectAdapter(friendList);
        rvFriends.setAdapter(adapter);

        // 클릭 리스너 설정
        etDestination.setOnClickListener(v -> launchDestinationSearch());
        etStartTime.setOnClickListener(v -> showDateTimePicker(true));
        etEndTime.setOnClickListener(v -> showDateTimePicker(false));
        btnCreate.setOnClickListener(v -> createGroup());

        // 데이터 로드
        fetchGroupSelectableMembers();
    }

    /**
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
                            // 목적지 이름 표시 (📍 아이콘 추가)
                            etDestination.setText("📍 " + destinationName);

                            // 선택되었음을 표시하기 위해 스타일 변경 (초록색 배경)
                            etDestination.setTextColor(getResources().getColor(R.color.white, null));
                            etDestination.setBackgroundResource(R.drawable.button_destination_selected);
                            etDestination.setTextSize(17); // 텍스트 크기 증가

                            // 좌표 정보도 로그로 출력
                            Log.d("CreateGroupActivity",
                                    String.format("목적지 선택됨: %s (%.6f, %.6f)",
                                            destinationName, destinationLat, destinationLng));

                            // 사용자에게 피드백
                            Toast.makeText(this, "📍 목적지가 설정되었어요!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    /**
     * 지도(MapsActivity)를 목적지 선택 모드로 실행
     */
    private void launchDestinationSearch() {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("PURPOSE", "SELECT_DESTINATION");
        // ⭐ [추가] MapsActivity로 username 전달
        intent.putExtra("username", loggedInUsername);
        destinationSelectorLauncher.launch(intent);
    }

    /**
     * 날짜와 시간을 선택할 수 있는 다이얼로그 표시 (시간은 스피너 스타일)
     */
    private void showDateTimePicker(final boolean isStart) {
        final Calendar currentCalendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            // 아이폰 스타일의 스크롤/드래그 방식 시간 선택 다이얼로그
            showSpinnerTimePicker(isStart, year, month, dayOfMonth, currentCalendar);
        }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * 아이폰 스타일의 스피너(드래그) 방식 시간 선택 다이얼로그
     */
    private void showSpinnerTimePicker(final boolean isStart, int year, int month, int dayOfMonth, Calendar currentCalendar) {
        // 커스텀 레이아웃 inflate
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_time_picker_spinner, null);

        // TimePicker 찾기
        TimePicker timePicker = dialogView.findViewById(R.id.time_picker_spinner);

        // 24시간 형식으로 설정
        timePicker.setIs24HourView(true);

        // 현재 시간으로 초기화
        timePicker.setHour(currentCalendar.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(currentCalendar.get(Calendar.MINUTE));

        // AlertDialog 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // 다이얼로그 배경 투명하게
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 취소 버튼
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 확인 버튼
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            int hourOfDay = timePicker.getHour();
            int minute = timePicker.getMinute();

            // 1. Calendar 객체를 현재 시점으로 생성 (TimeZone 정보 유지)
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); // KST TimeZone 강제

            // 2. 날짜/시간 필드를 명확하게 설정
            selectedCalendar.set(Calendar.YEAR, year);
            selectedCalendar.set(Calendar.MONTH, month);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedCalendar.set(Calendar.MINUTE, minute);

            // ⭐ [핵심 수정 반영] 초와 밀리초를 0으로 강제 초기화하여 날짜 오버플로우 방지
            selectedCalendar.set(Calendar.SECOND, 0);
            selectedCalendar.set(Calendar.MILLISECOND, 0);

            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);

            // displayFormat도 KST를 사용하도록 강제 (onCreate에서 이미 설정되었지만, 안전을 위해)
            displayFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

            if (isStart) {
                startTimeCalendar = selectedCalendar;
                etStartTime.setText(displayFormat.format(startTimeCalendar.getTime()));
            } else {
                endTimeCalendar = selectedCalendar;
                etEndTime.setText(displayFormat.format(endTimeCalendar.getTime()));
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 서버에서 그룹에 초대할 수 있는 멤버 목록을 가져옴
     */
    private void fetchGroupSelectableMembers() {
        // ⭐ [수정] ApiClient.getClient(this) -> ApiClient.getRetrofit(this)
        FriendApiService apiService = ApiClient.getFriendApiService(this);
        Call<List<User>> call = apiService.getGroupSelectableMembers();

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    friendList.clear();
                    friendList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    Log.d("CreateGroupActivity", "초대 가능 멤버 로드 성공. 수: " + response.body().size());
                } else {
                    Toast.makeText(CreateGroupActivity.this, "😥 친구 목록을 불러올 수 없어요", Toast.LENGTH_SHORT).show();
                    Log.e("CreateGroupActivity", "멤버 로드 실패. 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(CreateGroupActivity.this, "🌐 인터넷 연결을 확인해주세요", Toast.LENGTH_LONG).show();
                Log.e("CreateGroupActivity", "네트워크 오류", t);
            }
        });
    }
    /**
     * 입력된 정보로 그룹 생성을 서버에 요청하고, 성공 시 MapsActivity로 이동하여 위치 공유를 시작합니다.
     */
    private void createGroup() {
        // ⭐ [삭제] onCreate에서 이미 설정했으므로 제거했습니다. 중복 설정은 오류를 유발합니다.
        // serverFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));

        String groupName = etGroupName.getText().toString().trim();
        List<Long> selectedMemberIds = adapter.getSelectedFriendIds();

        if (groupName.isEmpty() || destinationName == null || destinationLat == 0.0 || selectedMemberIds.isEmpty()) {
            Toast.makeText(this, "📝 그룹 이름, 목적지, 친구를 모두 입력해주세요", Toast.LENGTH_LONG).show();
            return;
        }

        // Calendar 객체에 KST가 강제 설정되어 있으므로, 정확한 KST 기반 TimeStamp와 문자열이 나옵니다.
        String startTimeStr = serverFormat.format(startTimeCalendar.getTime());
        String endTimeStr = serverFormat.format(endTimeCalendar.getTime());

        long endTimeMillis = endTimeCalendar.getTimeInMillis();

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

        // ⭐ [오류 해결] Call<Map<String, String>>에 맞게 onResponse 시그니처 수정
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    Map<String, String> responseBody = response.body();
                    String groupIdStr = responseBody.get("groupId"); // 서버에서 반환한 그룹 ID 키

                    if (groupIdStr != null) {
                        try {
                            long newGroupId = Long.parseLong(groupIdStr);

                            // 🚀 --- [1.2: Firebase에 목적지 정보 저장 코드 추가] ---
                            // 목적지 정보가 유효할 때만 Firebase에 저장
                            if (destinationName != null && destinationLat != 0.0 && destinationLng != 0.0) {
                                // 'group_destinations' 라는 새 경로 사용
                                DatabaseReference destinationRef = FirebaseDatabase.getInstance()
                                        .getReference("group_destinations")
                                        .child(String.valueOf(newGroupId))
                                        .child("destination");

                                HashMap<String, Object> destinationData = new HashMap<>();
                                destinationData.put("name", destinationName);
                                destinationData.put("latitude", destinationLat);
                                destinationData.put("longitude", destinationLng);

                                String startTimeStr = serverFormat.format(startTimeCalendar.getTime()); // 상단에서 선언된 변수 재활용
                                String endTimeStr = serverFormat.format(endTimeCalendar.getTime()); // 상단에서 선언된 변수 재활용

                                destinationData.put("endTimeMillis", endTimeMillis);
                                destinationData.put("startTime", startTimeStr);
                                destinationData.put("endTime", endTimeStr);

                                // Firebase에 데이터 쓰기
                                destinationRef.setValue(destinationData)
                                        .addOnSuccessListener(aVoid -> Log.d("CreateGroupActivity", "Firebase 목적지 저장 성공"))
                                        .addOnFailureListener(e -> Log.e("CreateGroupActivity", "Firebase 목적지 저장 실패", e));
                            }
                            // 🚀 --- [1.2 끝] ---

                            Toast.makeText(CreateGroupActivity.this, "그룹이 만들어졌어요! 위치 공유를 시작할게요", Toast.LENGTH_LONG).show();

                            // ⭐ [핵심 수정] MapsActivity로 ID 및 username 전달하여 위치 공유 시작
                            Intent intent = new Intent(CreateGroupActivity.this, MapsActivity.class);
                            intent.putExtra("groupId", newGroupId);
                            intent.putExtra("username", loggedInUsername); // MapsActivity에 username 전달

                            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);

                            finish();

                        } catch (NumberFormatException e) {
                            Log.e("CreateGroupActivity", "그룹 ID를 파싱할 수 없습니다: " + groupIdStr, e);
                            Toast.makeText(CreateGroupActivity.this, "⚠️ 그룹은 만들어졌지만 지도를 열 수 없어요", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(CreateGroupActivity.this, "⚠️ 그룹은 만들어졌지만 지도를 열 수 없어요", Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    String errorBody = "N/A";
                    try {
                        // errorBody를 가져올 때 response.errorBody()가 null인지 체크합니다.
                        if (response.errorBody() != null) errorBody = response.errorBody().string();
                    } catch (Exception e) {
                        Log.e("CreateGroupActivity", "Error body parsing failed", e);
                    }
                    Log.e("CreateGroupActivity", "그룹 생성 실패. 코드: " + response.code() + ", 본문: " + errorBody);

                    if (response.code() == 403 || response.code() == 401) {
                        handleAuthErrorAndRedirect();
                    } else {
                        Toast.makeText(CreateGroupActivity.this, "😥 그룹을 만들 수 없어요. 다시 시도해주세요", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(CreateGroupActivity.this, "🌐 인터넷 연결을 확인해주세요", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 인증 오류(401/403) 발생 시 토큰 삭제 및 로그인 화면으로 이동
     */
    private void handleAuthErrorAndRedirect() {
        Toast.makeText(this, "⏰ 로그인 시간이 만료되었어요. 다시 로그인해주세요", Toast.LENGTH_LONG).show();

        // [수정] TokenManager 생성자는 인자가 없습니다.
        TokenManager tokenManager = new TokenManager();

        // [수정] deleteToken -> deleteTokens (s 붙임)
        tokenManager.deleteTokens();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}