package com.example.gps.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.api.ApiClient;
import com.example.gps.api.GroupApiService;
import com.example.gps.dto.GroupListResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(Long groupId, String groupName);
    }

    private final List<GroupListResponse> groupList;
    private final OnGroupClickListener listener;
    private final String loggedInUsername;
    private final Context context;

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

        // 1. 기본 정보 설정
        holder.tvGroupName.setText(group.getGroupName());
        holder.tvDestination.setText(group.getDestinationName());
        holder.tvMemberCount.setText(group.getMemberCount() + "명");

        // ▼▼▼ [추가됨] 2. 인원수(사람 아이콘) 클릭 시 참여자 목록 팝업 띄우기 ▼▼▼
        holder.tvMemberCount.setOnClickListener(v -> {
            // [주의] GroupListResponse DTO에 getMemberIds() (또는 getMemberNames) 메소드가 있어야 합니다.
            // 서버에서 명단 리스트(List<String>)를 받아온다고 가정합니다.
            List<String> members = group.getMemberIds(); // <-- DTO에 이 메소드가 있는지 확인하세요!

            if (members != null && !members.isEmpty()) {
                // 다이얼로그 생성
                new AlertDialog.Builder(context)
                        .setTitle("참여자 목록") // 팝업 제목
                        .setItems(members.toArray(new String[0]), null) // 리스트 표시
                        .setPositiveButton("닫기", null) // 닫기 버튼
                        .show();
            } else {
                // 명단 데이터가 없을 경우 처리
                Toast.makeText(context, "참여자 상세 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        // ▲▲▲ [여기까지 추가됨] ▲▲▲


        // 3. 방장 권한 확인 및 삭제 버튼 로직
        if (group.getCreatedByUsername() != null && group.getCreatedByUsername().equals(loggedInUsername)) {
            holder.btnDeleteGroup.setVisibility(View.VISIBLE);
            holder.btnDeleteGroup.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("그룹 삭제")
                        .setMessage("'" + group.getGroupName() + "' 그룹을 정말 삭제하시겠습니까?")
                        .setPositiveButton("삭제", (dialog, which) -> {
                            deleteGroupApiCall(group, holder.getAdapterPosition());
                        })
                        .setNegativeButton("취소", null)
                        .show();
            });
        } else {
            holder.btnDeleteGroup.setVisibility(View.GONE);
        }

        // 4. 그룹 클릭 리스너 (채팅방 입장 등)
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

    // 그룹 삭제 API 호출
    private void deleteGroupApiCall(GroupListResponse group, int position) {
        if (position == RecyclerView.NO_POSITION) return;

        GroupApiService apiService = ApiClient.getGroupApiService(context);
        Call<Map<String, String>> call = apiService.deleteGroup(group.getGroupId());

        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "그룹이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    // --- 🔽 [수정/추가] ---

                    // 1. 로컬 목록에서 즉시 제거 (기존 코드)
                    groupList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, groupList.size());

                    // 2. MapsActivity로 돌아가서 버튼을 숨기도록 Intent 전송
                    Intent intent = new Intent(context, com.example.gps.activities.MapsActivity.class);

                    // 3. (중요) groupId를 -1L로 설정하거나 아예 보내지 않습니다.
                    // 이렇게 하면 MapsActivity의 onNewIntent가 groupId를 -1L로 인식합니다.
                    intent.putExtra("groupId", -1L);
                    intent.putExtra("username", loggedInUsername); // username은 계속 전달

                    SharedPreferences prefs = context.getSharedPreferences("user_info", Context.MODE_PRIVATE);
                    String savedUsername = prefs.getString("username", null);

                    // 4. MapsActivity의 기존 인스턴스를 재활용
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);

                    // --- 🔼 [여기까지 수정/추가] --
                } else {
                    Toast.makeText(context, "그룹 삭제에 실패했습니다. (권한 없음)", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                Toast.makeText(context, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        final TextView tvGroupName;
        final TextView tvDestination;
        final TextView tvMemberCount;
        final ImageView btnDeleteGroup;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            btnDeleteGroup = itemView.findViewById(R.id.btnDeleteGroup);
        }
    }
}