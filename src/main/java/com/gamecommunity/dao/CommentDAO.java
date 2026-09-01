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

/**
 * 댓글과 관련된 DB 작업을 담당하는 DAO입니다.
 *
 * 댓글 조회, 작성, 수정, 삭제와 대댓글 작성에 필요한
 * 부모 댓글 검증 작업을 담당합니다.
 */
public class CommentDAO {

    // =========================================================
    // 게시글의 댓글 조회
    // =========================================================

    /**
     * 특정 게시글에 작성된 댓글을 조회합니다.
     *
     * 부모 댓글과 대댓글의 관계를 유지하기 위해
     * 부모 댓글 → 대댓글 순서로 정렬합니다.
     *
     * @param postId 댓글을 조회할 게시글 번호
     * @return 해당 게시글의 댓글 목록
     */
    public List<CommentDTO> findByPostId(long postId) {

        List<CommentDTO> comments = new ArrayList<>();

        String sql = """
                SELECT
                    C.COMMENT_ID,
                    C.POST_ID,
                    C.PARENT_COMMENT_ID,
                    C.MEMBER_NO,
                    M.NICKNAME,
                    C.CONTENT,
                    C.LIKE_COUNT,
                    C.DISLIKE_COUNT,
                    C.IS_DELETED,
                    C.CREATED_AT,
                    C.UPDATED_AT
                FROM POST_COMMENT C
                JOIN MEMBER M
                    ON C.MEMBER_NO = M.MEMBER_NO
                WHERE C.POST_ID = ?
                ORDER BY
                    NVL(C.PARENT_COMMENT_ID, C.COMMENT_ID),
                    CASE
                        WHEN C.PARENT_COMMENT_ID IS NULL THEN 0
                        ELSE 1
                    END,
                    C.CREATED_AT,
                    C.COMMENT_ID
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            // 조회할 게시글 번호를 SQL의 ?에 전달합니다.
            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // ResultSet 한 행을 CommentDTO로 변환합니다.
                    comments.add(map(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return comments;
    }

    // =========================================================
    // 댓글 작성
    // =========================================================

    /**
     * 게시글에 댓글 또는 대댓글을 작성합니다.
     *
     * parentCommentId가 null이면 일반 댓글이고,
     * 값이 있으면 해당 부모 댓글의 대댓글로 저장합니다.
     *
     * @param postId 댓글을 작성할 게시글 번호
     * @param parentCommentId 부모 댓글 번호, 일반 댓글이면 null
     * @param memberNo 댓글 작성자 회원 번호
     * @param content 댓글 내용
     * @return 생성된 댓글 번호, 실패하면 0
     */
    public long save(
            long postId,
            Long parentCommentId,
            long memberNo,
            String content
    ) {
        Connection conn = null;

        try {
            conn = DBUtil.getConnection();

            // 댓글 작성 중간에 오류가 발생하면 전체 작업을 취소하기 위해
            // 자동 커밋을 끕니다.
            conn.setAutoCommit(false);

            // 대댓글이라면 부모 댓글이 실제로 존재하는지 확인합니다.
            if (parentCommentId != null
                    && !isValidParent(conn, postId, parentCommentId)) {
                conn.rollback();
                return 0L;
            }

            // 새 댓글에 사용할 번호를 생성합니다.
            long commentId = nextCommentId(conn);

            String sql = """
                    INSERT INTO POST_COMMENT (
                        COMMENT_ID,
                        POST_ID,
                        PARENT_COMMENT_ID,
                        MEMBER_NO,
                        CONTENT,
                        LIKE_COUNT,
                        DISLIKE_COUNT,
                        IS_DELETED,
                        CREATED_AT,
                        UPDATED_AT
                    )
                    VALUES (
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        0,
                        0,
                        'N',
                        SYSDATE,
                        NULL
                    )
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, commentId);
                pstmt.setLong(2, postId);

                // 부모 댓글 번호가 없으면 DB에 NULL을 저장합니다.
                if (parentCommentId == null) {
                    pstmt.setNull(3, java.sql.Types.NUMERIC);
                } else {
                    pstmt.setLong(3, parentCommentId);
                }

                pstmt.setLong(4, memberNo);
                pstmt.setString(5, content);

