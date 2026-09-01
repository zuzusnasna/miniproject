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

/**
 * 게시글 작성 요청을 처리하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 로그인 여부 확인
 * 2. 제목 / 내용 확인
 * 3. 게시판과 게임 정보 확인
 * 4. 게시글 DTO 생성
 * 5. DB에 게시글 저장
 * 6. 게임 게시판으로 이동
 */
@WebServlet("/post-write")
public class PostWriteServlet extends HttpServlet {

    // 게시글 DB 작업을 담당하는 DAO입니다.
    private final PostDAO postDAO = new PostDAO();

    /**
     * 게시글 작성 요청을 처리합니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");

        // =====================================================
        // 1. 로그인 여부 확인
        // =====================================================

        // 기존 세션만 확인하고 새로운 세션은 만들지 않습니다.
        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendRedirect(
                    request.getContextPath() + "/login.html"
            );
            return;
        }

        // 세션에 저장된 로그인 회원 정보를 가져옵니다.
        MemberDTO member =
                (MemberDTO) session.getAttribute("member");

        if (member == null) {
            response.sendRedirect(
                    request.getContextPath() + "/login.html"
            );
            return;
        }

        // =====================================================
        // 2. 작성할 게시글 정보 가져오기
        // =====================================================

        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String categoryIdParam = request.getParameter("categoryId");
        String gameIdParam = request.getParameter("gameId");

        // 제목과 내용은 반드시 입력해야 합니다.
        if (title == null || title.isBlank()
                || content == null || content.isBlank()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "제목과 내용을 입력해주세요."
            );
            return;
        }

        // =====================================================
        // 3. 게임 / 게시판 번호 변환
        // =====================================================

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

        // =====================================================
        // 4. 게임과 게시판 관계 확인
        // =====================================================

        // CATEGORY 번호는 게임 ID를 기준으로 만들어집니다.
        // 예: 게임 110 → 게시판 1101, 1102, 1103 ...
        // 따라서 categoryId에서 gameId * 10을 빼면 게시판 번호가 나옵니다.
        long boardType = categoryId - (gameId * 10);

        // 현재는 1~9번 게시판까지 허용합니다.
        if (boardType < 1 || boardType > 9) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게임과 게시판 정보가 일치하지 않습니다."
            );
            return;
        }

        // =====================================================
        // 5. 게시글 DTO 생성
        // =====================================================

        PostDTO post = new PostDTO();

        // 어떤 게시판에 작성할 글인지 저장합니다.
        post.setCategoryId(categoryId);

        // 현재 로그인한 회원을 작성자로 저장합니다.
        post.setMemberNo(member.getMemberNo());

        // 사용자가 입력한 제목과 내용을 저장합니다.
        post.setTitle(title);
        post.setContent(content);

        // =====================================================
        // 6. DB에 게시글 저장
        // =====================================================

        boolean result = postDAO.save(post);

        // =====================================================
        // 7. 저장 결과 처리
        // =====================================================

        if (result) {
            // 작성이 완료되면 작성했던 게임 게시판으로 돌아갑니다.
            response.sendRedirect(
                    request.getContextPath()
                            + "/game.html?gameId=" + gameId
                            + "&categoryId=" + categoryId
            );
            return;
        }

        // DB 저장에 실패한 경우 서버 오류를 반환합니다.
        response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "게시글 작성에 실패했습니다."
        );
    }
}
