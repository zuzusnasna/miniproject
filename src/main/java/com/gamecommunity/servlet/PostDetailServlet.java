package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryDAO;
import com.gamecommunity.dao.CategoryManagerDAO;
import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dao.PostDAO;
import com.gamecommunity.dao.PostLikeDAO;
import com.gamecommunity.dto.CategoryDTO;
import com.gamecommunity.dto.MemberDTO;
import com.gamecommunity.dto.PostDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 게시글 상세 정보를 조회하는 Servlet입니다.
 *
 * 화면(HTML)을 Servlet에서 직접 만들지 않고,
 * 게시글 데이터를 JSON으로 반환하는 역할만 담당합니다.
 *
 * 요청 흐름
 * 1. postId 확인
 * 2. 게시글 조회
 * 3. 게임 / 게시판 정보 조회
 * 4. 좋아요 / 나빠요 수 조회
 * 5. 로그인 회원의 삭제 권한 확인
 * 6. JSON 응답 반환
 *
 * 실제 화면은 post-detail.html과 post-detail.js에서 담당합니다.
 */
@WebServlet("/post-detail")
public class PostDetailServlet extends HttpServlet {

    // =========================================================
    // DAO
    // =========================================================

    // 게임 및 게시판 정보를 조회합니다.
    private final CategoryDAO categoryDAO = new CategoryDAO();

    // 게시글 정보를 조회합니다.
    private final PostDAO postDAO = new PostDAO();

    // 좋아요 / 나빠요 개수를 조회합니다.
    private final PostLikeDAO postLikeDAO = new PostLikeDAO();

    // 시스템 관리자 여부를 확인합니다.
    private final MemberDAO memberDAO = new MemberDAO();

    // 카테고리 관리자 여부를 확인합니다.
    private final CategoryManagerDAO categoryManagerDAO =
            new CategoryManagerDAO();

