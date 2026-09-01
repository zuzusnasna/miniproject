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
 * 1. categoryId 확인
 * 2. 특정 게시판이면 해당 게시판의 게시글 조회
 * 3. categoryId가 없으면 전체 게시글 조회
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

        // =====================================================
        // 1. 응답 형식 설정
        // =====================================================

        response.setContentType("application/json; charset=UTF-8");

        // =====================================================
        // 2. categoryId 확인
        // =====================================================

        String categoryIdParam =
                request.getParameter("categoryId");

        // =====================================================
        // 3. 게시글 조회
        // =====================================================

        List<PostDTO> postList = getPostList(categoryIdParam, response);

        // categoryId가 잘못된 경우 getPostList()에서 이미 응답을 보냈습니다.
        if (postList == null) {
            return;
        }

        // =====================================================
        // 4. 게시글 목록을 JSON으로 변환
        // =====================================================

        String json = createJson(postList);

        // =====================================================
        // 5. JSON 응답 반환
        // =====================================================

        response.getWriter().write(json);
    }

    /**
     * categoryId가 있으면 해당 게시판의 글을 조회하고,
     * 없으면 전체 게시글을 조회합니다.
     */
    private List<PostDTO> getPostList(
            String categoryIdParam,
            HttpServletResponse response
    ) throws IOException {

        // categoryId가 전달되지 않은 경우 전체 게시글을 조회합니다.
        if (categoryIdParam == null || categoryIdParam.isBlank()) {
            return postDAO.findAll();
        }

        long categoryId;

        try {
            // URL의 문자열 categoryId를 숫자로 변환합니다.
            categoryId = Long.parseLong(categoryIdParam);
        } catch (NumberFormatException e) {
            // 숫자로 변환할 수 없으면 잘못된 요청입니다.
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 categoryId입니다."
            );
            return null;
        }

        // 해당 게시판에 작성된 게시글만 조회합니다.
        return postDAO.findByCategoryId(categoryId);
    }

    /**
     * 게시글 목록을 JSON 배열 문자열로 변환합니다.
     */
    private String createJson(List<PostDTO> postList) {

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < postList.size(); i++) {

            PostDTO post = postList.get(i);

            // 두 번째 게시글부터는 앞에 쉼표를 추가합니다.
            if (i > 0) {
                json.append(',');
            }

            // 닉네임이 있으면 닉네임을 사용하고,
            // 없으면 username을 대신 사용합니다.
            String writerName = post.getNickname() != null
                    ? post.getNickname()
                    : post.getUsername();

            // 게시글 하나를 JSON 객체로 변환합니다.
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

        json.append(']');

        return json.toString();
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
