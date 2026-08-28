package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CommentLikeDAO {

    public boolean addLike(long commentId, long memberNo, String likeType) {
        if (!"LIKE".equals(likeType) && !"DISLIKE".equals(likeType)) return false;

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String checkSql = "SELECT LIKE_TYPE FROM COMMENT_LIKE WHERE COMMENT_ID = ? AND MEMBER_NO = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setLong(1, commentId);
                pstmt.setLong(2, memberNo);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            String insertSql = """
                    INSERT INTO COMMENT_LIKE (COMMENT_ID, MEMBER_NO, LIKE_TYPE, CREATED_AT)
                    VALUES (?, ?, ?, SYSDATE)
                    """;
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setLong(1, commentId);
                pstmt.setLong(2, memberNo);
                pstmt.setString(3, likeType);
                pstmt.executeUpdate();
            }

            String countColumn = "LIKE".equals(likeType) ? "LIKE_COUNT" : "DISLIKE_COUNT";
            String updateSql = "UPDATE POST_COMMENT SET " + countColumn + " = " + countColumn + " + 1 WHERE COMMENT_ID = ? AND IS_DELETED = 'N'";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setLong(1, commentId);
                if (pstmt.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (Exception ignored) {}
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (Exception ignored) {}
        }
    }

    public int getLikeCount(long commentId) {
        return getCount(commentId, "LIKE_COUNT");
    }

    public int getDislikeCount(long commentId) {
        return getCount(commentId, "DISLIKE_COUNT");
    }

    private int getCount(long commentId, String column) {
        String sql = "SELECT " + column + " FROM POST_COMMENT WHERE COMMENT_ID = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, commentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
