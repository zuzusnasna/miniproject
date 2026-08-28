package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryDAO;
import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dto.CategoryDTO;
import com.gamecommunity.dto.MemberDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
// 시스템 관리자의 게시판 승인 기능 구현간 추가된 클래스
@WebServlet("/admin/categories")
public class AdminCategoryApprovalServlet extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final MemberDAO memberDAO = new MemberDAO();

    // 승인 대기 목록 반환
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");

        // 관리자 권한(SYS_MANAGER) 체크
        if (!isAdmin(request, response)) {
            return;
        }

        List<CategoryDTO> list = categoryDAO.findPendingCategories();

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            CategoryDTO c = list.get(i);
            if (i > 0) json.append(",");
            json.append(String.format("{\"categoryId\":%d,\"parentId\":%d,\"categoryName\":\"%s\",\"createdAt\":\"%s\"}",
                    c.getCategoryId(),
                    c.getParentId(),
                    escapeJson(c.getCategoryName()),
                    c.getCreatedAt()));
        }
        json.append("]");

        response.getWriter().write(json.toString());
    }

    // 승인 / 거절 실행
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        // 관리자 권한(SYS_MANAGER) 체크
        if (!isAdmin(request, response)) {
            return;
        }

        String action = request.getParameter("action"); // approve or reject
        String categoryIdParam = request.getParameter("categoryId");

        if (categoryIdParam == null || action == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"파라미터가 누락되었습니다.\"}");
            return;
        }

        long categoryId = Long.parseLong(categoryIdParam);
        boolean result = false;

        if ("approve".equals(action)) {
            result = categoryDAO.approveCategory(categoryId);
        } else if ("reject".equals(action)) {
            result = categoryDAO.rejectCategory(categoryId);
        }

        response.getWriter().write("{\"success\":" + result + "}");
    }

    // 시스템 관리자(SYS_MANAGER) 권한 확인 헬퍼 메서드
    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        MemberDTO member = session == null ? null : (MemberDTO) session.getAttribute("member");

        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
            return false;
        }

        // MemberDAO의 isSystemManager 메서드로 MEMBER_ROLE 검증
        if (!memberDAO.isSystemManager(member.getMemberNo())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":\"시스템 관리자 권한이 없습니다.\"}");
            return false;
        }

        return true;
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}