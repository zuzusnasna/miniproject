package com.gamecommunity.servlet;

import com.gamecommunity.dao.PostDAO;
import com.gamecommunity.dto.MemberDTO;
import com.gamecommunity.dto.PostDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/post-edit")
public class PostEditServlet extends HttpServlet {

    private final PostDAO postDAO = new PostDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        Long postId = parsePostId(request, response);
        if (postId == null) return;

        PostDTO post = postDAO.findById(postId);
        if (post == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "게시글을 찾을 수 없습니다.");
            return;
        }

        response.getWriter().print("{\"postId\":" + post.getPostId()
                + ",\"categoryId\":" + post.getCategoryId()
                + ",\"title\":\"" + escapeJson(post.getTitle())
                + "\",\"content\":\"" + escapeJson(post.getContent()) + "\"}");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        MemberDTO member = session == null ? null : (MemberDTO) session.getAttribute("member");
        if (member == null) {
            response.sendRedirect(request.getContextPath() + "/login.html");
            return;
        }

        Long postId = parsePostId(request, response);
        if (postId == null) return;

        PostDTO post = postDAO.findById(postId);
        if (post == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "게시글을 찾을 수 없습니다.");
            return;
        }

        // 게시글 수정은 작성자 본인만 가능
        if (post.getMemberNo() == null || !post.getMemberNo().equals(member.getMemberNo())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "게시글 수정 권한이 없습니다.");
            return;
        }

        String title = request.getParameter("title");
        String content = request.getParameter("content");
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "제목과 내용을 입력해주세요.");
            return;
        }

        post.setTitle(title.trim());
        post.setContent(content);

        if (postDAO.update(post)) {
            response.sendRedirect(request.getContextPath() + "/post-detail?postId=" + postId);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "게시글 수정에 실패했습니다.");
        }
    }

    private Long parsePostId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String value = request.getParameter("postId");
        try {
            if (value == null || value.isBlank()) throw new NumberFormatException();
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "잘못된 게시글 번호입니다.");
            return null;
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
