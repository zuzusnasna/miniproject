package com.gamecommunity.servlet;

import com.gamecommunity.dao.CommentDAO;
import com.gamecommunity.dto.CommentDTO;
import com.gamecommunity.dto.MemberDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * 댓글 조회, 등록, 수정, 삭제 요청을 처리합니다.
 *
 * 요청 흐름
 * 1. GET    /comments  → 댓글 목록 조회
 * 2. POST   /comments  → 댓글 등록
 * 3. PUT    /comments  → 댓글 수정
 * 4. DELETE /comments  → 댓글 삭제
 */
@WebServlet("/comments")
public class CommentServlet extends HttpServlet {

    // 댓글 관련 DB 작업을 담당하는 DAO입니다.
    private final CommentDAO commentDAO = new CommentDAO();

    // =========================================================
    // 댓글 목록 조회
    // =========================================================

    /**
     * 특정 게시글의 댓글 목록을 JSON으로 반환합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json; charset=UTF-8");

        // URL에서 게시글 번호를 가져옵니다.
        Long postId = parseLong(request.getParameter("postId"));

        // 게시글 번호가 잘못된 경우 요청을 종료합니다.
        if (postId == null) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시글 번호가 올바르지 않습니다."
            );
            return;
        }

        // 현재 로그인한 회원을 확인합니다.
        MemberDTO loginMember = getLoginMember(request);
        Long loginMemberNo =
                loginMember == null ? null : loginMember.getMemberNo();

        // DB에서 해당 게시글의 댓글을 조회합니다.
        List<CommentDTO> comments = commentDAO.findByPostId(postId);

        // 댓글 목록을 JSON 배열로 만들기 시작합니다.
        StringBuilder json =
                new StringBuilder("{\"success\":true,\"comments\":[");

        for (int i = 0; i < comments.size(); i++) {
            CommentDTO comment = comments.get(i);

            // 두 번째 댓글부터는 JSON 요소 사이에 쉼표를 넣습니다.
            if (i > 0) {
                json.append(',');
            }

            // 삭제된 댓글인지 확인합니다.
            boolean deleted = "Y".equals(comment.getIsDeleted());

            // 현재 로그인한 회원이 작성한 댓글인지 확인합니다.
            boolean mine =
                    loginMemberNo != null
                            && loginMemberNo.equals(comment.getMemberNo());

            // 댓글 하나를 JSON 객체로 변환합니다.
            json.append('{')
                    .append("\"commentId\":")
                    .append(comment.getCommentId())
                    .append(',')
                    .append("\"parentCommentId\":")
                    .append(
                            comment.getParentCommentId() == null
                                    ? "null"
                                    : comment.getParentCommentId()
                    )
                    .append(',')
                    .append("\"nickname\":\"")
                    .append(jsonEscape(comment.getNickname()))
                    .append("\",")
                    .append("\"content\":\"")
                    .append(
                            jsonEscape(
                                    deleted
                                            ? "삭제된 댓글입니다."
                                            : comment.getContent()
                            )
                    )
                    .append("\",")
                    .append("\"likeCount\":")
                    .append(comment.getLikeCount())
                    .append(',')
                    .append("\"dislikeCount\":")
                    .append(comment.getDislikeCount())
                    .append(',')
                    .append("\"deleted\":")
                    .append(deleted)
                    .append(',')
                    .append("\"mine\":")
                    .append(mine)
                    .append(',')
                    .append("\"createdAt\":\"")
                    .append(jsonEscape(comment.getCreatedAt()))
                    .append("\",")
                    .append("\"updatedAt\":")
                    .append(
                            comment.getUpdatedAt() == null
                                    ? "null"
                                    : "\""
                                            + jsonEscape(comment.getUpdatedAt())
                                            + "\""
                    )
                    .append('}');
        }

        json.append("]}");

        // 완성된 JSON을 브라우저로 전달합니다.
        response.getWriter().write(json.toString());
    }

    // =========================================================
    // 댓글 등록
    // =========================================================

    /**
     * 로그인한 회원이 댓글을 등록합니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        // 댓글 작성에는 로그인이 필요합니다.
        MemberDTO member = requireLogin(request, response);
        if (member == null) {
            return;
        }

        Long postId = parseLong(request.getParameter("postId"));
        Long parentId =
                parseOptionalLong(request.getParameter("parentCommentId"));
        String content = trim(request.getParameter("content"));

        // 게시글 번호가 없거나 댓글 내용이 비어 있거나 너무 길면 등록하지 않습니다.
        if (postId == null || content.isEmpty() || content.length() > 1000) {
            writeJson(
                    response,
                    false,
                    "댓글 내용을 1~1000자로 입력해주세요."
            );
            return;
        }

        // 댓글을 DB에 저장합니다.
        long commentId = commentDAO.save(
                postId,
                parentId,
                member.getMemberNo(),
                content
        );

        // 저장 결과를 JSON으로 반환합니다.
        writeJson(
                response,
                commentId > 0,
                commentId > 0
                        ? "댓글이 등록되었습니다."
                        : "댓글 등록에 실패했습니다."
        );
    }

    // =========================================================
    // 댓글 수정
    // =========================================================

    /**
     * 로그인한 회원이 자신의 댓글을 수정합니다.
     */
    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        // 댓글 수정에는 로그인이 필요합니다.
        MemberDTO member = requireLogin(request, response);
        if (member == null) {
            return;
        }

