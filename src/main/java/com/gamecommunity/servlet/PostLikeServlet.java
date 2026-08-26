package com.gamecommunity.servlet;

import com.gamecommunity.dao.PostLikeDAO;
import com.gamecommunity.dto.MemberDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/post-like")
public class PostLikeServlet extends HttpServlet {

    private final PostLikeDAO postLikeDAO = new PostLikeDAO();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // =========================
        // 로그인 확인
        // =========================

        HttpSession session = request.getSession(false);

        if (session == null) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요합니다."
            );

            return;
        }

        MemberDTO member =
                (MemberDTO) session.getAttribute("member");

        if (member == null) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요합니다."
            );

            return;
        }


        // =========================
        // 게시글 번호 확인
        // =========================

        String postIdParam =
                request.getParameter("postId");

        if (postIdParam == null ||
                postIdParam.isBlank()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시글 번호가 없습니다."
            );

            return;
        }

        long postId;

        try {

            postId =
                    Long.parseLong(postIdParam);

        } catch (NumberFormatException e) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 게시글 번호입니다."
            );

            return;
        }


        // =========================
        // 좋아요 / 나빠요 구분
        // =========================

        String likeType =
                request.getParameter("likeType");

        if (likeType == null ||
                likeType.isBlank()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "추천 타입이 없습니다."
            );

            return;
        }

        likeType =
                likeType.toUpperCase();


        if (!"LIKE".equals(likeType) &&
                !"DISLIKE".equals(likeType)) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 추천 타입입니다."
            );

            return;
        }


        // =========================
        // 회원 번호
        // =========================

        long memberNo =
                member.getMemberNo();


        // =========================
        // 좋아요 / 나빠요 저장
        // =========================

        boolean result =
                postLikeDAO.addLike(
                        postId,
                        memberNo,
                        likeType
                );


        // =========================
        // 성공
        // =========================

        if (result) {

            int likeCount =
                    postLikeDAO.getLikeCount(postId);

            int dislikeCount =
                    postLikeDAO.getDislikeCount(postId);


            response.getWriter().write(
                    "{"
                            + "\"success\":true,"
                            + "\"likeCount\":" + likeCount + ","
                            + "\"dislikeCount\":" + dislikeCount
                            + "}"
            );


            if ("LIKE".equals(likeType)) {

                System.out.println(
                        "좋아요 성공"
                );

            } else {

                System.out.println(
                        "나빠요 성공"
                );
            }


            System.out.println(
                    "현재 좋아요 수 = " + likeCount
            );

            System.out.println(
                    "현재 나빠요 수 = " + dislikeCount
            );


        } else {

            // =========================
            // 이미 추천한 경우
            // =========================

            response.getWriter().write(
                    "{"
                            + "\"success\":false,"
                            + "\"message\":\"이미 좋아요 또는 나빠요를 누른 게시글입니다.\""
                            + "}"
            );


            System.out.println(
                    "추천 실패 - 이미 좋아요/나빠요를 누름"
            );
        }
    }
}
