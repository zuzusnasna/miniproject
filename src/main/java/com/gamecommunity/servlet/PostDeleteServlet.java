package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryManagerDAO;
import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dao.PostDAO;
import com.gamecommunity.dto.MemberDTO;
import com.gamecommunity.dto.PostDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/post-delete")
public class PostDeleteServlet extends HttpServlet {

    private final PostDAO postDAO = new PostDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final CategoryManagerDAO categoryManagerDAO = new CategoryManagerDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        MemberDTO loginMember = (MemberDTO) session.getAttribute("member");
        if (loginMember == null || loginMember.getMemberNo() == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        String postIdParam = request.getParameter("postId");
        if (postIdParam == null || postIdParam.isBlank()) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "게시글 번호가 없습니다.");
            return;
        }

        long postId;
        try {
            postId = Long.parseLong(postIdParam);
        } catch (NumberFormatException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "잘못된 게시글 번호입니다.");
            return;
        }

        PostDTO post = postDAO.findById(postId);
        if (post == null) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "게시글을 찾을 수 없습니다.");
            return;
        }

        long memberNo = loginMember.getMemberNo();
        long categoryId = post.getCategoryId() == null ? -1L : post.getCategoryId();

        // 1. 시스템 관리자: 모든 게시글 삭제 가능
        if (!memberDAO.isSystemManager(memberNo)) {
            // 2. 카테고리 관리자: 본인이 관리하는 카테고리의 게시글만 삭제 가능
            boolean categoryManager = categoryId != -1L
                    && categoryManagerDAO.isManagerOfCategory(memberNo, categoryId);

            // 3. 일반 회원: 본인이 작성한 게시글만 삭제 가능
            boolean author = post.getMemberNo() != null
                    && post.getMemberNo().equals(memberNo);

            if (!categoryManager && !author) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        "이 게시글을 삭제할 권한이 없습니다.");
                return;
            }
        }

        // 기존 논리 삭제 방식 유지: IS_DELETED = 'Y'
        boolean deleted = postDAO.delete(postId);
        if (!deleted) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "게시글 삭제에 실패했습니다.");
            return;
        }

        response.getWriter().write("""
            {
                "success": true,
                "message": "게시글이 삭제되었습니다."
            }
            """);
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.getWriter().write("{\"success\":false,\"message\":\"" + escapeJson(message) + "\"}");
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
