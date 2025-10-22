package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberSharingAdapter extends RecyclerView.Adapter<MemberSharingAdapter.MemberViewHolder> {

    private List<User> memberList;
    private Long loggedInUserId; // ⭐ [수정] final 제거, setter를 통해 갱신 가능하도록 함
    // 체크박스 상태를 저장하는 맵 (UserID -> Checked/Allowed 상태)
    private final Map<Long, Boolean> checkStates = new HashMap<>();

    public MemberSharingAdapter(List<User> memberList, Long loggedInUserId) {
        this.memberList = memberList;
        this.loggedInUserId = loggedInUserId;
        for(User user : memberList) {
            checkStates.put(user.getId(), true);
        }
    }

    // ⭐ [추가] loggedInUserId를 설정하는 메서드
    public void setLoggedInUserId(Long userId) {
        this.loggedInUserId = userId;
    }

    // [추가] Getter도 추가하여 필요시 ID를 가져올 수 있게 함
    public Long getLoggedInUserId() {
        return loggedInUserId;
    }

    public void updateMembers(List<User> newMembers) {
        this.memberList = newMembers;
        checkStates.clear();
        for(User user : newMembers) {
            checkStates.put(user.getId(), true);
        }
        notifyDataSetChanged();
    }

    // 특정 유저의 현재 체크 상태(즉, 허용 상태)를 반환
    public boolean isUserChecked(Long userId) {
        return checkStates.getOrDefault(userId, true);
    }

    // 전체 멤버 목록 반환
    public List<User> getMembers() {
        return memberList;
    }


    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_sharing_setting, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User member = memberList.get(position);

        // ⭐ [수정] 로그인된 사용자(자신)의 아이템은 숨깁니다.
        if (member.getId().equals(loggedInUserId)) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        holder.tvUsername.setText(member.getUsername());

        Boolean isAllowed = checkStates.getOrDefault(member.getId(), true);
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(isAllowed);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkStates.put(member.getId(), isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        final TextView tvUsername;
        final CheckBox checkBox;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_member_username);
            checkBox = itemView.findViewById(R.id.cb_sharing_allowed);
        }
    }
}
