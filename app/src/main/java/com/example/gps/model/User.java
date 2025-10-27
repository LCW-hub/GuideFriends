package com.example.gps.model;

public class User {

    // ⭐ [추가] 서버의 Long 타입 ID 필드
    private Long id;

    private String username;
    private String password;
    private String email;
    private String phoneNum;

    // 1. 기본 생성자
    public User() {}

    // 2. 전체 필드 생성자 (ID 제외)
    public User(String username, String password, String email, String phoneNum) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNum = phoneNum;
    }

    // ⭐ 3. [추가] ID와 Username만 받는 생성자 (오류 해결용)
    public User(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    // ⭐ 4. [추가] ID Getter/Setter
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    // getter & setter (기존 유지)
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNum() { return phoneNum; }
    public void setPhoneNum(String phone) { this.phoneNum = phone; }
}
