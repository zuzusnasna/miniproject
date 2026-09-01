package com.gamecommunity.dao;

import com.gamecommunity.DBUtil;
import com.gamecommunity.dto.CategoryDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CATEGORY 테이블과 관련된 DB 작업을 담당하는 DAO입니다.
 *
 * 카테고리 조회뿐만 아니라 카테고리 관리자가 게시판을 추가 신청하고,
 * 시스템 관리자가 신청한 게시판을 승인/거절하는 작업도 담당합니다.
 */
public class CategoryDAO {

    // =========================================================
    // 카테고리 조회
    // =========================================================

    /**
     * 활성화된 전체 카테고리를 조회합니다.
     *
     * @return 활성 상태인 모든 카테고리 목록
     */
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

            // 조회된 카테고리를 하나씩 DTO에 담습니다.
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

            System.out.println("전체 카테고리 개수 = " + categoryList.size());

        } catch (Exception e) {
            System.out.println("CATEGORY 전체 조회 오류");
            e.printStackTrace();
        }

        return categoryList;
    }

    /**
     * DEPTH 값으로 카테고리를 조회합니다.
     *
     * 예를 들어 /categories?depth=2 요청에서 게임 카테고리를 조회할 때 사용합니다.
     *
     * @param depth 조회할 카테고리 깊이
     * @return 해당 DEPTH의 카테고리 목록
     */
    public List<CategoryDTO> findByDepth(int depth) {

        System.err.println("🔥🔥🔥 CATEGORY DAO 진입 🔥🔥🔥");
        System.err.println("depth = " + depth);

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
                ORDER BY CATEGORY_ID
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            System.out.println("========================================");
            System.out.println("========== CATEGORY findByDepth ==========");
            System.out.println("요청 depth = " + depth);
            System.out.println("========================================");

            // 현재 애플리케이션이 어느 DB에 연결되어 있는지 확인합니다.
            printDatabaseInfo(conn);

            // -------------------------------------------------
            // CATEGORY 전체 개수 확인
            // 디버깅을 위해 현재 테이블의 전체 개수를 출력합니다.
            // -------------------------------------------------
            String countSql = """
                    SELECT COUNT(*)
                    FROM CATEGORY
                    """;

            try (
                    PreparedStatement countPstmt = conn.prepareStatement(countSql);
                    ResultSet countRs = countPstmt.executeQuery()
            ) {
                if (countRs.next()) {
                    System.out.println("CATEGORY 전체 개수 = " + countRs.getInt(1));
                }
            }

            // -------------------------------------------------
            // DEPTH별 카테고리 개수 확인
            // 현재 DB에 어떤 DEPTH의 데이터가 있는지 확인합니다.
            // -------------------------------------------------
            String depthCountSql = """
                    SELECT DEPTH, COUNT(*) AS CNT
                    FROM CATEGORY
                    GROUP BY DEPTH
                    ORDER BY DEPTH
                    """;

            try (
                    PreparedStatement depthPstmt = conn.prepareStatement(depthCountSql);
                    ResultSet depthRs = depthPstmt.executeQuery()
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

            // -------------------------------------------------
            // 실제 DEPTH 조회
            // -------------------------------------------------
            pstmt.setInt(1, depth);

            System.out.println("실제 CATEGORY 조회 시작");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CategoryDTO category = new CategoryDTO();

                    // 조회한 한 행의 데이터를 DTO로 변환합니다.
                    setCategoryData(rs, category);
                    categoryList.add(category);

                    System.out.println(
                            "조회됨 -> "
                                    + "ID=" + category.getCategoryId()
                                    + ", NAME=" + category.getCategoryName()
                                    + ", DEPTH=" + category.getDepth()
                                    + ", PARENT=" + category.getParentId()
                                    + ", ACTIVE=" + category.getIsActive()
                                    + ", ICON=" + category.getIconUrl()
                                    + ", SORT=" + category.getSortOrder()
                    );
                }
            }

            System.out.println("DEPTH " + depth + " 조회 결과 = " + categoryList.size());
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("========== CATEGORY DAO ERROR ==========");
            e.printStackTrace();
        }

        return categoryList;
    }

    /**
     * 부모 카테고리 ID를 기준으로 하위 카테고리를 조회합니다.
     *
     * 예: 게임 카테고리 ID가 3이면 PARENT_ID가 3인 게시판들을 조회합니다.
     *
     * @param parentId 부모 카테고리 번호
     * @return 해당 부모를 가진 활성 카테고리 목록
     */
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
            System.out.println("========== CATEGORY findByParentId ==========");
            printDatabaseInfo(conn);
            System.out.println("요청 parentId = " + parentId);

            pstmt.setLong(1, parentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CategoryDTO category = new CategoryDTO();

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
                    "parentId " + parentId
                            + " 결과 개수 = " + categoryList.size()
            );

        } catch (Exception e) {
            System.out.println("CATEGORY 부모 조회 오류");
            e.printStackTrace();
        }

        return categoryList;
    }

    /**
     * CATEGORY_ID로 카테고리 하나를 조회합니다.
     *
     * @param categoryId 조회할 카테고리 번호
     * @return 조회된 카테고리, 없으면 null
     */
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
            System.out.println("========== CATEGORY findById ==========");
            printDatabaseInfo(conn);
            System.out.println("요청 categoryId = " + categoryId);

            pstmt.setLong(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    CategoryDTO category = new CategoryDTO();

                    setCategoryData(rs, category);

                    System.out.println("카테고리 찾음: " + category.getCategoryName());
                    return category;
                }
            }

        } catch (Exception e) {
            System.out.println("CATEGORY ID 조회 오류");
            e.printStackTrace();
        }

        System.out.println("카테고리를 찾지 못했습니다.");
        return null;
    }

    // =========================================================
    // 카테고리 관리자 게시판 추가 신청
    // =========================================================

    /**
     * 같은 부모 카테고리 아래에 동일한 이름의 게시판이 이미 있는지 확인합니다.
     *
     * @param categoryName 새로 만들 게시판 이름
     * @param parentId 게시판이 속할 부모 게임 카테고리 번호
     * @return 이미 존재하면 true, 없으면 false
     */
    public boolean existsByCategoryNameAndParent(String categoryName, long parentId) {

        String sql = """
                SELECT COUNT(*)
                FROM CATEGORY
                WHERE CATEGORY_NAME = ?
                  AND PARENT_ID = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, categoryName);
            pstmt.setLong(2, parentId);

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

    /**
     * 부모 게임 아래에 새로운 하위 게시판을 생성합니다.
     *
     * 생성 직후에는 IS_ACTIVE = 'N'으로 저장합니다.
     * 즉, 바로 사용자에게 공개하지 않고 시스템 관리자의 승인 대기 상태로 만듭니다.
     *
     * CATEGORY_ID는 현재 부모 카테고리의 하위 ID 중 가장 큰 값에 1을 더해 만듭니다.
     * 현재 프로젝트의 규칙상 부모 ID 뒤에 4~9 범위의 번호를 사용하는 구조입니다.
     *
     * @param categoryName 새 게시판 이름
     * @param parentId 부모 게임 카테고리 번호
     * @return 1 = 생성 성공, 0 = 실패, -1 = 하위 게시판 최대 개수 초과
     */
    public int insertSubCategory(String categoryName, long parentId) {

        long nextId = 0;

        // 부모 카테고리의 기존 하위 게시판 중 가장 큰 ID를 찾습니다.
        String checkSql = """
                SELECT NVL(MAX(CATEGORY_ID), ? * 10) + 1
                FROM CATEGORY
                WHERE PARENT_ID = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(checkSql)
        ) {
            pstmt.setLong(1, parentId);
            pstmt.setLong(2, parentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    nextId = rs.getLong(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        // 부모 ID * 10 + 9를 넘으면 게시판을 더 만들 수 없습니다.
        if (nextId > (parentId * 10) + 9) {
            return -1;
        }

        // 승인되기 전까지는 IS_ACTIVE = 'N' 상태로 저장합니다.
        String insertSql = """
                INSERT INTO CATEGORY (
                    CATEGORY_ID,
                    PARENT_ID,
                    CATEGORY_NAME,
                    DEPTH,
                    IS_ACTIVE,
                    CREATED_AT
                )
                VALUES (?, ?, ?, 3, 'N', SYSDATE)
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(insertSql)
        ) {
            pstmt.setLong(1, nextId);
            pstmt.setLong(2, parentId);
            pstmt.setString(3, categoryName);

            return pstmt.executeUpdate() > 0 ? 1 : 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // =========================================================
    // 시스템 관리자 게시판 신청 처리
    // =========================================================

    /**
     * 승인 대기 중인 하위 게시판을 승인합니다.
     *
     * IS_ACTIVE를 N → Y로 변경하면 일반 사용자에게 게시판이 노출됩니다.
     *
     * @param categoryId 승인할 게시판 번호
     * @return 승인 성공 여부
     */
    public boolean approveCategory(long categoryId) {

        String sql = """
                UPDATE CATEGORY
                SET IS_ACTIVE = 'Y'
                WHERE CATEGORY_ID = ?
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, categoryId);
            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 승인 대기 중인 게시판 신청을 거절합니다.
     *
     * 아직 승인되지 않은 IS_ACTIVE = 'N' 데이터만 삭제합니다.
     * 이미 승인된 게시판이 실수로 삭제되지 않도록 조건을 추가했습니다.
     *
     * @param categoryId 거절할 게시판 번호
     * @return 거절 성공 여부
     */
    public boolean rejectCategory(long categoryId) {

        String sql = """
                DELETE FROM CATEGORY
                WHERE CATEGORY_ID = ?
                  AND IS_ACTIVE = 'N'
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setLong(1, categoryId);
            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 시스템 관리자가 확인할 승인 대기 게시판 목록을 조회합니다.
     *
     * 부모 CATEGORY를 다시 JOIN해서 게시판이 어느 게임에 속하는지 함께 조회합니다.
     *
     * @return 승인 대기 중인 하위 게시판 목록
     */
    public List<CategoryDTO> findPendingCategories() {

        List<CategoryDTO> list = new ArrayList<>();

        String sql = """
                SELECT
                    C.CATEGORY_ID,
                    C.PARENT_ID,
                    P.CATEGORY_NAME AS PARENT_CATEGORY_NAME,
                    C.CATEGORY_NAME,
                    C.DEPTH,
                    C.IS_ACTIVE,
                    TO_CHAR(C.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS CREATED_AT
                FROM CATEGORY C
                LEFT JOIN CATEGORY P
                    ON C.PARENT_ID = P.CATEGORY_ID
                WHERE C.DEPTH = 3
                  AND C.IS_ACTIVE = 'N'
                ORDER BY C.CREATED_AT DESC
                """;

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                CategoryDTO dto = new CategoryDTO();

                dto.setCategoryId(rs.getLong("CATEGORY_ID"));
                dto.setParentId(rs.getLong("PARENT_ID"));
                dto.setParentCategoryName(rs.getString("PARENT_CATEGORY_NAME"));
                dto.setCategoryName(rs.getString("CATEGORY_NAME"));
                dto.setIsActive(rs.getString("IS_ACTIVE"));
                dto.setCreatedAt(rs.getString("CREATED_AT"));

                list.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // 공통 내부 메서드
    // =========================================================

    /**
     * ResultSet 한 행의 데이터를 CategoryDTO에 넣습니다.
     *
     * 여러 조회 메서드에서 같은 컬럼을 DTO에 넣기 때문에
     * 중복 코드를 줄이기 위해 공통 메서드로 분리했습니다.
     */
    private void setCategoryData(ResultSet rs, CategoryDTO category) throws SQLException {

        // 카테고리 고유 번호
        category.setCategoryId(rs.getLong("CATEGORY_ID"));

        // 부모 카테고리 번호
        // Oracle에서 NULL을 숫자로 읽으면 0이 될 수 있으므로 wasNull()로 확인합니다.
        long parentId = rs.getLong("PARENT_ID");

        if (rs.wasNull()) {
            category.setParentId(null);
        } else {
            category.setParentId(parentId);
        }

        // 카테고리 이름
        category.setCategoryName(rs.getString("CATEGORY_NAME"));

        // 카테고리 깊이
        category.setDepth(rs.getInt("DEPTH"));

        // 활성화 여부
        category.setIsActive(rs.getString("IS_ACTIVE"));

        // 생성일
        category.setCreatedAt(rs.getString("CREATED_AT"));

        // 아이콘 이미지 주소
        category.setIconUrl(rs.getString("ICON_URL"));

        // 화면에 표시할 정렬 순서
        category.setSortOrder(rs.getInt("SORT_ORDER"));
    }

    /**
     * 현재 애플리케이션이 연결된 Oracle DB 정보를 콘솔에 출력합니다.
     *
     * 개발 중 DB 연결 문제를 확인하기 위한 디버깅용 메서드입니다.
     */
    private void printDatabaseInfo(Connection conn) throws SQLException {

        System.out.println("========== 현재 DB 정보 ==========");
        System.out.println("JDBC URL = " + conn.getMetaData().getURL());
        System.out.println("JDBC USER = " + conn.getMetaData().getUserName());

        String sql = """
                SELECT
                    SYS_CONTEXT('USERENV', 'DB_NAME') AS DB_NAME,
                    SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS SERVICE_NAME,
                    SYS_CONTEXT('USERENV', 'INSTANCE_NAME') AS INSTANCE_NAME,
                    USER AS USERNAME
                FROM DUAL
                """;

        try (
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            if (rs.next()) {
                System.out.println("DB NAME = " + rs.getString("DB_NAME"));
                System.out.println("SERVICE = " + rs.getString("SERVICE_NAME"));
                System.out.println("INSTANCE = " + rs.getString("INSTANCE_NAME"));
                System.out.println("USER = " + rs.getString("USERNAME"));
            }
        }

        System.out.println("=================================");
    }
}
