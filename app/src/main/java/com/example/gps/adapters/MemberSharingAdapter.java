package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import de.hdodenhof.circleimageview.CircleImageView;
import com.example.gps.api.ApiClient;
import com.example.gps.R;
import com.example.gps.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberSharingAdapter extends RecyclerView.Adapter<MemberSharingAdapter.MemberViewHolder> {

    private List<User> memberList;
    private Long loggedInUserId;
    // 체크박스 상태를 저장하는 맵 (UserID -> Checked/Allowed 상태)
    private final Map<Long, Boolean> checkStates = new HashMap<>();

    public MemberSharingAdapter(List<User> memberList, Long loggedInUserId) {
        this.memberList = memberList;
        this.loggedInUserId = loggedInUserId;
        // 💡 [수정] 생성자에서 checkStates 초기화 로직은 제거하고 updateMembers에서 처리합니다.
    }

    // ⭐ [추가] loggedInUserId를 설정하는 메서드 (기존 유지)
    public void setLoggedInUserId(Long userId) {
        this.loggedInUserId = userId;
    }

    // [추가] Getter도 추가하여 필요시 ID를 가져올 수 있게 함 (기존 유지)
    public Long getLoggedInUserId() {
        return loggedInUserId;
    }

    // 1. 멤버 목록을 업데이트하고 checkStates를 기본값(true)으로 초기화합니다.
    public void updateMembers(List<User> newMembers) {
        this.memberList = newMembers;
        checkStates.clear();
        // 💡 규칙이 로드되기 전까지는 모든 멤버를 기본값(공유 허용: true)으로 설정합니다.
        for(User user : newMembers) {
            checkStates.put(user.getId(), true);
        }
        notifyDataSetChanged();
    }

    // 2. ⭐️ [핵심 추가] 서버에서 가져온 현재 공유 규칙을 반영하는 메서드
    /**
     * 서버에서 가져온 초기 공유 규칙을 설정합니다. (내가 Sharer일 때 상대방에게 허용 여부)
     * @param rules Map<TargetUserId, IsAllowed>
     */
    public void setInitialSharingRules(Map<Long, Boolean> rules) {
        // rules 맵을 checkStates에 병합합니다.
        // updateMembers에서 이미 모든 멤버를 true로 초기화했으므로,
        // 서버에서 'false'로 명시한 규칙만 덮어쓰여 차단 상태가 반영됩니다.
        checkStates.putAll(rules);
        notifyDataSetChanged(); // UI를 갱신하여 체크박스 상태를 반영
    }

    // 특정 유저의 현재 체크 상태(즉, 허용 상태)를 반환 (기존 유지)
    public boolean isUserChecked(Long userId) {
        return checkStates.getOrDefault(userId, true);
    }

    // 전체 멤버 목록 반환 (기존 유지)
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

        // ⭐ 로그인된 사용자(자신)의 아이템은 숨깁니다. (기존 유지)
        if (member.getId().equals(loggedInUserId)) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        holder.tvUsername.setText(member.getUsername());

        // 프로필 이미지 로드
        String imageUrl = member.getProfileImageUrl();
        Object loadTarget = null;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String baseUrl = ApiClient.getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                loadTarget = imageUrl;
            } else if (imageUrl.startsWith("/")) {
                loadTarget = baseUrl + imageUrl;
            } else {
                loadTarget = baseUrl + "/" + imageUrl;
            }
        }
        
        Glide.with(holder.itemView.getContext())
                .load(loadTarget)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.ivMemberProfile);

        // ⭐️ [핵심] checkStates 맵에서 현재 상태를 가져와 체크박스를 설정합니다.
        // checkStates는 setInitialSharingRules에서 서버 규칙으로 초기화됩니다.
        Boolean isAllowed = checkStates.getOrDefault(member.getId(), true);

        // 리스너 재사용 문제 방지
        holder.checkBox.setOnCheckedChangeListener(null);

        // 초기 상태 설정
        holder.checkBox.setChecked(isAllowed);

        // 새 리스너 설정: 상태 변경 시 checkStates 맵 업데이트
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkStates.put(member.getId(), isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView ivMemberProfile;
        final TextView tvUsername;
        final CheckBox checkBox;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMemberProfile = itemView.findViewById(R.id.iv_member_profile);
            tvUsername = itemView.findViewById(R.id.tv_member_username);
            checkBox = itemView.findViewById(R.id.cb_sharing_allowed);
        }
    }
}