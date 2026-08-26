package com.gamecommunity.servlet;

import com.gamecommunity.dao.PostDAO;
import com.gamecommunity.dao.PostLikeDAO;
import com.gamecommunity.dto.MemberDTO;
import com.gamecommunity.dto.PostDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/post-detail")
public class PostDetailServlet extends HttpServlet {

    private final PostDAO postDAO = new PostDAO();
    private final PostLikeDAO postLikeDAO = new PostLikeDAO();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        String postIdParam =
                request.getParameter("postId");

        if (postIdParam == null ||
                postIdParam.isBlank()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시글 번호가 없습니다."
            );

            return;
        }

        long postId;

        try {

            postId =
                    Long.parseLong(postIdParam);

        } catch (NumberFormatException e) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 게시글 번호입니다."
            );

            return;
        }


        // =========================
        // 게시글 조회
        // =========================

        PostDTO post =
                postDAO.findById(postId);

        if (post == null) {

            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    "게시글을 찾을 수 없습니다."
            );

            return;
        }


        // =========================
        // 좋아요 / 나빠요 개수
        // =========================

        int likeCount =
                postLikeDAO.getLikeCount(postId);

        int dislikeCount =
                postLikeDAO.getDislikeCount(postId);


        // =========================
        // HTML 시작
        // =========================

        response.getWriter().println("""
                <!DOCTYPE html>
                <html lang="ko">

                <head>

                    <meta charset="UTF-8">

                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">

                    <title>게시글 상세</title>

                    <style>

                        body {
                            font-family: Arial, sans-serif;
                            width: 800px;
                            margin: 40px auto;
                        }

                        h1 {
                            margin-bottom: 20px;
                        }

                        .info {
                            color: #666;
                            padding-bottom: 15px;
                            border-bottom: 1px solid #ddd;
                        }

                        .content {
                            min-height: 300px;
                            padding: 30px 10px;
                            white-space: pre-wrap;
                            border-bottom: 1px solid #ddd;
                        }

                        .like-area {
                            margin-top: 25px;
                            padding: 20px;
                            border: 1px solid #ddd;
                            text-align: center;
                        }

                        .like-area button {
                            padding: 10px 20px;
                            margin: 0 5px;
                            font-size: 16px;
                            cursor: pointer;
                        }

                        .like-button {
                            background-color: #e8f1ff;
                            border: 1px solid #8ab4f8;
                        }

                        .dislike-button {
                            background-color: #ffecec;
                            border: 1px solid #f08a8a;
                        }

                        .count {
                            font-weight: bold;
                            margin-left: 5px;
                        }

                        .buttons {
                            margin-top: 25px;
                        }

                        .buttons button {
                            padding: 8px 15px;
                            cursor: pointer;
                        }

                    </style>

                </head>

                <body>
                """);


        // =========================
        // 제목
        // =========================

        response.getWriter().println(
                "<h1>" +
                        escapeHtml(post.getTitle()) +
                        "</h1>"
        );


        // =========================
        // 게시글 정보
        // =========================

        response.getWriter().println(
                "<div class='info'>" +

                        "작성자: " +
                        escapeHtml(post.getUsername()) +

                        " | 조회수: " +
                        post.getViewCount() +

                        " | 작성일: " +
                        post.getCreatedAt() +

                        "</div>"
        );


        // =========================
        // 내용
        // =========================

        response.getWriter().println(
                "<div class='content'>" +

                        escapeHtml(post.getContent()) +

                        "</div>"
        );


        // =========================
        // 좋아요 / 나빠요
        // =========================

        response.getWriter().println(
                "<div class='like-area'>" +

                        "<button " +
                        "type='button' " +
                        "class='like-button' " +
                        "onclick=\"recommendPost('LIKE')\">" +

                        "👍 좋아요 " +

                        "<span id='likeCount' class='count'>" +
                        likeCount +
                        "</span>" +

                        "</button>" +


                        "<button " +
                        "type='button' " +
                        "class='dislike-button' " +
                        "onclick=\"recommendPost('DISLIKE')\">" +

                        "👎 나빠요 " +

                        "<span id='dislikeCount' class='count'>" +
                        dislikeCount +
                        "</span>" +

                        "</button>" +

                        "</div>"
        );


        // =========================
        // 목록 버튼
        // =========================

        response.getWriter().println("""
                <div class="buttons">

                    <button
                        type="button"
                        onclick="location.href='post.html'">

                        목록으로

                    </button>

                </div>
                """);


        // =========================
        // JavaScript
        // =========================

        response.getWriter().println("""
                <script>

                    function recommendPost(type) {

                        const postId =
                            new URLSearchParams(
                                location.search
                            ).get("postId");


                        if (!postId) {

                            alert(
                                "게시글 번호가 없습니다."
                            );

                            return;
                        }


                        fetch("post-like", {

                            method: "POST",

                            headers: {

                                "Content-Type":
                                    "application/x-www-form-urlencoded"

                            },

                            body:
                                "postId=" +
                                encodeURIComponent(postId) +

                                "&likeType=" +
                                encodeURIComponent(type)

                        })


                        .then(response => {

                            if (response.status === 401) {

                                alert(
                                    "로그인이 필요합니다."
                                );

                                location.href =
                                    "login.html";

                                return null;
                            }


                            return response.json();

                        })


                        .then(data => {

                            if (!data) {
                                return;
                            }


                            if (data.success) {

                                document
                                    .getElementById("likeCount")
                                    .textContent =
                                    data.likeCount;


                                document
                                    .getElementById("dislikeCount")
                                    .textContent =
                                    data.dislikeCount;


                                if (type === "LIKE") {

                                    alert(
                                        "좋아요를 눌렀습니다."
                                    );

                                } else {

                                    alert(
                                        "나빠요를 눌렀습니다."
                                    );
                                }


                            } else {

                                alert(
                                    data.message
                                );

                            }

                        })


                        .catch(error => {

                            console.error(error);

                            alert(
                                "추천 처리 중 오류가 발생했습니다."
                            );

                        });

                    }

                </script>
                """);


        response.getWriter().println("""
                </body>
                </html>
                """);
    }


    // =========================
    // HTML escape
    // =========================

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }
}

