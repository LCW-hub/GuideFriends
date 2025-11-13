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
// ⭐️ [수정] FriendApiService 임포트
import com.example.gps.api.FriendApiService;
import com.example.gps.dto.FriendResponse; // ⭐️ [추가] FriendResponse DTO 임포트
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
    private RecyclerView rvPendingRequests, rvFriendsList, rvSentRequests;

    private FriendAdapter friendAdapter;
    private PendingRequestAdapter pendingRequestAdapter;
    private SentRequestAdapter sentRequestAdapter;

    private List<User> friendList = new ArrayList<>();
    private List<User> pendingList = new ArrayList<>();
    private List<User> sentList = new ArrayList<>();

    // ⭐️ [수정] UserApi -> FriendApiService
    private FriendApiService friendApiService;
    private String currentUsername;

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

        // ⭐️ [수정] MapsActivity와 동일한 방식으로 FriendApiService 초기화
        friendApiService = ApiClient.getFriendApiService(this);

        // UI 요소 초기화
        etFriendUsername = findViewById(R.id.etFriendUsername);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        rvPendingRequests = findViewById(R.id.rvPendingRequests);
        rvFriendsList = findViewById(R.id.rvFriendsList);
        rvSentRequests = findViewById(R.id.rvSentRequests);

        setupRecyclerViews();
        setupClickListeners();

        // 화면이 생성될 때 친구 목록과 요청 목록을 서버에서 불러옴
        fetchFriends();
        fetchPendingRequests();
        fetchSentRequests();
    }

    // RecyclerView 설정
    private void setupRecyclerViews() {
        rvFriendsList.setLayoutManager(new LinearLayoutManager(this));
        // ⭐️ [수정] friendList가 null일 수 있으므로 안전하게 초기화
        friendAdapter = new FriendAdapter(friendList, this);
        rvFriendsList.setAdapter(friendAdapter);

        rvPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        pendingRequestAdapter = new PendingRequestAdapter(pendingList, this);
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
        // ⭐️ [수정] "fromUsername" 제거 (서버가 토큰에서 가져옴)
        requestBody.put("toUsername", toUsername);

        // ⭐️ [수정] userApi -> friendApiService
        friendApiService.requestFriend(requestBody).enqueue(new Callback<Map<String, Object>>() {
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
                    String errorMsg = "요청에 실패했습니다.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " (코드: " + response.code() + ")";
                        }
                    } catch (Exception e) {}
                    Toast.makeText(FriendsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Log.e(TAG, "Friend request failed: " + t.getMessage());
                Toast.makeText(FriendsActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // '보낸 요청' 목록 불러오기
    private void fetchSentRequests() {
        // ⭐️ [수정] userApi -> friendApiService, 파라미터 제거
        friendApiService.getSentFriendRequests().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sentList.clear();
                    sentList.addAll(response.body());
                    sentRequestAdapter.notifyDataSetChanged();
                } else { // ⭐️ [추가] 실패 로그
                    Log.e(TAG, "Fetch sent requests failed, Code: " + response.code());
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
        // ⭐️ [수정] userApi -> friendApiService, 파라미터 제거, DTO 사용
        friendApiService.getFriends().enqueue(new Callback<List<FriendResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<FriendResponse>> call, @NonNull Response<List<FriendResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // ⭐️ [추가] MapsActivity와 동일하게 DTO -> Model 변환
                    List<FriendResponse> friendResponses = response.body();
                    List<User> friends = new ArrayList<>();
                    for (FriendResponse fr : friendResponses) {
                        User user = new User();
                        user.setId(fr.getFriendId());
                        user.setUsername(fr.getFriendUsername());
                        user.setProfileImageUrl(fr.getProfileImageUrl()); // 프로필 이미지 URL 복사
                        friends.add(user);
                    }

                    friendList.clear();
                    friendList.addAll(friends); // 변환된 리스트 추가
                    friendAdapter.setFriends(friendList); // ⭐️ [수정] 어댑터에 새 리스트 설정
                } else { // ⭐️ [추가] 실패 로그
                    Log.e(TAG, "Fetch friends failed, Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FriendResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Fetch friends failed: " + t.getMessage());
            }
        });
    }

    // 받은 친구 요청 목록 불러오기
    private void fetchPendingRequests() {
        // ⭐️ [수정] userApi -> friendApiService, 파라미터 제거
        friendApiService.getPendingFriendRequests().enqueue(new Callback<List<User>>() {
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
        // ⭐️ [수정] "currentUsername" 제거 (서버가 토큰에서 가져옴)
        requestBody.put("requestUsername", user.getUsername()); // 요청 보낸 사람

        // ⭐️ [수정] userApi -> friendApiService
        friendApiService.acceptFriend(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().get("status"))) {
                    Toast.makeText(FriendsActivity.this, user.getUsername() + "님의 친구 요청을 수락했습니다.", Toast.LENGTH_SHORT).show();

                    // ⭐️ [수정] 목록 새로고침 (가장 확실한 방법)
                    fetchPendingRequests();
                    fetchFriends();
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
    public void onDecline(User user) {
        Map<String, String> requestBody = new HashMap<>();
        // ⭐️ [수정] "declinerUsername" 제거 (서버가 토큰에서 가져옴)
        requestBody.put("requesterUsername", user.getUsername());

        // ⭐️ [수정] userApi -> friendApiService
        friendApiService.declineFriendRequest(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendsActivity.this, user.getUsername() + "님의 요청을 거절했습니다.", Toast.LENGTH_SHORT).show();
                    fetchPendingRequests(); // ⭐️ 목록 새로고침
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

    // --- SentRequestAdapter의 버튼 클릭 처리 ---

    @Override
    public void onCancel(User user) {
        Map<String, String> requestBody = new HashMap<>();
        // ⭐️ [수정] "fromUsername" 제거 (서버가 토큰에서 가져옴)
        requestBody.put("toUsername", user.getUsername());

        // ⭐️ [수정] userApi -> friendApiService
        friendApiService.cancelFriendRequest(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendsActivity.this, "요청을 취소했습니다.", Toast.LENGTH_SHORT).show();
                    fetchSentRequests(); // ⭐️ 목록 새로고침
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

    // --- FriendAdapter의 버튼 클릭 처리 ---

    @Override
    public void onDeleteClick(User friend) {
        // ⭐️ [수정] MapsActivity와 동일하게 RESTful API 호출 방식으로 변경

        if (friend.getId() == null) {
            Toast.makeText(this, "친구 ID가 없어 삭제할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ⭐️ [수정] userApi -> friendApiService, ID로 삭제
        friendApiService.deleteFriend(friend.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendsActivity.this, friend.getUsername() + "님을 친구 목록에서 삭제했습니다.", Toast.LENGTH_SHORT).show();
                    fetchFriends(); // ⭐️ 목록 새로고침
                } else {
                    Toast.makeText(FriendsActivity.this, "삭제에 실패했습니다. (코드: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(FriendsActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}