package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.R;
import com.example.gps.dto.FriendResponse; // 서버 DTO import
import java.util.ArrayList;
import java.util.List;

public class FriendSelectAdapter extends RecyclerView.Adapter<FriendSelectAdapter.FriendViewHolder> {

    private List<FriendResponse> friendList;
    private List<Long> selectedFriendIds = new ArrayList<>();

    public FriendSelectAdapter(List<FriendResponse> friendList) {
        this.friendList = friendList;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_select, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        FriendResponse friend = friendList.get(position);
        holder.checkBox.setText(friend.getFriendUsername());

        holder.checkBox.setOnCheckedChangeListener(null); // 리스너 초기화
        holder.checkBox.setChecked(selectedFriendIds.contains(friend.getFriendId()));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedFriendIds.contains(friend.getFriendId())) {
                    selectedFriendIds.add(friend.getFriendId());
                }
            } else {
                selectedFriendIds.remove(friend.getFriendId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    // 선택된 친구들의 ID 목록을 반환하는 메소드
    public List<Long> getSelectedFriendIds() {
        return selectedFriendIds;
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cbFriend);
        }
    }
}