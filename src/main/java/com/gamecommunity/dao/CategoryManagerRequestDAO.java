package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CategoryManagerRequestDAO {

    public boolean hasPendingRequest(long memberNo) {
        String sql = "SELECT COUNT(*) FROM CATEGORY_MANAGER_REQUEST WHERE MEMBER_NO = ? AND REQUEST_STATUS = 'PENDING'";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasApprovedRequest(long memberNo) {
        String sql = "SELECT COUNT(*) FROM CATEGORY_MANAGER_REQUEST WHERE MEMBER_NO = ? AND REQUEST_STATUS = 'APPROVED'";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertRequest(long memberNo) {
        String sql = "INSERT INTO CATEGORY_MANAGER_REQUEST (REQUEST_ID, MEMBER_NO, REQUEST_STATUS, REQUESTED_AT) VALUES (CATEGORY_MANAGER_REQUEST_SEQ.NEXTVAL, ?, 'PENDING', SYSDATE)";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
