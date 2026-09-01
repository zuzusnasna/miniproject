package com.gamecommunity.servlet;

import com.gamecommunity.dao.PostLikeDAO;
import com.gamecommunity.dto.MemberDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * 게시글의 좋아요 / 나빠요 요청을 처리하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 로그인 여부 확인
 * 2. 게시글 번호 확인
 * 3. 추천 타입 확인
 * 4. 회원 번호 확인
 * 5. 추천 저장
 * 6. 결과와 최신 추천 수를 JSON으로 반환
 */
@WebServlet("/post-like")
public class PostLikeServlet extends HttpServlet {

    // 게시글 추천 관련 DB 작업을 담당합니다.
    private final PostLikeDAO postLikeDAO = new PostLikeDAO();

    /**
     * 좋아요 또는 나빠요 요청을 처리합니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        // =====================================================
        // 1. 로그인 여부 확인
        // =====================================================

        HttpSession session = request.getSession(false);

        if (session == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
            return;
        }

        // 로그인할 때 세션에 저장한 회원 정보를 가져옵니다.
        MemberDTO member =
                (MemberDTO) session.getAttribute("member");

        if (member == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
            return;
        }

        // =====================================================
        // 2. 게시글 번호 확인
        // =====================================================

        String postIdParam = request.getParameter("postId");

        if (postIdParam == null || postIdParam.isBlank()) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시글 번호가 없습니다."
            );
            return;
        }

        long postId;

        try {
            postId = Long.parseLong(postIdParam);
        } catch (NumberFormatException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 게시글 번호입니다."
            );
            return;
        }

        // =====================================================
        // 3. 좋아요 / 나빠요 타입 확인
        // =====================================================

        String likeType = request.getParameter("likeType");

        if (likeType == null || likeType.isBlank()) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "추천 타입이 없습니다."
            );
            return;
        }

        // 전달된 값의 대소문자를 통일합니다.
        likeType = likeType.toUpperCase();

        // 추천 타입은 LIKE와 DISLIKE만 허용합니다.
        if (!"LIKE".equals(likeType)
                && !"DISLIKE".equals(likeType)) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 추천 타입입니다."
            );
            return;
        }

        // =====================================================
        // 4. 회원 번호 확인
        // =====================================================

        long memberNo = member.getMemberNo();

        // =====================================================
        // 5. 추천 저장
        // =====================================================

        // DAO에서 중복 추천 여부를 확인하고 추천을 저장합니다.
        boolean result = postLikeDAO.addLike(
                postId,
                memberNo,
                likeType
        );

        // =====================================================
        // 6. 추천 저장 성공
        // =====================================================

        if (result) {
            sendSuccess(response, postId);
            return;
        }

        // =====================================================
        // 7. 중복 추천 처리
        // =====================================================

        // 이미 추천한 게시글이라면 실패 응답을 반환합니다.
        response.getWriter().write(
                "{"
                        + "\"success\":false,"
                        + "\"message\":\"이미 좋아요 또는 나빠요를 누른 게시글입니다.\""
                        + "}"
        );
    }

    /**
     * 추천 저장 성공 시 최신 좋아요 / 나빠요 수를 조회해서 반환합니다.
     */
    private void sendSuccess(
            HttpServletResponse response,
            long postId
    ) throws IOException {

        int likeCount = postLikeDAO.getLikeCount(postId);
        int dislikeCount = postLikeDAO.getDislikeCount(postId);

        response.getWriter().write(
                "{"
                        + "\"success\":true,"
                        + "\"likeCount\":" + likeCount + ","
                        + "\"dislikeCount\":" + dislikeCount
                        + "}"
        );
    }

    /**
     * 요청 처리 중 문제가 발생했을 때
     * HTTP 상태 코드와 메시지를 함께 반환합니다.
     */
    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(status);

        response.getWriter().write(
                "{\"success\":false,\"message\":\""
                        + escapeJson(message)
                        + "\"}"
        );
    }

    /**
     * JSON 문자열에 사용할 수 있도록 특수문자를 처리합니다.
     */
    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
