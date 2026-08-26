package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;
import com.gamecommunity.dto.CategoryDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {


    // =========================================================
    // 전체 카테고리 조회
    // =========================================================

    public List<CategoryDTO> findAll() {

        List<CategoryDTO> categoryList = new ArrayList<>();

        String sql = """
            SELECT
                CATEGORY_ID,
                PARENT_ID,
                CATEGORY_NAME,
                DEPTH,
                IS_ACTIVE,
                TO_CHAR(CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT,
                ICON_URL,
                SORT_ORDER
            FROM CATEGORY
            WHERE IS_ACTIVE = 'Y'
            ORDER BY DEPTH, SORT_ORDER, CATEGORY_ID
            """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {

            while (rs.next()) {

                CategoryDTO category = new CategoryDTO();

                category.setCategoryId(
                        rs.getLong("CATEGORY_ID")
                );

                long parentId = rs.getLong("PARENT_ID");

                if (rs.wasNull()) {
                    category.setParentId(null);
                } else {
                    category.setParentId(parentId);
                }

                category.setCategoryName(
                        rs.getString("CATEGORY_NAME")
                );

                category.setDepth(
                        rs.getInt("DEPTH")
                );

                category.setIsActive(
                        rs.getString("IS_ACTIVE")
                );

                category.setCreatedAt(
                        rs.getString("CREATED_AT")
                );

                category.setIconUrl(
                        rs.getString("ICON_URL")
                );

                category.setSortOrder(
                        rs.getInt("SORT_ORDER")
                );

                categoryList.add(category);
            }

        } catch (Exception e) {

            System.out.println("CATEGORY 전체 조회 오류");
            e.printStackTrace();
        }

        return categoryList;
    }


    // =========================================================
    // DEPTH로 카테고리 조회
    // /categories?depth=2
    // =========================================================

    public List<CategoryDTO> findByDepth(int depth) {

        List<CategoryDTO> categoryList = new ArrayList<>();

        String sql = """
            SELECT
                CATEGORY_ID,
                PARENT_ID,
                CATEGORY_NAME,
                DEPTH,
                IS_ACTIVE,
                TO_CHAR(CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT,
                ICON_URL,
                SORT_ORDER
            FROM CATEGORY
            WHERE DEPTH = ?
              AND IS_ACTIVE = 'Y'
            ORDER BY PARENT_ID, SORT_ORDER, CATEGORY_ID
            """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, depth);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    CategoryDTO category = new CategoryDTO();

                    category.setCategoryId(
                            rs.getLong("CATEGORY_ID")
                    );

                    long parentId = rs.getLong("PARENT_ID");

                    if (rs.wasNull()) {
                        category.setParentId(null);
                    } else {
                        category.setParentId(parentId);
                    }

                    category.setCategoryName(
                            rs.getString("CATEGORY_NAME")
                    );

                    category.setDepth(
                            rs.getInt("DEPTH")
                    );

                    category.setIsActive(
                            rs.getString("IS_ACTIVE")
                    );

                    category.setCreatedAt(
                            rs.getString("CREATED_AT")
                    );

                    category.setIconUrl(
                            rs.getString("ICON_URL")
                    );

                    category.setSortOrder(
                            rs.getInt("SORT_ORDER")
                    );

                    categoryList.add(category);
                }
            }

        } catch (Exception e) {

            System.out.println("CATEGORY DEPTH 조회 오류");
            e.printStackTrace();
        }

        return categoryList;
    }


    // =========================================================
    // 부모 카테고리 ID로 자식 카테고리 조회
    // /categories?parentId=3
    // =========================================================

    public List<CategoryDTO> findByParentId(Long parentId) {

        List<CategoryDTO> categoryList = new ArrayList<>();

        String sql = """
            SELECT
                CATEGORY_ID,
                PARENT_ID,
                CATEGORY_NAME,
                DEPTH,
                IS_ACTIVE,
                TO_CHAR(CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT,
                ICON_URL,
                SORT_ORDER
            FROM CATEGORY
            WHERE PARENT_ID = ?
              AND IS_ACTIVE = 'Y'
            ORDER BY SORT_ORDER, CATEGORY_ID
            """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setLong(1, parentId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    CategoryDTO category = new CategoryDTO();

                    category.setCategoryId(
                            rs.getLong("CATEGORY_ID")
                    );

                    category.setParentId(
                            rs.getLong("PARENT_ID")
                    );

                    category.setCategoryName(
                            rs.getString("CATEGORY_NAME")
                    );

                    category.setDepth(
                            rs.getInt("DEPTH")
                    );

                    category.setIsActive(
                            rs.getString("IS_ACTIVE")
                    );

                    category.setCreatedAt(
                            rs.getString("CREATED_AT")
                    );

                    category.setIconUrl(
                            rs.getString("ICON_URL")
                    );

                    category.setSortOrder(
                            rs.getInt("SORT_ORDER")
                    );

                    categoryList.add(category);
                }
            }

        } catch (Exception e) {

            System.out.println("CATEGORY 부모 조회 오류");
            e.printStackTrace();
        }

        return categoryList;
    }


    // =========================================================
    // CATEGORY_ID로 카테고리 1개 조회
    // =========================================================

    public CategoryDTO findById(Long categoryId) {

        String sql = """
            SELECT
                CATEGORY_ID,
                PARENT_ID,
                CATEGORY_NAME,
                DEPTH,
                IS_ACTIVE,
                TO_CHAR(CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT,
                ICON_URL,
                SORT_ORDER
            FROM CATEGORY
            WHERE CATEGORY_ID = ?
              AND IS_ACTIVE = 'Y'
            """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setLong(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {

                    CategoryDTO category = new CategoryDTO();

                    category.setCategoryId(
                            rs.getLong("CATEGORY_ID")
                    );

                    long parentId = rs.getLong("PARENT_ID");

                    if (rs.wasNull()) {
                        category.setParentId(null);
                    } else {
                        category.setParentId(parentId);
                    }

                    category.setCategoryName(
                            rs.getString("CATEGORY_NAME")
                    );

                    category.setDepth(
                            rs.getInt("DEPTH")
                    );

                    category.setIsActive(
                            rs.getString("IS_ACTIVE")
                    );

                    category.setCreatedAt(
                            rs.getString("CREATED_AT")
                    );

                    category.setIconUrl(
                            rs.getString("ICON_URL")
                    );

                    category.setSortOrder(
                            rs.getInt("SORT_ORDER")
                    );

                    return category;
                }
            }

        } catch (Exception e) {

            System.out.println("CATEGORY ID 조회 오류");
            e.printStackTrace();
        }

        return null;
    }
}