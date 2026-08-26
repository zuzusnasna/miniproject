package com.gamecommunity.servlet;

import com.gamecommunity.dao.PostDAO;
import com.gamecommunity.dto.PostDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/posts")
public class PostListServlet extends HttpServlet {

    private final PostDAO postDAO = new PostDAO();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");

        List<PostDTO> postList = postDAO.findAll();

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < postList.size(); i++) {

            PostDTO post = postList.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                    .append("\"postId\":").append(post.getPostId()).append(",")
                    .append("\"categoryId\":").append(post.getCategoryId()).append(",")
                    .append("\"memberNo\":").append(post.getMemberNo()).append(",")
                    .append("\"username\":\"").append(escape(post.getUsername())).append("\",")
                    .append("\"title\":\"").append(escape(post.getTitle())).append("\",")
                    .append("\"content\":\"").append(escape(post.getContent())).append("\",")
                    .append("\"viewCount\":").append(post.getViewCount()).append(",")
                    .append("\"likeCount\":").append(post.getLikeCount()).append(",")
                    .append("\"dislikeCount\":").append(post.getDislikeCount()).append(",")
                    .append("\"createdAt\":\"").append(escape(post.getCreatedAt())).append("\"")
                    .append("}");
        }

        json.append("]");

        response.getWriter().write(json.toString());
    }


    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}