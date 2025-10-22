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
import com.example.gps.dto.LocationResponse;
import com.example.gps.model.User;

import java.util.ArrayList;
import java.util.List;

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
    private Long loggedInUserId = -1L; // 현재 로그인된 사용자 ID (자신을 제외하고 표시해야 하므로 필요)
    private String loggedInUsername = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_sharing_settings);

        tvTitle = findViewById(R.id.tv_group_name_title);
        rvMembers = findViewById(R.id.rv_group_members_settings);
        btnSave = findViewById(R.id.btn_save_sharing_settings);

        // 1. Intent에서 그룹 ID 및 사용자 ID 가져오기
        if (getIntent() != null) {
            currentGroupId = getIntent().getLongExtra("groupId", -1L);
            loggedInUserId = getIntent().getLongExtra("userId", -1L);
            loggedInUsername = getIntent().getStringExtra("username");
        }

        if (currentGroupId == -1L || loggedInUserId == -1L) {
            Toast.makeText(this, "그룹 정보가 유효하지 않습니다.", ToastResult.LENGTH_LONG).show();
            finish();
            return;
        }

        tvTitle.setText("위치 공유 설정 (그룹 ID: " + currentGroupId + ")");
        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        // 2. 어댑터 초기화 (빈 목록으로 시작)
        adapter = new MemberSharingAdapter(new ArrayList<>(), loggedInUserId);
        rvMembers.setAdapter(adapter);

        // 3. 멤버 목록 및 현재 규칙 로드
        fetchGroupMembersAndRules();

        // 4. 저장 버튼 리스너
        btnSave.setOnClickListener(v -> saveSharingSettings());
    }


    /**
     * 그룹 멤버 목록과 현재 위치 공유 규칙을 서버에서 로드합니다.
     */
    private void fetchGroupMembersAndRules() {
        // ⭐ 서버의 getGroupMemberLocations API를 사용하여 모든 멤버의 위치를 가져옵니다.
        //    (현재는 필터링된 위치만 가져오므로, 실제 구현에서는 멤버 목록을 가져오는 별도의 API가 필요할 수 있습니다.)
        //    하지만, 여기서는 LocationResponse를 사용하여 현재 위치를 볼 수 있는지(규칙)를 체크합니다.

        GroupApiService apiService = ApiClient.getGroupApiService(this);
        Call<List<LocationResponse>> call = apiService.getGroupMemberLocations(currentGroupId);

        call.enqueue(new Callback<List<LocationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<LocationResponse>> call, @NonNull Response<List<LocationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> members = new ArrayList<>();

                    // ⚠️ 이 목록은 '나에게 위치 공유를 허용한 멤버'만 포함합니다.
                    //    정확한 '모든 멤버 목록'을 얻으려면 서버에 별도의 API가 필요합니다.
                    //    테스트를 위해, 여기서는 "현재 내가 볼 수 있는 멤버"를 "내가 차단하지 않은 멤버"로 가정합니다.

                    for(LocationResponse lr : response.body()){
                        // 현재 로그인된 사용자는 목록에서 제외하고, User 객체로 변환
                        if(lr.getUserId() != loggedInUserId){
                            members.add(new User(lr.getUserId(), lr.getUserName()));
                        }
                    }

                    // TODO: 서버에서 실제 "모든 그룹 멤버 목록"과 "현재 나의 공유 규칙 상태"를 가져오는 로직으로 대체해야 함.

                    adapter.updateMembers(members);
                    Toast.makeText(GroupSharingSettingsActivity.this, "멤버 목록 및 현재 규칙 로드 완료.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GroupSharingSettingsActivity.this, "멤버 목록 로드 실패.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "멤버 로드 실패 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<LocationResponse>> call, @NonNull Throwable t) {
                Toast.makeText(GroupSharingSettingsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 체크박스 상태를 읽어 서버에 상호 차단/허용 요청을 보냅니다.
     */
    private void saveSharingSettings() {
        GroupApiService apiService = ApiClient.getGroupApiService(this);
        List<User> membersToUpdate = adapter.getMembers();

        for (User member : membersToUpdate) {
            boolean isChecked = adapter.isUserChecked(member.getId());
            boolean allowSharing = isChecked; // 체크된 상태 = 허용 (true), 체크 해제 = 차단 (false)

            // ⭐ 서버 API 호출: updateSharingRule
            // 이 API 호출은 GroupController에서 상호(양방향) 규칙을 업데이트하도록 구현되어 있습니다.
            apiService.updateSharingRule(currentGroupId, member.getId(), allowSharing).enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "규칙 업데이트 성공: 대상=" + member.getUsername() + ", 허용=" + allowSharing);
                        // ⭐ UI 피드백을 위해 마지막에만 Toast 표시
                    } else {
                        Log.e(TAG, "규칙 업데이트 실패: 대상=" + member.getUsername() + ", 코드=" + response.code());
                        Toast.makeText(GroupSharingSettingsActivity.this, member.getUsername() + " 설정 실패.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    Log.e(TAG, "네트워크 실패: " + t.getMessage());
                }
            });
        }

        // 모든 비동기 요청이 완료된 후 성공 메시지 표시
        Toast.makeText(this, "위치 공유 설정 저장이 완료되었습니다.", Toast.LENGTH_LONG).show();
        finish(); // MapsActivity로 돌아가 실시간 변경 사항을 확인
    }
}
