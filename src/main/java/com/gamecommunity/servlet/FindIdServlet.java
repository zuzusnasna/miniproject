package com.gamecommunity.servlet;

import com.gamecommunity.dao.MemberDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/find-id")
public class FindIdServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String phone = request.getParameter("phone");

        String username = memberDAO.findUsernameByNameAndPhone(name, phone);

        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(
                username == null
                        ? "{\"success\":false}"
                        : "{\"success\":true,\"username\":\"" + escapeJson(username) + "\"}"
        );
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
