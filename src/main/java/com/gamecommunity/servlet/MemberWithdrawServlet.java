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
// 회원 탈퇴
@WebServlet("/member-withdraw")
public class MemberWithdrawServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");

        // 1. 로그인 확인
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("member") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\": false, \"message\": \"로그인이 필요합니다.\"}");
            return;
        }

        MemberDTO sessionMember = (MemberDTO) session.getAttribute("member");

        // 2. DB 업데이트 (탈퇴 처리)
        boolean result = memberDAO.withdrawMember(sessionMember.getMemberNo());

        if (result) {
            // 3. 성공 시 세션 파기 (강제 로그아웃)
            session.invalidate();
            response.getWriter().write("{\"success\": true, \"message\": \"탈퇴 처리가 완료되었습니다.\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\": false, \"message\": \"탈퇴 처리 중 오류가 발생했습니다.\"}");
        }
    }
}