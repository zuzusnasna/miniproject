package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryDAO;
import com.gamecommunity.dto.CategoryDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 게임 / 게시판 카테고리 목록 조회를 담당하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 응답 형식 설정
 * 2. depth 파라미터 확인
 * 3. 카테고리 조회
 * 4. 조회 결과를 JSON으로 변환
 * 5. JSON 응답 반환
 */
@WebServlet("/categories")
public class CategoryServlet extends HttpServlet {

    // 카테고리 DB 조회를 담당합니다.
    private final CategoryDAO categoryDAO = new CategoryDAO();

    /**
     * 카테고리 목록을 조회합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // =====================================================
        // 1. 응답 형식 설정
        // =====================================================

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // =====================================================
        // 2. depth 파라미터 확인
        // =====================================================

        String depthParam = request.getParameter("depth");

        // =====================================================
        // 3. 카테고리 조회
        // =====================================================

        List<CategoryDTO> categoryList;

        if (depthParam != null && !depthParam.isBlank()) {
            // depth가 전달되면 해당 depth의 카테고리만 조회합니다.
            int depth;

            try {
                depth = Integer.parseInt(depthParam);
            } catch (NumberFormatException e) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "잘못된 depth입니다."
                );
                return;
            }

            categoryList = categoryDAO.findByDepth(depth);
        } else {
            // depth가 없으면 모든 카테고리를 조회합니다.
            categoryList = categoryDAO.findAll();
        }

        // =====================================================
        // 4. JSON 배열 생성
        // =====================================================

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < categoryList.size(); i++) {

            CategoryDTO category = categoryList.get(i);

            // 두 번째 카테고리부터 앞에 쉼표를 추가합니다.
            if (i > 0) {
                json.append(',');
            }

            json.append('{')
                    .append("\"categoryId\":")
                    .append(category.getCategoryId())
                    .append(',')
                    .append("\"parentId\":");

            // 부모 카테고리가 없는 경우 JSON null로 반환합니다.
            if (category.getParentId() == null) {
                json.append("null");
            } else {
                json.append(category.getParentId());
            }

            json.append(',')
                    .append("\"categoryName\":\"")
                    .append(escape(category.getCategoryName()))
                    .append("\",")
                    .append("\"depth\":")
                    .append(category.getDepth())
                    .append(',')
                    .append("\"isActive\":\"")
                    .append(escape(category.getIsActive()))
                    .append("\",")
                    .append("\"createdAt\":\"")
                    .append(escape(category.getCreatedAt()))
                    .append("\",")
                    .append("\"iconUrl\":\"")
                    .append(escape(category.getIconUrl()))
                    .append("\",")
                    .append("\"sortOrder\":")
                    .append(category.getSortOrder())
                    .append('}');
        }

        json.append(']');

        // =====================================================
        // 5. JSON 응답 반환
        // =====================================================

        response.getWriter().write(json.toString());
    }

    /**
     * JSON 문자열에서 문제가 될 수 있는 특수문자를 처리합니다.
     */
    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
