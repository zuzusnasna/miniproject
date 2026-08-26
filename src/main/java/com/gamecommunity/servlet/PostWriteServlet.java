package com.gamecommunity.servlet;

import com.gamecommunity.dao.PostDAO;
import com.gamecommunity.dto.MemberDTO;
import com.gamecommunity.dto.PostDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/post-write")
public class PostWriteServlet extends HttpServlet {

    private final PostDAO postDAO = new PostDAO();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("========== POST-WRITE 요청 들어옴 ==========");

        request.setCharacterEncoding("UTF-8");

        System.out.println("1. 인코딩 완료");

        HttpSession session = request.getSession(false);

        System.out.println("2. 세션 = " + session);

        if (session == null) {
            System.out.println("세션 없음");

            response.sendRedirect(
                    request.getContextPath() + "/login.html"
            );
            return;
        }

        MemberDTO member =
                (MemberDTO) session.getAttribute("member");

        System.out.println("3. 회원 = " + member);

        if (member == null) {
            System.out.println("회원 정보 없음");

            response.sendRedirect(
                    request.getContextPath() + "/login.html"
            );
            return;
        }

        System.out.println("4. 회원번호 = " + member.getMemberNo());

        String title = request.getParameter("title");
        String content = request.getParameter("content");

        System.out.println("5. 제목 = " + title);
        System.out.println("6. 내용 = " + content);

        PostDTO post = new PostDTO();

        post.setCategoryId(1L);
        post.setMemberNo(member.getMemberNo());
        post.setTitle(title);
        post.setContent(content);

        System.out.println("7. PostDTO 생성 완료");
        System.out.println("8. PostDAO.save() 호출 직전");

        boolean result = postDAO.save(post);

        System.out.println("9. PostDAO.save() 결과 = " + result);

        if (result) {

            System.out.println("10. 게시글 작성 성공");

            response.sendRedirect(
                    request.getContextPath() + "/post.html"
            );

        } else {

            System.out.println("10. 게시글 작성 실패");

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "게시글 작성에 실패했습니다."
            );
        }
    }
}