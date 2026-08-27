package com.gamecommunity.servlet;

import com.gamecommunity.dao.MemberDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/find-password")
public class FindPasswordServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String name = request.getParameter("name");
        String phone = request.getParameter("phone");
        String newPassword = request.getParameter("newPassword");

        boolean verified = memberDAO.existsByUsernameNameAndPhone(
                username, name, phone);

        if (!verified) {
            response.sendRedirect(request.getContextPath()
                    + "/find-password.html?error=true");
            return;
        }

        if (newPassword == null || newPassword.isBlank()) {
            response.sendRedirect(request.getContextPath()
                    + "/find-password.html?verified=true");
            return;
        }

        boolean updated = memberDAO.updatePassword(username, newPassword);

        if (updated) {
            response.sendRedirect(request.getContextPath()
                    + "/login.html?passwordChanged=true");
        } else {
            response.sendRedirect(request.getContextPath()
                    + "/find-password.html?updateError=true");
        }
    }
}
