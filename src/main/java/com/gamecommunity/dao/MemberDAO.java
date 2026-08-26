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
                       JOIN_STATUS
                       
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

                    return member;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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

            // 1. 회원 생성
            try (PreparedStatement pstmt =
                         conn.prepareStatement(memberSql)) {

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

            // 2. 방금 가입한 회원의 MEMBER_NO 조회
            long memberNo;

            try (PreparedStatement pstmt =
                         conn.prepareStatement(memberNoSql)) {

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

            // 3. USER 권한 부여
            try (PreparedStatement pstmt =
                         conn.prepareStatement(roleSql)) {

                pstmt.setLong(1, memberNo);

                int roleResult = pstmt.executeUpdate();
                if (roleResult == 0) {
                    conn.rollback();
                    return 0;
                }
            }

            // 모두 성공
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

    // 회원이 작성한 게시글이 받은 좋아요 총 개수
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


    // 회원이 작성한 게시글이 받은 나빠요 총 개수
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

    // 회원정보 수정 시 로그인 아이디(USERNAME)는 변경하지 않는다.
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
}
