package com.gamecommunity.servlet;

import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dto.MemberDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * 로그인한 회원의 정보를 조회하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 로그인 세션 확인
 * 2. 로그인한 회원 정보 확인
 * 3. 회원의 추천 수 조회
 * 4. 회원 정보를 JSON으로 변환
 * 5. 클라이언트에 JSON 응답
 */
@WebServlet("/member-info")
public class MemberInfoServlet extends HttpServlet {

    // 회원 정보와 추천 수를 조회하는 DAO입니다.
    private final MemberDAO memberDAO = new MemberDAO();

    /**
     * 로그인한 회원의 정보를 조회합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // =====================================================
        // 1. 응답 형식 설정
        // =====================================================

        // 이 Servlet은 화면 자체가 아니라 회원 정보를 JSON으로 반환합니다.
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // =====================================================
        // 2. 로그인 세션 확인
        // =====================================================

        // 기존 세션만 가져옵니다.
        // 세션이 없으면 새로운 세션을 만들지 않습니다.
        HttpSession session = request.getSession(false);

        if (session == null) {
            sendUnauthorized(response);
            return;
        }

        // 세션에 저장해 둔 로그인 회원 정보를 가져옵니다.
        MemberDTO member =
                (MemberDTO) session.getAttribute("member");

        // 세션은 있지만 로그인 회원 정보가 없다면 로그인 상태가 아닙니다.
        if (member == null) {
            sendUnauthorized(response);
            return;
        }

        // =====================================================
        // 3. 회원 정보 조회
        // =====================================================

        // 로그인한 회원의 번호를 가져옵니다.
        long memberNo = member.getMemberNo();

        // 해당 회원이 작성한 게시글에 받은 좋아요 수를 조회합니다.
        int receivedLikeCount =
                memberDAO.getReceivedLikeCount(memberNo);

        // 해당 회원이 작성한 게시글에 받은 나빠요 수를 조회합니다.
        int receivedDislikeCount =
                memberDAO.getReceivedDislikeCount(memberNo);

        // =====================================================
        // 4. JSON 문자열에 사용할 값 준비
        // =====================================================

        // 회원 정보 안에 따옴표나 역슬래시가 들어 있을 수 있으므로
        // JSON 문자열에 넣기 전에 특수문자를 처리합니다.
        String name = escapeJson(member.getName());
        String username = escapeJson(member.getUsername());
        String phone = escapeJson(member.getPhone());
        String nickname = escapeJson(member.getNickname());

        // =====================================================
        // 5. JSON 응답 생성
        // =====================================================

        String json =
                "{"
                        + "\"memberNo\":" + memberNo + ","
                        + "\"name\":\"" + name + "\","
                        + "\"username\":\"" + username + "\","
                        + "\"phone\":\"" + phone + "\","
                        + "\"nickname\":\"" + nickname + "\","
                        + "\"userLevel\":" + member.getUserLevel() + ","
                        + "\"receivedLikeCount\":" + receivedLikeCount + ","
                        + "\"receivedDislikeCount\":" + receivedDislikeCount
                        + "}";

        // =====================================================
        // 6. 클라이언트에 응답
        // =====================================================

        // JavaScript의 fetch()가 이 JSON을 받아 회원 정보 화면에 표시합니다.
        response.getWriter().write(json);
    }

    /**
     * 로그인하지 않은 경우 공통으로 사용하는 응답입니다.
     */
    private void sendUnauthorized(HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        response.getWriter().write(
                "{\"message\":\"로그인이 필요합니다.\"}"
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
                .replace("\"", "\\\"");
    }
}
