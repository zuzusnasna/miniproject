package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryDAO;
import com.gamecommunity.dao.CategoryManagerRequestDAO;
import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dto.CategoryDTO;
import com.gamecommunity.dto.MemberDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/category-manager-request")
public class CategoryManagerRequestServlet extends HttpServlet {
    private final CategoryManagerRequestDAO requestDAO = new CategoryManagerRequestDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final MemberDAO memberDAO = new MemberDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        MemberDTO member = getMember(request);
        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
            return;
        }

        int likes = memberDAO.getReceivedLikeCount(member.getMemberNo());
        String status = requestDAO.getRequestStatus(member.getMemberNo());
        List<CategoryDTO> games = categoryDAO.findByDepth(2);

        StringBuilder json = new StringBuilder("{\"success\":true,\"eligible\":").append(likes >= 50)
                .append(",\"likes\":").append(likes)
                .append(",\"status\":").append(status == null ? "null" : "\"" + escape(status) + "\"")
                .append(",\"games\":[");
        for (int i = 0; i < games.size(); i++) {
            if (i > 0) json.append(',');
            CategoryDTO game = games.get(i);
            json.append("{\"categoryId\":").append(game.getCategoryId())
                    .append(",\"categoryName\":\"").append(escape(game.getCategoryName())).append("\"}");
        }
        json.append("]}");
        response.getWriter().write(json);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        MemberDTO member = getMember(request);
        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
            return;
        }

        long memberNo = member.getMemberNo();
        int likes = memberDAO.getReceivedLikeCount(memberNo);
        if (likes < 50) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":\"받은 좋아요가 50개 이상이어야 신청할 수 있습니다.\"}");
            return;
        }

        if (requestDAO.hasPendingRequest(memberNo) || requestDAO.hasApprovedRequest(memberNo)) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write("{\"success\":false,\"message\":\"이미 신청했거나 카테고리 관리자 권한을 보유하고 있습니다.\"}");
            return;
        }

        String categoryIdParam = request.getParameter("categoryId");
        if (categoryIdParam == null || categoryIdParam.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"관리할 게임을 선택해주세요.\"}");
            return;
        }

        try {
            long categoryId = Long.parseLong(categoryIdParam);
            CategoryDTO game = categoryDAO.findById(categoryId);
            if (game == null || game.getDepth() != 2) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"관리할 게임을 올바르게 선택해주세요.\"}");
                return;
            }

            boolean result = requestDAO.insertRequest(memberNo, categoryId);
            response.getWriter().write("{\"success\":" + result + ",\"message\":\"카테고리 관리자 권한 신청이 접수되었습니다.\"}");
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"잘못된 게임입니다.\"}");
        }
    }

    private MemberDTO getMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (MemberDTO) session.getAttribute("member");
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
