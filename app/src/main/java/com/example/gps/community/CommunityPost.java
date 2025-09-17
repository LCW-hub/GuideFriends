package com.example.gps.community;

public class CommunityPost {
    private String author;
    private String title;
    private String content;
    private String timeAgo;
    private String emoji;
    private int likes;
    private int comments;

    public CommunityPost(String author, String title, String content, String timeAgo, String emoji, int likes, int comments) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.timeAgo = timeAgo;
        this.emoji = emoji;
        this.likes = likes;
        this.comments = comments;
    }

    // Getters
    public String getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTimeAgo() { return timeAgo; }
    public String getEmoji() { return emoji; }
    public int getLikes() { return likes; }
    public int getComments() { return comments; }

    // Setters
    public void setAuthor(String author) { this.author = author; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public void setLikes(int likes) { this.likes = likes; }
    public void setComments(int comments) { this.comments = comments; }
} 