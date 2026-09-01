package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryDAO;
import com.gamecommunity.dao.PostDAO;
import com.gamecommunity.dto.CategoryDTO;
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
 * 2. 제목 / 내용 / 게시판 번호 확인
 * 3. 게시판이 실제로 존재하고 활성화되어 있는지 확인
 * 4. 게시글 DTO 생성
 * 5. DB에 게시글 저장
 * 6. 작성한 게시판으로 이동
 */
@WebServlet("/post-write")
public class PostWriteServlet extends HttpServlet {

    // 게시글 DB 작업을 담당합니다.
    private final PostDAO postDAO = new PostDAO();

    // 게시판 존재 여부와 게임 정보를 확인합니다.
    private final CategoryDAO categoryDAO = new CategoryDAO();

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

        HttpSession session = request.getSession(false);

        if (session == null) {
            redirectToLogin(request, response);
            return;
        }

        // 세션에 저장된 로그인 회원 정보를 가져옵니다.
        MemberDTO member =
                (MemberDTO) session.getAttribute("member");

        if (member == null) {
            redirectToLogin(request, response);
            return;
        }

        // =====================================================
        // 2. 작성할 게시글 정보 가져오기
        // =====================================================

        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String categoryIdParam = request.getParameter("categoryId");

        // 제목과 내용은 반드시 입력해야 합니다.
        if (title == null || title.isBlank()
                || content == null || content.isBlank()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "제목과 내용을 입력해주세요."
            );
            return;
        }

        // 게시판 번호도 반드시 필요합니다.
        if (categoryIdParam == null || categoryIdParam.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시판 정보가 없습니다."
            );
            return;
        }

        // =====================================================
        // 3. 게시판 번호 변환
        // =====================================================

        long categoryId;

        try {
            categoryId = Long.parseLong(categoryIdParam);
        } catch (NumberFormatException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시판 정보가 올바르지 않습니다."
            );
            return;
        }

        // =====================================================
        // 4. 게시판 존재 및 활성화 여부 확인
        // =====================================================

        // 기존처럼 gameId와 categoryId의 숫자 규칙을 Servlet에서 계산하지 않습니다.
        // 실제 CATEGORY 테이블에서 게시판 정보를 조회합니다.
        CategoryDTO category = categoryDAO.findById(categoryId);

        if (category == null) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "존재하지 않거나 사용할 수 없는 게시판입니다."
            );
            return;
        }

        // =====================================================
        // 5. 부모 게임 정보 확인
        // =====================================================

        long gameId = category.getParentId();

        if (gameId <= 0) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시판의 게임 정보가 올바르지 않습니다."
            );
            return;
        }

        // =====================================================
        // 6. 게시글 DTO 생성
        // =====================================================

        PostDTO post = new PostDTO();

        // 어떤 게시판에 작성할 글인지 저장합니다.
        post.setCategoryId(categoryId);

        // 현재 로그인한 회원을 작성자로 저장합니다.
        post.setMemberNo(member.getMemberNo());

        // 사용자가 입력한 제목과 내용을 저장합니다.
        post.setTitle(title.trim());
        post.setContent(content.trim());

        // =====================================================
        // 7. DB에 게시글 저장
        // =====================================================

        boolean result = postDAO.save(post);

        // =====================================================
        // 8. 저장 결과 처리
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

    /**
     * 로그인 페이지로 이동합니다.
     */
    private void redirectToLogin(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.sendRedirect(
                request.getContextPath() + "/login.html"
        );
    }
}
