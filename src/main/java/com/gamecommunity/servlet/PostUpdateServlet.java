package com.gamecommunity.servlet;

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

@WebServlet("/post-update")
public class PostUpdateServlet extends HttpServlet {

    private final PostDAO postDAO = new PostDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        HttpSession session = request.getSession(false);
        MemberDTO member = session == null ? null : (MemberDTO) session.getAttribute("member");

        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
            return;
        }

        String postIdParam = request.getParameter("postId");
        long postId;

        try {
            postId = Long.parseLong(postIdParam);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"잘못된 게시글 번호입니다.\"}");
            return;
        }

        PostDTO post = postDAO.findById(postId);

        if (post == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"success\":false,\"message\":\"게시글을 찾을 수 없습니다.\"}");
            return;
        }

        if (post.getMemberNo() == null || !post.getMemberNo().equals(member.getMemberNo())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":\"게시글 수정 권한이 없습니다.\"}");
            return;
        }

        String title = jsonEscape(post.getTitle());
        String content = jsonEscape(post.getContent());

        response.getWriter().write(
                "{\"success\":true,\"postId\":" + postId
                        + ",\"categoryId\":" + post.getCategoryId()
                        + ",\"title\":\"" + title + "\""
                        + ",\"content\":\"" + content + "\"}"
        );
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        HttpSession session = request.getSession(false);
        MemberDTO member = session == null ? null : (MemberDTO) session.getAttribute("member");

        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
            return;
        }

        String postIdParam = request.getParameter("postId");
        String title = request.getParameter("title");
        String content = request.getParameter("content");

        if (postIdParam == null || postIdParam.isBlank()
                || title == null || title.isBlank()
                || content == null || content.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"제목과 내용을 입력해주세요.\"}");
            return;
        }

        long postId;
        try {
            postId = Long.parseLong(postIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"잘못된 게시글 번호입니다.\"}");
            return;
        }

        PostDTO post = postDAO.findById(postId);

        if (post == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"success\":false,\"message\":\"게시글을 찾을 수 없습니다.\"}");
            return;
        }

        // 일반 회원은 본인이 작성한 게시글만 수정할 수 있다.
        if (post.getMemberNo() == null || !post.getMemberNo().equals(member.getMemberNo())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":\"게시글 수정 권한이 없습니다.\"}");
            return;
        }

        post.setTitle(title.trim());
        post.setContent(content);

        boolean result = postDAO.update(post);

        if (!result) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"message\":\"게시글 수정에 실패했습니다.\"}");
            return;
        }

        response.getWriter().write(
                "{\"success\":true,\"message\":\"게시글이 수정되었습니다.\",\"postId\":" + postId + "}"
        );
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
