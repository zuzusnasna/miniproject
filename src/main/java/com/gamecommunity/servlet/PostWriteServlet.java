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

@WebServlet("/post-write")
public class PostWriteServlet extends HttpServlet {

    private final PostDAO postDAO = new PostDAO();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("========== POST-WRITE 요청 들어옴 ==========");
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/login.html");
            return;
        }

        MemberDTO member = (MemberDTO) session.getAttribute("member");
        if (member == null) {
            response.sendRedirect(request.getContextPath() + "/login.html");
            return;
        }

        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String categoryIdParam = request.getParameter("categoryId");
        String gameIdParam = request.getParameter("gameId");

        long categoryId;
        long gameId;

        try {
            categoryId = Long.parseLong(categoryIdParam);
            gameId = Long.parseLong(gameIdParam);
        } catch (NumberFormatException | NullPointerException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시판 정보가 올바르지 않습니다."
            );
            return;
        }

        // CATEGORY 규칙 검증:
        // 게임 110 -> 게시판 1101/1102/1103처럼 게임ID*10 + 1~3만 허용한다.
        long boardType = categoryId - (gameId * 10);
        if (boardType < 1 || boardType > 3) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게임과 게시판 정보가 일치하지 않습니다."
            );
            return;
        }

        PostDTO post = new PostDTO();
        post.setCategoryId(categoryId);
        post.setMemberNo(member.getMemberNo());
        post.setTitle(title);
        post.setContent(content);

        boolean result = postDAO.save(post);

        if (result) {
            response.sendRedirect(
                    request.getContextPath()
                            + "/game.html?gameId=" + gameId
                            + "&categoryId=" + categoryId
            );
        } else {
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "게시글 작성에 실패했습니다."
            );
        }
    }
}
