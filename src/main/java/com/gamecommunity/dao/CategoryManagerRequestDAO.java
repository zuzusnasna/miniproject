package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;
import com.gamecommunity.dto.CategoryManagerRequestDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CategoryManagerRequestDAO {

    public String getRequestStatus(long memberNo) {
        String sql = "SELECT REQUEST_STATUS FROM CATEGORY_MANAGER_REQUEST WHERE MEMBER_NO = ? ORDER BY REQUEST_ID DESC FETCH FIRST 1 ROW ONLY";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString("REQUEST_STATUS") : null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean insertRequest(long memberNo, long categoryId) {
        String sql = "INSERT INTO CATEGORY_MANAGER_REQUEST (REQUEST_ID, MEMBER_NO, CATEGORY_ID, REQUEST_STATUS, REQUESTED_AT) VALUES (CATEGORY_MANAGER_REQUEST_SEQ.NEXTVAL, ?, ?, 'PENDING', SYSDATE)";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            pstmt.setLong(2, categoryId);
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

    public List<CategoryManagerRequestDTO> findPendingRequests() {
        List<CategoryManagerRequestDTO> list = new ArrayList<>();
        String sql = """
            SELECT r.REQUEST_ID, r.MEMBER_NO, m.USERNAME, m.NICKNAME,
                   NVL((SELECT SUM(p.LIKE_COUNT) FROM POST p WHERE p.MEMBER_NO = m.MEMBER_NO AND p.IS_DELETED = 'N'), 0) AS RECEIVED_LIKES,
                   r.CATEGORY_ID, c.CATEGORY_NAME, r.REQUEST_STATUS,
                   TO_CHAR(r.REQUESTED_AT, 'YYYY-MM-DD HH24:MI:SS') AS REQUESTED_AT
            FROM CATEGORY_MANAGER_REQUEST r
            JOIN MEMBER m ON m.MEMBER_NO = r.MEMBER_NO
            JOIN CATEGORY c ON c.CATEGORY_ID = r.CATEGORY_ID
            WHERE r.REQUEST_STATUS = 'PENDING'
            ORDER BY r.REQUESTED_AT, r.REQUEST_ID
            """;
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                CategoryManagerRequestDTO dto = new CategoryManagerRequestDTO();
                dto.setRequestId(rs.getLong("REQUEST_ID"));
                dto.setMemberNo(rs.getLong("MEMBER_NO"));
                dto.setUsername(rs.getString("USERNAME"));
                dto.setNickname(rs.getString("NICKNAME"));
                dto.setReceivedLikeCount(rs.getInt("RECEIVED_LIKES"));
                dto.setCategoryId(rs.getLong("CATEGORY_ID"));
                dto.setCategoryName(rs.getString("CATEGORY_NAME"));
                dto.setRequestStatus(rs.getString("REQUEST_STATUS"));
                dto.setRequestedAt(rs.getString("REQUESTED_AT"));
                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean approveRequest(long requestId) {
        String selectSql = "SELECT MEMBER_NO, CATEGORY_ID FROM CATEGORY_MANAGER_REQUEST WHERE REQUEST_ID = ? AND REQUEST_STATUS = 'PENDING' FOR UPDATE";
        String roleSql = "INSERT INTO MEMBER_ROLE (MEMBER_NO, ROLE_ID, GRANTED_AT) SELECT ?, ROLE_ID, SYSDATE FROM ROLE WHERE ROLE_NAME = 'CATEGORY_MANAGER' AND NOT EXISTS (SELECT 1 FROM MEMBER_ROLE WHERE MEMBER_NO = ? AND ROLE_ID = ROLE.ROLE_ID)";
        String managerSql = "INSERT INTO CATEGORY_MANAGER (MEMBER_NO, CATEGORY_ID) SELECT ?, ? FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CATEGORY_MANAGER WHERE MEMBER_NO = ? AND CATEGORY_ID = ?)";
        String requestSql = "UPDATE CATEGORY_MANAGER_REQUEST SET REQUEST_STATUS = 'APPROVED', PROCESSED_AT = SYSDATE WHERE REQUEST_ID = ? AND REQUEST_STATUS = 'PENDING'";

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            long memberNo;
            long categoryId;
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setLong(1, requestId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return false; }
                    memberNo = rs.getLong("MEMBER_NO");
                    categoryId = rs.getLong("CATEGORY_ID");
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(roleSql)) {
                pstmt.setLong(1, memberNo);
                pstmt.setLong(2, memberNo);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(managerSql)) {
                pstmt.setLong(1, memberNo);
                pstmt.setLong(2, categoryId);
                pstmt.setLong(3, memberNo);
                pstmt.setLong(4, categoryId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(requestSql)) {
                pstmt.setLong(1, requestId);
                if (pstmt.executeUpdate() != 1) { conn.rollback(); return false; }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (Exception ignored) {}
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }

    public boolean rejectRequest(long requestId) {
        String sql = "UPDATE CATEGORY_MANAGER_REQUEST SET REQUEST_STATUS = 'REJECTED', PROCESSED_AT = SYSDATE WHERE REQUEST_ID = ? AND REQUEST_STATUS = 'PENDING'";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, requestId);
            return pstmt.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
