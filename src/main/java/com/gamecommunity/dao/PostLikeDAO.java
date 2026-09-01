package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 게시글 좋아요 / 싫어요와 관련된 DB 작업을 담당하는 DAO입니다.
 */
public class PostLikeDAO {

    // =========================================================
    // 좋아요 / 싫어요 등록
    // =========================================================

    /**
     * 회원이 게시글에 좋아요 또는 싫어요를 등록합니다.
     *
     * 같은 회원이 같은 게시글에 중복으로 추천하는 것은 막습니다.
     *
     * @param postId   게시글 번호
     * @param memberNo 회원 번호
     * @param likeType "LIKE" 또는 "DISLIKE"
     * @return 등록 성공 여부
     */
    public boolean addLike(long postId, long memberNo, String likeType) {

        // 허용된 추천 종류인지 먼저 확인합니다.
        if (!"LIKE".equals(likeType) && !"DISLIKE".equals(likeType)) {
            return false;
        }

        // 실제 추천 기록을 저장하는 SQL입니다.
        String insertSql = """
                INSERT INTO POST_LIKE (
                    POST_ID,
                    MEMBER_NO,
                    LIKE_TYPE,
                    CREATED_AT
                )
                VALUES (?, ?, ?, SYSDATE)
                """;

        // 좋아요인지 싫어요인지에 따라 POST의 다른 컬럼을 증가시킵니다.
        String updateSql;

        if ("LIKE".equals(likeType)) {
            updateSql = """
                    UPDATE POST
                    SET LIKE_COUNT = LIKE_COUNT + 1
                    WHERE POST_ID = ?
                    """;
        } else {
            updateSql = """
                    UPDATE POST
                    SET DISLIKE_COUNT = DISLIKE_COUNT + 1
                    WHERE POST_ID = ?
                    """;
        }

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();

            // 추천 기록 저장과 게시글 숫자 증가를 하나의 작업으로 처리합니다.
            conn.setAutoCommit(false);

            // -------------------------------------------------
            // 1. 이미 추천했는지 확인
            // -------------------------------------------------
            String checkSql = """
                    SELECT LIKE_TYPE
                    FROM POST_LIKE
                    WHERE POST_ID = ?
                      AND MEMBER_NO = ?
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setLong(1, postId);
                pstmt.setLong(2, memberNo);

                try (ResultSet rs = pstmt.executeQuery()) {
                    // 이미 추천 기록이 있으면 중복 추천을 허용하지 않습니다.
                    if (rs.next()) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // -------------------------------------------------
            // 2. 추천 기록 저장
            // -------------------------------------------------
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setLong(1, postId);
                pstmt.setLong(2, memberNo);
                pstmt.setString(3, likeType);

                pstmt.executeUpdate();
            }

            // -------------------------------------------------
            // 3. 게시글의 좋아요 / 싫어요 숫자 증가
            // -------------------------------------------------
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setLong(1, postId);
                pstmt.executeUpdate();
            }

            // 모든 DB 작업이 성공했으므로 최종 반영합니다.
            conn.commit();

            return true;

        } catch (Exception e) {
            e.printStackTrace();

            // 중간에 오류가 발생하면 지금까지의 작업을 취소합니다.
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception rollbackException) {
                rollbackException.printStackTrace();
            }

            return false;

        } finally {
            // 연결을 사용한 뒤에는 반드시 원래 상태로 돌려놓고 닫습니다.
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // =========================================================
    // 게시글 좋아요 개수 조회
    // =========================================================

    /**
     * 특정 게시글의 좋아요 개수를 조회합니다.
     */
    public int getLikeCount(long postId) {

        String sql = """
                SELECT LIKE_COUNT
                FROM POST
                WHERE POST_ID = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("LIKE_COUNT");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // =========================================================
    // 게시글 싫어요 개수 조회
    // =========================================================

    /**
     * 특정 게시글의 싫어요 개수를 조회합니다.
     */
    public int getDislikeCount(long postId) {

        String sql = """
                SELECT DISLIKE_COUNT
                FROM POST
                WHERE POST_ID = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("DISLIKE_COUNT");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // =========================================================
    // 회원의 추천 상태 조회
    // =========================================================

    /**
     * 회원이 특정 게시글에 어떤 추천을 했는지 조회합니다.
     *
     * @return "LIKE", "DISLIKE", 추천하지 않았다면 null
     */
    public String getLikeType(long postId, long memberNo) {

        String sql = """
                SELECT LIKE_TYPE
                FROM POST_LIKE
                WHERE POST_ID = ?
                  AND MEMBER_NO = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, postId);
            pstmt.setLong(2, memberNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("LIKE_TYPE");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // 회원이 받은 좋아요 개수
    // =========================================================

    /**
     * 특정 회원이 작성한 게시글에 받은 좋아요 총 개수를 조회합니다.
     * 삭제된 게시글의 추천은 제외합니다.
     */
    public int getReceivedLikeCount(long memberNo) {

        String sql = """
                SELECT COUNT(*)
                FROM POST_LIKE PL
                JOIN POST P
                    ON PL.POST_ID = P.POST_ID
                WHERE P.MEMBER_NO = ?
                  AND P.IS_DELETED = 'N'
                  AND PL.LIKE_TYPE = 'LIKE'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, memberNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // =========================================================
    // 회원이 받은 싫어요 개수
    // =========================================================

    /**
     * 특정 회원이 작성한 게시글에 받은 싫어요 총 개수를 조회합니다.
     * 삭제된 게시글의 추천은 제외합니다.
     */
    public int getReceivedDislikeCount(long memberNo) {

        String sql = """
                SELECT COUNT(*)
                FROM POST_LIKE PL
                JOIN POST P
                    ON PL.POST_ID = P.POST_ID
                WHERE P.MEMBER_NO = ?
                  AND P.IS_DELETED = 'N'
                  AND PL.LIKE_TYPE = 'DISLIKE'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, memberNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
