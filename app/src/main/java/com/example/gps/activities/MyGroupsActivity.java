package com.example.gps.activities;

import android.os.Bundle;
import android.widget.Toast;
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

public class MyGroupsActivity extends AppCompatActivity {

    private RecyclerView rvGroups;
    private GroupListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_groups);

        rvGroups = findViewById(R.id.rvGroups);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        fetchMyGroups();
    }

    private void fetchMyGroups() {
        Call<List<GroupListResponse>> call = ApiClient.getGroupApiService(this).getMyGroups();
        call.enqueue(new Callback<List<GroupListResponse>>() {
            @Override
            public void onResponse(Call<List<GroupListResponse>> call, Response<List<GroupListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new GroupListAdapter(response.body(), MyGroupsActivity.this);
                    rvGroups.setAdapter(adapter);
                } else {
                    Toast.makeText(MyGroupsActivity.this, "그룹 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<GroupListResponse>> call, Throwable t) {
                Toast.makeText(MyGroupsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}