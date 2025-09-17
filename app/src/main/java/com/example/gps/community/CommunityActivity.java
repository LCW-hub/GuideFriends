package com.example.gps.community;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import com.example.gps.Comment;
import com.example.gps.adapters.CommentAdapter;
import com.example.gps.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import android.widget.LinearLayout;
import com.google.firebase.FirebaseApp;
import android.util.Log;

public class CommunityActivity extends AppCompatActivity {
    private List<CommunityPost> postList;
    private CommunityAdapter adapter;
    private FirebaseFirestore db;
    private CollectionReference postsRef;
    private boolean isAdmin = true; // 관리자 여부(임시)
    private String currentUserId = "user_" + System.currentTimeMillis(); // 임시 사용자 ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        // Firebase 초기화 확인 및 초기화
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            Log.d("CommunityActivity", "Firebase 초기화 완료");
        } catch (Exception e) {
            // Firebase 초기화 실패 시 기본 초기화 시도
            FirebaseApp.initializeApp(this);
            Log.e("CommunityActivity", "Firebase 초기화 실패: " + e.getMessage(), e);
        }

        // Firestore 초기화
        db = FirebaseFirestore.getInstance();
        postsRef = db.collection("posts");
        Log.d("CommunityActivity", "Firestore 초기화 완료");

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("산책 커뮤니티");

        // RecyclerView 및 Adapter 초기화
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        postList = new ArrayList<>();
        adapter = new CommunityAdapter(postList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 삭제 콜백 연결
        adapter.setOnPostDeleteListener((position, post) -> {
            // Firestore에서 삭제
            postsRef.whereEqualTo("title", post.getTitle())
                    .whereEqualTo("content", post.getContent())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            doc.getReference().delete();
                        }
                    });
        });
        
        // 좋아요 콜백 연결
        adapter.setOnPostLikeListener((position, post) -> {
            // Firestore에서 좋아요 수 업데이트 (사용자별 중복 방지)
            postsRef.whereEqualTo("title", post.getTitle())
                    .whereEqualTo("content", post.getContent())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String postId = doc.getId();
                            // 사용자가 이미 좋아요를 눌렀는지 확인
                            doc.getReference().collection("likes").document(currentUserId)
                                    .get()
                                    .addOnSuccessListener(likeDoc -> {
                                        if (likeDoc.exists()) {
                                            // 이미 좋아요를 눌렀으면 취소
                                            likeDoc.getReference().delete();
                                            long currentLikes = doc.getLong("likes") != null ? doc.getLong("likes") : 0;
                                            doc.getReference().update("likes", Math.max(0, currentLikes - 1));
                                        } else {
                                            // 좋아요 추가
                                            Map<String, Object> likeData = new HashMap<>();
                                            likeData.put("userId", currentUserId);
                                            likeData.put("timestamp", System.currentTimeMillis());
                                            doc.getReference().collection("likes").document(currentUserId).set(likeData);
                                            long currentLikes = doc.getLong("likes") != null ? doc.getLong("likes") : 0;
                                            doc.getReference().update("likes", currentLikes + 1);
                                        }
                                    });
                        }
                    });
        });
        
        // 댓글 콜백 연결
        adapter.setOnPostCommentListener((position, post) -> {
            showCommentDialog(post);
        });

        // Firestore에서 게시글 실시간 불러오기
        postsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) return;
                postList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    String author = doc.getString("author");
                    String title = doc.getString("title");
                    String content = doc.getString("content");
                    String timeAgo = doc.getString("timeAgo");
                    String emoji = doc.getString("emoji");
                    int likes = doc.getLong("likes") != null ? doc.getLong("likes").intValue() : 0;
                    int comments = doc.getLong("comments") != null ? doc.getLong("comments").intValue() : 0;
                    postList.add(new CommunityPost(author, title, content, timeAgo, emoji, likes, comments));
                }
                adapter.notifyDataSetChanged();
            }
        });

        // 글쓰기 버튼 클릭 시 다이얼로그 표시
        Button btnWritePost = findViewById(R.id.btnWritePost);
        btnWritePost.setOnClickListener(v -> showWriteDialog());
    }

    private void showWriteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.write_post_dialog, null);
        EditText etTitle = dialogView.findViewById(R.id.etDialogTitle);
        EditText etContent = dialogView.findViewById(R.id.etDialogContent);
        EditText etEmoji = dialogView.findViewById(R.id.etDialogEmoji);
        Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnSubmit = dialogView.findViewById(R.id.btnDialogSubmit);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            String emoji = etEmoji.getText().toString().trim();
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "제목과 내용을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("author", "익명");
            postMap.put("title", title);
            postMap.put("content", content);
            postMap.put("timeAgo", "방금 전");
            postMap.put("emoji", emoji.isEmpty() ? "😊" : emoji);
            postMap.put("likes", 0);
            postMap.put("comments", 0);
            postMap.put("timestamp", System.currentTimeMillis());
            postsRef.add(postMap)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "게시글이 성공적으로 업로드되었습니다.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e("CommunityActivity", "업로드 실패: " + e.getMessage(), e);
                    Toast.makeText(this, "업로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        });
        dialog.show();
    }

    private void showCommentDialog(CommunityPost post) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.comment_dialog, null);
        RecyclerView rvComments = dialogView.findViewById(R.id.rvComments);
        EditText etComment = dialogView.findViewById(R.id.etComment);
        Button btnAddComment = dialogView.findViewById(R.id.btnAddComment);
        Button btnCloseComment = dialogView.findViewById(R.id.btnCloseComment);
        
        List<Comment> commentList = new ArrayList<>();
        CommentAdapter commentAdapter = new CommentAdapter(commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
        
        // 댓글 삭제 콜백 연결
        commentAdapter.setOnCommentDeleteListener((position, comment) -> {
            // Firestore에서 댓글 삭제
            postsRef.whereEqualTo("title", post.getTitle())
                    .whereEqualTo("content", post.getContent())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String postId = doc.getId();
                            // 댓글 ID로 삭제 (실제로는 댓글 내용으로 찾기)
                            db.collection("posts").document(postId).collection("comments")
                                    .whereEqualTo("content", comment.getContent())
                                    .whereEqualTo("author", comment.getAuthor())
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(commentDocs -> {
                                        for (DocumentSnapshot commentDoc : commentDocs) {
                                            commentDoc.getReference().delete();
                                        }
                                    });
                        }
                    });
        });
        
        // 답글 콜백 연결
        commentAdapter.setOnCommentReplyListener((position, comment) -> {
            // 답글 입력 UI 표시
            LinearLayout llReplyInput = dialogView.findViewById(R.id.llReplyInput);
            EditText etReply = dialogView.findViewById(R.id.etReply);
            Button btnAddReply = dialogView.findViewById(R.id.btnAddReply);
            
            llReplyInput.setVisibility(View.VISIBLE);
            etReply.requestFocus();
            
            btnAddReply.setOnClickListener(v -> {
                String replyContent = etReply.getText().toString().trim();
                if (replyContent.isEmpty()) {
                    Toast.makeText(this, "답글을 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 답글 저장
                postsRef.whereEqualTo("title", post.getTitle())
                        .whereEqualTo("content", post.getContent())
                        .limit(1)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                String postId = doc.getId();
                                Map<String, Object> replyMap = new HashMap<>();
                                replyMap.put("author", "익명");
                                replyMap.put("content", replyContent);
                                replyMap.put("timeAgo", "방금 전");
                                replyMap.put("timestamp", System.currentTimeMillis());
                                replyMap.put("parentCommentId", comment.getAuthor() + "_" + comment.getTimestamp());
                                
                                db.collection("posts").document(postId).collection("comments")
                                        .add(replyMap)
                                        .addOnSuccessListener(documentReference -> {
                                            etReply.setText("");
                                            llReplyInput.setVisibility(View.GONE);
                                        });
                            }
                        });
            });
        });
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        // 댓글 목록 불러오기
        postsRef.whereEqualTo("title", post.getTitle())
                .whereEqualTo("content", post.getContent())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String postId = doc.getId();
                        db.collection("posts").document(postId).collection("comments")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                        if (error != null) return;
                                        commentList.clear();
                                        for (DocumentSnapshot commentDoc : value.getDocuments()) {
                                            String author = commentDoc.getString("author");
                                            String content = commentDoc.getString("content");
                                            String timeAgo = commentDoc.getString("timeAgo");
                                            long timestamp = commentDoc.getLong("timestamp") != null ? commentDoc.getLong("timestamp") : 0;
                                            commentList.add(new Comment(author, content, timeAgo, timestamp));
                                        }
                                        commentAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
        
        // 댓글 추가
        btnAddComment.setOnClickListener(v -> {
            String commentContent = etComment.getText().toString().trim();
            if (commentContent.isEmpty()) {
                Toast.makeText(this, "댓글을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            postsRef.whereEqualTo("title", post.getTitle())
                    .whereEqualTo("content", post.getContent())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String postId = doc.getId();
                            Map<String, Object> commentMap = new HashMap<>();
                            commentMap.put("author", "익명");
                            commentMap.put("content", commentContent);
                            commentMap.put("timeAgo", "방금 전");
                            commentMap.put("timestamp", System.currentTimeMillis());
                            
                            db.collection("posts").document(postId).collection("comments")
                                    .add(commentMap)
                                    .addOnSuccessListener(documentReference -> {
                                        etComment.setText("");
                                        Toast.makeText(this, "댓글이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                        // 댓글 수 업데이트
                                        long currentComments = doc.getLong("comments") != null ? doc.getLong("comments") : 0;
                                        doc.getReference().update("comments", currentComments + 1);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("CommunityActivity", "댓글 추가 실패: " + e.getMessage(), e);
                                        Toast.makeText(this, "댓글 추가 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    });
        });
        
        // 닫기 버튼
        btnCloseComment.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 