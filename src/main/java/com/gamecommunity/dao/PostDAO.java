package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;
import com.gamecommunity.dto.PostDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostDAO {


    // =========================================================
// 게시글 목록
// =========================================================
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
                post.setNickname(rs.getString("NICKNAME"));

                postList.add(post);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return postList;
    }

    // =========================================================
// 카테고리별 게시글 목록
// =========================================================
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

                    postList.add(post);
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
    public boolean save(PostDTO post) {

        System.out.println("SAVE 1 - DB 연결 시작");

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
                Connection conn = DBUtil.getConnection()
        ) {

            System.out.println("SAVE 2 - DB 연결 성공");

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                System.out.println("SAVE 3 - PreparedStatement 생성");

                // POST_ID는 DB에서 MAX(POST_ID) + 1로 생성
                // 따라서 Java에서 post.getPostId()를 받지 않는다.
                pstmt.setLong(1, post.getCategoryId());
                pstmt.setLong(2, post.getMemberNo());
                pstmt.setString(3, post.getTitle());
                pstmt.setString(4, post.getContent());

                System.out.println("SAVE 4 - 파라미터 설정 완료");

                int result = pstmt.executeUpdate();

                System.out.println("SAVE 5 - executeUpdate 완료 = " + result);

                return result > 0;
            }

        } catch (Exception e) {

            System.out.println("SAVE ERROR");
            e.printStackTrace();

            return false;
        }
    }


    // =========================================================
// 게시글 상세 조회
// =========================================================
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

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    // =========================================================
// 좋아요 증가
// =========================================================
    // 게시글 논리 삭제
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

            int result = pstmt.executeUpdate();

            return result > 0;

        } catch (Exception e) {

            System.out.println("LIKE ERROR");
            e.printStackTrace();

            return false;
        }
    }


    // =========================================================
// 나빠요 증가
// =========================================================
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

            int result = pstmt.executeUpdate();

            return result > 0;

        } catch (Exception e) {

            System.out.println("DISLIKE ERROR");
            e.printStackTrace();

            return false;
        }
    }
    public boolean increaseViewCount(Long postId) {

        String sql = """
            UPDATE POST
            SET VIEW_COUNT = VIEW_COUNT + 1
            WHERE POST_ID = ?
              AND IS_DELETED = 'N'
            """;

        try (
                java.sql.Connection conn = DBUtil.getConnection();
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, postId);
            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("VIEW COUNT ERROR");
            e.printStackTrace();
            return false;
        }
    }
    // =========================================================
// 공지사항 목록 조회
// =========================================================
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
          AND P.IS_NOTICE = 'Y'  -- ★ 공지사항만 가져오는 조건
        ORDER BY P.POST_ID DESC
        """;

        try (
                java.sql.Connection conn = com.gamecommunity.DBUtil.getConnection();
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                java.sql.ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
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

                noticeList.add(post);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return noticeList;
    }

}
