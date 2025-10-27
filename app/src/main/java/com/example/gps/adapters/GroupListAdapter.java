package com.example.gps.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.dto.GroupListResponse;

import java.util.List;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupViewHolder> {

    // 1. 클릭 리스너 인터페이스 정의
    public interface OnGroupClickListener {
        void onGroupClick(Long groupId, String groupName);
    }

    private final List<GroupListResponse> groupList;
    private final OnGroupClickListener listener;

    // 2. 생성자에서 OnGroupClickListener 리스너를 받습니다.
    public GroupListAdapter(List<GroupListResponse> groupList, OnGroupClickListener listener) {
        this.groupList = groupList;
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

        // DTO 메서드와 일치
        holder.tvGroupName.setText(group.getGroupName());
        holder.tvDestination.setText(group.getDestinationName());

        // 항목 클릭 리스너 구현
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // DTO 메서드와 일치
                listener.onGroupClick(group.getGroupId(), group.getGroupName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    // ViewHolder 정의
    static class GroupViewHolder extends RecyclerView.ViewHolder {
        final TextView tvGroupName;
        final TextView tvDestination;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvDestination = itemView.findViewById(R.id.tvDestination);
        }
    }
}
