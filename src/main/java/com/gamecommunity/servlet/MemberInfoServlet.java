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
 * 회원 정보 창에서 필요한 회원 기본 정보와
 * 받은 좋아요 / 싫어요 개수를 JSON으로 반환합니다.
 */
@WebServlet("/member-info")
public class MemberInfoServlet extends HttpServlet {

    // 회원 정보와 추천 수를 조회하기 위해 MemberDAO를 사용합니다.
    private final MemberDAO memberDAO = new MemberDAO();

    /**
     * 로그인한 회원의 정보를 조회합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // 응답을 JSON 형식으로 설정합니다.
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 기존 로그인 세션이 있는지 확인합니다.
        HttpSession session = request.getSession(false);

        // 세션 자체가 없다면 로그인하지 않은 상태입니다.
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                    "{\"message\":\"로그인이 필요합니다.\"}"
            );
            return;
        }

        // 세션에 저장해 둔 로그인 회원 정보를 가져옵니다.
        MemberDTO member =
                (MemberDTO) session.getAttribute("member");

        // 세션은 있지만 회원 정보가 없다면 역시 로그인하지 않은 상태입니다.
        if (member == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                    "{\"message\":\"로그인이 필요합니다.\"}"
            );
            return;
        }

        // -------------------------------------------------
        // 회원 정보 조회
        // -------------------------------------------------
        long memberNo = member.getMemberNo();

        // 해당 회원이 작성한 게시글에 받은 추천 수를 조회합니다.
        int receivedLikeCount =
                memberDAO.getReceivedLikeCount(memberNo);

        int receivedDislikeCount =
                memberDAO.getReceivedDislikeCount(memberNo);

        // JSON 문자열에 넣기 전에 특수문자를 처리합니다.
        String name = escapeJson(member.getName());
        String username = escapeJson(member.getUsername());
        String phone = escapeJson(member.getPhone());
        String nickname = escapeJson(member.getNickname());

        // -------------------------------------------------
        // JSON 응답 생성
        // -------------------------------------------------
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

        // 완성된 회원 정보를 클라이언트에 전달합니다.
        response.getWriter().write(json);
    }

    /**
     * JSON 문자열에서 문제가 될 수 있는 문자를 이스케이프합니다.
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
