package com.gamecommunity.servlet;

import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dto.MemberDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 회원가입 요청을 처리하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 요청 인코딩 설정
 * 2. 회원가입 정보 가져오기
 * 3. 필수 입력값 확인
 * 4. 회원 DTO 생성
 * 5. 아이디 중복 확인
 * 6. 닉네임 중복 확인
 * 7. 회원가입 처리
 * 8. 결과에 따라 페이지 이동
 */
@WebServlet("/signup")
public class SignupServlet extends HttpServlet {

    // 회원 정보 DB 작업을 담당합니다.
    private final MemberDAO memberDAO = new MemberDAO();

    /**
     * 회원가입을 처리합니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // =====================================================
        // 1. 요청 인코딩 설정
        // =====================================================

        request.setCharacterEncoding("UTF-8");

        // =====================================================
        // 2. 회원가입 정보 가져오기
        // =====================================================

        String name = request.getParameter("name");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String nickname = request.getParameter("nickname");
        String phone = request.getParameter("phone");

        // =====================================================
        // 3. 필수 입력값 확인
        // =====================================================

        if (!hasRequiredValues(
                name,
                username,
                password,
                nickname,
                phone
        )) {
            redirect(
                    request,
                    response,
                    "/signup.html?error=required"
            );
            return;
        }

        // =====================================================
        // 4. 회원 DTO 생성
        // =====================================================

        MemberDTO member = createMember(
                name,
                username,
                password,
                nickname,
                phone
        );

        // =====================================================
        // 5. 아이디 중복 확인
        // =====================================================

        if (memberDAO.existsByUsername(username)) {
            redirect(
                    request,
                    response,
                    "/signup.html?error=username"
            );
            return;
        }

        // =====================================================
        // 6. 닉네임 중복 확인
        // =====================================================

        if (memberDAO.existsByNickname(nickname)) {
            redirect(
                    request,
                    response,
                    "/signup.html?error=nickname"
            );
            return;
        }

        // =====================================================
        // 7. 회원가입 처리
        // =====================================================

        int result = memberDAO.insertMember(member);

        // =====================================================
        // 8. 처리 결과에 따른 페이지 이동
        // =====================================================

        if (result > 0) {
            // 회원가입 성공 → 로그인 페이지로 이동합니다.
            redirect(
                    request,
                    response,
                    "/login.html?signup=success"
            );
            return;
        }

        // 회원가입 실패 → 회원가입 페이지로 돌아갑니다.
        redirect(
                request,
                response,
                "/signup.html?error=fail"
        );
    }

    /**
     * 회원가입에 필요한 값이 모두 입력되었는지 확인합니다.
     */
    private boolean hasRequiredValues(
            String name,
            String username,
            String password,
            String nickname,
            String phone
    ) {

        return name != null && !name.isBlank()
                && username != null && !username.isBlank()
                && password != null && !password.isBlank()
                && nickname != null && !nickname.isBlank()
                && phone != null && !phone.isBlank();
    }

    /**
     * 입력받은 회원 정보를 MemberDTO에 담습니다.
     */
    private MemberDTO createMember(
            String name,
            String username,
            String password,
            String nickname,
            String phone
    ) {

        MemberDTO member = new MemberDTO();

        member.setName(name);
        member.setUsername(username);
        member.setPassword(password);
        member.setNickname(nickname);
        member.setPhone(phone);

        return member;
    }

    /**
     * 프로젝트의 contextPath를 포함해서 페이지를 이동합니다.
     */
    private void redirect(
            HttpServletRequest request,
            HttpServletResponse response,
            String path
    ) throws IOException {

        response.sendRedirect(
                request.getContextPath() + path
        );
    }
}
