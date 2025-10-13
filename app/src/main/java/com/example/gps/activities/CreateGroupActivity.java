package com.example.gps.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.adapters.FriendSelectAdapter;
import com.example.gps.api.ApiClient;
import com.example.gps.api.FriendApiService;
import com.example.gps.api.GroupApiService;
import com.example.gps.dto.CreateGroupRequest;
import com.example.gps.dto.FriendResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.Intent;
import android.app.Activity;

public class CreateGroupActivity extends AppCompatActivity {

    private Button btnSelectDestination;
    private Button btnStartTime, btnEndTime;
    private EditText etGroupName;
    private RecyclerView rvFriends;
    private Button btnCreate;
    private FriendSelectAdapter adapter;

    private String selectedPlaceName;
    private Double selectedPlaceLat;
    private Double selectedPlaceLng;

    // friendList 변수를 클래스 멤버로 유지하여 어댑터와 데이터를 공유합니다.
    private List<FriendResponse> friendList = new ArrayList<>();
    // 선택된 시간을 저장할 Calendar 객체
    private Calendar startTimeCalendar = Calendar.getInstance();
    private Calendar endTimeCalendar = Calendar.getInstance();

    private ActivityResultLauncher<Intent> destinationSelectorLauncher;
    // 서버로 보낼 날짜 형식을 지정
    private SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        etGroupName = findViewById(R.id.etGroupName);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        rvFriends = findViewById(R.id.rvFriends);
        btnCreate = findViewById(R.id.btnCreate);
        btnSelectDestination = findViewById(R.id.btnSelectDestination);

        setupDestinationSelectorLauncher();

        btnSelectDestination.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapsActivity.class);
            // MapsActivity에 '선택 모드'임을 알리는 신호를 보냄
            intent.putExtra("PURPOSE", "SELECT_DESTINATION");
            destinationSelectorLauncher.launch(intent);
        });



        rvFriends.setLayoutManager(new LinearLayoutManager(this));

        btnStartTime.setOnClickListener(v -> showDateTimePicker(true));
        btnEndTime.setOnClickListener(v -> showDateTimePicker(false));

        // ## ✅ 'Expected 1 argument...' 오류 해결 ##
        // 어댑터를 비어있는 리스트로 먼저 초기화하고 RecyclerView에 설정합니다.
        adapter = new FriendSelectAdapter(friendList);
        rvFriends.setAdapter(adapter);

        fetchFriends();

        btnCreate.setOnClickListener(v -> createGroup());
    }

    private void setupDestinationSelectorLauncher() {
        destinationSelectorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 결과가 성공적으로 왔는지 확인
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        // MapsActivity에서 보낸 장소 정보를 추출
                        selectedPlaceName = data.getStringExtra("PLACE_NAME");
                        selectedPlaceLat = data.getDoubleExtra("PLACE_LAT", 0.0);
                        selectedPlaceLng = data.getDoubleExtra("PLACE_LNG", 0.0);

                        // 버튼 텍스트를 선택된 장소 이름으로 변경
                        if (selectedPlaceName != null && !selectedPlaceName.isEmpty()) {
                            btnSelectDestination.setText(selectedPlaceName);
                        }
                    }
                }
        );
    }

    private void showDateTimePicker(final boolean isStart) {
        final Calendar currentCalendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    // 날짜가 선택되면, 이어서 시간 선택 다이얼로그를 보여줌
                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (timeView, hourOfDay, minute) -> {
                                // 최종 선택된 날짜와 시간을 Calendar 객체에 저장
                                Calendar selectedCalendar = Calendar.getInstance();
                                selectedCalendar.set(year, month, dayOfMonth, hourOfDay, minute);

                                // 화면에 선택된 날짜와 시간 표시
                                SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);
                                if (isStart) {
                                    startTimeCalendar = selectedCalendar;
                                    btnStartTime.setText(displayFormat.format(startTimeCalendar.getTime()));
                                } else {
                                    endTimeCalendar = selectedCalendar;
                                    btnEndTime.setText(displayFormat.format(endTimeCalendar.getTime()));
                                }
                            }, currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE), true);
                    timePickerDialog.show();
                },
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH),
                currentCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void fetchFriends() {
        // FriendApiService를 생성할 때도 getClient(this)를 사용합니다.
        FriendApiService apiService = ApiClient.getClient(this).create(FriendApiService.class);
        Call<List<FriendResponse>> call = apiService.getFriends();

        // ## ✅ 'enqueue' 타입 불일치 오류 해결 ##
        // FriendResponse 리스트를 받는 올바른 콜백을 사용합니다.
        call.enqueue(new Callback<List<FriendResponse>>() {
            @Override
            public void onResponse(Call<List<FriendResponse>> call, Response<List<FriendResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    friendList.clear();
                    friendList.addAll(response.body());
                    adapter.notifyDataSetChanged(); // 데이터가 변경되었음을 어댑터에 알립니다.
                } else {
                    Toast.makeText(CreateGroupActivity.this, "친구 목록을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<FriendResponse>> call, Throwable t) {
                Toast.makeText(CreateGroupActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("CreateGroupActivity", "Network error", t);
            }
        });
    }

    private void createGroup() {
        if (adapter == null) {
            Toast.makeText(this, "친구 목록을 먼저 불러와주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String groupName = etGroupName.getText().toString();
        String startTimeStr = serverFormat.format(startTimeCalendar.getTime());
        String endTimeStr = serverFormat.format(endTimeCalendar.getTime());
        List<Long> selectedMemberIds = adapter.getSelectedFriendIds();

        if (selectedPlaceName == null || selectedPlaceLat == null || selectedPlaceLng == null) {
            Toast.makeText(this, "목적지를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ## ✅ 'Cannot resolve symbol' 오류 해결 ##
        // 변수 이름을 'request'로 통일합니다.

        CreateGroupRequest request = new CreateGroupRequest();
        request.setName(groupName);
        request.setDestinationName(selectedPlaceName);
        request.setDestinationLat(selectedPlaceLat);
        request.setDestinationLng(selectedPlaceLng);
        request.setStartTime(startTimeStr);
        request.setEndTime(endTimeStr);
        request.setMemberIds(selectedMemberIds);

        GroupApiService groupApiService = ApiClient.getGroupApiService(this);
        Call<Map<String, String>> call = groupApiService.createGroup(request);



        call.enqueue(new Callback<Map<String, String>>() { // Callback 타입도 Map<String, String>으로 변경
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateGroupActivity.this, "그룹이 생성되었습니다!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateGroupActivity.this, "그룹 생성 실패 (코드: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(CreateGroupActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}