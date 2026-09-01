package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryDAO;
import com.gamecommunity.dao.CategoryManagerRequestDAO;
import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dto.CategoryDTO;
import com.gamecommunity.dto.MemberDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * 카테고리 관리자 권한 신청을 처리하는 Servlet입니다.
 *
 * GET  : 현재 회원의 신청 가능 여부와 게임 목록을 조회합니다.
 * POST : 선택한 게임의 카테고리 관리자 권한을 신청합니다.
 *
 * 요청 흐름
 * 1. 로그인 회원 확인
 * 2. 받은 좋아요 수와 기존 신청 상태 확인
 * 3. 신청 가능한 게임 목록 조회
 * 4. 신청 조건 확인
 * 5. 선택한 게임 확인
 * 6. 관리자 신청 저장
 * 7. JSON 응답 반환
 */
@WebServlet("/category-manager-request")
public class CategoryManagerRequestServlet extends HttpServlet {

    // 카테고리 관리자 신청 DB 작업을 담당합니다.
    private final CategoryManagerRequestDAO requestDAO =
            new CategoryManagerRequestDAO();

    // 게임 / 카테고리 정보를 조회합니다.
    private final CategoryDAO categoryDAO = new CategoryDAO();

    // 회원의 받은 좋아요 수를 조회합니다.
    private final MemberDAO memberDAO = new MemberDAO();

    /**
     * 현재 회원의 관리자 신청 가능 상태를 조회합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // =====================================================
        // 1. 로그인 회원 확인
        // =====================================================

        MemberDTO member = getMember(request);

        if (member == null) {
            writeJson(
                    response,
                    "{\"success\":false,\"message\":\"로그인이 필요합니다.\"}"
            );
            return;
        }

        // =====================================================
        // 2. 회원의 신청 상태 확인
        // =====================================================

        long memberNo = member.getMemberNo();

        // 지금까지 받은 좋아요 개수를 조회합니다.
        int likes = memberDAO.getReceivedLikeCount(memberNo);

        // 현재 신청 상태를 조회합니다.
        String status = requestDAO.getRequestStatus(memberNo);

        // =====================================================
        // 3. 관리할 수 있는 게임 목록 조회
        // =====================================================

        // depth 2는 게임 카테고리를 의미합니다.
        List<CategoryDTO> games = categoryDAO.findByDepth(2);

        // =====================================================
        // 4. 조회 결과를 JSON으로 생성
        // =====================================================

        StringBuilder json = new StringBuilder();

        json.append("{\"success\":true")
                .append(",\"eligible\":")
                .append(isEligible(likes))
                .append(",\"likes\":")
                .append(likes)
                .append(",\"status\":")
                .append(toJsonString(status))
                .append(",\"games\":[");

        // 게임 목록을 JSON 배열로 변환합니다.
        appendGames(json, games);

        json.append("]}");

        // =====================================================
        // 5. JSON 응답 반환
        // =====================================================

        writeJson(response, json.toString());
    }

    /**
     * 카테고리 관리자 권한을 신청합니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // =====================================================
        // 1. 로그인 회원 확인
        // =====================================================

        MemberDTO member = getMember(request);

        if (member == null) {
            writeJson(
                    response,
                    "{\"success\":false,\"message\":\"로그인이 필요합니다.\"}"
            );
            return;
        }

        long memberNo = member.getMemberNo();

        // =====================================================
        // 2. 신청 조건 확인
        // =====================================================

        int likes = memberDAO.getReceivedLikeCount(memberNo);

        if (!isEligible(likes)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeJson(
                    response,
                    "{\"success\":false,\"message\":\"받은 좋아요가 50개 이상이어야 신청할 수 있습니다.\"}"
            );
            return;
        }

        // 이미 신청했거나 승인된 회원은 다시 신청할 수 없습니다.
        if (requestDAO.hasPendingRequest(memberNo)
                || requestDAO.hasApprovedRequest(memberNo)) {

            response.setStatus(HttpServletResponse.SC_CONFLICT);
            writeJson(
                    response,
                    "{\"success\":false,\"message\":\"이미 신청했거나 카테고리 관리자 권한을 보유하고 있습니다.\"}"
            );
            return;
        }

        // =====================================================
        // 3. 선택한 게임 확인
        // =====================================================

        String categoryIdParam = request.getParameter("categoryId");

        if (categoryIdParam == null || categoryIdParam.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(
                    response,
                    "{\"success\":false,\"message\":\"관리할 게임을 선택해주세요.\"}"
            );
            return;
        }

        long categoryId;

        try {
            categoryId = Long.parseLong(categoryIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(
                    response,
                    "{\"success\":false,\"message\":\"잘못된 게임입니다.\"}"
            );
            return;
        }

        // 전달받은 categoryId가 실제 게임 카테고리인지 확인합니다.
        CategoryDTO game = categoryDAO.findById(categoryId);

        if (game == null || game.getDepth() != 2) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(
                    response,
                    "{\"success\":false,\"message\":\"관리할 게임을 올바르게 선택해주세요.\"}"
            );
            return;
        }

        // =====================================================
        // 4. 관리자 신청 저장
        // =====================================================

        boolean result = requestDAO.insertRequest(memberNo, categoryId);

        // =====================================================
        // 5. 처리 결과 반환
        // =====================================================

        writeJson(
                response,
                "{\"success\":" + result
                        + ",\"message\":\"카테고리 관리자 권한 신청이 접수되었습니다.\"}"
        );
    }

    /**
     * 현재 로그인한 회원 정보를 세션에서 가져옵니다.
     */
    private MemberDTO getMember(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        return (MemberDTO) session.getAttribute("member");
    }

    /**
     * 관리자 신청 조건을 확인합니다.
     *
     * 현재 프로젝트 정책상 받은 좋아요 50개 이상이면 신청할 수 있습니다.
     */
    private boolean isEligible(int likes) {
        return likes >= 50;
    }

    /**
     * 게임 목록을 JSON 배열 안에 추가합니다.
     */
    private void appendGames(
            StringBuilder json,
            List<CategoryDTO> games
    ) {

        for (int i = 0; i < games.size(); i++) {

            if (i > 0) {
                json.append(',');
            }

            CategoryDTO game = games.get(i);

            json.append("{\"categoryId\":")
                    .append(game.getCategoryId())
                    .append(",\"categoryName\":\"")
                    .append(escape(game.getCategoryName()))
                    .append("\"}");
        }
    }

    /**
     * 문자열을 JSON 문자열 형식으로 변환합니다.
     * 값이 null이면 JSON의 null을 반환합니다.
     */
    private String toJsonString(String value) {

        if (value == null) {
            return "null";
        }

        return "\"" + escape(value) + "\"";
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

    /**
     * JSON 응답을 클라이언트에 전달합니다.
     */
    private void writeJson(
            HttpServletResponse response,
            String json
    ) throws IOException {

        response.getWriter().write(json);
    }
}