        Long commentId = parseLong(request.getParameter("commentId"));
        String content = trim(request.getParameter("content"));

        // 댓글 번호 또는 내용이 올바르지 않으면 수정하지 않습니다.
        if (commentId == null || content.isEmpty() || content.length() > 1000) {
            writeJson(
                    response,
                    false,
                    "댓글 내용을 1~1000자로 입력해주세요."
            );
            return;
        }

        // 작성자 본인의 댓글인지 DAO에서 확인하면서 수정합니다.
        boolean success = commentDAO.update(
                commentId,
                member.getMemberNo(),
                content
        );

        writeJson(
                response,
                success,
                success
                        ? "댓글이 수정되었습니다."
                        : "댓글을 수정할 수 없습니다."
        );
    }

    // =========================================================
    // 댓글 삭제
    // =========================================================

    /**
     * 로그인한 회원이 자신의 댓글을 삭제합니다.
     */
    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json; charset=UTF-8");

        // 댓글 삭제에도 로그인이 필요합니다.
        MemberDTO member = requireLogin(request, response);
        if (member == null) {
            return;
        }

        Long commentId = parseLong(request.getParameter("commentId"));

        // 댓글 번호가 없으면 삭제하지 않습니다.
        if (commentId == null) {
            writeJson(
                    response,
                    false,
                    "댓글 번호가 올바르지 않습니다."
            );
            return;
        }

        // 작성자 본인의 댓글인지 확인하면서 삭제합니다.
        boolean success = commentDAO.delete(
                commentId,
                member.getMemberNo()
        );

        writeJson(
                response,
                success,
                success
                        ? "댓글이 삭제되었습니다."
                        : "댓글을 삭제할 수 없습니다."
        );
    }

    // =========================================================
    // 로그인 회원 확인
    // =========================================================

    /**
     * 로그인한 회원 정보를 가져옵니다.
     * 로그인하지 않았다면 null을 반환합니다.
     */
    private MemberDTO getLoginMember(HttpServletRequest request) {

        // 기존 세션만 확인하고 새로운 세션은 만들지 않습니다.
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        return (MemberDTO) session.getAttribute("member");
    }

    /**
     * 로그인 여부를 확인합니다.
     * 로그인하지 않았다면 401 응답을 반환합니다.
     */
    private MemberDTO requireLogin(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        MemberDTO member = getLoginMember(request);

        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(response, false, "로그인이 필요합니다.");
            return null;
        }

        return member;
    }

    // =========================================================
    // 요청값 변환 / 정리
    // =========================================================

    /**
     * 문자열을 Long으로 변환합니다.
     * 변환할 수 없으면 null을 반환합니다.
     */
    private Long parseLong(String value) {

        try {
            if (value == null || value.isBlank()) {
                return null;
            }

            return Long.parseLong(value);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 선택적으로 전달되는 숫자값을 처리합니다.
     */
    private Long parseOptionalLong(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return parseLong(value);
    }

    /**
     * null이면 빈 문자열로 바꾸고 앞뒤 공백을 제거합니다.
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    // =========================================================
    // JSON 응답 생성
    // =========================================================

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
                        + jsonEscape(message)
                        + "\"}"
        );
    }

    /**
     * JSON 문자열에 들어갈 수 없는 특수문자를 이스케이프합니다.
     */
    private String jsonEscape(String value) {

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
