package com.example.gps.adapters;

import android.app.AlertDialog; // 1. import 추가
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // 1. import 추가
import android.widget.TextView;
import android.widget.Toast; // 1. import 추가
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.api.ApiClient; // 1. import 추가
import com.example.gps.api.GroupApiService; // 1. import 추가
import com.example.gps.dto.GroupListResponse; // 1. import 추가 (DTO가 수정되었다고 가정)

import java.util.List;
import java.util.Map; // 1. import 추가

import retrofit2.Call; // 1. import 추가
import retrofit2.Callback; // 1. import 추가
import retrofit2.Response; // 1. import 추가

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupViewHolder> {

    // 1. 클릭 리스너 인터페이스 정의 (기존과 동일)
    public interface OnGroupClickListener {
        void onGroupClick(Long groupId, String groupName);
    }

    private final List<GroupListResponse> groupList;
    private final OnGroupClickListener listener;

    // ▼▼▼ [ 2. 필드 2개 추가 ] ▼▼▼
    private final String loggedInUsername; // 현재 로그인 사용자 이름
    private final Context context; // API 호출 및 다이얼로그용
    // ▲▲▲ [ 여기까지 추가 ] ▲▲▲

    // ▼▼▼ [ 3. 생성자 수정 ] ▼▼▼
    // (MyGroupsActivity에서 Context와 loggedInUsername을 전달받습니다)
    public GroupListAdapter(List<GroupListResponse> groupList, Context context, String loggedInUsername, OnGroupClickListener listener) {
        this.groupList = groupList;
        this.context = context;
        this.loggedInUsername = loggedInUsername;
        this.listener = listener;
    }
    // ▲▲▲ [ 여기까지 수정 ] ▲▲▲

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 이 코드는 item_group.xml (수정된 버전)을 inflate합니다.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupListResponse group = groupList.get(position);

        // --- 기존 정보 설정 ---
        holder.tvGroupName.setText(group.getGroupName());
        holder.tvDestination.setText(group.getDestinationName());

        // ▼▼▼ [ 4. 새로 추가된 뷰 설정 ] ▼▼▼

        // 멤버 수 설정 (GroupListResponse DTO에 getMemberCount()가 추가되었다고 가정)
        holder.tvMemberCount.setText(group.getMemberCount() + "명");

        // 방장 확인 및 삭제 버튼 로직
        // (GroupListResponse DTO에 getCreatedByUsername()이 추가되었다고 가정)
        if (group.getCreatedByUsername() != null && group.getCreatedByUsername().equals(loggedInUsername)) {
            // 내가 방장인 경우 -> 삭제 버튼 보이기
            holder.btnDeleteGroup.setVisibility(View.VISIBLE);
            holder.btnDeleteGroup.setOnClickListener(v -> {
                // 삭제 확인 다이얼로그 표시
                new AlertDialog.Builder(context)
                        .setTitle("그룹 삭제")
                        .setMessage("'" + group.getGroupName() + "' 그룹을 정말 삭제하시겠습니까?")
                        .setPositiveButton("삭제", (dialog, which) -> {
                            // "삭제" 누르면 API 호출
                            deleteGroupApiCall(group, holder.getAdapterPosition());
                        })
                        .setNegativeButton("취소", null)
                        .show();
            });
        } else {
            // 내가 방장이 아닌 경우 -> 삭제 버튼 숨기기
            holder.btnDeleteGroup.setVisibility(View.GONE);
        }
        // ▲▲▲ [ 여기까지 추가 ] ▲▲▲


        // --- 기존 항목 클릭 리스너 ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGroupClick(group.getGroupId(), group.getGroupName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    // ▼▼▼ [ 5. 삭제 API 호출 메소드 (신규 추가) ] ▼▼▼
    private void deleteGroupApiCall(GroupListResponse group, int position) {
        // 아이템이 유효한지 확인
        if (position == RecyclerView.NO_POSITION) return;

        // ApiClient를 통해 GroupApiService 가져오기
        GroupApiService apiService = ApiClient.getGroupApiService(context);
        // deleteGroup API 호출
        Call<Map<String, String>> call = apiService.deleteGroup(group.getGroupId());

        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    // 서버에서 성공적으로 삭제된 경우
                    Toast.makeText(context, "그룹이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    // 1. 데이터 목록(groupList)에서 아이템 제거
                    groupList.remove(position);
                    // 2. 어댑터에 아이템이 제거되었음을 알림 (애니메이션)
                    notifyItemRemoved(position);
                    // 3. (선택사항) 위치 변경이 있을 수 있으므로 나머지 아이템 갱신
                    notifyItemRangeChanged(position, groupList.size());
                } else {
                    // 서버가 삭제를 거부한 경우 (예: 권한 없음)
                    Toast.makeText(context, "그룹 삭제에 실패했습니다. (권한 없음)", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                // 네트워크 오류 발생
                Toast.makeText(context, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    // ▲▲▲ [ 여기까지 추가 ] ▲▲▲

    // ▼▼▼ [ 6. ViewHolder 정의 수정 ] ▼▼▼
    static class GroupViewHolder extends RecyclerView.ViewHolder {
        final TextView tvGroupName;
        final TextView tvDestination;
        final TextView tvMemberCount; // <-- [추가]
        final ImageView btnDeleteGroup; // <-- [추가]

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount); // <-- [추가]
            btnDeleteGroup = itemView.findViewById(R.id.btnDeleteGroup); // <-- [추가]
        }
    }
    // ▲▲▲ [ 여기까지 수정 ] ▲▲▲
}