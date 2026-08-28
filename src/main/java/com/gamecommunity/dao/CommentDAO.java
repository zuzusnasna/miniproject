package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;
import com.gamecommunity.dto.CommentDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    public List<CommentDTO> findByPostId(long postId) {
        List<CommentDTO> comments = new ArrayList<>();
        String sql = """
                SELECT C.COMMENT_ID, C.POST_ID, C.PARENT_COMMENT_ID, C.MEMBER_NO,
                       M.NICKNAME, C.CONTENT, C.LIKE_COUNT, C.DISLIKE_COUNT,
                       C.IS_DELETED, C.CREATED_AT, C.UPDATED_AT
                FROM POST_COMMENT C
                JOIN MEMBER M ON C.MEMBER_NO = M.MEMBER_NO
                WHERE C.POST_ID = ?
                ORDER BY NVL(C.PARENT_COMMENT_ID, C.COMMENT_ID),
                         CASE WHEN C.PARENT_COMMENT_ID IS NULL THEN 0 ELSE 1 END,
                         C.CREATED_AT, C.COMMENT_ID
                """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) comments.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return comments;
    }

    public long save(long postId, Long parentCommentId, long memberNo, String content) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (parentCommentId != null && !isValidParent(conn, postId, parentCommentId)) {
                conn.rollback();
                return 0L;
            }

            long commentId = nextCommentId(conn);
            String sql = """
                    INSERT INTO POST_COMMENT
                    (COMMENT_ID, POST_ID, PARENT_COMMENT_ID, MEMBER_NO, CONTENT,
                     LIKE_COUNT, DISLIKE_COUNT, IS_DELETED, CREATED_AT, UPDATED_AT)
                    VALUES (?, ?, ?, ?, ?, 0, 0, 'N', SYSDATE, NULL)
                    """;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, commentId);
                pstmt.setLong(2, postId);
                if (parentCommentId == null) pstmt.setNull(3, java.sql.Types.NUMERIC);
                else pstmt.setLong(3, parentCommentId);
                pstmt.setLong(4, memberNo);
                pstmt.setString(5, content);
                if (pstmt.executeUpdate() != 1) {
                    conn.rollback();
                    return 0L;
                }
            }
            conn.commit();
            return commentId;
        } catch (Exception e) {
            e.printStackTrace();
            rollback(conn);
            return 0L;
        } finally {
            close(conn);
        }
    }

    public boolean update(long commentId, long memberNo, String content) {
        String sql = """
                UPDATE POST_COMMENT
                SET CONTENT = ?, UPDATED_AT = SYSDATE
                WHERE COMMENT_ID = ? AND MEMBER_NO = ? AND IS_DELETED = 'N'
                """;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, content);
            pstmt.setLong(2, commentId);
            pstmt.setLong(3, memberNo);
            return pstmt.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(long commentId, long memberNo) {
        String sql = """
                UPDATE POST_COMMENT
                SET IS_DELETED = 'Y', UPDATED_AT = SYSDATE
                WHERE COMMENT_ID = ? AND MEMBER_NO = ? AND IS_DELETED = 'N'
                """;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, commentId);
            pstmt.setLong(2, memberNo);
            return pstmt.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isValidParent(Connection conn, long postId, long parentId) throws Exception {
        String sql = """
                SELECT 1 FROM POST_COMMENT
                WHERE COMMENT_ID = ? AND POST_ID = ?
                  AND PARENT_COMMENT_ID IS NULL AND IS_DELETED = 'N'
                """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, parentId);
            pstmt.setLong(2, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private long nextCommentId(Connection conn) throws Exception {
        // 현재 DDL에 댓글 시퀀스가 확인되지 않아 테이블 잠금 후 안전하게 다음 번호를 만든다.
        try (Statement lock = conn.createStatement()) {
            lock.execute("LOCK TABLE POST_COMMENT IN EXCLUSIVE MODE");
        }
        String sql = "SELECT NVL(MAX(COMMENT_ID), 0) + 1 FROM POST_COMMENT";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        throw new IllegalStateException("댓글 번호 생성 실패");
    }

    private CommentDTO map(ResultSet rs) throws Exception {
        CommentDTO dto = new CommentDTO();
        dto.setCommentId(rs.getLong("COMMENT_ID"));
        dto.setPostId(rs.getLong("POST_ID"));
        long parentId = rs.getLong("PARENT_COMMENT_ID");
        dto.setParentCommentId(rs.wasNull() ? null : parentId);
        dto.setMemberNo(rs.getLong("MEMBER_NO"));
        dto.setNickname(rs.getString("NICKNAME"));
        dto.setNickname(rs.getString("NICKNAME"));
        dto.setUsername(rs.getString("NICKNAME"));
        dto.setContent(rs.getString("CONTENT"));
        dto.setLikeCount(rs.getInt("LIKE_COUNT"));
        dto.setDislikeCount(rs.getInt("DISLIKE_COUNT"));
        dto.setIsDeleted(rs.getString("IS_DELETED"));
        Timestamp created = rs.getTimestamp("CREATED_AT");
        Timestamp updated = rs.getTimestamp("UPDATED_AT");
        dto.setCreatedAt(created == null ? "" : created.toString());
        dto.setUpdatedAt(updated == null ? null : updated.toString());
        return dto;
    }

    private void rollback(Connection conn) {
        try { if (conn != null) conn.rollback(); } catch (Exception ignored) {}
    }

    private void close(Connection conn) {
        try {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        } catch (Exception ignored) {}
    }
}
