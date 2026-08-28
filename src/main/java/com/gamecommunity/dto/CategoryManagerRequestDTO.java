package com.gamecommunity.dto;

public class CategoryManagerRequestDTO {
    private long requestId;
    private long memberNo;
    private String username;
    private String nickname;
    private int receivedLikeCount;
    private long categoryId;
    private String categoryName;
    private String requestStatus;
    private String requestedAt;

    public long getRequestId() { return requestId; }
    public void setRequestId(long requestId) { this.requestId = requestId; }
    public long getMemberNo() { return memberNo; }
    public void setMemberNo(long memberNo) { this.memberNo = memberNo; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public int getReceivedLikeCount() { return receivedLikeCount; }
    public void setReceivedLikeCount(int receivedLikeCount) { this.receivedLikeCount = receivedLikeCount; }
    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }
    public String getRequestedAt() { return requestedAt; }
    public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }
}
