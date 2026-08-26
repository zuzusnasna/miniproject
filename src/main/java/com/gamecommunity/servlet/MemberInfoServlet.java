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

@WebServlet("/member-info")
public class MemberInfoServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            response.getWriter().write(
                    "{\"message\":\"로그인이 필요합니다.\"}"
            );

            return;
        }

        MemberDTO member =
                (MemberDTO) session.getAttribute("member");

        if (member == null) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            response.getWriter().write(
                    "{\"message\":\"로그인이 필요합니다.\"}"
            );

            return;
        }

        long memberNo = member.getMemberNo();

        int receivedLikeCount =
                memberDAO.getReceivedLikeCount(memberNo);

        int receivedDislikeCount =
                memberDAO.getReceivedDislikeCount(memberNo);

        String name =
                escapeJson(member.getName());

        String username =
                escapeJson(member.getUsername());

        String phone =
                escapeJson(member.getPhone());

        String nickname =
                escapeJson(member.getNickname());

        String json =
                "{"
                        + "\"memberNo\":" + memberNo + ","
                        + "\"name\":\"" + name + "\","
                        + "\"username\":\"" + username + "\","
                        + "\"phone\":\"" + phone + "\","
                        + "\"nickname\":\"" + nickname + "\","
                        + "\"userLevel\":" + member.getUserLevel() + ","
                        + "\"receivedLikeCount\":" + receivedLikeCount + ","
                        + "\"receivedDislikeCount\":" + receivedDislikeCount
                        + "}";

        response.getWriter().write(json);
    }


    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}