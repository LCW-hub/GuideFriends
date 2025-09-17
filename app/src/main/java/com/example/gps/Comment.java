package com.example.gps;

import java.util.List;
import java.util.ArrayList;

public class Comment {
    private String author;
    private String content;
    private String timeAgo;
    private long timestamp;
    private String parentCommentId; // 답글인 경우 부모 댓글 ID
    private List<Comment> replies; // 답글 목록

    public Comment() {
        // Firestore에서 사용하기 위한 빈 생성자
    }

    public Comment(String author, String content, String timeAgo, long timestamp) {
        this.author = author;
        this.content = content;
        this.timeAgo = timeAgo;
        this.timestamp = timestamp;
        this.replies = new ArrayList<>();
    }

    public Comment(String author, String content, String timeAgo, long timestamp, String parentCommentId) {
        this.author = author;
        this.content = content;
        this.timeAgo = timeAgo;
        this.timestamp = timestamp;
        this.parentCommentId = parentCommentId;
        this.replies = new ArrayList<>();
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }
} 