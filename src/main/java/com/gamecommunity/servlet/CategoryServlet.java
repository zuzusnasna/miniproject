package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryDAO;
import com.gamecommunity.dto.CategoryDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/categories")
public class CategoryServlet extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");

        String depthParam = request.getParameter("depth");

        List<CategoryDTO> categoryList;

        // =========================================
        // 하위 카테고리 조회
        // /categories?depth=2
        // =========================================
        if ("2".equals(depthParam)) {

            categoryList = categoryDAO.findByDepth(2);

        } else {

            // 전체 카테고리
            categoryList = categoryDAO.findAll();
        }

        // =========================================
        // JSON 생성
        // =========================================

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < categoryList.size(); i++) {

            CategoryDTO category = categoryList.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{")

                    .append("\"categoryId\":")
                    .append(category.getCategoryId())
                    .append(",")

                    .append("\"parentId\":");

            if (category.getParentId() == null) {
                json.append("null");
            } else {
                json.append(category.getParentId());
            }

            json.append(",")

                    .append("\"categoryName\":\"")
                    .append(escape(category.getCategoryName()))
                    .append("\",")

                    .append("\"depth\":")
                    .append(category.getDepth())
                    .append(",")

                    .append("\"isActive\":\"")
                    .append(escape(category.getIsActive()))
                    .append("\",")

                    .append("\"iconUrl\":\"")
                    .append(escape(category.getIconUrl()))
                    .append("\",")

                    .append("\"sortOrder\":")
                    .append(category.getSortOrder())

                    .append("}");
        }

        json.append("]");

        response.getWriter().write(json.toString());
    }


    // =========================================
    // JSON escape
    // =========================================

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