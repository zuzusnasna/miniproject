package com.gamecommunity.servlet;

import com.gamecommunity.dao.CommentLikeDAO;
import com.gamecommunity.dto.MemberDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/comment-like")
public class CommentLikeServlet extends HttpServlet {
    private final CommentLikeDAO commentLikeDAO = new CommentLikeDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        HttpSession session = request.getSession(false);
        MemberDTO member = session == null ? null : (MemberDTO) session.getAttribute("member");
        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
            return;
        }

        long commentId;
        try {
            commentId = Long.parseLong(request.getParameter("commentId"));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "댓글 번호가 올바르지 않습니다.");
            return;
        }

        String likeType = request.getParameter("likeType");
        if (likeType == null) likeType = "";
        likeType = likeType.toUpperCase();
        if (!"LIKE".equals(likeType) && !"DISLIKE".equals(likeType)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "추천 타입이 올바르지 않습니다.");
            return;
        }

        boolean ok = commentLikeDAO.addLike(commentId, member.getMemberNo(), likeType);
        if (!ok) {
            response.getWriter().write("{\"success\":false,\"message\":\"이미 좋아요 또는 나빠요를 누른 댓글입니다.\"}");
            return;
        }

        int likeCount = commentLikeDAO.getLikeCount(commentId);
        int dislikeCount = commentLikeDAO.getDislikeCount(commentId);
        response.getWriter().write("{\"success\":true,\"likeCount\":" + likeCount + ",\"dislikeCount\":" + dislikeCount + "}");
    }
}
