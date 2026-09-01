package com.gamecommunity.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * 로그아웃 요청을 처리하는 Servlet입니다.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    /**
     * 로그아웃은 GET 방식으로 처리합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // 기존 세션이 있는 경우에만 가져옵니다.
        // 세션이 없더라도 새로운 세션을 만들 필요가 없기 때문에 false를 사용합니다.
        HttpSession session = request.getSession(false);

        // 로그인 세션이 존재한다면 세션을 완전히 종료합니다.
        if (session != null) {
            session.invalidate();
        }

        // 로그아웃 후 로그인 화면으로 이동합니다.
        response.sendRedirect(
                request.getContextPath() + "/login.html"
        );
    }
}
