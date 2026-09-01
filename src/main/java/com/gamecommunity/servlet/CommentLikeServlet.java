package com.gamecommunity.servlet;

import com.gamecommunity.dao.CommentLikeDAO;
import com.gamecommunity.dto.MemberDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * 댓글 좋아요 / 싫어요 요청을 처리하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 로그인 여부 확인
 * 2. 댓글 번호 확인
 * 3. 추천 종류 확인
 * 4. 추천 정보 저장
 * 5. 현재 좋아요 / 싫어요 개수 조회
 * 6. JSON 응답 반환
 */
@WebServlet("/comment-like")
public class CommentLikeServlet extends HttpServlet {

    // 댓글 추천 DB 작업을 담당합니다.
    private final CommentLikeDAO commentLikeDAO = new CommentLikeDAO();

    /**
     * 댓글에 좋아요 또는 싫어요를 등록합니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // =====================================================
        // 1. 요청 / 응답 인코딩 설정
        // =====================================================

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // =====================================================
        // 2. 로그인 여부 확인
        // =====================================================

        MemberDTO member = getMember(request);

        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(
                    response,
                    "{\"success\":false,\"message\":\"로그인이 필요합니다.\"}"
            );
            return;
        }

        // =====================================================
        // 3. 댓글 번호 확인
        // =====================================================

        String commentIdParam = request.getParameter("commentId");
        long commentId;

        try {
            // 요청으로 전달된 댓글 번호를 숫자로 변환합니다.
            commentId = Long.parseLong(commentIdParam);
        } catch (Exception e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "댓글 번호가 올바르지 않습니다."
            );
            return;
        }

        // =====================================================
        // 4. 추천 종류 확인
        // =====================================================

        String likeType = request.getParameter("likeType");
        likeType = normalizeLikeType(likeType);

        // LIKE / DISLIKE 이외의 값은 허용하지 않습니다.
        if (!isValidLikeType(likeType)) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "추천 타입이 올바르지 않습니다."
            );
            return;
        }

        // =====================================================
        // 5. 추천 정보 저장
        // =====================================================

        boolean result = commentLikeDAO.addLike(
                commentId,
                member.getMemberNo(),
                likeType
        );

        if (!result) {
            writeJson(
                    response,
                    "{\"success\":false,\"message\":\"이미 좋아요 또는 나빠요를 누른 댓글입니다.\"}"
            );
            return;
        }

        // =====================================================
        // 6. 현재 추천 개수 조회
        // =====================================================

        int likeCount = commentLikeDAO.getLikeCount(commentId);
        int dislikeCount = commentLikeDAO.getDislikeCount(commentId);

        // =====================================================
        // 7. 결과 반환
        // =====================================================

        String json = "{\"success\":true"
                + ",\"likeCount\":" + likeCount
                + ",\"dislikeCount\":" + dislikeCount
                + "}";

        writeJson(response, json);
    }

    /**
     * 세션에서 로그인 회원 정보를 가져옵니다.
     */
    private MemberDTO getMember(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        return (MemberDTO) session.getAttribute("member");
    }

    /**
     * 추천 타입을 대문자로 통일합니다.
     *
     * 예: like → LIKE
     */
    private String normalizeLikeType(String likeType) {

        if (likeType == null) {
            return "";
        }

        return likeType.toUpperCase();
    }

    /**
     * 추천 타입이 허용된 값인지 확인합니다.
     */
    private boolean isValidLikeType(String likeType) {
        return "LIKE".equals(likeType)
                || "DISLIKE".equals(likeType);
    }

    /**
     * JSON 응답을 클라이언트에 전달합니다.
     */
    private void writeJson(
            HttpServletResponse response,
            String json
    ) throws IOException {

        response.getWriter().write(json);
    }
}
