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

@WebServlet("/post-delete")
public class PostDeleteServlet extends HttpServlet {

    private final PostDAO postDAO =
            new PostDAO();


    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        response.setContentType(
                "application/json; charset=UTF-8"
        );


        // =====================================================
        // 로그인 확인
        // =====================================================

        HttpSession session =
                request.getSession(false);


        if (session == null) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.getWriter().write("""
                {
                    "success": false,
                    "message": "로그인이 필요합니다."
                }
                """);

            return;
        }


        MemberDTO loginMember =
                (MemberDTO) session.getAttribute(
                        "member"
                );


        if (loginMember == null) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.getWriter().write("""
                {
                    "success": false,
                    "message": "로그인이 필요합니다."
                }
                """);

            return;
        }


        // =====================================================
        // 게시글 번호 확인
        // =====================================================

        String postIdParam =
                request.getParameter("postId");


        if (
                postIdParam == null ||
                        postIdParam.isBlank()
        ) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            response.getWriter().write("""
                {
                    "success": false,
                    "message": "게시글 번호가 없습니다."
                }
                """);

            return;
        }


        long postId;


        try {

            postId =
                    Long.parseLong(postIdParam);

        } catch (NumberFormatException e) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            response.getWriter().write("""
                {
                    "success": false,
                    "message": "잘못된 게시글 번호입니다."
                }
                """);

            return;
        }


        // =====================================================
        // 게시글 조회
        // =====================================================

        PostDTO post =
                postDAO.findById(postId);


        if (post == null) {

            response.setStatus(
                    HttpServletResponse.SC_NOT_FOUND
            );

            response.getWriter().write("""
                {
                    "success": false,
                    "message": "게시글을 찾을 수 없습니다."
                }
                """);

            return;
        }


        // =====================================================
        // 작성자 확인
        // =====================================================

        if (
                !loginMember
                        .getUsername()
                        .equals(post.getUsername())
        ) {

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN
            );

            response.getWriter().write("""
                {
                    "success": false,
                    "message": "게시글 작성자만 삭제할 수 있습니다."
                }
                """);

            return;
        }


        // =====================================================
        // 논리 삭제
        // =====================================================

        boolean deleted =
                postDAO.delete(postId);


        if (!deleted) {

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            response.getWriter().write("""
                {
                    "success": false,
                    "message": "게시글 삭제에 실패했습니다."
                }
                """);

            return;
        }


        // =====================================================
        // 삭제 성공
        // =====================================================

        response.getWriter().write("""
            {
                "success": true,
                "message": "게시글이 삭제되었습니다."
            }
            """);
    }
}