package com.example.gps.adapters;
// 예시 패키지. 프로젝트 구조에 맞게 수정하세요.

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R; // R.layout.item_message_sent, R.layout.item_message_received 에 사용
import com.example.gps.models.Message;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messageList;
    private final String currentUsername; // 현재 로그인한 사용자 이름
    private final SimpleDateFormat timeFormatter;

    public MessageAdapter(List<Message> messageList, String currentUsername) {
        this.messageList = messageList;
        this.currentUsername = currentUsername;
        // 시간 포맷 정의 (예: 오후 1:05)
        this.timeFormatter = new SimpleDateFormat("a h:mm", Locale.getDefault());
    }

    // 뷰 타입 결정 메서드
    // 메시지 객체의 senderUsername과 현재 사용자의 currentUsername을 비교하여 타입을 반환합니다.
    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderUsername().equals(currentUsername)) {
            return VIEW_TYPE_SENT; // 내가 보낸 메시지
        } else {
            return VIEW_TYPE_RECEIVED; // 상대방이 보낸 메시지
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            // R.layout.item_message_sent는 오른쪽에 말풍선이 위치한 레이아웃입니다.
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
        } else {
            // R.layout.item_message_received는 왼쪽에 말풍선이 위치한 레이아웃입니다.
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // 새 메시지가 추가되었을 때 리스트를 업데이트하고 스크롤을 맨 아래로 이동시키기 위한 헬퍼 메서드
    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
        // ChatRoomActivity에서 notifyItemInserted 호출 후 rvMessages.scrollToPosition(adapter.getItemCount() - 1) 필요
    }

    /**
     * ViewHolder 클래스
     */
    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName; // 받은 메시지에만 필요 (보낸 메시지에는 자신의 이름이 필요 없음)
        TextView tvContent;
        TextView tvTimestamp;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            // 뷰 타입에 따라 findViewById를 다르게 처리할 수 있지만,
            // 여기서는 레이아웃 ID를 통일하여 (이름 TextView는 RECEIVED 타입에만 존재할 수 있음) 사용합니다.
            tvContent = itemView.findViewById(R.id.tvMessageContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);

            // tvSenderName은 item_message_received.xml에만 있을 수 있습니다.
            // item_message_sent.xml에는 이 ID가 없어도 NullPointerException이 발생하지 않습니다.
        }

        public void bind(Message message) {
            tvContent.setText(message.getContent());
            tvTimestamp.setText(timeFormatter.format(message.getTimestamp()));

            // 받은 메시지 타입일 경우에만 이름을 표시 (VIEW_TYPE_SENT 레이아웃에는 이 TextView가 없을 수 있음)
            if (getItemViewType() == VIEW_TYPE_RECEIVED && tvSenderName != null) {
                tvSenderName.setText(message.getSenderUsername());
            }
        }
    }
}