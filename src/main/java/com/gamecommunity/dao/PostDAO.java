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
            SELECT P.POST_ID, P.CATEGORY_ID, P.MEMBER_NO, M.USERNAME, M.NICKNAME,
                   P.TITLE, P.CONTENT, P.VIEW_COUNT, P.LIKE_COUNT, P.DISLIKE_COUNT,
                   P.IS_NOTICE, P.IS_DELETED,
                   TO_CHAR(P.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT
            FROM POST P JOIN MEMBER M ON P.MEMBER_NO = M.MEMBER_NO
            WHERE P.IS_DELETED = 'N'
            ORDER BY P.POST_ID DESC
            """;
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) postList.add(mapPost(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return postList;
    }

    // =========================================================
// 카테고리별 게시글 목록
// =========================================================
    public List<PostDTO> findByCategoryId(long categoryId) {
        List<PostDTO> postList = new ArrayList<>();
        String sql = """
        SELECT P.POST_ID, P.CATEGORY_ID, P.MEMBER_NO, M.USERNAME, M.NICKNAME,
               P.TITLE, P.CONTENT, P.VIEW_COUNT, P.LIKE_COUNT, P.DISLIKE_COUNT,
               P.IS_NOTICE, P.IS_DELETED,
               TO_CHAR(P.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT
        FROM POST P JOIN MEMBER M ON P.MEMBER_NO = M.MEMBER_NO
        WHERE P.IS_DELETED = 'N' AND P.CATEGORY_ID = ?
        ORDER BY P.POST_ID DESC
        """;
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) postList.add(mapPost(rs)); }
        } catch (Exception e) { e.printStackTrace(); }
        return postList;
    }

    // =========================================================
// 게시글 작성
// =========================================================
    public boolean save(PostDTO post) {
        String sql = """
        INSERT INTO POST (POST_ID, CATEGORY_ID, MEMBER_NO, TITLE, CONTENT,
                          VIEW_COUNT, LIKE_COUNT, DISLIKE_COUNT, IS_NOTICE, IS_DELETED, CREATED_AT)
        VALUES ((SELECT NVL(MAX(POST_ID), 0) + 1 FROM POST), ?, ?, ?, ?, 0, 0, 0, 'N', 'N', SYSDATE)
        """;
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, post.getCategoryId());
            pstmt.setLong(2, post.getMemberNo());
            pstmt.setString(3, post.getTitle());
            pstmt.setString(4, post.getContent());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // =========================================================
// 게시글 상세 조회
// =========================================================
    public PostDTO findById(Long postId) {
        String sql = """
        SELECT P.POST_ID, P.CATEGORY_ID, P.MEMBER_NO, M.USERNAME, M.NICKNAME,
               P.TITLE, P.CONTENT, P.VIEW_COUNT, P.LIKE_COUNT, P.DISLIKE_COUNT,
               P.IS_NOTICE, P.IS_DELETED,
               TO_CHAR(P.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT
        FROM POST P JOIN MEMBER M ON P.MEMBER_NO = M.MEMBER_NO
        WHERE P.POST_ID = ? AND P.IS_DELETED = 'N'
        """;
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return mapPost(rs); }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // =========================================================
// 게시글 수정
// =========================================================
    public boolean update(PostDTO post) {
        String sql = """
            UPDATE POST
            SET TITLE = ?, CONTENT = ?, UPDATED_AT = SYSDATE
            WHERE POST_ID = ? AND IS_DELETED = 'N'
            """;
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, post.getTitle());
            pstmt.setString(2, post.getContent());
            pstmt.setLong(3, post.getPostId());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("POST UPDATE ERROR");
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
// 게시글 논리 삭제
// =========================================================
    public boolean delete(Long postId) {
        String sql = """UPDATE POST SET IS_DELETED = 'Y' WHERE POST_ID = ? AND IS_DELETED = 'N'""";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId); return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean increaseLike(Long postId) {
        String sql = """UPDATE POST SET LIKE_COUNT = LIKE_COUNT + 1 WHERE POST_ID = ? AND IS_DELETED = 'N'""";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId); return pstmt.executeUpdate() > 0;
        } catch (Exception e) { System.out.println("LIKE ERROR"); e.printStackTrace(); return false; }
    }

    public boolean increaseDislike(Long postId) {
        String sql = """UPDATE POST SET DISLIKE_COUNT = DISLIKE_COUNT + 1 WHERE POST_ID = ? AND IS_DELETED = 'N'""";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId); return pstmt.executeUpdate() > 0;
        } catch (Exception e) { System.out.println("DISLIKE ERROR"); e.printStackTrace(); return false; }
    }

    public boolean increaseViewCount(Long postId) {
        String sql = """UPDATE POST SET VIEW_COUNT = VIEW_COUNT + 1 WHERE POST_ID = ? AND IS_DELETED = 'N'""";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId); return pstmt.executeUpdate() > 0;
        } catch (Exception e) { System.out.println("VIEW COUNT ERROR"); e.printStackTrace(); return false; }
    }

    // =========================================================
// 공지사항 목록 조회
// =========================================================
    public List<PostDTO> findNotices() {
        List<PostDTO> noticeList = new ArrayList<>();
        String sql = """
        SELECT P.POST_ID, P.CATEGORY_ID, P.MEMBER_NO, M.USERNAME, M.NICKNAME,
               P.TITLE, P.CONTENT, P.VIEW_COUNT, P.LIKE_COUNT, P.DISLIKE_COUNT,
               P.IS_NOTICE, P.IS_DELETED,
               TO_CHAR(P.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT
        FROM POST P JOIN MEMBER M ON P.MEMBER_NO = M.MEMBER_NO
        WHERE P.IS_DELETED = 'N' AND P.IS_NOTICE = 'Y'
        ORDER BY P.POST_ID DESC
        """;
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) noticeList.add(mapPost(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return noticeList;
    }

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
