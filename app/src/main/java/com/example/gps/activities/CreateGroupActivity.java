package com.example.gps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
import com.example.gps.model.SearchResult;
import com.example.gps.model.User;
import com.example.gps.utils.TokenManager; // ⭐ [추가] TokenManager를 import합니다.

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText etGroupName, etDestination, etStartTime, etEndTime;
    private RecyclerView rvFriends;
    private Button btnCreate;
    private FriendSelectAdapter adapter;
    private List<User> friendList = new ArrayList<>();

    private double destinationLat = 0.0;
    private double destinationLng = 0.0;

    // ---------------------------------------------------------------------------------------------
    // 1. 액티비티 생명주기 및 초기 설정
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        etGroupName = findViewById(R.id.etGroupName);
        etDestination = findViewById(R.id.etDestination);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        rvFriends = findViewById(R.id.rvFriends);
        btnCreate = findViewById(R.id.btnCreate);

        rvFriends.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FriendSelectAdapter(friendList);
        rvFriends.setAdapter(adapter);

        handleIncomingDestination(getIntent());

        etDestination.setFocusable(false);
        etDestination.setOnClickListener(v -> launchDestinationSearch());

        fetchGroupSelectableMembers();
        btnCreate.setOnClickListener(v -> createGroup());
    }

    // ... (onNewIntent, handleIncomingDestination, launchDestinationSearch 메서드는 동일하게 유지)

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingDestination(intent);
    }

    private void handleIncomingDestination(Intent intent) {
        if (intent != null && intent.hasExtra("destination_result")) {
            SearchResult selectedPlace = intent.getParcelableExtra("destination_result");

            if (selectedPlace != null) {
                destinationLat = selectedPlace.getLatitude();
                destinationLng = selectedPlace.getLongitude();
                etDestination.setText(selectedPlace.getTitle());
                Toast.makeText(this, "목적지: " + selectedPlace.getTitle() + " 설정 완료", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchDestinationSearch() {
        Toast.makeText(this, "지도 화면으로 이동하여 목적지를 검색해주세요.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(CreateGroupActivity.this, MapsActivity.class);
        intent.putExtra("mode", "destination_selection");

        startActivity(intent);
    }

    // ... (fetchGroupSelectableMembers 메서드도 동일하게 유지)

    private void fetchGroupSelectableMembers() {
        FriendApiService apiService = ApiClient.getClient(this).create(FriendApiService.class);
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


    // ---------------------------------------------------------------------------------------------
    // 3. 그룹 생성 로직 (오류 처리 수정)
    // ---------------------------------------------------------------------------------------------

    private void createGroup() {
        String groupName = etGroupName.getText().toString().trim();
        String destinationName = etDestination.getText().toString().trim();
        List<Long> selectedMemberIds = adapter.getSelectedFriendIds();

        if (groupName.isEmpty() || destinationName.isEmpty() || destinationLat == 0.0 || selectedMemberIds.isEmpty()) {
            Toast.makeText(this, "그룹 이름, 목적지, 최소 한 명의 친구를 선택해야 합니다.", Toast.LENGTH_LONG).show();
            return;
        }

        String startTimeStr = etStartTime.getText().toString();
        String endTimeStr = etEndTime.getText().toString();

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

        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateGroupActivity.this, "그룹이 생성되었습니다!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorBody = "N/A";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e("CreateGroupActivity", "Error body parsing failed", e);
                    }

                    Log.e("CreateGroupActivity", "그룹 생성 실패. 코드: " + response.code() + ", 본문: " + errorBody);

                    // ⭐ [수정] 403, 401 오류 발생 시 로그인 화면으로 리디렉션
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

    // ⭐ [추가] 인증 오류(401/403) 발생 시 토큰 삭제 및 로그인 화면으로 리디렉션
    private void handleAuthErrorAndRedirect() {
        Toast.makeText(this, "세션이 만료되었습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show();

        // TokenManager를 사용하여 토큰 및 사용자 정보 삭제
        // ⚠️ TokenManager에 deleteToken() 및 deleteUsername() 메서드가 구현되어 있어야 합니다.
        TokenManager tokenManager = new TokenManager(getApplicationContext());
        tokenManager.deleteToken();
        // tokenManager.deleteUsername(); // 사용자 이름도 삭제하여 완전히 로그아웃 상태로 만듦

        // 로그인 화면으로 이동 (액티비티 스택을 비우고 이동하여, 뒤로 가기로 돌아올 수 없게 함)
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}