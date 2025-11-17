package com.example.gps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Toolbar import 추가
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.adapters.MessageAdapter;
import com.example.gps.models.Message;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;


public class ChatRoomActivity extends AppCompatActivity {

    private static final String TAG = "ChatRoomActivity";
    private FirebaseFirestore db;
    private CollectionReference chatCollection;

    private Long groupId;
    private String groupName;
    private String loggedInUsername;

    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private MessageAdapter adapter;
    private List<Message> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        // Intent 데이터 수신 (이전 단계에서 완료)
        groupId = getIntent().getLongExtra("groupId", -1L);
        groupName = getIntent().getStringExtra("groupName");
        loggedInUsername = getIntent().getStringExtra("username");

        // ⭐ [수정] 1. Toolbar 초기화 및 ActionBar로 설정
        Toolbar toolbar = findViewById(R.id.toolbarChat); // XML에서 ID를 'toolbarChat'으로 변경했다고 가정
        setSupportActionBar(toolbar);

        // ⭐ [수정] 2. 제목 설정 및 뒤로 가기 버튼 활성화
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(groupName); // 그룹 이름으로 제목 설정
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 뒤로 가기 버튼 활성화
        }

        // ⭐ 3. Firebase Firestore 인스턴스 초기화
        db = FirebaseFirestore.getInstance();

        // ⭐ 4. 특정 그룹의 채팅 메시지 컬렉션 참조 설정
        chatCollection = db.collection("chats")
                .document(String.valueOf(groupId))
                .collection("messages");

        // UI 요소 초기화
        rvMessages = findViewById(R.id.rvMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        ImageView btnSend = findViewById(R.id.btnSend);

        // Adapter 및 RecyclerView 초기화
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList, loggedInUsername);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        // 메시지 전송 버튼 리스너 설정
        btnSend.setOnClickListener(v -> sendMessage());

        // 실시간 메시지 리스너 시작
        startListeningForMessages();
    }

    // ⭐ [추가] 툴바의 뒤로 가기 버튼 클릭 이벤트 처리
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // 현재 Activity 종료
        return true;
    }

    // --- 메뉴 구성 ---
    // 1. 메뉴 XML 파일을 로드하여 Toolbar에 표시합니다.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 메뉴를 표시하지 않음
        return false;
    }

    // 2. 메뉴 항목이 클릭되었을 때의 동작을 정의합니다.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    // =========================================================================
    // A. 메시지 전송 로직
    // =========================================================================
    private void sendMessage() {
        String messageContent = etMessageInput.getText().toString().trim();

        if (TextUtils.isEmpty(messageContent)) {
            Toast.makeText(this, "메시지를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 새로운 Message 객체 생성
        Message newMessage = new Message(
                loggedInUsername,
                messageContent,
                System.currentTimeMillis()
        );

        // 2. Firestore에 메시지 저장 (비동기)
        chatCollection.add(newMessage)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "메시지 전송 성공: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "메시지 전송 실패", e);
                    Toast.makeText(this, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });

        // 3. 입력창 초기화
        etMessageInput.setText("");
    }

    // =========================================================================
    // B. 실시간 메시지 수신 로직
    // =========================================================================
    private void startListeningForMessages() {
        // 메시지 시간(timestamp)을 기준으로 오름차순 정렬하여 쿼리
        chatCollection.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(this, (snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "메시지 리스너 실패", e);
                        return;
                    }

                    if (snapshots != null) {
                        // 데이터 목록 업데이트
                        messageList.clear();

                        for (QueryDocumentSnapshot doc : snapshots) {
                            // Firestore 문서를 Message 객체로 변환
                            Message message = doc.toObject(Message.class);
                            messageList.add(message);
                        }

                        // UI 업데이트
                        adapter.notifyDataSetChanged();

                        // 스크롤을 항상 가장 아래(가장 최근 메시지)로 이동
                        if (messageList.size() > 0) {
                            rvMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }
}