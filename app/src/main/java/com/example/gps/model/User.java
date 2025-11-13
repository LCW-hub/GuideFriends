package com.example.gps.model;

public class User {

    private Long id;
    private String username;
    private String password;
    private String email;
    private String phoneNum;
    private String nickname;

    // ⭐️ [이 필드를 추가하세요] ⭐️
    private String profileImageUrl;

    // 1. 기본 생성자
    public User() {}

    // 2. 전체 필드 생성자 (ID 제외)
    public User(String username, String password, String email, String phoneNum) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNum = phoneNum;
    }

    // ⭐️ [수정] 닉네임 포함하는 생성자 추가 (선택 사항)
    public User(String username, String password, String email, String phoneNum, String nickname) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNum = phoneNum;
        this.nickname = nickname;
    }

    // ⭐ 3. [추가] ID와 Username만 받는 생성자 (오류 해결용)
    public User(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNum() { return phoneNum; }
    public void setPhoneNum(String phone) { this.phoneNum = phone; }

    // ⭐️ [이 Getter/Setter를 추가하세요] ⭐️
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}