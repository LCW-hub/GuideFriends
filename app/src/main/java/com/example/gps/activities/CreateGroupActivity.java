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

public class CreateGroupActivity extends AppCompatActivity {

    private EditText etGroupName, etDestination, etStartTime, etEndTime;
    private RecyclerView rvFriends;
    private Button btnCreate;
    private FriendSelectAdapter adapter;
    // friendList 변수를 클래스 멤버로 유지하여 어댑터와 데이터를 공유합니다.
    private List<FriendResponse> friendList = new ArrayList<>();

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

        // ## ✅ 'Expected 1 argument...' 오류 해결 ##
        // 어댑터를 비어있는 리스트로 먼저 초기화하고 RecyclerView에 설정합니다.
        adapter = new FriendSelectAdapter(friendList);
        rvFriends.setAdapter(adapter);

        fetchFriends();

        btnCreate.setOnClickListener(v -> createGroup());
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
        String destination = etDestination.getText().toString();
        String startTimeStr = etStartTime.getText().toString();
        String endTimeStr = etEndTime.getText().toString();
        List<Long> selectedMemberIds = adapter.getSelectedFriendIds();

        // ## ✅ 'Cannot resolve symbol' 오류 해결 ##
        // 변수 이름을 'request'로 통일합니다.
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName(groupName);
        request.setDestinationName(destination);
        request.setDestinationLat(37.5665);
        request.setDestinationLng(126.9780);
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