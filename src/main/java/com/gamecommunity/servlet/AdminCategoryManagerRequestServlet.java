package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryManagerRequestDAO;
import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dto.CategoryManagerRequestDTO;
import com.gamecommunity.dto.MemberDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * 카테고리 관리자 신청을 관리하는 Servlet입니다.
 *
 * GET  : 대기 중인 신청 목록 조회
 * POST : 신청 승인 또는 거절
 *
 * 요청 흐름
 * 1. 시스템 관리자 여부 확인
 * 2. 요청에 필요한 데이터 확인
 * 3. DAO를 통해 조회 또는 승인/거절 처리
 * 4. JSON 응답 반환
 */
@WebServlet("/admin/category-manager-requests")
public class AdminCategoryManagerRequestServlet extends HttpServlet {

    // 카테고리 관리자 신청 관련 DB 작업을 담당합니다.
    private final CategoryManagerRequestDAO requestDAO =
            new CategoryManagerRequestDAO();

    // 로그인한 회원이 시스템 관리자인지 확인합니다.
    private final MemberDAO memberDAO = new MemberDAO();

    /**
     * 대기 중인 카테고리 관리자 신청 목록을 조회합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // =====================================================
        // 1. 시스템 관리자 여부 확인
        // =====================================================

        if (!isAdmin(request, response)) {
            return;
        }

        // =====================================================
        // 2. 응답 형식 설정
        // =====================================================

        response.setContentType("application/json; charset=UTF-8");

        // =====================================================
        // 3. 대기 중인 신청 조회
        // =====================================================

        List<CategoryManagerRequestDTO> requestList =
                requestDAO.findPendingRequests();

        // =====================================================
        // 4. JSON 변환 및 응답
        // =====================================================

        writeJson(response, createJson(requestList));
    }

    /**
     * 카테고리 관리자 신청을 승인하거나 거절합니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // =====================================================
        // 1. 시스템 관리자 여부 확인
        // =====================================================

        if (!isAdmin(request, response)) {
            return;
        }

        // =====================================================
        // 2. 요청 데이터 확인
        // =====================================================

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        String action = request.getParameter("action");
        String requestIdParam = request.getParameter("requestId");

        if (requestIdParam == null || requestIdParam.isBlank()
                || action == null || action.isBlank()) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "요청 정보가 누락되었습니다."
            );
            return;
        }

        // =====================================================
        // 3. 신청 번호 변환
        // =====================================================

        long requestId;

        try {
            requestId = Long.parseLong(requestIdParam);
        } catch (NumberFormatException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 요청 번호입니다."
            );
            return;
        }

        // =====================================================
        // 4. 승인 / 거절 처리
        // =====================================================

        boolean result;

        if ("approve".equals(action)) {
            result = requestDAO.approveRequest(requestId);
        } else if ("reject".equals(action)) {
            result = requestDAO.rejectRequest(requestId);
        } else {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 처리 요청입니다."
            );
            return;
        }

        // =====================================================
        // 5. 처리 결과 반환
        // =====================================================

        writeJson(
                response,
                "{\"success\":" + result + "}"
        );
    }

    /**
     * 현재 로그인한 회원이 시스템 관리자인지 확인합니다.
     */
    private boolean isAdmin(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
            return false;
        }

        MemberDTO member =
                (MemberDTO) session.getAttribute("member");

        if (member == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
            return false;
        }

        if (!memberDAO.isSystemManager(member.getMemberNo())) {
            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "시스템 관리자 권한이 없습니다."
            );
            return false;
        }

        return true;
    }

    /**
     * 신청 목록을 JSON 배열로 변환합니다.
     */
    private String createJson(
            List<CategoryManagerRequestDTO> requestList
    ) {

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < requestList.size(); i++) {

            CategoryManagerRequestDTO request =
                    requestList.get(i);

            // 두 번째 신청부터 앞에 쉼표를 추가합니다.
            if (i > 0) {
                json.append(',');
            }

            json.append('{')
                    .append("\"requestId\":")
                    .append(request.getRequestId())
                    .append(',')
                    .append("\"memberNo\":")
                    .append(request.getMemberNo())
                    .append(',')
                    .append("\"username\":\"")
                    .append(escape(request.getUsername()))
                    .append("\",")
                    .append("\"nickname\":\"")
                    .append(escape(request.getNickname()))
                    .append("\",")
                    .append("\"receivedLikeCount\":")
                    .append(request.getReceivedLikeCount())
                    .append(',')
                    .append("\"categoryId\":")
                    .append(request.getCategoryId())
                    .append(',')
                    .append("\"categoryName\":\"")
                    .append(escape(request.getCategoryName()))
                    .append("\",")
                    .append("\"requestedAt\":\"")
                    .append(escape(request.getRequestedAt()))
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

    /**
     * JSON 응답을 클라이언트에 전달합니다.
     */
    private void writeJson(
            HttpServletResponse response,
            String json
    ) throws IOException {

        response.getWriter().write(json);
    }

    /**
     * 오류 상태 코드와 메시지를 JSON으로 반환합니다.
     */
    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(status);

        writeJson(
                response,
                "{\"success\":false,\"message\":\""
                        + escape(message)
                        + "\"}"
        );
    }
}