                // 정확히 1개의 댓글이 생성되어야 성공으로 처리합니다.
                if (pstmt.executeUpdate() != 1) {
                    conn.rollback();
                    return 0L;
                }
            }

            // 모든 작업이 성공했으므로 DB에 반영합니다.
            conn.commit();
            return commentId;

        } catch (Exception e) {
            e.printStackTrace();

            // 오류가 발생하면 지금까지 진행한 작업을 취소합니다.
            rollback(conn);
            return 0L;

        } finally {
            // 사용한 DB 연결을 닫습니다.
            close(conn);
        }
    }

    // =========================================================
    // 댓글 수정
    // =========================================================

    /**
     * 댓글 작성자 본인의 댓글만 수정합니다.
     * 이미 삭제된 댓글은 수정할 수 없습니다.
     *
     * @param commentId 수정할 댓글 번호
     * @param memberNo 수정하려는 회원 번호
     * @param content 새 댓글 내용
     * @return 수정 성공 여부
     */
    public boolean update(long commentId, long memberNo, String content) {

        String sql = """
                UPDATE POST_COMMENT
                SET
                    CONTENT = ?,
                    UPDATED_AT = SYSDATE
                WHERE COMMENT_ID = ?
                  AND MEMBER_NO = ?
                  AND IS_DELETED = 'N'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, content);
            pstmt.setLong(2, commentId);
            pstmt.setLong(3, memberNo);

            return pstmt.executeUpdate() == 1;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // 댓글 삭제
    // =========================================================

    /**
     * 댓글 작성자 본인의 댓글을 논리 삭제합니다.
     *
     * 실제 행을 DELETE하지 않고 IS_DELETED를 Y로 변경합니다.
     * 따라서 댓글 데이터를 DB에 남겨둘 수 있습니다.
     *
     * @param commentId 삭제할 댓글 번호
     * @param memberNo 삭제하려는 회원 번호
     * @return 삭제 성공 여부
     */
    public boolean delete(long commentId, long memberNo) {

        String sql = """
                UPDATE POST_COMMENT
                SET
                    IS_DELETED = 'Y',
                    UPDATED_AT = SYSDATE
                WHERE COMMENT_ID = ?
                  AND MEMBER_NO = ?
                  AND IS_DELETED = 'N'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, commentId);
            pstmt.setLong(2, memberNo);

            return pstmt.executeUpdate() == 1;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // 대댓글 부모 댓글 검증
    // =========================================================

    /**
     * 대댓글의 부모 댓글이 유효한지 확인합니다.
     *
     * 부모 댓글은
     * 1. 같은 게시글에 속해야 하고
     * 2. 일반 댓글이어야 하며
     * 3. 삭제되지 않은 댓글이어야 합니다.
     */
    private boolean isValidParent(
            Connection conn,
            long postId,
            long parentId
    ) throws Exception {

        String sql = """
                SELECT 1
                FROM POST_COMMENT
                WHERE COMMENT_ID = ?
                  AND POST_ID = ?
                  AND PARENT_COMMENT_ID IS NULL
                  AND IS_DELETED = 'N'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, parentId);
            pstmt.setLong(2, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // =========================================================
    // 댓글 번호 생성
    // =========================================================

    /**
     * 새로운 댓글 번호를 생성합니다.
     *
     * 현재 프로젝트에서는 댓글 전용 시퀀스를 사용하지 않기 때문에
     * 테이블을 잠근 후 가장 큰 COMMENT_ID + 1을 사용합니다.
     */
    private long nextCommentId(Connection conn) throws Exception {

        // 동시에 여러 사용자가 댓글을 작성할 때 같은 번호가 생성되는 것을 막습니다.
        try (Statement lock = conn.createStatement()) {
            lock.execute("LOCK TABLE POST_COMMENT IN EXCLUSIVE MODE");
        }

        String sql = "SELECT NVL(MAX(COMMENT_ID), 0) + 1 FROM POST_COMMENT";

        try (
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        throw new IllegalStateException("댓글 번호 생성 실패");
    }

    // =========================================================
    // ResultSet → CommentDTO 변환
    // =========================================================

    /**
     * DB에서 조회한 댓글 한 행을 CommentDTO로 변환합니다.
     * 조회 메서드에서 반복되는 DTO 세팅 코드를 하나로 모아두었습니다.
     */
    private CommentDTO map(ResultSet rs) throws Exception {

        CommentDTO dto = new CommentDTO();

        dto.setCommentId(rs.getLong("COMMENT_ID"));
        dto.setPostId(rs.getLong("POST_ID"));

        // 부모 댓글이 없는 일반 댓글은 null로 저장합니다.
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

    // =========================================================
    // 트랜잭션 롤백
    // =========================================================

    /**
     * DB 작업 중 오류가 발생했을 때 트랜잭션을 취소합니다.
     */
    private void rollback(Connection conn) {
        try {
            if (conn != null) {
                conn.rollback();
            }
        } catch (Exception ignored) {
            // 롤백 과정에서 발생한 오류는 별도로 처리하지 않습니다.
        }
    }

    // =========================================================
    // DB 연결 종료
    // =========================================================

    /**
     * 사용이 끝난 DB 연결을 닫습니다.
     */
    private void close(Connection conn) {
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
