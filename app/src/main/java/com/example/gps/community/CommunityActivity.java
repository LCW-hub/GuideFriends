package com.example.gps.community;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.api.ApiClient;
import com.example.gps.api.BoardApiService;
import com.example.gps.model.Board;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityActivity extends AppCompatActivity {
    private List<Board> postList;
    private CommunityAdapter adapter;
    private BoardApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        // 툴바
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("산책 커뮤니티");

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        postList = new ArrayList<>();
        adapter = new CommunityAdapter(postList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Retrofit 서비스 초기화
        apiService = ApiClient.getClient().create(BoardApiService.class);

        // 게시글 목록 불러오기
        loadPosts();

        // 삭제 콜백
        adapter.setOnPostDeleteListener((position, post) -> {
            apiService.deleteBoard(post.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        postList.remove(position);
                        adapter.notifyItemRemoved(position);
                        Toast.makeText(CommunityActivity.this, "삭제 성공", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(CommunityActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // 글쓰기 버튼
        Button btnWritePost = findViewById(R.id.btnWritePost);
        btnWritePost.setOnClickListener(v -> showWriteDialog());
    }

    private void loadPosts() {
        apiService.getBoards().enqueue(new Callback<List<Board>>() {
            @Override
            public void onResponse(Call<List<Board>> call, Response<List<Board>> response) {
                if (response.isSuccessful()) {
                    postList.clear();
                    postList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<Board>> call, Throwable t) {
                Toast.makeText(CommunityActivity.this, "불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWriteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.write_post_dialog, null);
        EditText etTitle = dialogView.findViewById(R.id.etDialogTitle);
        EditText etContent = dialogView.findViewById(R.id.etDialogContent);
        Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnSubmit = dialogView.findViewById(R.id.btnDialogSubmit);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "제목과 내용을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            Board newBoard = new Board(title, content);
            apiService.createBoard(newBoard).enqueue(new Callback<Board>() {
                @Override
                public void onResponse(Call<Board> call, Response<Board> response) {
                    if (response.isSuccessful()) {
                        postList.add(0, response.body()); // 최신글 맨 위에 추가
                        adapter.notifyItemInserted(0);
                        dialog.dismiss();
                        Toast.makeText(CommunityActivity.this, "게시글 작성 성공", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Board> call, Throwable t) {
                    Toast.makeText(CommunityActivity.this, "작성 실패", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
