package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;
import com.gamecommunity.dto.MemberDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MemberDAO {

    public MemberDTO findByUsername(String username) {

        String sql = """
                SELECT MEMBER_NO,
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
                    member.setUserLevel(rs.getInt("USER_LEVEL"));
                    member.setJoinStatus(rs.getString("JOIN_STATUS"));
                    member.setNickname(rs.getString("NICKNAME"));
                    member.setAccountStatus(rs.getString("ACCOUNT_STATUS"));
                    return member;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 이름과 전화번호가 일치하는 회원의 아이디를 조회한다.
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

    // 아이디, 이름, 전화번호가 모두 일치하는 회원인지 확인한다.
    public boolean existsByUsernameNameAndPhone(String username, String name, String phone) {

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

    // 비밀번호를 변경한다.
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

    // 회원과 USER 권한을 하나의 트랜잭션으로 생성한다.
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
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(memberSql)) {
                pstmt.setString(1, member.getName());
                pstmt.setString(2, member.getUsername());
                pstmt.setString(3, member.getPassword());
                pstmt.setString(4, member.getPhone());
                pstmt.setString(5, member.getNickname());

                int result = pstmt.executeUpdate();
                if (result == 0) {
                    conn.rollback();
                    return 0;
                }
            }

            long memberNo;
            try (PreparedStatement pstmt = conn.prepareStatement(memberNoSql)) {
                pstmt.setString(1, member.getUsername());

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        memberNo = rs.getLong("MEMBER_NO");
                    } else {
                        conn.rollback();
                        return 0;
                    }
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(roleSql)) {
                pstmt.setLong(1, memberNo);

                int roleResult = pstmt.executeUpdate();
                if (roleResult == 0) {
                    conn.rollback();
                    return 0;
                }
            }

            conn.commit();
            return 1;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception rollbackException) {
                    rollbackException.printStackTrace();
                }
            }
            e.printStackTrace();
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

    public boolean existsByUsername(String username) {
        String sql = """
            SELECT COUNT(*)
            FROM MEMBER
            WHERE USERNAME = ?
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean existsByNickname(String nickname) {
        String sql = """
            SELECT COUNT(*)
            FROM MEMBER
            WHERE NICKNAME = ?
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean existsByNicknameExceptMember(String nickname, long memberNo) {
        String sql = """
        SELECT COUNT(*)
        FROM MEMBER
        WHERE NICKNAME = ?
          AND MEMBER_NO <> ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            pstmt.setLong(2, memberNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getReceivedLikeCount(long memberNo) {
        String sql = """
                SELECT NVL(SUM(LIKE_COUNT), 0)
                FROM POST
                WHERE MEMBER_NO = ?
                  AND IS_DELETED = 'N'
                """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getReceivedDislikeCount(long memberNo) {
        String sql = """
                SELECT NVL(SUM(DISLIKE_COUNT), 0)
                FROM POST
                WHERE MEMBER_NO = ?
                  AND IS_DELETED = 'N'
                """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

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

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
    public boolean withdrawMember(long memberNo) {
        // 회원 탈퇴 및 아이디/닉네임/개인정보 Unknown 처리
        String sql = """
            UPDATE MEMBER
            SET ACCOUNT_STATUS = 'WITHDRAWN',
                USERNAME       = 'Unknown_' || MEMBER_NO,
                NICKNAME       = 'Unknown',
                NAME           = 'Unknown',
                PHONE          = '00000000000',
                PASSWORD       = 'UNKNOWN_ACCOUNT_DISABLED',
                UPDATED_AT     = SYSDATE
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

    // 시스템 관리자의 권한 확인 메소드(게시판 승인, 삭제 기능 구현간 추가)
    public boolean isSystemManager(long memberNo) {
        String sql = """
        SELECT COUNT(*)
        FROM MEMBER_ROLE MR
        JOIN ROLE R ON MR.ROLE_ID = R.ROLE_ID
        WHERE MR.MEMBER_NO = ?
          AND R.ROLE_NAME = 'SYS_MANAGER'
        """;
        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, memberNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
