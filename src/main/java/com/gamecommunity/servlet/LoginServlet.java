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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        MemberDTO member = memberDAO.findByUsername(username);

        if (member != null && member.getPassword().equals(password)) {

            // 🔥 탈퇴한 회원인지 확인
            if ("WITHDRAWN".equals(member.getAccountStatus())) {
                response.sendRedirect(request.getContextPath() + "/login.html?error=withdrawn");
                return;
            }

            HttpSession session = request.getSession();
            session.setAttribute("member", member);
            response.sendRedirect(request.getContextPath() + "/home.html");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/login.html?error=true");
    }
}
