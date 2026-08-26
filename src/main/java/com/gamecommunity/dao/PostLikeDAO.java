package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PostLikeDAO {

    /**
     * 좋아요 / 나빠요 추가
     *
     * likeType
     *  - LIKE
     *  - DISLIKE
     */
    public boolean addLike(long postId, long memberNo, String likeType) {

        if (!"LIKE".equals(likeType) && !"DISLIKE".equals(likeType)) {
            return false;
        }

        String insertSql = """
                INSERT INTO POST_LIKE (
                    POST_ID,
                    MEMBER_NO,
                    LIKE_TYPE,
                    CREATED_AT
                )
                VALUES (?, ?, ?, SYSDATE)
                """;

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

            conn.setAutoCommit(false);

            // 이미 좋아요 / 나빠요를 했는지 확인
            String checkSql = """
                    SELECT LIKE_TYPE
                    FROM POST_LIKE
                    WHERE POST_ID = ?
                      AND MEMBER_NO = ?
                    """;

            try (PreparedStatement pstmt =
                         conn.prepareStatement(checkSql)) {

                pstmt.setLong(1, postId);
                pstmt.setLong(2, memberNo);

                try (ResultSet rs = pstmt.executeQuery()) {

                    if (rs.next()) {

                        // 이미 좋아요 또는 나빠요를 한 경우
                        conn.rollback();

                        return false;
                    }
                }
            }

            // 추천 기록 저장
            try (PreparedStatement pstmt =
                         conn.prepareStatement(insertSql)) {

                pstmt.setLong(1, postId);
                pstmt.setLong(2, memberNo);
                pstmt.setString(3, likeType);

                pstmt.executeUpdate();
            }

            // POST 카운트 증가
            try (PreparedStatement pstmt =
                         conn.prepareStatement(updateSql)) {

                pstmt.setLong(1, postId);

                pstmt.executeUpdate();
            }

            conn.commit();

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception rollbackException) {
                rollbackException.printStackTrace();
            }

            return false;

        } finally {

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


    /**
     * 좋아요 개수
     */
    public int getLikeCount(long postId) {

        String sql = """
                SELECT LIKE_COUNT
                FROM POST
                WHERE POST_ID = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
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


    /**
     * 나빠요 개수
     */
    public int getDislikeCount(long postId) {

        String sql = """
                SELECT DISLIKE_COUNT
                FROM POST
                WHERE POST_ID = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
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


    /**
     * 회원이 해당 게시글에
     * 좋아요 / 나빠요를 했는지 확인
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
                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
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
    /**
     * 회원이 받은 좋아요 수
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


    /**
     * 회원이 받은 나빠요 수
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

