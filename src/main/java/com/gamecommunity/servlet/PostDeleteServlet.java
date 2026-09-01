package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryManagerDAO;
import com.gamecommunity.dao.MemberDAO;
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
 * 게시글 삭제 요청을 처리하는 Servlet입니다.
 *
 * 삭제는 실제 데이터를 지우는 것이 아니라
 * PostDAO를 통해 논리 삭제 방식으로 처리합니다.
 *
 * 삭제 권한
 * 1. 시스템 관리자
 * 2. 해당 카테고리 관리자
 * 3. 게시글 작성자 본인
 */
@WebServlet("/post-delete")
public class PostDeleteServlet extends HttpServlet {

    // 게시글 삭제를 담당하는 DAO입니다.
    private final PostDAO postDAO = new PostDAO();

    // 시스템 관리자인지 확인하기 위해 사용합니다.
    private final MemberDAO memberDAO = new MemberDAO();

    // 해당 카테고리의 관리자인지 확인하기 위해 사용합니다.
    private final CategoryManagerDAO categoryManagerDAO =
            new CategoryManagerDAO();

    /**
     * 게시글 삭제 요청을 처리합니다.
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

        // 기존 세션만 확인하고 새로운 세션은 만들지 않습니다.
        HttpSession session = request.getSession(false);

        if (session == null) {
            writeJson(response, false, "로그인이 필요합니다.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 세션에 저장된 로그인 회원 정보를 가져옵니다.
        MemberDTO loginMember =
                (MemberDTO) session.getAttribute("member");

        if (loginMember == null) {
            writeJson(response, false, "로그인이 필요합니다.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // =====================================================
        // 2. 게시글 번호 확인
        // =====================================================

        String postIdParam = request.getParameter("postId");

        if (postIdParam == null || postIdParam.isBlank()) {
            writeJson(response, false, "게시글 번호가 없습니다.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        long postId;

        try {
            postId = Long.parseLong(postIdParam);
        } catch (NumberFormatException e) {
            writeJson(response, false, "잘못된 게시글 번호입니다.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // =====================================================
        // 3. 게시글 조회
        // =====================================================

        PostDTO post = postDAO.findById(postId);

        if (post == null) {
            writeJson(response, false, "게시글을 찾을 수 없습니다.");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // =====================================================
        // 4. 삭제 권한 확인
        // =====================================================

        long memberNo = loginMember.getMemberNo();
        long categoryId = post.getCategoryId();

        // 시스템 관리자는 모든 게시글을 삭제할 수 있습니다.
        boolean isSystemManager =
                memberDAO.isSystemManager(memberNo);

        // 카테고리 관리자는 자신이 관리하는 카테고리의
        // 게시글을 삭제할 수 있습니다.
        boolean isCategoryManager =
                categoryManagerDAO.isManagerOfCategory(
                        memberNo,
                        categoryId
                );

        // 일반 사용자는 자신이 작성한 게시글만 삭제할 수 있습니다.
        boolean isAuthor =
                memberNo == post.getMemberNo();

        // 세 가지 권한 중 하나라도 있으면 삭제할 수 있습니다.
        boolean canDelete =
                isSystemManager
                        || isCategoryManager
                        || isAuthor;

        if (!canDelete) {
            writeJson(
                    response,
                    false,
                    "게시글을 삭제할 권한이 없습니다."
            );
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // =====================================================
        // 5. 게시글 논리 삭제
        // =====================================================

        // DB에서 게시글을 실제로 삭제하지 않고 삭제 상태로 변경합니다.
        boolean deleted = postDAO.delete(postId);

        if (!deleted) {
            writeJson(
                    response,
                    false,
                    "게시글 삭제에 실패했습니다."
            );
            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );
            return;
        }

        // =====================================================
        // 6. 삭제 성공 응답
        // =====================================================

        writeJson(response, true, "게시글이 삭제되었습니다.");
    }

    /**
     * 성공 여부와 메시지를 JSON 형태로 반환합니다.
     */
    private void writeJson(
            HttpServletResponse response,
            boolean success,
            String message
    ) throws IOException {

        response.getWriter().write(
                "{\"success\":"
                        + success
                        + ",\"message\":\""
                        + escapeJson(message)
                        + "\"}"
        );
    }

    /**
     * JSON 문자열에서 문제가 될 수 있는 특수문자를 처리합니다.
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
