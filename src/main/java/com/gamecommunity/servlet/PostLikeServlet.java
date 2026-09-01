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
 * 4. DB에 추천 저장
 * 5. 현재 추천 수를 조회해서 JSON으로 반환
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
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // =====================================================
        // 1. 로그인 여부 확인
        // =====================================================

        // 기존 세션만 확인합니다.
        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
            return;
        }

        // 세션에 저장된 로그인 회원 정보를 가져옵니다.
        MemberDTO member =
                (MemberDTO) session.getAttribute("member");

        if (member == null) {
            response.sendError(
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
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시글 번호가 없습니다."
            );
            return;
        }

        long postId;

        try {
            postId = Long.parseLong(postIdParam);
        } catch (NumberFormatException e) {
            response.sendError(
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
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "추천 타입이 없습니다."
            );
            return;
        }

        // 소문자로 전달되더라도 LIKE / DISLIKE로 처리할 수 있도록
        // 대문자로 통일합니다.
        likeType = likeType.toUpperCase();

        // 허용되는 추천 타입은 LIKE와 DISLIKE 두 가지뿐입니다.
        if (!"LIKE".equals(likeType)
                && !"DISLIKE".equals(likeType)) {

            response.sendError(
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

        // DAO에서 중복 추천 여부를 확인한 뒤 DB에 저장합니다.
        boolean result = postLikeDAO.addLike(
                postId,
                memberNo,
                likeType
        );

        // =====================================================
        // 6. 추천 저장 결과 처리
        // =====================================================

        if (result) {

            // 추천 저장에 성공하면 최신 추천 수를 다시 조회합니다.
            int likeCount = postLikeDAO.getLikeCount(postId);
            int dislikeCount = postLikeDAO.getDislikeCount(postId);

            // 최신 좋아요 / 나빠요 개수를 JSON으로 반환합니다.
            response.getWriter().write(
                    "{"
                            + "\"success\":true,"
                            + "\"likeCount\":" + likeCount + ","
                            + "\"dislikeCount\":" + dislikeCount
                            + "}"
            );

            // 개발 중 결과를 확인할 수 있도록 콘솔에 기록합니다.
            if ("LIKE".equals(likeType)) {
                System.out.println("좋아요 성공");
            } else {
                System.out.println("나빠요 성공");
            }

            System.out.println("현재 좋아요 수 = " + likeCount);
            System.out.println("현재 나빠요 수 = " + dislikeCount);
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

        System.out.println("추천 실패 - 이미 좋아요/나빠요를 누름");
    }
}
