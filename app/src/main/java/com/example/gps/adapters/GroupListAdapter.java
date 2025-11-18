package com.example.gps.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.dto.GroupListResponse;

import java.util.List;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupViewHolder> {

    // ⭐ [수정 1] 클릭 리스너 인터페이스를 하나로 정의 (삭제 기능 포함)
    public interface OnGroupClickListener {
        void onGroupClick(Long groupId, String groupName);
        void onDeleteClick(Long groupId, int position); // 삭제 버튼 클릭 시 호출
    }

    private final List<GroupListResponse> groupList;
    private final OnGroupClickListener listener;

    // 필드 2개 유지
    private final String loggedInUsername;
    private final Context context;

    // ⭐ [수정 2] 생성자 수정: Context와 loggedInUsername을 멤버 변수에 저장합니다.
    public GroupListAdapter(List<GroupListResponse> groupList, Context context, String loggedInUsername, OnGroupClickListener listener) {
        this.groupList = groupList;
        this.context = context;
        this.loggedInUsername = loggedInUsername;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupListResponse group = groupList.get(position);

        // --- 기존 정보 설정 ---
        holder.tvGroupName.setText(group.getGroupName());
        // GroupListResponse에 getDestinationName()이 있다고 가정
        holder.tvDestination.setText(group.getDestinationName());

        // 멤버 수 설정 (GroupListResponse DTO에 getMemberCount()가 있다고 가정)
        holder.tvMemberCount.setText(group.getMemberCount() + "명");

        // ▼▼▼ [ 핵심 삭제 버튼 로직 수정 ] ▼▼▼

        // 방장 확인 및 삭제 버튼 로직
        // (GroupListResponse DTO에 getCreatedByUsername()이 있다고 가정)
        if (group.getCreatedByUsername() != null && group.getCreatedByUsername().equals(loggedInUsername)) {
            // 내가 방장인 경우 -> 삭제 버튼 보이기
            holder.btnDeleteGroup.setVisibility(View.VISIBLE);

            // ⭐ [수정 3] 삭제 버튼 클릭 시, 다이얼로그를 통해 확인 후 Activity의 onDeleteClick 호출
            holder.btnDeleteGroup.setOnClickListener(v -> {
                // 삭제 확인 다이얼로그 표시
                new AlertDialog.Builder(context)
                        .setTitle("그룹 삭제")
                        .setMessage("'" + group.getGroupName() + "' 그룹과 모든 위치 정보를 삭제하시겠습니까?")
                        .setPositiveButton("삭제", (dialog, which) -> {
                            // Activity(MyGroupsActivity)의 onDeleteClick 메서드를 호출하여 삭제 로직 위임
                            if (listener != null) {
                                listener.onDeleteClick(group.getGroupId(), holder.getAdapterPosition());
                            }
                        })
                        .setNegativeButton("취소", null)
                        .show();
            });
        } else {
            // 내가 방장이 아닌 경우 -> 삭제 버튼 숨기기
            holder.btnDeleteGroup.setVisibility(View.GONE);
        }
        // ▲▲▲ [ 로직 수정 완료 ] ▲▲▲


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

    // ⭐ [삭제] deleteGroupApiCall 메서드는 MyGroupsActivity로 이동해야 합니다.

    // ⭐ [수정 4] GroupViewHolder 이너 클래스 정의
    // (모든 DTO 필드를 반영하고, XML ID와 일치하도록 수정)
    static class GroupViewHolder extends RecyclerView.ViewHolder {
        final TextView tvGroupName;
        final TextView tvDestination;
        final TextView tvMemberCount;
        final ImageView btnDeleteGroup; // ImageView로 연결

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            // XML ID: btnDeleteGroup와 연결
            btnDeleteGroup = itemView.findViewById(R.id.btnDeleteGroup);
        }
    }
}