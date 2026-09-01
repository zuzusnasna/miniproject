package com.gamecommunity.servlet;

import com.gamecommunity.dao.MemberDAO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 아이디 찾기 요청을 처리하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 이름과 전화번호를 전달받습니다.
 * 2. 회원 정보를 DB에서 조회합니다.
 * 3. 조회 결과를 JSON으로 반환합니다.
 */
@WebServlet("/find-id")
public class FindIdServlet extends HttpServlet {

    // 회원 DB 조회를 담당합니다.
    private final MemberDAO memberDAO = new MemberDAO();

    /**
     * 이름과 전화번호를 이용해 아이디를 찾습니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // =====================================================
        // 1. 요청 / 응답 인코딩 설정
        // =====================================================

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // =====================================================
        // 2. 사용자가 입력한 정보 가져오기
        // =====================================================

        String name = request.getParameter("name");
        String phone = request.getParameter("phone");

        // =====================================================
        // 3. DB에서 아이디 조회
        // =====================================================

        String username = memberDAO.findUsernameByNameAndPhone(
                name,
                phone
        );

        // =====================================================
        // 4. 조회 결과 JSON 생성
        // =====================================================

        String json;

        if (username == null) {
            // 일치하는 회원을 찾지 못한 경우입니다.
            json = "{\"success\":false}";
        } else {
            // 조회된 아이디를 JSON에 담습니다.
            json = "{\"success\":true,\"username\":\""
                    + escapeJson(username)
                    + "\"}";
        }

        // =====================================================
        // 5. JSON 응답 반환
        // =====================================================

        response.getWriter().write(json);
    }

    /**
     * JSON 문자열에 문제가 생기지 않도록 특수문자를 처리합니다.
     */
    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
