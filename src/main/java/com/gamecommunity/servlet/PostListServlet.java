package com.gamecommunity.servlet;

import com.gamecommunity.dao.PostDAO;
import com.gamecommunity.dto.PostDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 게시글 목록 조회 요청을 처리하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. categoryId가 전달되었는지 확인
 * 2. 특정 게시판이면 해당 게시판의 글 조회
 * 3. categoryId가 없으면 전체 글 조회
 * 4. 조회 결과를 JSON 배열로 변환
 * 5. 브라우저에 JSON 응답
 */
@WebServlet("/posts")
public class PostListServlet extends HttpServlet {

    // 게시글 조회를 담당하는 DAO입니다.
    private final PostDAO postDAO = new PostDAO();

    /**
     * 게시글 목록을 조회합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // 브라우저에서 JSON으로 받을 수 있도록 응답 형식을 지정합니다.
        response.setContentType("application/json; charset=UTF-8");

        // =====================================================
        // 1. 게시판 번호 확인
        // =====================================================

        String categoryIdParam =
                request.getParameter("categoryId");

        List<PostDTO> postList;

        // =====================================================
        // 2. 특정 게시판의 게시글 조회
        // =====================================================

        if (categoryIdParam != null && !categoryIdParam.isBlank()) {

            long categoryId;

            try {
                // URL의 문자열 categoryId를 숫자로 변환합니다.
                categoryId = Long.parseLong(categoryIdParam);
            } catch (NumberFormatException e) {
                // 숫자로 변환할 수 없다면 잘못된 요청입니다.
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "잘못된 categoryId입니다."
                );
                return;
            }

            // 해당 게시판에 작성된 게시글만 조회합니다.
            postList = postDAO.findByCategoryId(categoryId);

        } else {

            // =================================================
            // 3. 게시판 번호가 없으면 전체 게시글 조회
            // =================================================

            postList = postDAO.findAll();
        }

        // =====================================================
        // 4. 조회 결과를 JSON 배열로 변환
        // =====================================================

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < postList.size(); i++) {

            PostDTO post = postList.get(i);

            // JSON 객체 사이에는 쉼표가 필요합니다.
            // 첫 번째 객체 앞에는 쉼표를 넣지 않습니다.
            if (i > 0) {
                json.append(',');
            }

            // 닉네임이 있으면 닉네임을 사용하고,
            // 없으면 username을 대신 사용합니다.
            String writerName =
                    post.getNickname() != null
                            ? post.getNickname()
                            : post.getUsername();

            // 게시글 하나를 JSON 객체로 만듭니다.
            json.append('{')
                    .append("\"postId\":")
                    .append(post.getPostId())
                    .append(',')
                    .append("\"categoryId\":")
                    .append(post.getCategoryId())
                    .append(',')
                    .append("\"memberNo\":")
                    .append(post.getMemberNo())
                    .append(',')
                    .append("\"username\":\"")
                    .append(escape(post.getUsername()))
                    .append("\",")
                    .append("\"nickname\":\"")
                    .append(escape(writerName))
                    .append("\",")
                    .append("\"title\":\"")
                    .append(escape(post.getTitle()))
                    .append("\",")
                    .append("\"content\":\"")
                    .append(escape(post.getContent()))
                    .append("\",")
                    .append("\"viewCount\":")
                    .append(post.getViewCount())
                    .append(',')
                    .append("\"likeCount\":")
                    .append(post.getLikeCount())
                    .append(',')
                    .append("\"dislikeCount\":")
                    .append(post.getDislikeCount())
                    .append(',')
                    .append("\"createdAt\":\"")
                    .append(escape(post.getCreatedAt()))
                    .append("\"")
                    .append('}');
        }

        // JSON 배열을 닫습니다.
        json.append(']');

        // 완성된 게시글 목록을 브라우저에 전달합니다.
        response.getWriter().write(json.toString());
    }

    /**
     * JSON 문자열에서 문제가 될 수 있는 특수문자를 처리합니다.
     */
    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
