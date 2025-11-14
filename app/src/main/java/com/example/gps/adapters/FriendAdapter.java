package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// ⭐️ [Glide Imports]
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import de.hdodenhof.circleimageview.CircleImageView;
import com.example.gps.api.ApiClient;
import android.util.Log;

import com.example.gps.R;
import com.example.gps.model.User;
import java.util.List;
import java.util.ArrayList;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private List<User> friends;
    private OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(User friend);
    }

    public FriendAdapter(List<User> friends, OnDeleteClickListener listener) {
        this.friends = friends != null ? friends : new ArrayList<>();
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

        // --- (이름 표시 로직 - 수정 없음) ---
        String displayName = friend.getNickname();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = friend.getUsername();
        }
        // ... (이하 이름 로직 동일) ...
        holder.tvFriendUsername.setText(displayName);


        // --- ⭐️ [START OF REVISED CODE] 프로필 이미지 로드 로직 (수정됨) ⭐️ ---

        String imageUrl = friend.getProfileImageUrl();
        Object loadTarget = null;

        if (imageUrl != null && !imageUrl.isEmpty()) {
            String baseUrl = ApiClient.getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            // ⭐️ [핵심 수정] ⭐️
            // URL이 "/"로 시작하면(상대 경로), 앞에 baseUrl을 붙여줍니다.
            // (예: "/static/..." 또는 "/media/profiles/...")
            if (imageUrl.startsWith("/")) {
                loadTarget = baseUrl + imageUrl;
            } else {
                // "http://"로 시작하는 절대 경로면 그대로 사용합니다.
                loadTarget = imageUrl;
            }
            Log.d("FriendAdapter", "Loading image for " + friend.getUsername() + ": " + loadTarget);

        } else {
            Log.d("FriendAdapter", "No image URL for " + friend.getUsername() + ". Loading placeholder.");
        }

        // Glide로 이미지 로드
        Glide.with(holder.itemView.getContext())
                .load(loadTarget)
                .placeholder(R.drawable.ic_person) // 기본 이미지
                .error(R.drawable.ic_person)       // 에러 시 이미지
                .diskCacheStrategy(DiskCacheStrategy.NONE) // 프로필 사진은 캐시 스킵
                .skipMemoryCache(true)
                .into(holder.ivFriendProfile); // ⭐️ ViewHolder의 이미지뷰

        // --- ⭐️ [END OF REVISED CODE] ⭐️ ---


        // --- (기존 삭제 버튼 로직 - 수정 없음) ---
        holder.btnDeleteFriend.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(friend);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friends != null ? friends.size() : 0;
    }

    public void setFriends(List<User> newFriends) {
        if (this.friends == null) {
            this.friends = new ArrayList<>();
        }
        this.friends.clear();
        if (newFriends != null) {
            this.friends.addAll(newFriends);
        }
        notifyDataSetChanged();
    }

    // [ViewHolder - 수정 없음]
    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvFriendUsername;
        Button btnDeleteFriend;
        CircleImageView ivFriendProfile;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFriendUsername = itemView.findViewById(R.id.tvFriendUsername);
            btnDeleteFriend = itemView.findViewById(R.id.btnDeleteFriend);
            ivFriendProfile = itemView.findViewById(R.id.iv_friend_profile);
        }
    }
}