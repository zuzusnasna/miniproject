package com.gamecommunity.servlet;

import com.gamecommunity.dao.MemberDAO;
import com.gamecommunity.dto.MemberDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * 회원 정보 수정 요청을 처리하는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 로그인 여부 확인
 * 2. 수정할 회원 정보 가져오기
 * 3. 필수 입력값 확인
 * 4. 닉네임 중복 확인
 * 5. 회원 정보 수정
 * 6. 세션 정보 갱신
 * 7. 결과 반환
 */
@WebServlet("/member-update")
public class MemberUpdateServlet extends HttpServlet {

    // 회원 정보 DB 작업을 담당합니다.
    private final MemberDAO memberDAO = new MemberDAO();

    /**
     * 로그인한 회원의 정보를 수정합니다.
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
        // 2. 로그인 여부 확인
        // =====================================================

        HttpSession session = request.getSession(false);
        MemberDTO sessionMember = getLoginMember(session);

        if (sessionMember == null) {
            // 로그인하지 않은 경우 로그인 페이지로 이동합니다.
            response.sendRedirect(
                    request.getContextPath() + "/login.html"
            );
            return;
        }

        // =====================================================
        // 3. 수정할 회원 정보 가져오기
        // =====================================================

        String name = request.getParameter("name");
        String password = request.getParameter("password");
        String nickname = request.getParameter("nickname");
        String phone = request.getParameter("phone");

        // =====================================================
        // 4. 필수 입력값 확인
        // =====================================================

        if (!hasRequiredValues(name, nickname, phone)) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "필수 입력값을 모두 입력해주세요."
            );
            return;
        }

        // =====================================================
        // 5. 닉네임 중복 확인
        // =====================================================

        boolean nicknameDuplicated =
                memberDAO.existsByNicknameExceptMember(
                        nickname,
                        sessionMember.getMemberNo()
                );

        if (nicknameDuplicated) {
            response.sendError(
                    HttpServletResponse.SC_CONFLICT,
                    "이미 사용 중인 닉네임입니다."
            );
            return;
        }

        // =====================================================
        // 6. 회원 정보 수정
        // =====================================================

        updateMember(
                sessionMember,
                name,
                password,
                nickname,
                phone
        );

        boolean updated = memberDAO.updateMember(sessionMember);

        // =====================================================
        // 7. 처리 결과 반환
        // =====================================================

        if (updated) {
            // DB 수정이 성공하면 세션의 회원 정보도 최신 정보로 변경합니다.
            session.setAttribute("member", sessionMember);

            response.sendRedirect(
                    request.getContextPath()
                            + "/mypage.html?success=true"
            );
            return;
        }

        response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "정보 수정에 실패했습니다."
        );
    }

    /**
     * 세션에서 로그인한 회원 정보를 가져옵니다.
     */
    private MemberDTO getLoginMember(HttpSession session) {

        if (session == null) {
            return null;
        }

        return (MemberDTO) session.getAttribute("member");
    }

    /**
     * 회원 정보 수정에 필요한 값이 모두 입력되었는지 확인합니다.
     */
    private boolean hasRequiredValues(
            String name,
            String nickname,
            String phone
    ) {

        return name != null && !name.isBlank()
                && nickname != null && !nickname.isBlank()
                && phone != null && !phone.isBlank();
    }

    /**
     * 세션에 저장된 회원 객체에 수정된 정보를 반영합니다.
     *
     * 비밀번호를 입력하지 않은 경우 기존 비밀번호를 유지합니다.
     */
    private void updateMember(
            MemberDTO member,
            String name,
            String password,
            String nickname,
            String phone
    ) {

        member.setName(name);

        if (password != null && !password.isBlank()) {
            member.setPassword(password);
        }

        member.setNickname(nickname);
        member.setPhone(phone);
    }
}
