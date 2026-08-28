package com.gamecommunity.servlet;

import com.gamecommunity.dao.CommentDAO;
import com.gamecommunity.dto.CommentDTO;
import com.gamecommunity.dto.MemberDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/comments")
public class CommentServlet extends HttpServlet {
    private final CommentDAO commentDAO = new CommentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        Long postId = parseLong(request.getParameter("postId"));
        if (postId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "게시글 번호가 올바르지 않습니다.");
            return;
        }

        MemberDTO loginMember = getLoginMember(request);
        Long loginMemberNo = loginMember == null ? null : loginMember.getMemberNo();
        List<CommentDTO> comments = commentDAO.findByPostId(postId);

        StringBuilder json = new StringBuilder("{\"success\":true,\"comments\":[");
        for (int i = 0; i < comments.size(); i++) {
            CommentDTO c = comments.get(i);
            if (i > 0) json.append(',');
            boolean deleted = "Y".equals(c.getIsDeleted());
            boolean mine = loginMemberNo != null && loginMemberNo.equals(c.getMemberNo());
            json.append('{')
                    .append("\"commentId\":").append(c.getCommentId()).append(',')
                    .append("\"parentCommentId\":").append(c.getParentCommentId() == null ? "null" : c.getParentCommentId()).append(',')
                    .append("\"username\":\"").append(jsonEscape(c.getUsername())).append("\",")
                    .append("\"content\":\"").append(jsonEscape(deleted ? "삭제된 댓글입니다." : c.getContent())).append("\",")
                    .append("\"likeCount\":").append(c.getLikeCount()).append(',')
                    .append("\"dislikeCount\":").append(c.getDislikeCount()).append(',')
                    .append("\"deleted\":").append(deleted).append(',')
                    .append("\"mine\":").append(mine).append(',')
                    .append("\"createdAt\":\"").append(jsonEscape(c.getCreatedAt())).append("\",")
                    .append("\"updatedAt\":").append(c.getUpdatedAt() == null ? "null" : "\"" + jsonEscape(c.getUpdatedAt()) + "\"")
                    .append('}');
        }
        json.append("]}");
        response.getWriter().write(json.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        MemberDTO member = requireLogin(request, response);
        if (member == null) return;

        Long postId = parseLong(request.getParameter("postId"));
        Long parentId = parseOptionalLong(request.getParameter("parentCommentId"));
        String content = trim(request.getParameter("content"));
        if (postId == null || content.isEmpty() || content.length() > 1000) {
            writeJson(response, false, "댓글 내용을 1~1000자로 입력해주세요.");
            return;
        }

        long commentId = commentDAO.save(postId, parentId, member.getMemberNo(), content);
        writeJson(response, commentId > 0, commentId > 0 ? "댓글이 등록되었습니다." : "댓글 등록에 실패했습니다.");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        MemberDTO member = requireLogin(request, response);
        if (member == null) return;

        Long commentId = parseLong(request.getParameter("commentId"));
        String content = trim(request.getParameter("content"));
        if (commentId == null || content.isEmpty() || content.length() > 1000) {
            writeJson(response, false, "댓글 내용을 1~1000자로 입력해주세요.");
            return;
        }
        boolean ok = commentDAO.update(commentId, member.getMemberNo(), content);
        writeJson(response, ok, ok ? "댓글이 수정되었습니다." : "댓글을 수정할 수 없습니다.");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        MemberDTO member = requireLogin(request, response);
        if (member == null) return;

        Long commentId = parseLong(request.getParameter("commentId"));
        if (commentId == null) {
            writeJson(response, false, "댓글 번호가 올바르지 않습니다.");
            return;
        }
        boolean ok = commentDAO.delete(commentId, member.getMemberNo());
        writeJson(response, ok, ok ? "댓글이 삭제되었습니다." : "댓글을 삭제할 수 없습니다.");
    }

    private MemberDTO requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MemberDTO member = getLoginMember(request);
        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(response, false, "로그인이 필요합니다.");
        }
        return member;
    }

    private MemberDTO getLoginMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (MemberDTO) session.getAttribute("member");
    }

    private Long parseLong(String value) {
        try { return value == null || value.isBlank() ? null : Long.parseLong(value); }
        catch (NumberFormatException e) { return null; }
    }

    private Long parseOptionalLong(String value) {
        return value == null || value.isBlank() ? null : parseLong(value);
    }

    private String trim(String value) { return value == null ? "" : value.trim(); }

    private void writeJson(HttpServletResponse response, boolean success, String message) throws IOException {
        response.getWriter().write("{\"success\":" + success + ",\"message\":\"" + jsonEscape(message) + "\"}");
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }
}
