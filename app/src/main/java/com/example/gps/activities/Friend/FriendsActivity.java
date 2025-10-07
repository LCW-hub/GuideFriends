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
import com.example.gps.adapters.FriendAdapter;
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
        SentRequestAdapter.OnCancelListener,
        FriendAdapter.OnDeleteClickListener  {

    private static final String TAG = "FriendsActivity";

    private EditText etFriendUsername;
    private Button btnAddFriend;
    private RecyclerView rvPendingRequests, rvFriendsList;

    private FriendAdapter friendAdapter;
    private PendingRequestAdapter pendingRequestAdapter;

    private List<User> friendList = new ArrayList<>();
    private List<User> pendingList = new ArrayList<>();

    private UserApi userApi;
    private String currentUsername; // 현재 로그인된 사용자 아이디

    private RecyclerView rvSentRequests;
    private SentRequestAdapter sentRequestAdapter;
    private List<User> sentList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        // 이전 Activity(MapsActivity)에서 현재 사용자 이름을 받아옴
        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null || currentUsername.isEmpty()) {
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish(); // 사용자 정보 없으면 액티비티 종료
            return;
        }

        userApi = ApiClient.getClient(this).create(UserApi.class);

        // UI 요소 초기화
        etFriendUsername = findViewById(R.id.etFriendUsername);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        rvPendingRequests = findViewById(R.id.rvPendingRequests);
        rvFriendsList = findViewById(R.id.rvFriendsList);
        rvSentRequests = findViewById(R.id.rvSentRequests);
        rvFriendsList = findViewById(R.id.rvFriendsList);

        setupRecyclerViews();
        setupClickListeners();

        // 화면이 생성될 때 친구 목록과 요청 목록을 서버에서 불러옴
        fetchFriends();
        fetchPendingRequests();
        fetchSentRequests();
    }

    // RecyclerView 설정
    private void setupRecyclerViews() {
        // 친구 목록 RecyclerView
        rvFriendsList.setLayoutManager(new LinearLayoutManager(this));
        friendAdapter = new FriendAdapter(friendList, this);
        rvFriendsList.setAdapter(friendAdapter);

        // 친구 요청 목록 RecyclerView
        rvPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        pendingRequestAdapter = new PendingRequestAdapter(pendingList, this); // 'this'를 리스너로 전달
        rvPendingRequests.setAdapter(pendingRequestAdapter);

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

                    Toast.makeText(FriendsActivity.this, message, Toast.LENGTH_SHORT).show(); // ✅ 서버 메시지 사용

                    if ("success".equals(status)) {
                        etFriendUsername.setText("");
                        fetchSentRequests(); // ✅ 요청 성공 시 '보낸 요청' 목록 새로고침
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

    // 7. '보낸 요청' 목록을 불러오는 새로운 메서드를 클래스 내부에 추가합니다.
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

    // 내 친구 목록 불러오기
    private void fetchFriends() {
        userApi.getFriends(currentUsername).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    friendList.clear();
                    friendList.addAll(response.body());
                    friendAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                Log.e(TAG, "Fetch friends failed: " + t.getMessage());
            }
        });
    }

    // 받은 친구 요청 목록 불러오기
    private void fetchPendingRequests() {
        // 새로 추가한 API를 정확히 호출합니다.
        userApi.getPendingFriendRequests(currentUsername).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    pendingList.clear();
                    pendingList.addAll(response.body());
                    pendingRequestAdapter.notifyDataSetChanged();
                    Log.d(TAG, "받은 요청 " + pendingList.size() + "개 불러오기 성공");
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

    // --- PendingRequestAdapter의 버튼 클릭 처리 ---

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

                    // --- 화면 즉시 업데이트 로직 ---
                    // 1. '받은 요청' 목록에서 해당 유저를 제거합니다.
                    pendingList.remove(user);

                    // 2. '내 친구' 목록에 해당 유저를 추가합니다.
                    friendList.add(user);

                    // 3. 두 목록의 어댑터에게 데이터가 변경되었음을 알려 화면을 새로 그리게 합니다.
                    pendingRequestAdapter.notifyDataSetChanged();
                    friendAdapter.notifyDataSetChanged();
                    // ------------------------------------

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
    public void onDeleteClick(User friend) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("myUsername", currentUsername);
        requestBody.put("friendUsername", friend.getUsername());

        userApi.deleteFriend(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendsActivity.this, friend.getUsername() + "님을 친구 목록에서 삭제했습니다.", Toast.LENGTH_SHORT).show();
                    // UI에서 즉시 제거
                    friendList.remove(friend);
                    friendAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(FriendsActivity.this, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(FriendsActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 8. '취소' 버튼 클릭을 처리하는 onCancel 메서드를 클래스 내부에 추가합니다.
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
                    // UI에서 즉시 제거
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
        requestBody.put("declinerUsername", currentUsername);     // 거절하는 사람 (나)
        requestBody.put("requesterUsername", user.getUsername()); // 요청을 보냈던 사람

        userApi.declineFriendRequest(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendsActivity.this, user.getUsername() + "님의 요청을 거절했습니다.", Toast.LENGTH_SHORT).show();

                    // UI에서 즉시 제거
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