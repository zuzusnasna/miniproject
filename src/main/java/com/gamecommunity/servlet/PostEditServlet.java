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

@WebServlet("/post-update")
public class PostEditServlet extends HttpServlet {
    private final PostDAO postDAO = new PostDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        Long postId = parsePostId(request, response);
        if (postId == null) return;
        PostDTO post = postDAO.findById(postId);
        if (post == null) { response.sendError(404, "게시글을 찾을 수 없습니다."); return; }
        response.getWriter().print("{\"success\":true,\"postId\":" + post.getPostId()
                + ",\"categoryId\":" + post.getCategoryId()
                + ",\"title\":\"" + escapeJson(post.getTitle())
                + "\",\"content\":\"" + escapeJson(post.getContent()) + "\"}");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        MemberDTO member = session == null ? null : (MemberDTO) session.getAttribute("member");
        if (member == null) { response.sendError(401, "로그인이 필요합니다."); return; }

        Long postId = parsePostId(request, response);
        if (postId == null) return;
        PostDTO post = postDAO.findById(postId);
        if (post == null) { response.sendError(404, "게시글을 찾을 수 없습니다."); return; }
        if (post.getMemberNo() == null || !post.getMemberNo().equals(member.getMemberNo())) {
            response.sendError(403, "게시글 수정 권한이 없습니다."); return;
        }

        String title = request.getParameter("title");
        String content = request.getParameter("content");
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            response.sendError(400, "제목과 내용을 입력해주세요."); return;
        }
        post.setTitle(title.trim());
        post.setContent(content);

        response.setContentType("application/json; charset=UTF-8");
        if (postDAO.update(post)) {
            response.getWriter().print("{\"success\":true,\"message\":\"게시글이 수정되었습니다.\"}");
        } else {
            response.sendError(500, "게시글 수정에 실패했습니다.");
        }
    }

    private Long parsePostId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try { return Long.parseLong(request.getParameter("postId")); }
        catch (Exception e) { response.sendError(400, "잘못된 게시글 번호입니다."); return null; }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }
}
