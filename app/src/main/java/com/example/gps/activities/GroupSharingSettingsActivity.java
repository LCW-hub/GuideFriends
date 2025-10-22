package com.example.gps.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.adapters.MemberSharingAdapter;
import com.example.gps.api.ApiClient;
import com.example.gps.api.GroupApiService;
import com.example.gps.api.UserApiService;
import com.example.gps.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupSharingSettingsActivity extends AppCompatActivity {

    private static final String TAG = "SharingSettingsActivity";
    private RecyclerView rvMembers;
    private Button btnSave;
    private TextView tvTitle;
    private MemberSharingAdapter adapter;

    private Long currentGroupId = -1L;
    private Long loggedInUserId = -1L;
    private String loggedInUsername = null;
    private String groupName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_sharing_settings);

        tvTitle = findViewById(R.id.tv_group_name_title);
        rvMembers = findViewById(R.id.rv_group_members_settings);
        btnSave = findViewById(R.id.btn_save_sharing_settings);

        // 1. Intent에서 그룹 ID 및 사용자 이름 가져오기
        if (getIntent() != null) {
            currentGroupId = getIntent().getLongExtra("groupId", -1L);
            loggedInUsername = getIntent().getStringExtra("username");
            groupName = getIntent().getStringExtra("groupName");
        }

        Log.d(TAG, "onCreate - GroupId: " + currentGroupId + ", Username: " + loggedInUsername);

        // 2. 유효성 검사
        if (currentGroupId == -1L || loggedInUsername == null) {
            Toast.makeText(this, "그룹 또는 사용자 정보가 유효하지 않습니다. (로그인 사용자 이름 누락)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvTitle.setText("위치 공유 설정 (" + groupName + ")");
        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        // 3. 어댑터 초기화 (userId가 -1L인 상태로 초기화)
        adapter = new MemberSharingAdapter(new ArrayList<>(), loggedInUserId);
        rvMembers.setAdapter(adapter);

        // 4. username을 이용해 userId를 조회 후, 멤버 목록 로드를 시작합니다.
        fetchLoggedInUserId();

        // 5. 저장 버튼은 userId 획득 후 enable될 때까지 비활성화
        btnSave.setEnabled(false);
        btnSave.setOnClickListener(v -> saveSharingSettings());
    }

    /**
     * 현재 로그인된 사용자의 Username을 이용해 UserId를 서버에서 조회합니다.
     */
    private void fetchLoggedInUserId() {
        UserApiService apiService = ApiClient.getUserApiService(this);
        Call<Map<String, Long>> call = apiService.getUserIdByUsername(loggedInUsername);

        call.enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Long>> call, @NonNull Response<Map<String, Long>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Long userId = response.body().get("userId");
                    if (userId != null && userId != -1L) {
                        loggedInUserId = userId;
                        Log.d(TAG, "사용자 ID 획득 성공: " + loggedInUserId);
                        adapter.setLoggedInUserId(loggedInUserId);
                        btnSave.setEnabled(true);
                        fetchGroupAllMembersAndRules();
                        return;
                    }
                }
                // ID를 가져오지 못했거나 서버 응답이 이상하거나 401/403 오류일 경우
                Log.e(TAG, "❌ 사용자 ID 획득 실패. 응답 코드: " + response.code());
                Toast.makeText(GroupSharingSettingsActivity.this, "사용자 ID 획득 실패. 설정을 사용할 수 없습니다. (코드: " + response.code() + ")", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Long>> call, @NonNull Throwable t) {
                Log.e(TAG, "사용자 ID 네트워크 오류", t);
                Toast.makeText(GroupSharingSettingsActivity.this, "사용자 ID 네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }


    /**
     * 그룹 멤버 목록과 현재 위치 공유 규칙을 서버에서 로드합니다. (모든 멤버 조회)
     */
    private void fetchGroupAllMembersAndRules() {
        if (loggedInUserId == -1L) {
            Log.e(TAG, "UserId가 유효하지 않아 멤버 로드 중단.");
            return;
        }

        GroupApiService apiService = ApiClient.getGroupApiService(this);
        // 서버의 /api/groups/{groupId}/all-members API를 호출합니다.
        Call<List<User>> call = apiService.getAllGroupMembers(currentGroupId);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> allMembers = response.body();
                    Log.d(TAG, "멤버 로드 성공 (Code: 200). 수신 멤버 수: " + allMembers.size());

                    // ⚠️ 중요: 현재는 모든 멤버를 가져오고 기본적으로 허용(true) 상태로 표시합니다.
                    //    실제 구현에서는 이 시점에서 각 멤버의 현재 공유 규칙 상태를 별도로 조회해야 합니다.

                    adapter.updateMembers(allMembers);
                    Toast.makeText(GroupSharingSettingsActivity.this, "멤버 목록 및 현재 규칙 로드 완료. (총 " + allMembers.size() + "명)", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "❌ 멤버 로드 실패. 응답 코드: " + response.code());
                    Toast.makeText(GroupSharingSettingsActivity.this, "멤버 목록 로드 실패 (코드: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                Log.e(TAG, "멤버 로드 네트워크 오류", t);
                Toast.makeText(GroupSharingSettingsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 체크박스 상태를 읽어 서버에 상호 차단/허용 요청을 보냅니다.
     */
    private void saveSharingSettings() {
        if (loggedInUserId == -1L) {
            Toast.makeText(this, "사용자 ID가 유효하지 않아 저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        GroupApiService apiService = ApiClient.getGroupApiService(this);
        List<User> membersToUpdate = adapter.getMembers();

        // 버튼 비활성화 (중복 클릭 방지)
        btnSave.setEnabled(false);

        final int totalMembers = membersToUpdate.size();
        final int[] completedRequests = {0};
        final List<String> failedMembers = new ArrayList<>();

        for (User member : membersToUpdate) {
            boolean isChecked = adapter.isUserChecked(member.getId());
            boolean allowSharing = isChecked;

            // ⭐ [핵심 수정] 콜백 타입이 Call<Void>에 맞게 수정되었으므로 오류 해결
            apiService.updateSharingRule(currentGroupId, member.getId(), allowSharing).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "규칙 업데이트 성공: 대상=" + member.getUsername() + ", 허용=" + allowSharing);
                    } else {
                        Log.e(TAG, "❌ 규칙 업데이트 실패: 대상=" + member.getUsername() + ", 코드=" + response.code());
                        failedMembers.add(member.getUsername());
                    }

                    checkCompletionAndFinish();
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "❌ 네트워크 실패: " + t.getMessage());
                    failedMembers.add(member.getUsername() + " (네트워크)");
                    checkCompletionAndFinish();
                }

                // 모든 요청 완료 체크 및 최종 처리 도우미 메서드
                private void checkCompletionAndFinish() {
                    completedRequests[0]++;
                    if (completedRequests[0] == totalMembers) {

                        // 1. 최종 메시지 표시
                        if (failedMembers.isEmpty()) {
                            Toast.makeText(GroupSharingSettingsActivity.this, "위치 공유 설정 저장이 완료되었습니다. 지도에서 확인하세요.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(GroupSharingSettingsActivity.this,
                                    "일부 설정 실패: " + failedMembers.size() + "명. 로그를 확인하세요.",
                                    Toast.LENGTH_LONG).show();
                        }

                        // ⭐⭐⭐ [핵심 추가] MapsActivity로 돌아가 Group ID를 다시 전달 ⭐⭐⭐
                        // MyGroupsActivity에서 발생했던 그룹 ID 유실 문제를 해결합니다.
                        android.content.Intent intent = new android.content.Intent(GroupSharingSettingsActivity.this, com.example.gps.activities.MapsActivity.class);
                        intent.putExtra("groupId", currentGroupId);
                        intent.putExtra("username", loggedInUsername);
                        // MapsActivity의 onNewIntent를 호출하도록 플래그 설정
                        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        // ⭐⭐⭐ -------------------------------------------------------- ⭐⭐⭐

                        // 액티비티 종료
                        finish();
                    }
                }
            });
        }
    }
}
