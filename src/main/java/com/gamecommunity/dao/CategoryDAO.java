package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;
import com.gamecommunity.dto.CategoryDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

            System.out.println("========== CATEGORY findAll ==========");
            printDatabaseInfo(conn);

            while (rs.next()) {

                CategoryDTO category = new CategoryDTO();

                setCategoryData(rs, category);

                categoryList.add(category);

                System.out.println(
                        "카테고리: "
                                + category.getCategoryId()
                                + " / "
                                + category.getCategoryName()
                                + " / DEPTH="
                                + category.getDepth()
                );
            }

            System.out.println(
                    "전체 카테고리 개수 = "
                            + categoryList.size()
            );

        } catch (Exception e) {

            System.out.println("CATEGORY 전체 조회 오류");
            e.printStackTrace();
        }

        return categoryList;
    }


    // =========================================================
    // DEPTH로 카테고리 조회
    //
    // /categories?depth=2
    // =========================================================

    public List<CategoryDTO> findByDepth(int depth) {
        System.err.println("🔥🔥🔥 CATEGORY DAO 진입 🔥🔥🔥");
        System.err.println("depth = " + depth);
        List<CategoryDTO> categoryList = new ArrayList<>();

        System.out.println();
        System.out.println("========================================");
        System.out.println("========== CATEGORY findByDepth ==========");
        System.out.println("요청 depth = " + depth);
        System.out.println("========================================");


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
            ORDER BY CATEGORY_ID
            """;


        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            // =====================================================
            // 1. 현재 연결된 DB 확인
            // =====================================================

            printDatabaseInfo(conn);


            // =====================================================
            // 2. CATEGORY 전체 개수 확인
            // =====================================================

            String countSql = """
                SELECT COUNT(*)
                FROM CATEGORY
                """;

            try (
                    PreparedStatement countPstmt =
                            conn.prepareStatement(countSql);
                    ResultSet countRs =
                            countPstmt.executeQuery()
            ) {

                if (countRs.next()) {

                    System.out.println(
                            "CATEGORY 전체 개수 = "
                                    + countRs.getInt(1)
                    );
                }
            }


            // =====================================================
            // 3. DEPTH별 개수 확인
            // =====================================================

            String depthCountSql = """
                SELECT DEPTH, COUNT(*) AS CNT
                FROM CATEGORY
                GROUP BY DEPTH
                ORDER BY DEPTH
                """;

            try (
                    PreparedStatement depthPstmt =
                            conn.prepareStatement(depthCountSql);
                    ResultSet depthRs =
                            depthPstmt.executeQuery()
            ) {

                System.out.println("========== DEPTH별 개수 ==========");

                while (depthRs.next()) {

                    System.out.println(
                            "DEPTH = "
                                    + depthRs.getInt("DEPTH")
                                    + " / 개수 = "
                                    + depthRs.getInt("CNT")
                    );
                }
            }


            // =====================================================
            // 4. 실제 DEPTH 조회
            // =====================================================

            pstmt.setInt(1, depth);

            System.out.println();
            System.out.println(
                    "실제 CATEGORY 조회 시작"
            );

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    long categoryId =
                            rs.getLong("CATEGORY_ID");

                    long parentId =
                            rs.getLong("PARENT_ID");

                    String categoryName =
                            rs.getString("CATEGORY_NAME");

                    int categoryDepth =
                            rs.getInt("DEPTH");

                    String isActive =
                            rs.getString("IS_ACTIVE");

                    String iconUrl =
                            rs.getString("ICON_URL");

                    int sortOrder =
                            rs.getInt("SORT_ORDER");


                    System.out.println(
                            "조회됨 -> "
                                    + "ID=" + categoryId
                                    + ", NAME=" + categoryName
                                    + ", DEPTH=" + categoryDepth
                                    + ", PARENT=" + parentId
                                    + ", ACTIVE=" + isActive
                                    + ", ICON=" + iconUrl
                                    + ", SORT=" + sortOrder
                    );


                    CategoryDTO category =
                            new CategoryDTO();


                    category.setCategoryId(
                            categoryId
                    );


                    if (rs.wasNull()) {
                        category.setParentId(null);
                    } else {
                        category.setParentId(parentId);
                    }


                    category.setCategoryName(
                            categoryName
                    );


                    category.setDepth(
                            categoryDepth
                    );


                    category.setIsActive(
                            isActive
                    );


                    category.setCreatedAt(
                            rs.getString("CREATED_AT")
                    );


                    category.setIconUrl(
                            iconUrl
                    );


                    category.setSortOrder(
                            sortOrder
                    );


                    categoryList.add(category);
                }
            }


            // =====================================================
            // 5. 최종 결과
            // =====================================================

            System.out.println();
            System.out.println(
                    "DEPTH " + depth
                            + " 조회 결과 = "
                            + categoryList.size()
            );

            System.out.println("========================================");
            System.out.println();


        } catch (Exception e) {

            System.out.println();
            System.out.println("========== CATEGORY DAO ERROR ==========");

            e.printStackTrace();
        }


        return categoryList;
    }


    // =========================================================
    // 부모 카테고리 ID로 자식 카테고리 조회
    //
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
                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
        ) {

            System.out.println(
                    "========== CATEGORY findByParentId =========="
            );

            printDatabaseInfo(conn);

            System.out.println(
                    "요청 parentId = " + parentId
            );


            pstmt.setLong(1, parentId);


            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    CategoryDTO category =
                            new CategoryDTO();

                    setCategoryData(rs, category);

                    categoryList.add(category);


                    System.out.println(
                            "찾음: "
                                    + category.getCategoryId()
                                    + " / "
                                    + category.getCategoryName()
                    );
                }
            }


            System.out.println(
                    "parentId "
                            + parentId
                            + " 결과 개수 = "
                            + categoryList.size()
            );


        } catch (Exception e) {

            System.out.println(
                    "CATEGORY 부모 조회 오류"
            );

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
                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
        ) {

            System.out.println(
                    "========== CATEGORY findById =========="
            );

            printDatabaseInfo(conn);

            System.out.println(
                    "요청 categoryId = "
                            + categoryId
            );


            pstmt.setLong(1, categoryId);


            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {

                    CategoryDTO category =
                            new CategoryDTO();

                    setCategoryData(
                            rs,
                            category
                    );


                    System.out.println(
                            "카테고리 찾음: "
                                    + category.getCategoryName()
                    );


                    return category;
                }
            }


        } catch (Exception e) {

            System.out.println(
                    "CATEGORY ID 조회 오류"
            );

            e.printStackTrace();
        }


        System.out.println(
                "카테고리를 찾지 못했습니다."
        );


        return null;
    }


    // =========================================================
    // 현재 DB 정보 출력
    // =========================================================

    private void printDatabaseInfo(
            Connection conn
    ) throws SQLException {

        System.out.println(
                "========== 현재 DB 정보 =========="
        );

        System.out.println(
                "JDBC URL = "
                        + conn.getMetaData().getURL()
        );

        System.out.println(
                "JDBC USER = "
                        + conn.getMetaData().getUserName()
        );


        String sql = """
            SELECT
                SYS_CONTEXT('USERENV', 'DB_NAME') AS DB_NAME,
                SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS SERVICE_NAME,
                SYS_CONTEXT('USERENV', 'INSTANCE_NAME') AS INSTANCE_NAME,
                USER AS USERNAME
            FROM DUAL
            """;


        try (
                PreparedStatement pstmt =
                        conn.prepareStatement(sql);
                ResultSet rs =
                        pstmt.executeQuery()
        ) {

            if (rs.next()) {

                System.out.println(
                        "DB NAME = "
                                + rs.getString("DB_NAME")
                );

                System.out.println(
                        "SERVICE = "
                                + rs.getString("SERVICE_NAME")
                );

                System.out.println(
                        "INSTANCE = "
                                + rs.getString("INSTANCE_NAME")
                );

                System.out.println(
                        "USER = "
                                + rs.getString("USERNAME")
                );
            }
        }


        System.out.println(
                "================================="
        );
    }


    // =========================================================
    // ResultSet → CategoryDTO
    // =========================================================

    private void setCategoryData(
            ResultSet rs,
            CategoryDTO category
    ) throws SQLException {


        // CATEGORY_ID
        category.setCategoryId(
                rs.getLong("CATEGORY_ID")
        );


        // PARENT_ID
        long parentId =
                rs.getLong("PARENT_ID");


        if (rs.wasNull()) {

            category.setParentId(null);

        } else {

            category.setParentId(parentId);
        }


        // CATEGORY_NAME
        category.setCategoryName(
                rs.getString("CATEGORY_NAME")
        );


        // DEPTH
        category.setDepth(
                rs.getInt("DEPTH")
        );


        // IS_ACTIVE
        category.setIsActive(
                rs.getString("IS_ACTIVE")
        );


        // CREATED_AT
        category.setCreatedAt(
                rs.getString("CREATED_AT")
        );


        // ICON_URL
        category.setIconUrl(
                rs.getString("ICON_URL")
        );


        // SORT_ORDER
        category.setSortOrder(
                rs.getInt("SORT_ORDER")
        );
    }
}