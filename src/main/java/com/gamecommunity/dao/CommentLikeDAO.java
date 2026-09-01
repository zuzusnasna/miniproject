package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 댓글 좋아요 / 싫어요와 관련된 DB 작업을 담당하는 DAO입니다.
 */
public class CommentLikeDAO {

    // =========================================================
    // 댓글 좋아요 / 싫어요 등록
    // =========================================================

    /**
     * 댓글에 좋아요 또는 싫어요를 등록합니다.
     *
     * 이미 해당 댓글에 반응을 남긴 회원은 중복으로 등록할 수 없습니다.
     * 댓글의 좋아요/싫어요 숫자도 함께 증가시킵니다.
     *
     * @param commentId 댓글 번호
     * @param memberNo 반응을 남기는 회원 번호
     * @param likeType "LIKE" 또는 "DISLIKE"
     * @return 정상적으로 등록되면 true, 실패하면 false
     */
    public boolean addLike(long commentId, long memberNo, String likeType) {

        // 허용된 반응인지 먼저 확인합니다.
        if (!"LIKE".equals(likeType) && !"DISLIKE".equals(likeType)) {
            return false;
        }

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // -------------------------------------------------
            // 1. 이미 좋아요/싫어요를 남겼는지 확인
            // -------------------------------------------------
            String checkSql = """
                    SELECT LIKE_TYPE
                    FROM COMMENT_LIKE
                    WHERE COMMENT_ID = ?
                      AND MEMBER_NO = ?
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setLong(1, commentId);
                pstmt.setLong(2, memberNo);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // 이미 반응이 있으므로 중복 등록하지 않습니다.
                        conn.rollback();
                        return false;
                    }
                }
            }

            // -------------------------------------------------
            // 2. 회원의 반응을 COMMENT_LIKE 테이블에 저장
            // -------------------------------------------------
            String insertSql = """
                    INSERT INTO COMMENT_LIKE (
                        COMMENT_ID,
                        MEMBER_NO,
                        LIKE_TYPE,
                        CREATED_AT
                    )
                    VALUES (?, ?, ?, SYSDATE)
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setLong(1, commentId);
                pstmt.setLong(2, memberNo);
                pstmt.setString(3, likeType);
                pstmt.executeUpdate();
            }

            // -------------------------------------------------
            // 3. 댓글의 좋아요/싫어요 숫자를 1 증가
            // -------------------------------------------------
            String countColumn = "LIKE".equals(likeType)
                    ? "LIKE_COUNT"
                    : "DISLIKE_COUNT";

            String updateSql = """
                    UPDATE POST_COMMENT
                    SET %s = %s + 1
                    WHERE COMMENT_ID = ?
                      AND IS_DELETED = 'N'
                    """.formatted(countColumn, countColumn);

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setLong(1, commentId);

                // 댓글이 실제로 1개 수정되었는지 확인합니다.
                if (pstmt.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
            }

            // 모든 DB 작업이 성공했으므로 저장합니다.
            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();

            // 오류가 발생하면 지금까지의 DB 작업을 모두 취소합니다.
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception ignored) {
                // 롤백 과정에서 발생한 오류는 무시합니다.
            }

            return false;

        } finally {
            // DB 연결을 사용한 뒤 반드시 닫아줍니다.
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (Exception ignored) {
                // 연결 종료 과정에서 발생한 오류는 무시합니다.
            }
        }
    }

    // =========================================================
    // 댓글 좋아요 개수 조회
    // =========================================================

    /**
     * 댓글의 좋아요 개수를 조회합니다.
     *
     * @param commentId 댓글 번호
     * @return 좋아요 개수
     */
    public int getLikeCount(long commentId) {
        return getCount(commentId, "LIKE_COUNT");
    }

    // =========================================================
    // 댓글 싫어요 개수 조회
    // =========================================================

    /**
     * 댓글의 싫어요 개수를 조회합니다.
     *
     * @param commentId 댓글 번호
     * @return 싫어요 개수
     */
    public int getDislikeCount(long commentId) {
        return getCount(commentId, "DISLIKE_COUNT");
    }

    // =========================================================
    // 좋아요 / 싫어요 공통 조회 메서드
    // =========================================================

    /**
     * 좋아요와 싫어요 개수 조회에서 반복되는 DB 코드를 하나로 묶었습니다.
     */
    private int getCount(long commentId, String column) {

        String sql = "SELECT " + column + " FROM POST_COMMENT WHERE COMMENT_ID = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, commentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 댓글을 찾지 못했거나 조회에 실패하면 0을 반환합니다.
        return 0;
    }
}
