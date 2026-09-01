package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;
import com.gamecommunity.dto.CategoryManagerRequestDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 카테고리 관리자 권한 신청과 관련된 DB 작업을 담당하는 DAO입니다.
 *
 * 회원의 신청 상태 확인, 신청 등록, 관리자 신청 목록 조회,
 * 시스템 관리자의 승인/거절 처리를 담당합니다.
 */
public class CategoryManagerRequestDAO {

    // =========================================================
    // 회원의 최근 신청 상태 조회
    // =========================================================

    /**
     * 회원이 가장 최근에 신청한 카테고리 관리자 권한의 상태를 조회합니다.
     *
     * @param memberNo 회원 번호
     * @return PENDING / APPROVED / REJECTED, 신청 기록이 없으면 null
     */
    public String getRequestStatus(long memberNo) {

        String sql = """
                SELECT REQUEST_STATUS
                FROM CATEGORY_MANAGER_REQUEST
                WHERE MEMBER_NO = ?
                ORDER BY REQUEST_ID DESC
                FETCH FIRST 1 ROW ONLY
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, memberNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("REQUEST_STATUS");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // 관리자 권한 신청 등록
    // =========================================================

    /**
     * 회원의 카테고리 관리자 권한 신청을 등록합니다.
     * 처음 신청할 때 상태는 PENDING으로 저장됩니다.
     *
     * @param memberNo 신청 회원 번호
     * @param categoryId 관리자가 되고 싶은 카테고리 번호
     * @return 정상 등록되면 true, 실패하면 false
     */
    public boolean insertRequest(long memberNo, long categoryId) {

        String sql = """
                INSERT INTO CATEGORY_MANAGER_REQUEST (
                    REQUEST_ID,
                    MEMBER_NO,
                    CATEGORY_ID,
                    REQUEST_STATUS,
                    REQUESTED_AT
                )
                VALUES (
                    CATEGORY_MANAGER_REQUEST_SEQ.NEXTVAL,
                    ?,
                    ?,
                    'PENDING',
                    SYSDATE
                )
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, memberNo);
            pstmt.setLong(2, categoryId);

            return pstmt.executeUpdate() == 1;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 대기 중인 신청 존재 여부 확인
    // =========================================================

    /**
     * 회원에게 현재 처리되지 않은 관리자 신청이 있는지 확인합니다.
     */
    public boolean hasPendingRequest(long memberNo) {
        return existsByStatus(memberNo, "PENDING");
    }

    // =========================================================
    // 승인된 신청 존재 여부 확인
    // =========================================================

    /**
     * 회원에게 승인된 관리자 신청이 있는지 확인합니다.
     */
    public boolean hasApprovedRequest(long memberNo) {
        return existsByStatus(memberNo, "APPROVED");
    }

    // =========================================================
    // 신청 상태 공통 확인
    // =========================================================

