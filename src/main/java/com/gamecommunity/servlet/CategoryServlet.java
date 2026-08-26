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
    public void init() throws ServletException {
        super.init();

        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("CATEGORY SERVLET INIT 됨!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        System.err.println("🔥🔥🔥 CATEGORY SERVLET 진입 🔥🔥🔥");
        System.out.println();
        System.out.println("=================================");
        System.out.println("========== CATEGORY SERVLET ==========");
        System.out.println("CATEGORY SERVLET 진입!");
        System.out.println("URI = " + request.getRequestURI());
        System.out.println("Query = " + request.getQueryString());
        System.out.println("=================================");

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String depthParam = request.getParameter("depth");

        System.out.println("depthParam = " + depthParam);

        List<CategoryDTO> categoryList;

        if (depthParam != null && !depthParam.isBlank()) {

            int depth = Integer.parseInt(depthParam);

            System.out.println("DAO findByDepth 호출!");
            System.out.println("depth = " + depth);

            categoryList = categoryDAO.findByDepth(depth);

        } else {

            System.out.println("DAO findAll 호출!");

            categoryList = categoryDAO.findAll();
        }

        System.out.println(
                "DAO 결과 개수 = " + categoryList.size()
        );

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

                    .append("\"createdAt\":\"")
                    .append(escape(category.getCreatedAt()))
                    .append("\",")

                    .append("\"iconUrl\":\"")
                    .append(escape(category.getIconUrl()))
                    .append("\",")

                    .append("\"sortOrder\":")
                    .append(category.getSortOrder())

                    .append("}");
        }

        json.append("]");

        System.out.println("최종 JSON = " + json);

        response.getWriter().write(json.toString());

        System.out.println("JSON 응답 완료!");
        System.out.println("=================================");
    }


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