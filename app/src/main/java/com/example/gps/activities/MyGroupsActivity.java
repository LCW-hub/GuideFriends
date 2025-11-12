package com.example.gps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.adapters.GroupListAdapter;
import com.example.gps.api.ApiClient;
import com.example.gps.dto.GroupListResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// ⭐ [수정] GroupListAdapter.OnGroupClickListener 인터페이스 구현
public class MyGroupsActivity extends AppCompatActivity implements GroupListAdapter.OnGroupClickListener {

    private RecyclerView rvGroups;
    private GroupListAdapter adapter;
    private String loggedInUsername; // GroupSharingSettingsActivity로 전달하기 위해 필요

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_groups);

        // MapsActivity 등에서 전달된 username을 가져와 저장합니다.
        loggedInUsername = getIntent().getStringExtra("username");

        // ⭐ [수정] 레이아웃의 ID가 rvGroupsfriends이지만, rvGroups로 사용하는 것이 일반적이므로 편의상 rvGroups로 사용합니다.
        rvGroups = findViewById(R.id.rvGroups);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        fetchMyGroups();
    }

    // ⭐ [추가] 그룹 클릭 시 호출되는 메서드 구현
    @Override
    public void onGroupClick(Long groupId, String groupName) {
        // 그룹 클릭 시 GroupSharingSettingsActivity로 이동
        Intent intent = new Intent(MyGroupsActivity.this, GroupSharingSettingsActivity.class);

        // ⭐⭐ 상호 차단 설정을 위해 필수 정보 전달 ⭐⭐
        intent.putExtra("groupId", groupId);
        intent.putExtra("username", loggedInUsername);
        intent.putExtra("groupName", groupName); // 그룹 이름도 전달하여 설정 화면 제목에 표시

        startActivity(intent);
    }

    private void fetchMyGroups() {
        Call<List<GroupListResponse>> call = ApiClient.getGroupApiService(this).getMyGroups();
        call.enqueue(new Callback<List<GroupListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<GroupListResponse>> call, @NonNull Response<List<GroupListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // ⭐ [수정] 어댑터 생성 시 this(OnGroupClickListener)를 전달
                    adapter = new GroupListAdapter(
                            response.body(),
                            MyGroupsActivity.this, // 1. Context 전달
                            loggedInUsername,      // 2. 로그인한 사용자 이름 전달
                            MyGroupsActivity.this  // 3. OnGroupClickListener 전달
                    );
                    rvGroups.setAdapter(adapter);
                } else {
                    Toast.makeText(MyGroupsActivity.this, "그룹 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GroupListResponse>> call, @NonNull Throwable t) {
                Toast.makeText(MyGroupsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
