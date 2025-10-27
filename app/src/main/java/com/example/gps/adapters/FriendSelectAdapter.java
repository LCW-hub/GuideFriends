package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.R;
// ❌ FriendResponse 대신 서버 모델인 User를 사용합니다.
// import com.example.gps.dto.FriendResponse;
import com.example.gps.model.User; // ✅ User 모델 import
import java.util.ArrayList;
import java.util.List;

public class FriendSelectAdapter extends RecyclerView.Adapter<FriendSelectAdapter.FriendViewHolder> {

    // ⭐ [수정] 내부 리스트 타입을 List<User>로 변경
    private List<User> friendList;
    private List<Long> selectedFriendIds = new ArrayList<>();

    // ⭐ [수정] 생성자 타입을 List<User>로 변경
    public FriendSelectAdapter(List<User> friendList) {
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
        // ⭐ [수정] FriendResponse 대신 User 객체를 사용
        User friend = friendList.get(position);

        // ⭐ [수정] User 객체의 Getter 메서드를 사용 (setUsername, getId가 있다고 가정)
        holder.checkBox.setText(friend.getUsername());

        holder.checkBox.setOnCheckedChangeListener(null); // 리스너 초기화
        holder.checkBox.setChecked(selectedFriendIds.contains(friend.getId())); // ID 비교

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedFriendIds.contains(friend.getId())) {
                    selectedFriendIds.add(friend.getId());
                }
            } else {
                selectedFriendIds.remove(friend.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    // 선택된 친구들의 ID 목록을 반환하는 메소드 (로직 유지)
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