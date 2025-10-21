package com.example.gps.activities.Friend;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.adapters.PendingRequestAdapter;
import com.example.gps.adapters.SentRequestAdapter;
import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;
import com.example.gps.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendsActivity extends AppCompatActivity
        implements PendingRequestAdapter.OnRequestActionListener,
        SentRequestAdapter.OnCancelListener {

    private static final String TAG = "FriendsActivity";

    private EditText etFriendUsername;
    private Button btnAddFriend;
    private RecyclerView rvPendingRequests;
    private RecyclerView rvSentRequests;
    private PendingRequestAdapter pendingRequestAdapter;
    private SentRequestAdapter sentRequestAdapter;

    private List<User> pendingList = new ArrayList<>();
    private List<User> sentList = new ArrayList<>();

    private UserApi userApi;
    private String currentUsername; // 현재 로그인된 사용자 아이디

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null || currentUsername.isEmpty()) {
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userApi = ApiClient.getClient(this).create(UserApi.class);

        etFriendUsername = findViewById(R.id.etFriendUsername);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        rvPendingRequests = findViewById(R.id.rvPendingRequests);
        rvSentRequests = findViewById(R.id.rvSentRequests);

        setupRecyclerViews();
        setupClickListeners();

        // 친구 요청 관련 목록만 불러옵니다.
        fetchPendingRequests();
        fetchSentRequests();
    }

    // RecyclerView 설정
    private void setupRecyclerViews() {
        // 받은 친구 요청 목록 RecyclerView
        rvPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        pendingRequestAdapter = new PendingRequestAdapter(pendingList, this);
        rvPendingRequests.setAdapter(pendingRequestAdapter);

        // 보낸 친구 요청 목록 RecyclerView
        rvSentRequests.setLayoutManager(new LinearLayoutManager(this));
        sentRequestAdapter = new SentRequestAdapter(sentList, this);
        rvSentRequests.setAdapter(sentRequestAdapter);
    }

    // 버튼 클릭 리스너 설정
    private void setupClickListeners() {
        btnAddFriend.setOnClickListener(v -> {
            String friendUsername = etFriendUsername.getText().toString().trim();
            if (friendUsername.isEmpty()) {
                Toast.makeText(this, "친구 아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (friendUsername.equals(currentUsername)) {
                Toast.makeText(this, "자기 자신에게는 친구 요청을 보낼 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            requestFriend(friendUsername);
        });
    }

    // 서버에 친구 요청 보내기
    private void requestFriend(String toUsername) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("fromUsername", currentUsername);
        requestBody.put("toUsername", toUsername);

        userApi.requestFriend(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String status = (String) response.body().get("status");
                    String message = (String) response.body().get("message");
                    Toast.makeText(FriendsActivity.this, message, Toast.LENGTH_SHORT).show();

                    if ("success".equals(status)) {
                        etFriendUsername.setText("");
                        fetchSentRequests();
                    }
                } else {
                    Toast.makeText(FriendsActivity.this, "요청에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Log.e(TAG, "Friend request failed: " + t.getMessage());
                Toast.makeText(FriendsActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 보낸 요청 목록 불러오기
    private void fetchSentRequests() {
        userApi.getSentFriendRequests(currentUsername).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sentList.clear();
                    sentList.addAll(response.body());
                    sentRequestAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                Log.e(TAG, "Fetch sent requests failed: " + t.getMessage());
            }
        });
    }

    // 받은 친구 요청 목록 불러오기
    private void fetchPendingRequests() {
        userApi.getPendingFriendRequests(currentUsername).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    pendingList.clear();
                    pendingList.addAll(response.body());
                    pendingRequestAdapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "받은 요청 불러오기 실패, 응답 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                Log.e(TAG, "Fetch pending requests failed: " + t.getMessage());
            }
        });
    }

    // --- Adapter 버튼 클릭 처리 ---

    @Override
    public void onAccept(User user) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("currentUsername", currentUsername);
        requestBody.put("requestUsername", user.getUsername());

        userApi.acceptFriend(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().get("status"))) {
                    Toast.makeText(FriendsActivity.this, user.getUsername() + "님의 친구 요청을 수락했습니다.", Toast.LENGTH_SHORT).show();
                    // 요청 목록 새로고침
                    fetchPendingRequests();
                    // 친구 추가 페이지에서는 더 이상 친구 목록을 관리하지 않으므로 fetchFriends() 호출 불필요
                } else {
                    Toast.makeText(FriendsActivity.this, "요청 수락에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(FriendsActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCancel(User user) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("fromUsername", currentUsername);
        requestBody.put("toUsername", user.getUsername());

        userApi.cancelFriendRequest(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendsActivity.this, "요청을 취소했습니다.", Toast.LENGTH_SHORT).show();
                    sentList.remove(user);
                    sentRequestAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(FriendsActivity.this, "취소에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(FriendsActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDecline(User user) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("declinerUsername", currentUsername);
        requestBody.put("requesterUsername", user.getUsername());

        userApi.declineFriendRequest(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendsActivity.this, user.getUsername() + "님의 요청을 거절했습니다.", Toast.LENGTH_SHORT).show();
                    pendingList.remove(user);
                    pendingRequestAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(FriendsActivity.this, "거절에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(FriendsActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}