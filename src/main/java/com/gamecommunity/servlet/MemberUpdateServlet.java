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

@WebServlet("/member-update")
public class MemberUpdateServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("member") == null) {
            response.sendRedirect(request.getContextPath() + "/login.html");
            return;
        }

        MemberDTO sessionMember = (MemberDTO) session.getAttribute("member");

        String name = request.getParameter("name");
        String password = request.getParameter("password");
        String nickname = request.getParameter("nickname");
        String phone = request.getParameter("phone");

        sessionMember.setName(name);
        if (password != null && !password.isBlank()) {
            sessionMember.setPassword(password);
        }
        sessionMember.setNickname(nickname);
        sessionMember.setPhone(phone);

        if (memberDAO.updateMember(sessionMember)) {
            session.setAttribute("member", sessionMember);
            response.sendRedirect(request.getContextPath() + "/mypage.html?success=true");
            return;
        }

        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "정보 수정에 실패했습니다.");
    }
}
