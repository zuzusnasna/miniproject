package com.gamecommunity.servlet;

import com.gamecommunity.dao.CategoryDAO;
import com.gamecommunity.dao.CategoryManagerDAO;
import com.gamecommunity.dto.CategoryDTO;
import com.gamecommunity.dto.MemberDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
//--------카테고리 관리자 권한(게시탭 생성)
@WebServlet("/category-create")
public class CategoryCreateServlet extends HttpServlet {

    private final CategoryManagerDAO managerDAO = new CategoryManagerDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("member") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\": false, \"message\": \"로그인이 필요합니다.\"}");
            return;
        }

        MemberDTO loginMember = (MemberDTO) session.getAttribute("member");
        long memberNo = loginMember.getMemberNo();

        String categoryName = request.getParameter("categoryName");

        if (categoryName == null || categoryName.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"생성할 게시판 이름을 입력해주세요.\"}");
            return;
        }

        long parentId = managerDAO.getManagedCategoryId(memberNo);

        if (parentId == -1) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\": false, \"message\": \"카테고리 관리자 권한이 없습니다.\"}");
            return;
        }

        if (categoryDAO.existsByCategoryNameAndParent(categoryName, parentId)) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write("{\"success\": false, \"message\": \"이미 동일한 이름의 게시판이 존재합니다.\"}");
            return;
        }

        int result = categoryDAO.insertSubCategory(categoryName, parentId);

        // 7. 결과 응답 분기 처리
        if (result == 1) {
            // 성공
            response.getWriter().write("{\"success\": true, \"message\": \"게시판 생성 요청을 완료했습니다.\"}");
        } else if (result == -1) {
            // 9개 초과 제한
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"게시판은 최대 9개까지만 생성할 수 있습니다.\"}");
        } else {
            // 기타 서버 에러
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\": false, \"message\": \"게시판 생성에 실패했습니다.\"}");
        }
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");

        HttpSession session = request.getSession(false);
        MemberDTO loginMember = session == null ? null : (MemberDTO) session.getAttribute("member");

        if (loginMember == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"isManager\": false}");
            return;
        }

        long parentId = managerDAO.getManagedCategoryId(loginMember.getMemberNo());

        if (parentId == -1) {
            // 카테고리 관리자가 아님
            response.getWriter().write("{\"isManager\": false}");
        } else {
            // 🔥 DB에서 게임 ID(예: 310)의 실제 CATEGORY_NAME 조회
            CategoryDTO game = categoryDAO.findById(parentId);
            String gameName = (game != null && game.getCategoryName() != null)
                    ? game.getCategoryName()
                    : "게임 ID: " + parentId;

            // 🔥 JSON으로 gameName도 함께 응답
            response.getWriter().write(String.format(
                    "{\"isManager\": true, \"categoryId\": %d, \"gameName\": \"%s\"}",
                    parentId,
                    gameName.replace("\"", "\\\"")));
        }
    }
}