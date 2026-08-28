package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryManagerDAO;
import com.gamecommunity.dao.MemberDAO;
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

    private final PostDAO postDAO = new PostDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final CategoryManagerDAO categoryManagerDAO = new CategoryManagerDAO();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("""
                {
                    "success": false,
                    "message": "로그인이 필요합니다."
                }
                """);
            return;
        }

        MemberDTO loginMember =
                (MemberDTO) session.getAttribute("member");

        if (loginMember == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("""
                {
                    "success": false,
                    "message": "로그인이 필요합니다."
                }
                """);
            return;
        }

        String postIdParam = request.getParameter("postId");

        if (postIdParam == null || postIdParam.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
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
            postId = Long.parseLong(postIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("""
                {
                    "success": false,
                    "message": "잘못된 게시글 번호입니다."
                }
                """);
            return;
        }

        PostDTO post = postDAO.findById(postId);

        if (post == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("""
                {
                    "success": false,
                    "message": "게시글을 찾을 수 없습니다."
                }
                """);
            return;
        }

        // =====================================================
        // 삭제 권한 확인
        // =====================================================
        long memberNo = loginMember.getMemberNo();
        long categoryId = post.getCategoryId();

        // 시스템 관리자: 모든 게시글 삭제 가능
        boolean isSystemManager = memberDAO.isSystemManager(memberNo);

        // 카테고리 관리자: 자신이 관리하는 카테고리의 게시글 삭제 가능
        boolean isCategoryManager =
                categoryManagerDAO.isManagerOfCategory(memberNo, categoryId);

        // 일반 사용자: 자신이 작성한 게시글만 삭제 가능
        boolean isAuthor = memberNo == post.getMemberNo();

        if (!isSystemManager && !isCategoryManager && !isAuthor) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("""
                {
                    "success": false,
                    "message": "게시글을 삭제할 권한이 없습니다."
                }
                """);
            return;
        }

        // =====================================================
        // 논리 삭제
        // =====================================================
        boolean deleted = postDAO.delete(postId);

        if (!deleted) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("""
                {
                    "success": false,
                    "message": "게시글 삭제에 실패했습니다."
                }
                """);
            return;
        }

        response.getWriter().write("""
            {
                "success": true,
                "message": "게시글이 삭제되었습니다."
            }
            """);
    }
}