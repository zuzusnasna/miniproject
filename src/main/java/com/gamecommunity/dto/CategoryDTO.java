package com.gamecommunity.dto;

public class CategoryDTO {

    private Long categoryId;
    private Long parentId;

    private String categoryName;
    private int depth;
    private String isActive;
    private String createdAt;
    private String iconUrl;
    private int sortOrder;


    // =========================================================
    // CATEGORY_ID
    // =========================================================

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }


    // =========================================================
    // PARENT_ID
    // =========================================================

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }


    // =========================================================
    // CATEGORY_NAME
    // =========================================================

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }


    // =========================================================
    // DEPTH
    // =========================================================

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }


    // =========================================================
    // IS_ACTIVE
    // =========================================================

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }


    // =========================================================
    // CREATED_AT
    // =========================================================

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }


    // =========================================================
    // ICON_URL
    // =========================================================

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }


    // =========================================================
    // SORT_ORDER
    // =========================================================

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}