    /**
     * 회원의 신청 기록 중 특정 상태가 존재하는지 확인합니다.
     *
     * hasPendingRequest(), hasApprovedRequest()에서 공통으로 사용합니다.
     */
    private boolean existsByStatus(long memberNo, String status) {

        String sql = """
                SELECT COUNT(*)
                FROM CATEGORY_MANAGER_REQUEST
                WHERE MEMBER_NO = ?
                  AND REQUEST_STATUS = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, memberNo);
            pstmt.setString(2, status);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 시스템 관리자가 볼 대기 신청 목록 조회
    // =========================================================

    /**
     * 아직 승인/거절되지 않은 관리자 권한 신청 목록을 조회합니다.
     *
     * 회원 정보와 카테고리 정보를 함께 가져오기 때문에
     * 시스템 관리자 화면에서 신청 내용을 바로 표시할 수 있습니다.
     */
    public List<CategoryManagerRequestDTO> findPendingRequests() {

        List<CategoryManagerRequestDTO> list = new ArrayList<>();

        String sql = """
                SELECT
                    r.REQUEST_ID,
                    r.MEMBER_NO,
                    m.USERNAME,
                    m.NICKNAME,
                    NVL(
                        (
                            SELECT SUM(p.LIKE_COUNT)
                            FROM POST p
                            WHERE p.MEMBER_NO = m.MEMBER_NO
                              AND p.IS_DELETED = 'N'
                        ),
                        0
                    ) AS RECEIVED_LIKES,
                    r.CATEGORY_ID,
                    c.CATEGORY_NAME,
                    r.REQUEST_STATUS,
                    TO_CHAR(
                        r.REQUESTED_AT,
                        'YYYY-MM-DD HH24:MI:SS'
                    ) AS REQUESTED_AT
                FROM CATEGORY_MANAGER_REQUEST r
                JOIN MEMBER m
                    ON m.MEMBER_NO = r.MEMBER_NO
                JOIN CATEGORY c
                    ON c.CATEGORY_ID = r.CATEGORY_ID
                WHERE r.REQUEST_STATUS = 'PENDING'
                ORDER BY r.REQUESTED_AT, r.REQUEST_ID
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {

                CategoryManagerRequestDTO dto = new CategoryManagerRequestDTO();

                // DB에서 조회한 값을 DTO에 하나씩 넣습니다.
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

    // =========================================================
    // 관리자 권한 신청 승인
    // =========================================================

    /**
     * 시스템 관리자가 신청을 승인합니다.
     *
     * 승인 과정은 하나의 트랜잭션으로 처리합니다.
     * 1. 신청 정보 확인
     * 2. CATEGORY_MANAGER_ROLE 권한 부여
     * 3. CATEGORY_MANAGER 테이블에 관리 카테고리 등록
     * 4. 신청 상태를 APPROVED로 변경
     *
     * 중간에 하나라도 실패하면 전체 작업을 롤백합니다.
     */
    public boolean approveRequest(long requestId) {

        // 승인할 신청의 회원 번호와 카테고리 번호를 가져옵니다.
        String selectSql = """
                SELECT MEMBER_NO, CATEGORY_ID
                FROM CATEGORY_MANAGER_REQUEST
                WHERE REQUEST_ID = ?
                  AND REQUEST_STATUS = 'PENDING'
                FOR UPDATE
                """;

        // CATEGORY_MANAGER 역할이 없다면 회원에게 역할을 추가합니다.
        String roleSql = """
                INSERT INTO MEMBER_ROLE (
                    MEMBER_NO,
                    ROLE_ID,
                    GRANTED_AT
                )
                SELECT ?, ROLE_ID, SYSDATE
                FROM ROLE
                WHERE ROLE_NAME = 'CATEGORY_MANAGER'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM MEMBER_ROLE
                      WHERE MEMBER_NO = ?
                        AND ROLE_ID = ROLE.ROLE_ID
                  )
                """;

        // 회원과 관리 카테고리의 연결 정보를 등록합니다.
        String managerSql = """
                INSERT INTO CATEGORY_MANAGER (
                    MEMBER_NO,
                    CATEGORY_ID
                )
                SELECT ?, ?
                FROM DUAL
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM CATEGORY_MANAGER
                    WHERE MEMBER_NO = ?
                      AND CATEGORY_ID = ?
                )
                """;

        // 마지막으로 신청 상태를 승인으로 변경합니다.
        String requestSql = """
                UPDATE CATEGORY_MANAGER_REQUEST
                SET REQUEST_STATUS = 'APPROVED',
                    PROCESSED_AT = SYSDATE
                WHERE REQUEST_ID = ?
                  AND REQUEST_STATUS = 'PENDING'
                """;

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();

            // 승인 과정 전체를 하나의 작업으로 묶습니다.
            conn.setAutoCommit(false);

            long memberNo;
            long categoryId;

            // -------------------------------------------------
            // 1. 승인할 신청 정보 조회
            // -------------------------------------------------
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setLong(1, requestId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }

                    memberNo = rs.getLong("MEMBER_NO");
                    categoryId = rs.getLong("CATEGORY_ID");
                }
            }

            // -------------------------------------------------
            // 2. CATEGORY_MANAGER 역할 부여
            // -------------------------------------------------
            try (PreparedStatement pstmt = conn.prepareStatement(roleSql)) {
                pstmt.setLong(1, memberNo);
                pstmt.setLong(2, memberNo);
                pstmt.executeUpdate();
            }

            // -------------------------------------------------
            // 3. 관리 카테고리 등록
            // -------------------------------------------------
            try (PreparedStatement pstmt = conn.prepareStatement(managerSql)) {
                pstmt.setLong(1, memberNo);
                pstmt.setLong(2, categoryId);
                pstmt.setLong(3, memberNo);
                pstmt.setLong(4, categoryId);
                pstmt.executeUpdate();
            }

            // -------------------------------------------------
            // 4. 신청 상태를 APPROVED로 변경
            // -------------------------------------------------
            try (PreparedStatement pstmt = conn.prepareStatement(requestSql)) {
                pstmt.setLong(1, requestId);

                if (pstmt.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
            }

            // 모든 작업이 성공했으므로 실제 DB에 반영합니다.
            conn.commit();
            return true;

        } catch (Exception e) {
            // 하나라도 실패하면 지금까지의 작업을 전부 취소합니다.
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ignored) {
                    // 롤백 중 발생한 오류는 무시합니다.
                }
            }

            e.printStackTrace();
            return false;

        } finally {
            // DB 연결을 반드시 닫습니다.
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {
                    // 연결 종료 중 오류는 무시합니다.
                }
            }
        }
    }

    // =========================================================
    // 관리자 권한 신청 거절
    // =========================================================

    /**
     * 시스템 관리자가 대기 중인 신청을 거절합니다.
     *
     * @param requestId 거절할 신청 번호
     * @return 정상적으로 거절되면 true
     */
    public boolean rejectRequest(long requestId) {

        String sql = """
                UPDATE CATEGORY_MANAGER_REQUEST
                SET REQUEST_STATUS = 'REJECTED',
                    PROCESSED_AT = SYSDATE
                WHERE REQUEST_ID = ?
                  AND REQUEST_STATUS = 'PENDING'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, requestId);
            return pstmt.executeUpdate() == 1;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
