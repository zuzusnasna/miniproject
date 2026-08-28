package com.gamecommunity.dto;

public class MemberDTO {

    private Long memberNo;
    private String name;
    private String username;
    private String password;
    private String phone;
    private int userLevel;
    private String joinStatus;
    private String nickname;
    private String accountStatus;
    private int receivedLikeCount;
    private int receivedDislikeCount;

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Long getMemberNo() {
        return memberNo;
    }

    public void setMemberNo(Long memberNo) {
        this.memberNo = memberNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(int userLevel) {
        this.userLevel = userLevel;
    }

    public String getJoinStatus() {
        return joinStatus;
    }

    public void setJoinStatus(String joinStatus) {
        this.joinStatus = joinStatus;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getReceivedLikeCount() {
        return receivedLikeCount;
    }
    public void setReceivedLikeCount(int receivedLikeCount) {
        this.receivedLikeCount = receivedLikeCount;
    }

    public int getReceivedDislikeCount() {
        return receivedDislikeCount;
    }
    public void setReceivedDislikeCount(int receivedDislikeCount) {
        this.receivedDislikeCount = receivedDislikeCount;
    }


}