    /**
     * 게시글 상세 페이지 요청을 처리합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // =====================================================
        // 1. 게시글 번호 확인
        // =====================================================

        String postIdParam = request.getParameter("postId");

        if (postIdParam == null || postIdParam.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시글 번호가 없습니다."
            );
            return;
        }

        long postId;

        try {
            postId = Long.parseLong(postIdParam);
        } catch (NumberFormatException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 게시글 번호입니다."
            );
            return;
        }

        // =====================================================
        // 2. 게시글 조회
        // =====================================================

        // 상세 페이지에 들어왔으므로 조회수를 1 증가시킵니다.
        postDAO.increaseViewCount(postId);

        PostDTO post = postDAO.findById(postId);

        if (post == null) {
            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    "게시글을 찾을 수 없습니다."
            );
            return;
        }

        // =====================================================
        // 3. 게임 / 게시판 정보 조회
        // =====================================================

        // 게시글이 속한 게시판 번호입니다.
        long categoryId =
                post.getCategoryId() == null
                        ? 0L
                        : post.getCategoryId();

        // 현재 프로젝트의 CATEGORY_ID 규칙에 따라
        // 하위 게시판 ID에서 게임 ID를 계산합니다.
        long gameId = categoryId >= 1000
                ? categoryId / 10
                : categoryId;

        // 게임 카테고리를 DB에서 직접 조회합니다.
        // 기존 switch 하드코딩 방식은 제거했습니다.
        CategoryDTO gameCategory = categoryDAO.findById(gameId);

        String gameName = gameCategory == null
                ? "게임 커뮤니티"
                : gameCategory.getCategoryName();

        // =====================================================
        // 4. 추가 게시판 조회
        // =====================================================

        List<CategoryDTO> customBoards = new ArrayList<>();

        // 해당 게임에 속한 하위 게시판을 조회합니다.
        List<CategoryDTO> dbBoards =
                categoryDAO.findByParentId(gameId);

        if (dbBoards != null) {
            for (CategoryDTO category : dbBoards) {

                long suffix =
                        category.getCategoryId() - (gameId * 10);

                // 4~9번은 관리자가 추가 신청할 수 있는 게시판 영역입니다.
                if (suffix >= 4 && suffix <= 9) {
                    customBoards.add(category);
                }
            }

            // 게시판 번호 순서대로 정렬합니다.
            customBoards.sort(
                    Comparator.comparingLong(CategoryDTO::getCategoryId)
            );
        }

        // =====================================================
        // 5. 좋아요 / 나빠요 조회
        // =====================================================

        int likeCount = postLikeDAO.getLikeCount(postId);
        int dislikeCount = postLikeDAO.getDislikeCount(postId);

        // =====================================================
        // 6. 로그인 회원 확인
        // =====================================================

        HttpSession session = request.getSession(false);

        MemberDTO loginMember = session == null
                ? null
                : (MemberDTO) session.getAttribute("member");

        // 기본값은 삭제할 수 없음입니다.
        boolean canDelete = false;

        if (loginMember != null
                && loginMember.getMemberNo() != null) {

            long memberNo = loginMember.getMemberNo();

            // 게시글 작성자인지 확인합니다.
            boolean isAuthor =
                    post.getMemberNo() != null
                            && post.getMemberNo().equals(memberNo);

            // 시스템 관리자인지 확인합니다.
            boolean isSystemManager =
                    memberDAO.isSystemManager(memberNo);

            // 해당 게임의 카테고리 관리자인지 확인합니다.
            boolean isCategoryManager =
                    categoryManagerDAO.isManagerOfCategory(
                            memberNo,
                            categoryId
                    );

            // 작성자, 시스템 관리자, 카테고리 관리자 중
            // 하나라도 해당하면 삭제할 수 있습니다.
            canDelete =
                    isAuthor
                            || isSystemManager
                            || isCategoryManager;
        }

        // =====================================================
        // 7. HTML 페이지 요청 처리
        // =====================================================

        // 기존처럼 /post-detail?postId=1로 접근한 경우에는
        // 실제 화면 파일로 이동시킵니다.
        // 화면 자체는 post-detail.html이 담당합니다.
        String format = request.getParameter("format");

        if (!"json".equalsIgnoreCase(format)) {
            response.sendRedirect(
                    request.getContextPath()
                            + "/post-detail.html?postId="
                            + postId
            );
            return;
        }

        // =====================================================
        // 8. JSON 응답
        // =====================================================

        response.setContentType("application/json; charset=UTF-8");

        StringBuilder json = new StringBuilder();

        json.append('{')
                .append("\"postId\":")
                .append(postId)
                .append(',')
                .append("\"categoryId\":")
                .append(categoryId)
                .append(',')
                .append("\"gameId\":")
                .append(gameId)
                .append(',')
                .append("\"gameName\":\"")
                .append(escapeJson(gameName))
                .append("\",")
                .append("\"memberNo\":")
                .append(post.getMemberNo() == null
                        ? "null"
                        : post.getMemberNo())
                .append(',')
                .append("\"username\":\"")
                .append(escapeJson(post.getUsername()))
                .append("\",")
                .append("\"nickname\":\"")
                .append(escapeJson(post.getNickname()))
                .append("\",")
                .append("\"title\":\"")
                .append(escapeJson(post.getTitle()))
                .append("\",")
                .append("\"content\":\"")
                .append(escapeJson(post.getContent()))
                .append("\",")
                .append("\"viewCount\":")
                .append(post.getViewCount())
                .append(',')
                .append("\"likeCount\":")
                .append(likeCount)
                .append(',')
                .append("\"dislikeCount\":")
                .append(dislikeCount)
                .append(',')
                .append("\"createdAt\":\"")
                .append(escapeJson(post.getCreatedAt()))
                .append("\",")
                .append("\"canDelete\":")
                .append(canDelete)
                .append(',')
                .append("\"customBoards\":[");

        // =====================================================
        // 9. 추가 게시판 JSON 생성
        // =====================================================

        for (int i = 0; i < customBoards.size(); i++) {

            CategoryDTO board = customBoards.get(i);

            if (i > 0) {
                json.append(',');
            }

            json.append('{')
                    .append("\"categoryId\":")
                    .append(board.getCategoryId())
                    .append(',')
                    .append("\"categoryName\":\"")
                    .append(escapeJson(board.getCategoryName()))
                    .append("\"}");
        }

        json.append(']')
                .append('}');

        response.getWriter().write(json.toString());
    }

    /**
     * JSON 문자열에서 사용할 수 있도록 특수문자를 변환합니다.
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
