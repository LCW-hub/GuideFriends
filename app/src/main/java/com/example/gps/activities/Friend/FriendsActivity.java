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
import com.example.gps.api.FriendApiService;
import com.example.gps.dto.FriendResponse;
import com.example.gps.model.User;

// ⭐️ [추가] Firebase 실시간 감지용 Import
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ⭐️ [추가] Set/HashSet Import
import java.util.Set;
import java.util.HashSet;

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

    // ⭐️ [수정] 이 리스트를 어댑터가 직접 사용합니다.
    private List<User> friendList = new ArrayList<>();
    private List<User> pendingList = new ArrayList<>();
    private List<User> sentList = new ArrayList<>();

    private FriendApiService friendApiService;
    private String currentUsername;

    // --- ⭐️ [추가] 5단계: Presence (온라인 상태) 관련 변수 ---
    private DatabaseReference presenceRootRef; // "presence" 최상위 경로 참조
    private ValueEventListener presenceListener; // 온라인 상태 감지 리스너
    private final Set<Long> onlineUserIds = new HashSet<>(); // ⭐️ 온라인 상태인 유저 ID 캐시
    // --- ⭐️ [추가 끝] ---


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

        friendApiService = ApiClient.getFriendApiService(this);

        // UI 요소 초기화
        etFriendUsername = findViewById(R.id.etFriendUsername);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        rvPendingRequests = findViewById(R.id.rvPendingRequests);
        rvFriendsList = findViewById(R.id.rvFriendsList);
        rvSentRequests = findViewById(R.id.rvSentRequests);

        setupRecyclerViews();
        setupClickListeners();

        // ⭐️ [이동] fetch...() 호출은 onResume()으로 이동합니다.
    }

    // ⭐️ [추가] onResume()
    // 액티비티가 화면에 다시 나타날 때마다 목록을 새로고침하고 리스너를 시작합니다.
    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 생성될 때 친구 목록과 요청 목록을 서버에서 불러옴
        fetchFriends(); // ⭐️ 이 안에서 startPresenceListener()가 호출됩니다.
        fetchPendingRequests();
        fetchSentRequests();
    }

    // ⭐️ [추가] onPause()
    // 액티비티가 화면에서 사라질 때 리스너를 중지합니다. (배터리 절약)
    @Override
    protected void onPause() {
        super.onPause();
        stopPresenceListener(); // ⭐️ 온라인 감지 리스너 중지
    }

    // RecyclerView 설정
    private void setupRecyclerViews() {
        rvFriendsList.setLayoutManager(new LinearLayoutManager(this));
        // ⭐️ [수정] friendList를 어댑터에 전달 (원본 코드와 동일, 확인)
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
        // ... (기존 setupClickListeners 코드 - 수정 없음) ...
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
        // ... (기존 requestFriend 코드 - 수정 없음) ...
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("toUsername", toUsername);
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
        // ... (기존 fetchSentRequests 코드 - 수정 없음) ...
        friendApiService.getSentFriendRequests().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sentList.clear();
                    sentList.addAll(response.body());
                    sentRequestAdapter.notifyDataSetChanged();
                } else {
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
        friendApiService.getFriends().enqueue(new Callback<List<FriendResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<FriendResponse>> call, @NonNull Response<List<FriendResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    List<FriendResponse> friendResponses = response.body();

                    // ⭐️ [수정] MapsActivity와 동일하게 어댑터가 사용하는 'friendList'를 직접 수정
                    friendList.clear();

                    for (FriendResponse fr : friendResponses) {
                        User user = new User();
                        user.setId(fr.getFriendId());
                        user.setUsername(fr.getFriendUsername());
                        user.setProfileImageUrl(fr.getProfileImageUrl());

                        // ⭐️ [추가] API 응답 처리 시, 현재 캐시된 온라인 상태를 즉시 적용
                        user.setOnline(onlineUserIds.contains(user.getId()));

                        friendList.add(user); // ⭐️ 어댑터가 참조하는 리스트에 직접 추가
                    }

                    // ⭐️ [수정] 어댑터에 갱신 알림
                    friendAdapter.notifyDataSetChanged();

                    // ⭐️ [추가] 친구 목록 로드 성공 시, 온라인 상태 감지 리스너 시작
                    startPresenceListener();

                } else {
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
        // ... (기존 fetchPendingRequests 코드 - 수정 없음) ...
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

    // --- (이하 어댑터 클릭 리스너들: onAccept, onDecline, onCancel, onDeleteClick) ---
    // --- (모두 기존 코드와 동일, 수정 없음) ---
    @Override
    public void onAccept(User user) {
        // ... (기존 onAccept 코드) ...
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("requestUsername", user.getUsername());
        friendApiService.acceptFriend(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().get("status"))) {
                    Toast.makeText(FriendsActivity.this, user.getUsername() + "님의 친구 요청을 수락했습니다.", Toast.LENGTH_SHORT).show();
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
        // ... (기존 onDecline 코드) ...
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("requesterUsername", user.getUsername());
        friendApiService.declineFriendRequest(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendsActivity.this, user.getUsername() + "님의 요청을 거절했습니다.", Toast.LENGTH_SHORT).show();
                    fetchPendingRequests();
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

    @Override
    public void onCancel(User user) {
        // ... (기존 onCancel 코드) ...
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("toUsername", user.getUsername());
        friendApiService.cancelFriendRequest(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendsActivity.this, "요청을 취소했습니다.", Toast.LENGTH_SHORT).show();
                    fetchSentRequests();
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
    public void onDeleteClick(User friend) {
        // ... (기존 onDeleteClick 코드) ...
        if (friend.getId() == null) {
            Toast.makeText(this, "친구 ID가 없어 삭제할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        friendApiService.deleteFriend(friend.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendsActivity.this, friend.getUsername() + "님을 친구 목록에서 삭제했습니다.", Toast.LENGTH_SHORT).show();
                    fetchFriends();
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

    // --- ⭐️ [추가] 5단계: 온라인 상태 감지 리스너 관련 메서드 3개 ---
    // (MapsActivity에서 복사 + friendList를 사용하도록 수정)

    /**
     * Firebase의 "presence" 노드를 구독하여 실시간 온라인 상태를 감지합니다.
     */
    private void startPresenceListener() {
        if (presenceListener != null) {
            Log.d(TAG, "startPresenceListener: 이미 리스너가 실행 중입니다.");
            updateAdapterWithOnlineStatus();
            return;
        }

        presenceRootRef = FirebaseDatabase.getInstance().getReference("presence");
        Log.d(TAG, "startPresenceListener: 온라인 상태 감지 리스너 등록 시작.");

        presenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                onlineUserIds.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    try {
                        Long onlineUserId = Long.parseLong(userSnapshot.getKey());
                        onlineUserIds.add(onlineUserId);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Presence_Listener: 잘못된 User ID 형식 감지: " + userSnapshot.getKey());
                    }
                }
                Log.d(TAG, "Presence_Listener: 온라인 사용자 " + onlineUserIds.size() + "명 감지.");
                updateAdapterWithOnlineStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Presence_Listener: 온라인 상태 감지 실패", error.toException());
            }
        };
        presenceRootRef.addValueEventListener(presenceListener);
    }

    /**
     * 온라인 상태가 변경될 때마다 어댑터의 데이터를 갱신합니다.
     */
    private void updateAdapterWithOnlineStatus() {
        // ⭐️ [수정] myPageFriendsList -> friendList
        if (friendAdapter == null || friendList.isEmpty()) {
            return;
        }

        boolean needsUpdate = false;
        // ⭐️ [수정] myPageFriendsList -> friendList
        for (User user : friendList) {
            boolean isNowOnline = onlineUserIds.contains(user.getId());

            if (user.isOnline() != isNowOnline) {
                user.setOnline(isNowOnline);
                needsUpdate = true;
            }
        }

        if (needsUpdate) {
            Log.d(TAG, "updateAdapterWithOnlineStatus: 친구 목록 UI 갱신.");
            runOnUiThread(() -> {
                if (friendAdapter != null) {
                    friendAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    /**
     * 액티비티가 중지/종료될 때 리스너를 제거합니다.
     */
    private void stopPresenceListener() {
        if (presenceRootRef != null && presenceListener != null) {
            presenceRootRef.removeEventListener(presenceListener);
            presenceListener = null;
            Log.d(TAG, "stopPresenceListener: 온라인 상태 감지 리스너 제거 완료.");
        }
    }
    // --- ⭐️ [추가 끝] ---
}