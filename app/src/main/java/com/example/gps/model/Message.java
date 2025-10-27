package com.example.gps.models;
// 예시 패키지. 프로젝트 구조에 맞게 수정하세요.

import java.util.Date;

public class Message {
    private String senderUsername;
    private String content;
    private long timestamp; // 메시지 전송 시간 (밀리초)
    // TODO: 메시지 ID, 그룹 ID 등 필요한 필드를 추가할 수 있습니다.

    // 기본 생성자 (Firebase 등에서 객체 자동 매핑 시 필요)
    public Message() {}

    public Message(String senderUsername, String content, long timestamp) {
        this.senderUsername = senderUsername;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getter와 Setter
    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}