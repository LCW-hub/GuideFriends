package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.R;
import com.example.gps.model.User;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private List<User> friends;
    private OnDeleteClickListener listener; // ✅ 리스너 인터페이스

    // ✅ 클릭 이벤트를 Activity로 전달하기 위한 인터페이스
    public interface OnDeleteClickListener {
        void onDeleteClick(User friend);
    }

    public FriendAdapter(List<User> friends, OnDeleteClickListener listener) { // ✅ 생성자에 리스너 추가
        this.friends = friends;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User friend = friends.get(position);
        holder.tvFriendUsername.setText(friend.getUsername());
        // ✅ 삭제 버튼 클릭 시 리스너 호출
        holder.btnDeleteFriend.setOnClickListener(v -> listener.onDeleteClick(friend));
    }

    @Override
    public int getItemCount() {
        return friends != null ? friends.size() : 0;
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvFriendUsername;
        Button btnDeleteFriend; // ✅ 삭제 버튼 변수

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFriendUsername = itemView.findViewById(R.id.tvFriendUsername);
            btnDeleteFriend = itemView.findViewById(R.id.btnDeleteFriend); // ✅ UI와 연결
        }
    }
}