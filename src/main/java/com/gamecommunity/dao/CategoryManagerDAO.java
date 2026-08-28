package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// 카테고리 관리자 권한(게시탭 생성/게시글 관리)
public class CategoryManagerDAO {

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
            pstmt.setLong(1, memberNo);
            pstmt.setLong(2, categoryId);
            pstmt.setLong(3, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.out.println("관리자 권한 검증 중 오류 발생!");
            e.printStackTrace();
        }
        return false;
    }

    public long getManagedCategoryId(long memberNo) {
        String sql = "SELECT CATEGORY_ID FROM CATEGORY_MANAGER WHERE MEMBER_NO = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getLong("CATEGORY_ID");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}