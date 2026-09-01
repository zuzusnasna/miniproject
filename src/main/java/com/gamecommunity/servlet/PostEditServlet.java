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
 * 게시글 수정 화면에 필요한 정보를 조회하고
 * 게시글 수정 요청을 처리하는 Servlet입니다.
 *
 * GET  : 수정할 게시글 정보를 조회합니다.
 * POST : 게시글 제목과 내용을 수정합니다.
 *
 * 요청 흐름
 * 1. 로그인 여부 확인
 * 2. 게시글 번호 확인
 * 3. 게시글 조회
 * 4. 작성자 본인 여부 확인
 * 5. GET은 게시글 정보 반환 / POST는 수정 처리
 */
@WebServlet("/post-edit")
public class PostEditServlet extends HttpServlet {

    // 게시글 DB 작업을 담당합니다.
    private final PostDAO postDAO = new PostDAO();

    /**
     * 수정할 게시글의 기존 정보를 조회합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // =====================================================
        // 1. 응답 설정
        // =====================================================

        setJsonResponse(response);

        // =====================================================
        // 2. 로그인 여부 확인
        // =====================================================

        MemberDTO member = getLoginMember(request);

        if (member == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
            return;
        }

        // =====================================================
        // 3. 게시글 번호 확인
        // =====================================================

        Long postId = parsePostId(request, response);

        if (postId == null) {
            return;
        }

        // =====================================================
        // 4. 게시글 조회
        // =====================================================

        PostDTO post = postDAO.findById(postId);

        if (post == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "게시글을 찾을 수 없습니다."
            );
            return;
        }

        // =====================================================
        // 5. 수정 권한 확인
        // =====================================================

        if (!isWriter(post, member)) {
            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "게시글 수정 권한이 없습니다."
            );
            return;
        }

        // =====================================================
        // 6. 기존 게시글 정보 반환
        // =====================================================

        response.getWriter().print(createPostJson(post));
    }

    /**
     * 게시글 제목과 내용을 수정합니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // =====================================================
        // 1. 요청 / 응답 설정
        // =====================================================

        request.setCharacterEncoding("UTF-8");
        setJsonResponse(response);

        // =====================================================
        // 2. 로그인 여부 확인
        // =====================================================

        MemberDTO member = getLoginMember(request);

        if (member == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
            return;
        }

        // =====================================================
        // 3. 게시글 번호 확인
        // =====================================================

        Long postId = parsePostId(request, response);

        if (postId == null) {
            return;
        }

        // =====================================================
        // 4. 게시글 조회
        // =====================================================

        PostDTO post = postDAO.findById(postId);

        if (post == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "게시글을 찾을 수 없습니다."
            );
            return;
        }

        // =====================================================
        // 5. 수정 권한 확인
        // =====================================================

        if (!isWriter(post, member)) {
            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "게시글 수정 권한이 없습니다."
            );
            return;
        }

        // =====================================================
        // 6. 수정할 제목 / 내용 확인
        // =====================================================

        String title = request.getParameter("title");
        String content = request.getParameter("content");

        if (!hasPostContent(title, content)) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "제목과 내용을 입력해주세요."
            );
            return;
        }

        // =====================================================
        // 7. 게시글 내용 변경
        // =====================================================

        post.setTitle(title.trim());
        post.setContent(content);

        // =====================================================
        // 8. DB 수정
        // =====================================================

        boolean updated = postDAO.update(post);

        // =====================================================
        // 9. 수정 결과 반환
        // =====================================================

        if (updated) {
            response.getWriter().print(
                    "{\"success\":true"
                            + ",\"message\":\"게시글이 수정되었습니다.\""
                            + ",\"postId\":" + postId
                            + "}"
            );
            return;
        }

        sendError(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "게시글 수정에 실패했습니다."
        );
    }

    /**
     * 현재 로그인한 회원 정보를 세션에서 가져옵니다.
     */
    private MemberDTO getLoginMember(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        return (MemberDTO) session.getAttribute("member");
    }

    /**
     * 요청에서 게시글 번호를 숫자로 변환합니다.
     */
    private Long parsePostId(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        String postIdParam = request.getParameter("postId");

        if (postIdParam == null || postIdParam.isBlank()) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시글 번호가 필요합니다."
            );
            return null;
        }

        try {
            return Long.parseLong(postIdParam);
        } catch (NumberFormatException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 게시글 번호입니다."
            );
            return null;
        }
    }

    /**
     * 게시글 작성자와 현재 로그인 회원이 같은지 확인합니다.
     */
    private boolean isWriter(
            PostDTO post,
            MemberDTO member
    ) {

        return post.getMemberNo() != null
                && post.getMemberNo().equals(member.getMemberNo());
    }

    /**
     * 제목과 내용이 모두 입력되었는지 확인합니다.
     */
    private boolean hasPostContent(
            String title,
            String content
    ) {

        return title != null && !title.isBlank()
                && content != null && !content.isBlank();
    }

    /**
     * 게시글 정보를 JSON으로 변환합니다.
     */
    private String createPostJson(PostDTO post) {

        return "{\"success\":true"
                + ",\"postId\":" + post.getPostId()
                + ",\"categoryId\":" + post.getCategoryId()
                + ",\"title\":\"" + escapeJson(post.getTitle())
                + "\",\"content\":\"" + escapeJson(post.getContent())
                + "\"}";
    }

    /**
     * JSON 응답의 Content-Type과 인코딩을 설정합니다.
     */
    private void setJsonResponse(HttpServletResponse response) {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * 오류 응답을 JSON 형식으로 반환합니다.
     */
    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(status);
        response.getWriter().print(
                "{\"success\":false,\"message\":\""
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
