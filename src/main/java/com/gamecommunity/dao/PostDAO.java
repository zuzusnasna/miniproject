package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;
import com.gamecommunity.dto.PostDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시글과 관련된 DB 작업을 담당하는 DAO입니다.
 *
 * 게시글 조회, 작성, 수정, 삭제와
 * 조회수 / 좋아요 / 싫어요 증가 기능을 담당합니다.
 */
public class PostDAO {

    // =========================================================
    // 전체 게시글 조회
    // =========================================================

    /**
     * 삭제되지 않은 모든 게시글을 최신순으로 조회합니다.
     */
    public List<PostDTO> findAll() {

        List<PostDTO> postList = new ArrayList<>();

        String sql = """
                SELECT
                    P.POST_ID,
                    P.CATEGORY_ID,
                    P.MEMBER_NO,
                    M.USERNAME,
                    M.NICKNAME,
                    P.TITLE,
                    P.CONTENT,
                    P.VIEW_COUNT,
                    P.LIKE_COUNT,
                    P.DISLIKE_COUNT,
                    P.IS_NOTICE,
                    P.IS_DELETED,
                    TO_CHAR(P.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT
                FROM POST P
                JOIN MEMBER M
                    ON P.MEMBER_NO = M.MEMBER_NO
                WHERE P.IS_DELETED = 'N'
                ORDER BY P.POST_ID DESC
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                postList.add(mapPost(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return postList;
    }

    // =========================================================
    // 특정 카테고리의 게시글 조회
    // =========================================================

    /**
     * 특정 게임/게시판 카테고리에 속한 게시글만 조회합니다.
     * 삭제된 게시글은 제외합니다.
     *
     * @param categoryId 카테고리 번호
     */
    public List<PostDTO> findByCategoryId(long categoryId) {

        List<PostDTO> postList = new ArrayList<>();

        String sql = """
                SELECT
                    P.POST_ID,
                    P.CATEGORY_ID,
                    P.MEMBER_NO,
                    M.USERNAME,
                    M.NICKNAME,
                    P.TITLE,
                    P.CONTENT,
                    P.VIEW_COUNT,
                    P.LIKE_COUNT,
                    P.DISLIKE_COUNT,
                    P.IS_NOTICE,
                    P.IS_DELETED,
                    TO_CHAR(P.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT
                FROM POST P
                JOIN MEMBER M
                    ON P.MEMBER_NO = M.MEMBER_NO
                WHERE P.IS_DELETED = 'N'
                  AND P.CATEGORY_ID = ?
                ORDER BY P.POST_ID DESC
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    postList.add(mapPost(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return postList;
    }

    // =========================================================
    // 게시글 작성
    // =========================================================

    /**
     * 새로운 게시글을 저장합니다.
     * 처음 생성할 때 조회수와 좋아요/싫어요는 모두 0으로 시작합니다.
     */
    public boolean save(PostDTO post) {

        String sql = """
                INSERT INTO POST (
                    POST_ID,
                    CATEGORY_ID,
                    MEMBER_NO,
                    TITLE,
                    CONTENT,
                    VIEW_COUNT,
                    LIKE_COUNT,
                    DISLIKE_COUNT,
                    IS_NOTICE,
                    IS_DELETED,
                    CREATED_AT
                )
                VALUES (
                    (SELECT NVL(MAX(POST_ID), 0) + 1 FROM POST),
                    ?,
                    ?,
                    ?,
                    ?,
                    0,
                    0,
                    0,
                    'N',
                    'N',
                    SYSDATE
                )
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, post.getCategoryId());
            pstmt.setLong(2, post.getMemberNo());
            pstmt.setString(3, post.getTitle());
            pstmt.setString(4, post.getContent());

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // 게시글 1개 조회
    // =========================================================

    /**
     * 게시글 번호로 게시글 하나를 조회합니다.
     * 삭제된 게시글은 조회하지 않습니다.
     */
    public PostDTO findById(Long postId) {

        String sql = """
                SELECT
                    P.POST_ID,
                    P.CATEGORY_ID,
                    P.MEMBER_NO,
                    M.USERNAME,
                    M.NICKNAME,
                    P.TITLE,
                    P.CONTENT,
                    P.VIEW_COUNT,
                    P.LIKE_COUNT,
                    P.DISLIKE_COUNT,
                    P.IS_NOTICE,
                    P.IS_DELETED,
                    TO_CHAR(P.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT
                FROM POST P
                JOIN MEMBER M
                    ON P.MEMBER_NO = M.MEMBER_NO
                WHERE P.POST_ID = ?
                  AND P.IS_DELETED = 'N'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapPost(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // 게시글 수정
    // =========================================================

    /**
     * 게시글의 제목과 내용을 수정합니다.
     */
    public boolean update(PostDTO post) {

        String sql = """
                UPDATE POST
                SET TITLE = ?,
                    CONTENT = ?,
                    UPDATED_AT = SYSDATE
                WHERE POST_ID = ?
                  AND IS_DELETED = 'N'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, post.getTitle());
            pstmt.setString(2, post.getContent());
            pstmt.setLong(3, post.getPostId());

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // 게시글 삭제
    // =========================================================

    /**
     * 게시글을 논리적으로 삭제합니다.
     * 실제 데이터를 DELETE하지 않고 IS_DELETED를 Y로 변경합니다.
     */
    public boolean delete(Long postId) {

        String sql = """
                UPDATE POST
                SET IS_DELETED = 'Y'
                WHERE POST_ID = ?
                  AND IS_DELETED = 'N'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, postId);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // 좋아요 개수 증가
    // =========================================================

    /**
     * 게시글의 좋아요 개수를 1 증가시킵니다.
     */
    public boolean increaseLike(Long postId) {

        String sql = """
                UPDATE POST
                SET LIKE_COUNT = LIKE_COUNT + 1
                WHERE POST_ID = ?
                  AND IS_DELETED = 'N'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, postId);
            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // 싫어요 개수 증가
    // =========================================================

    /**
     * 게시글의 싫어요 개수를 1 증가시킵니다.
     */
    public boolean increaseDislike(Long postId) {

        String sql = """
                UPDATE POST
                SET DISLIKE_COUNT = DISLIKE_COUNT + 1
                WHERE POST_ID = ?
                  AND IS_DELETED = 'N'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, postId);
            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // 조회수 증가
    // =========================================================

    /**
     * 게시글을 조회할 때 조회수를 1 증가시킵니다.
     */
    public boolean increaseViewCount(Long postId) {

        String sql = """
                UPDATE POST
                SET VIEW_COUNT = VIEW_COUNT + 1
                WHERE POST_ID = ?
                  AND IS_DELETED = 'N'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, postId);
            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // 공지글 조회
    // =========================================================

    /**
     * 공지글만 따로 조회합니다.
     */
    public List<PostDTO> findNotices() {

        List<PostDTO> noticeList = new ArrayList<>();

        String sql = """
                SELECT
                    P.POST_ID,
                    P.CATEGORY_ID,
                    P.MEMBER_NO,
                    M.USERNAME,
                    M.NICKNAME,
                    P.TITLE,
                    P.CONTENT,
                    P.VIEW_COUNT,
                    P.LIKE_COUNT,
                    P.DISLIKE_COUNT,
                    P.IS_NOTICE,
                    P.IS_DELETED,
                    TO_CHAR(P.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT
                FROM POST P
                JOIN MEMBER M
                    ON P.MEMBER_NO = M.MEMBER_NO
                WHERE P.IS_DELETED = 'N'
                  AND P.IS_NOTICE = 'Y'
                ORDER BY P.POST_ID DESC
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                noticeList.add(mapPost(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return noticeList;
    }

    // =========================================================
    // ResultSet → PostDTO 변환
    // =========================================================

    /**
     * SQL 조회 결과(ResultSet)를 PostDTO 객체로 변환합니다.
     * 게시글 조회 메서드마다 같은 코드를 반복하지 않도록 분리했습니다.
     */
    private PostDTO mapPost(ResultSet rs) throws SQLException {

        PostDTO post = new PostDTO();

        post.setPostId(rs.getLong("POST_ID"));
        post.setCategoryId(rs.getLong("CATEGORY_ID"));
        post.setMemberNo(rs.getLong("MEMBER_NO"));
        post.setUsername(rs.getString("USERNAME"));
        post.setNickname(rs.getString("NICKNAME"));
        post.setTitle(rs.getString("TITLE"));
        post.setContent(rs.getString("CONTENT"));
        post.setViewCount(rs.getInt("VIEW_COUNT"));
        post.setLikeCount(rs.getInt("LIKE_COUNT"));
        post.setDislikeCount(rs.getInt("DISLIKE_COUNT"));
        post.setIsNotice(rs.getString("IS_NOTICE"));
        post.setIsDeleted(rs.getString("IS_DELETED"));
        post.setCreatedAt(rs.getString("CREATED_AT"));

        return post;
    }
}
