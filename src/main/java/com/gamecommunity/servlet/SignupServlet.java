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
        response.setContentType("text/html; charset=UTF-8");

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

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("필수 입력값을 모두 입력해주세요.");
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
            response.getWriter().println("이미 사용 중인 아이디입니다.");
            return;
        }

        if (memberDAO.existsByNickname(nickname)) {
            response.getWriter().println("이미 사용 중인 닉네임입니다.");
            return;
        }

        int result = memberDAO.insertMember(member);

        if (result > 0) {
            response.sendRedirect(request.getContextPath() + "/login.html");
        } else {
            response.getWriter().println("회원가입 실패");
        }
    }
}
