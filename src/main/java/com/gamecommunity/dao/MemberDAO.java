package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;
import com.gamecommunity.dto.MemberDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 회원과 관련된 DB 작업을 담당하는 DAO입니다.
 *
 * DAO(Data Access Object)는 직접 SQL을 실행하는 역할을 담당합니다.
 */
public class MemberDAO {

    // =========================================================
    // 로그인용 회원 조회
    // =========================================================

    /**
     * 아이디로 회원 정보를 조회합니다.
     * 로그인할 때 주로 사용합니다.
     *
     * @param username 로그인 아이디
     * @return 회원 정보, 회원이 없으면 null
     */
    public MemberDTO findByUsername(String username) {

        String sql = """
                SELECT
                    MEMBER_NO,
                    NAME,
                    USERNAME,
                    PASSWORD,
                    PHONE,
                    NICKNAME,
                    USER_LEVEL,
                    JOIN_STATUS,
                    ACCOUNT_STATUS
                FROM MEMBER
                WHERE USERNAME = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    MemberDTO member = new MemberDTO();

                    member.setMemberNo(rs.getLong("MEMBER_NO"));
                    member.setName(rs.getString("NAME"));
                    member.setUsername(rs.getString("USERNAME"));
                    member.setPassword(rs.getString("PASSWORD"));
                    member.setPhone(rs.getString("PHONE"));
                    member.setNickname(rs.getString("NICKNAME"));
                    member.setUserLevel(rs.getInt("USER_LEVEL"));
                    member.setJoinStatus(rs.getString("JOIN_STATUS"));
                    member.setAccountStatus(rs.getString("ACCOUNT_STATUS"));

                    return member;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // 아이디 찾기
    // =========================================================

    /**
     * 이름과 전화번호가 일치하는 회원의 아이디를 조회합니다.
     */
    public String findUsernameByNameAndPhone(String name, String phone) {

        String sql = """
                SELECT USERNAME
                FROM MEMBER
                WHERE NAME = ?
                  AND PHONE = ?
                  AND JOIN_STATUS = 'APPROVED'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("USERNAME");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // 비밀번호 찾기 대상 확인
    // =========================================================

    /**
     * 아이디, 이름, 전화번호가 모두 일치하는 회원인지 확인합니다.
     */
    public boolean existsByUsernameNameAndPhone(
            String username,
            String name,
            String phone
    ) {

        String sql = """
                SELECT COUNT(*)
                FROM MEMBER
                WHERE USERNAME = ?
                  AND NAME = ?
                  AND PHONE = ?
                  AND JOIN_STATUS = 'APPROVED'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, username);
            pstmt.setString(2, name);
            pstmt.setString(3, phone);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 비밀번호 변경
    // =========================================================

    /**
     * 회원의 비밀번호를 변경합니다.
     */
    public boolean updatePassword(String username, String newPassword) {

        String sql = """
                UPDATE MEMBER
                SET PASSWORD = ?,
                    UPDATED_AT = SYSDATE
                WHERE USERNAME = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, username);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 회원 가입
    // =========================================================

    /**
     * 회원 정보와 기본 USER 권한을 하나의 트랜잭션으로 생성합니다.
     *
     * 회원 생성과 권한 생성 중 하나라도 실패하면 전체 작업을 취소합니다.
     */
    public int insertMember(MemberDTO member) {

        String memberSql = """
                INSERT INTO MEMBER (
                    NAME,
                    USERNAME,
                    PASSWORD,
                    PHONE,
                    NICKNAME
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        String memberNoSql = """
                SELECT MEMBER_NO
                FROM MEMBER
                WHERE USERNAME = ?
                """;

        String roleSql = """
                INSERT INTO MEMBER_ROLE (
                    MEMBER_NO,
                    ROLE_ID,
                    GRANTED_AT
                )
                SELECT ?, ROLE_ID, SYSDATE
                FROM ROLE
                WHERE ROLE_NAME = 'USER'
                """;

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();

            // 회원 생성 + 기본 권한 등록을 하나의 작업으로 처리합니다.
            conn.setAutoCommit(false);

            // -------------------------------------------------
            // 1. MEMBER 테이블에 회원 정보 저장
            // -------------------------------------------------
            try (PreparedStatement pstmt = conn.prepareStatement(memberSql)) {
                pstmt.setString(1, member.getName());
                pstmt.setString(2, member.getUsername());
                pstmt.setString(3, member.getPassword());
                pstmt.setString(4, member.getPhone());
                pstmt.setString(5, member.getNickname());

                if (pstmt.executeUpdate() == 0) {
                    conn.rollback();
                    return 0;
                }
            }

            // -------------------------------------------------
            // 2. 방금 생성한 회원 번호 조회
            // -------------------------------------------------
            long memberNo;

            try (PreparedStatement pstmt = conn.prepareStatement(memberNoSql)) {
                pstmt.setString(1, member.getUsername());

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return 0;
                    }

                    memberNo = rs.getLong("MEMBER_NO");
                }
            }

            // -------------------------------------------------
            // 3. 기본 USER 권한 등록
            // -------------------------------------------------
            try (PreparedStatement pstmt = conn.prepareStatement(roleSql)) {
                pstmt.setLong(1, memberNo);

                if (pstmt.executeUpdate() == 0) {
                    conn.rollback();
                    return 0;
                }
            }

            // 모든 작업이 성공했으므로 실제 DB에 반영합니다.
            conn.commit();
            return 1;

        } catch (Exception e) {
            e.printStackTrace();

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception rollbackException) {
                    rollbackException.printStackTrace();
                }
            }

            return 0;

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // =========================================================
    // 아이디 중복 확인
    // =========================================================

    /**
     * 같은 아이디를 가진 회원이 있는지 확인합니다.
     */
    public boolean existsByUsername(String username) {

        String sql = """
                SELECT COUNT(*)
                FROM MEMBER
                WHERE USERNAME = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 닉네임 중복 확인
    // =========================================================

    /**
     * 같은 닉네임을 가진 회원이 있는지 확인합니다.
     */
    public boolean existsByNickname(String nickname) {

        String sql = """
                SELECT COUNT(*)
                FROM MEMBER
                WHERE NICKNAME = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, nickname);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 회원 수정 시 닉네임 중복 확인
    // =========================================================

    /**
     * 현재 수정 중인 회원 본인을 제외하고 닉네임 중복 여부를 확인합니다.
     */
    public boolean existsByNicknameExceptMember(
            String nickname,
            long memberNo
    ) {

        String sql = """
                SELECT COUNT(*)
                FROM MEMBER
                WHERE NICKNAME = ?
                  AND MEMBER_NO <> ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, nickname);
            pstmt.setLong(2, memberNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 회원이 받은 좋아요 개수
    // =========================================================

    /**
     * 회원이 작성한 게시글에 받은 좋아요의 총합을 조회합니다.
     */
    public int getReceivedLikeCount(long memberNo) {

        String sql = """
                SELECT NVL(SUM(LIKE_COUNT), 0)
                FROM POST
                WHERE MEMBER_NO = ?
                  AND IS_DELETED = 'N'
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
     * 회원이 작성한 게시글에 받은 싫어요의 총합을 조회합니다.
     */
    public int getReceivedDislikeCount(long memberNo) {

        String sql = """
                SELECT NVL(SUM(DISLIKE_COUNT), 0)
                FROM POST
                WHERE MEMBER_NO = ?
                  AND IS_DELETED = 'N'
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
    // 회원 정보 수정
    // =========================================================

    /**
     * 회원의 이름, 비밀번호, 닉네임, 전화번호를 수정합니다.
     */
    public boolean updateMember(MemberDTO member) {

        String sql = """
                UPDATE MEMBER
                SET NAME = ?,
                    PASSWORD = ?,
                    NICKNAME = ?,
                    PHONE = ?,
                    UPDATED_AT = SYSDATE
                WHERE MEMBER_NO = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, member.getName());
            pstmt.setString(2, member.getPassword());
            pstmt.setString(3, member.getNickname());
            pstmt.setString(4, member.getPhone());
            pstmt.setLong(5, member.getMemberNo());

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 회원 탈퇴
    // =========================================================

    /**
     * 회원 탈퇴를 처리합니다.
     *
     * 실제 회원 데이터를 삭제하지 않고 탈퇴 상태로 변경합니다.
     * 개인정보는 Unknown 값으로 변경합니다.
     */
    public boolean withdrawMember(long memberNo) {

        String sql = """
                UPDATE MEMBER
                SET ACCOUNT_STATUS = 'WITHDRAWN',
                    USERNAME = 'Unknown_' || MEMBER_NO,
                    NICKNAME = 'Unknown',
                    NAME = 'Unknown',
                    PHONE = '00000000000',
                    PASSWORD = 'UNKNOWN_ACCOUNT_DISABLED',
                    UPDATED_AT = SYSDATE
                WHERE MEMBER_NO = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, memberNo);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 시스템 관리자 권한 확인
    // =========================================================

    /**
     * 해당 회원이 시스템 관리자인지 확인합니다.
     * 게시판 승인/거절, 관리자 기능 등에서 사용합니다.
     */
    public boolean isSystemManager(long memberNo) {

        String sql = """
                SELECT COUNT(*)
                FROM MEMBER_ROLE MR
                JOIN ROLE R
                    ON MR.ROLE_ID = R.ROLE_ID
                WHERE MR.MEMBER_NO = ?
                  AND R.ROLE_NAME = 'SYS_MANAGER'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, memberNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
