package com.gamecommunity.servlet;

import com.gamecommunity.dao.MemberDAO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 비밀번호 찾기 및 변경 요청을 처리하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 아이디 / 이름 / 전화번호를 전달받습니다.
 * 2. 회원 정보가 일치하는지 확인합니다.
 * 3. 새 비밀번호가 입력되었는지 확인합니다.
 * 4. 새 비밀번호를 DB에 저장합니다.
 * 5. 처리 결과에 따라 페이지를 이동시킵니다.
 */
@WebServlet("/find-password")
public class FindPasswordServlet extends HttpServlet {

    // 회원 정보 조회 및 비밀번호 변경을 담당합니다.
    private final MemberDAO memberDAO = new MemberDAO();

    /**
     * 회원 본인 확인 후 비밀번호를 변경합니다.
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
        // 2. 사용자가 입력한 정보 가져오기
        // =====================================================

        String username = request.getParameter("username");
        String name = request.getParameter("name");
        String phone = request.getParameter("phone");
        String newPassword = request.getParameter("newPassword");

        // =====================================================
        // 3. 회원 본인 확인
        // =====================================================

        boolean verified = memberDAO.existsByUsernameNameAndPhone(
                username,
                name,
                phone
        );

        // 입력한 회원 정보가 DB와 일치하지 않으면 다시 입력받습니다.
        if (!verified) {
            redirect(
                    request,
                    response,
                    "/find-password.html?error=true"
            );
            return;
        }

        // =====================================================
        // 4. 새 비밀번호 입력 여부 확인
        // =====================================================

        // 아직 새 비밀번호를 입력하지 않았다면 인증 완료 상태로 이동합니다.
        if (newPassword == null || newPassword.isBlank()) {
            redirect(
                    request,
                    response,
                    "/find-password.html?verified=true"
            );
            return;
        }

        // =====================================================
        // 5. 비밀번호 변경
        // =====================================================

        boolean updated = memberDAO.updatePassword(
                username,
                newPassword
        );

        // =====================================================
        // 6. 처리 결과에 따른 페이지 이동
        // =====================================================

        if (updated) {
            // 비밀번호 변경 성공 → 로그인 페이지로 이동합니다.
            redirect(
                    request,
                    response,
                    "/login.html?passwordChanged=true"
            );
            return;
        }

        // 비밀번호 변경 실패 → 다시 비밀번호 찾기 페이지로 이동합니다.
        redirect(
                request,
                response,
                "/find-password.html?updateError=true"
        );
    }

    /**
     * 지정한 경로로 이동합니다.
     *
     * contextPath를 붙여서 프로젝트의 배포 경로가 달라져도
     * 정상적으로 이동할 수 있도록 합니다.
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
