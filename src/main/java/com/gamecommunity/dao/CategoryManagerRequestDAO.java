package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CategoryManagerRequestDAO {

    public String getRequestStatus(long memberNo) {
        String sql = "SELECT REQUEST_STATUS FROM CATEGORY_MANAGER_REQUEST WHERE MEMBER_NO = ? ORDER BY REQUEST_ID DESC";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("REQUEST_STATUS");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insertRequest(long memberNo) {
        String sql = "INSERT INTO CATEGORY_MANAGER_REQUEST (REQUEST_ID, MEMBER_NO, REQUEST_STATUS, REQUESTED_AT) VALUES (CATEGORY_MANAGER_REQUEST_SEQ.NEXTVAL, ?, 'PENDING', SYSDATE)";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            return pstmt.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasPendingRequest(long memberNo) {
        return existsByStatus(memberNo, "PENDING");
    }

    public boolean hasApprovedRequest(long memberNo) {
        return existsByStatus(memberNo, "APPROVED");
    }

    private boolean existsByStatus(long memberNo, String status) {
        String sql = "SELECT COUNT(*) FROM CATEGORY_MANAGER_REQUEST WHERE MEMBER_NO = ? AND REQUEST_STATUS = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            pstmt.setString(2, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean processRequest(long requestId, String status) {
        String sql = "UPDATE CATEGORY_MANAGER_REQUEST SET REQUEST_STATUS = ?, PROCESSED_AT = SYSDATE WHERE REQUEST_ID = ? AND REQUEST_STATUS = 'PENDING'";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setLong(2, requestId);
            return pstmt.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
