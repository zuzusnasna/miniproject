package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 카테고리 관리자와 관련된 DB 작업을 담당하는 DAO입니다.
 *
 * 주요 역할
 * 1. 특정 회원이 특정 카테고리의 관리자인지 확인
 * 2. 회원이 관리하고 있는 카테고리 ID 조회
 */
public class CategoryManagerDAO {

    // =========================================================
    // 카테고리 관리자 권한 확인
    // =========================================================

    /**
     * 회원이 해당 카테고리의 관리자인지 확인합니다.
     *
     * 부모 카테고리의 관리자라면 그 하위 게시판에서도 관리자 권한을
     * 사용할 수 있도록 부모 카테고리까지 함께 확인합니다.
     *
     * @param memberNo 확인할 회원 번호
     * @param categoryId 확인할 카테고리 번호
     * @return 관리자이면 true, 아니면 false
     */
    public boolean isManagerOfCategory(long memberNo, long categoryId) {

        String sql = """
                SELECT COUNT(*)
                FROM CATEGORY_MANAGER cm
                WHERE cm.MEMBER_NO = ?
                  AND (
                        cm.CATEGORY_ID = ?
                        OR cm.CATEGORY_ID = (
                            SELECT c.PARENT_ID
                            FROM CATEGORY c
                            WHERE c.CATEGORY_ID = ?
                        )
                      )
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            // 첫 번째 ? : 로그인한 회원 번호
            pstmt.setLong(1, memberNo);

            // 두 번째 ? : 현재 카테고리 번호
            pstmt.setLong(2, categoryId);

            // 세 번째 ? : 현재 카테고리의 부모 카테고리를 찾기 위한 번호
            pstmt.setLong(3, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (Exception e) {
            System.out.println("관리자 권한 검증 중 오류 발생!");
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 회원이 관리하는 카테고리 조회
    // =========================================================

    /**
     * 해당 회원이 관리하고 있는 카테고리 ID를 조회합니다.
     *
     * @param memberNo 회원 번호
     * @return 관리 중인 카테고리 ID, 없으면 -1
     */
    public long getManagedCategoryId(long memberNo) {

        String sql = """
                SELECT CATEGORY_ID
                FROM CATEGORY_MANAGER
                WHERE MEMBER_NO = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            // 조회할 회원 번호를 SQL의 ?에 전달합니다.
            pstmt.setLong(1, memberNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("CATEGORY_ID");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 관리하는 카테고리가 없거나 조회 중 오류가 발생한 경우
        return -1;
    }
}
