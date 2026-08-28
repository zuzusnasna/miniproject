package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryManagerRequestDAO;
import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dto.CategoryManagerRequestDTO;
import com.gamecommunity.dto.MemberDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin/category-manager-requests")
public class AdminCategoryManagerRequestServlet extends HttpServlet {
    private final CategoryManagerRequestDAO requestDAO = new CategoryManagerRequestDAO();
    private final MemberDAO memberDAO = new MemberDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAdmin(request, response)) return;
        response.setContentType("application/json; charset=UTF-8");

        List<CategoryManagerRequestDTO> list = requestDAO.findPendingRequests();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) json.append(',');
            CategoryManagerRequestDTO r = list.get(i);
            json.append("{\"requestId\":").append(r.getRequestId())
                    .append(",\"memberNo\":").append(r.getMemberNo())
                    .append(",\"username\":\"").append(escape(r.getUsername()))
                    .append("\",\"nickname\":\"").append(escape(r.getNickname()))
                    .append("\",\"receivedLikeCount\":").append(r.getReceivedLikeCount())
                    .append(",\"categoryId\":").append(r.getCategoryId())
                    .append(",\"categoryName\":\"").append(escape(r.getCategoryName()))
                    .append("\",\"requestedAt\":\"").append(escape(r.getRequestedAt())).append("\"}");
        }
        json.append(']');
        writeJson(response, json.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAdmin(request, response)) return;
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        String action = request.getParameter("action");
        String requestIdParam = request.getParameter("requestId");
        if (requestIdParam == null || action == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(response, "{\"success\":false,\"message\":\"요청 정보가 누락되었습니다.\"}");
            return;
        }

        try {
            long requestId = Long.parseLong(requestIdParam);
            boolean result;
            if ("approve".equals(action)) {
                result = requestDAO.approveRequest(requestId);
            } else if ("reject".equals(action)) {
                result = requestDAO.rejectRequest(requestId);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeJson(response, "{\"success\":false,\"message\":\"잘못된 처리 요청입니다.\"}");
                return;
            }
            writeJson(response, "{\"success\":" + result + "}");
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(response, "{\"success\":false,\"message\":\"잘못된 요청 번호입니다.\"}");
        }
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        MemberDTO member = session == null ? null : (MemberDTO) session.getAttribute("member");
        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(response, "{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
            return false;
        }
        if (!memberDAO.isSystemManager(member.getMemberNo())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeJson(response, "{\"success\":false,\"message\":\"시스템 관리자 권한이 없습니다.\"}");
            return false;
        }
        return true;
    }

    private void writeJson(HttpServletResponse response, String json) throws IOException {
        response.getWriter().write(json);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
