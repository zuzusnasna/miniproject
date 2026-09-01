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
 * 로그인 요청을 처리하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 로그인 화면에서 username, password를 전달받습니다.
 * 2. MemberDAO를 통해 회원 정보를 조회합니다.
 * 3. 비밀번호와 회원 상태를 확인합니다.
 * 4. 로그인에 성공하면 세션에 회원 정보를 저장합니다.
 * 5. 홈 화면으로 이동합니다.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    // 회원 정보를 DB에서 조회하기 위해 MemberDAO를 사용합니다.
    private final MemberDAO memberDAO = new MemberDAO();

    /**
     * 로그인 폼에서 POST 방식으로 전달된 요청을 처리합니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // 한글이 포함된 요청 데이터를 정상적으로 읽기 위해 UTF-8을 설정합니다.
        request.setCharacterEncoding("UTF-8");

        // 로그인 화면에서 입력한 아이디와 비밀번호를 가져옵니다.
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // 입력한 아이디로 회원 정보를 조회합니다.
        MemberDTO member = memberDAO.findByUsername(username);

        // 회원이 존재하고 입력한 비밀번호가 DB의 비밀번호와 같은지 확인합니다.
        if (member != null && member.getPassword().equals(password)) {

            // -------------------------------------------------
            // 탈퇴한 회원인지 확인
            // -------------------------------------------------
            if ("WITHDRAWN".equals(member.getAccountStatus())) {
                response.sendRedirect(
                        request.getContextPath()
                                + "/login.html?error=withdrawn"
                );
                return;
            }

            // -------------------------------------------------
            // 로그인 성공
            // -------------------------------------------------
            // 세션을 생성하고 로그인한 회원 정보를 저장합니다.
            HttpSession session = request.getSession();
            session.setAttribute("member", member);

            // 로그인 후 홈 화면으로 이동합니다.
            response.sendRedirect(
                    request.getContextPath() + "/home.html"
            );
            return;
        }

        // -------------------------------------------------
        // 로그인 실패
        // -------------------------------------------------
        // 아이디가 없거나 비밀번호가 틀린 경우 로그인 화면으로 돌아갑니다.
        response.sendRedirect(
                request.getContextPath() + "/login.html?error=true"
        );
    }
}
