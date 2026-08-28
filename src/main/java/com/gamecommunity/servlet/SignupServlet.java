package com.gamecommunity.servlet;

import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dto.MemberDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/signup")
public class SignupServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String nickname = request.getParameter("nickname");
        String phone = request.getParameter("phone");

        if (name == null || name.isBlank()
                || username == null || username.isBlank()
                || password == null || password.isBlank()
                || nickname == null || nickname.isBlank()
                || phone == null || phone.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/signup.html?error=required");
            return;
        }

        MemberDTO member = new MemberDTO();
        member.setName(name);
        member.setUsername(username);
        member.setPassword(password);
        member.setNickname(nickname);
        member.setPhone(phone);

        MemberDAO memberDAO = new MemberDAO();

        if (memberDAO.existsByUsername(username)) {
            response.sendRedirect(request.getContextPath() + "/signup.html?error=username");
            return;
        }

        if (memberDAO.existsByNickname(nickname)) {
            response.sendRedirect(request.getContextPath() + "/signup.html?error=nickname");
            return;
        }

        int result = memberDAO.insertMember(member);

        if (result > 0) {
            response.sendRedirect(request.getContextPath() + "/login.html?signup=success");
        } else {
            response.sendRedirect(request.getContextPath() + "/signup.html?error=fail");
        }
    }
}
