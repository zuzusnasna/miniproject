package com.gamecommunity.servlet;

import com.gamecommunity.dao.PostDAO;
import com.gamecommunity.dao.PostLikeDAO;
import com.gamecommunity.dto.PostDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/post-detail")
public class PostDetailServlet extends HttpServlet {

    private final PostDAO postDAO = new PostDAO();
    private final PostLikeDAO postLikeDAO = new PostLikeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        String postIdParam = request.getParameter("postId");
        if (postIdParam == null || postIdParam.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "게시글 번호가 없습니다.");
            return;
        }

        long postId;
        try {
            postId = Long.parseLong(postIdParam);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "잘못된 게시글 번호입니다.");
            return;
        }

        postDAO.increaseViewCount(postId);
        PostDTO post = postDAO.findById(postId);
        if (post == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "게시글을 찾을 수 없습니다.");
            return;
        }

        long categoryId = post.getCategoryId() == null ? 0L : post.getCategoryId();
        long gameId = categoryId >= 1000 ? categoryId / 10 : categoryId;
        String gameName = getGameName(gameId);
        String genreName = getGenreName(gameId);

        int likeCount = postLikeDAO.getLikeCount(postId);
        int dislikeCount = postLikeDAO.getDislikeCount(postId);

        response.getWriter().println("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>게시글 상세 - Game Hub</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
                    <link rel="stylesheet" href="css/gamehub.css">
                    <style>
                        body { margin:0; font-family:Arial,sans-serif; background:#f8f9fa; }
                        .post-detail-wrap { width:100%; max-width:1100px; margin:32px auto; padding:0 16px; box-sizing:border-box; }
                        .post-context-banner { margin-bottom:16px; padding:22px 26px; border-radius:12px; background:linear-gradient(135deg,#352064,#6941c6); color:#fff; box-shadow:0 3px 12px rgba(53,32,100,.12); }
                        .post-context-banner .genre { display:block; margin-bottom:4px; font-size:.78rem; font-weight:700; letter-spacing:.04em; opacity:.72; }
                        .post-context-banner .game-name { margin:0; font-size:1.55rem; font-weight:800; }
                        .post-context-banner .description { margin:5px 0 0; font-size:.92rem; opacity:.84; }
                        .post-community-layout { display:grid; grid-template-columns:200px minmax(0,1fr); gap:16px; align-items:start; }
                        .post-board-sidebar { background:#f8f7fb; border:1px solid #ece9f3; border-radius:12px; padding:18px 12px; }
                        .post-board-sidebar h3 { font-size:.9rem; font-weight:800; color:#777; padding:0 10px 10px; margin:0; }
                        .post-board-link { display:block; width:100%; padding:12px 14px; margin-bottom:5px; border-radius:8px; color:#333; font-weight:700; text-decoration:none; }
                        .post-board-link:hover, .post-board-link.active { background:#6941c6; color:#fff; }
                        .post-detail-card { background:#fff; border:1px solid #e9ecef; border-radius:12px; padding:28px; box-shadow:0 3px 12px rgba(0,0,0,.05); }
                        .post-detail-card h1 { margin-bottom:16px; font-size:1.8rem; font-weight:800; }
                        .info { color:#666; padding-bottom:15px; border-bottom:1px solid #ddd; }
                        .content { min-height:300px; padding:30px 10px; white-space:pre-wrap; border-bottom:1px solid #ddd; }
                        .like-area { margin-top:25px; padding:20px; border:1px solid #ddd; border-radius:8px; text-align:center; }
                        .like-area button { padding:10px 20px; margin:0 5px; font-size:16px; cursor:pointer; border-radius:6px; }
                        .like-button { background-color:#e8f1ff; border:1px solid #8ab4f8; }
                        .dislike-button { background-color:#ffecec; border:1px solid #f08a8a; }
                        .count { font-weight:bold; margin-left:5px; }
                        .buttons { margin-top:25px; text-align:right; }
                        @media(max-width:800px) { .post-community-layout { grid-template-columns:1fr; } .post-board-sidebar { display:flex; gap:6px; overflow:auto; } .post-board-sidebar h3 { display:none; } .post-board-link { white-space:nowrap; width:auto; margin:0; } }
                    </style>
                </head>
                <body>
                <main class="post-detail-wrap">
                """);

        response.getWriter().println(
                "<section class='post-context-banner'>" +
                        "<span class='genre'>" + escapeHtml(genreName) + " COMMUNITY</span>" +
                        "<h2 class='game-name'>" + escapeHtml(gameName) + "</h2>" +
                        "<p class='description'>" + escapeHtml(gameName) + " 커뮤니티 게시글입니다.</p>" +
                        "</section>"
        );

        response.getWriter().println("<div class='post-community-layout'>");
        response.getWriter().println("<aside class='post-board-sidebar'><h3>게시판</h3>" +
                boardLink(gameId, gameId * 10 + 1, categoryId, "자유게시판") +
                boardLink(gameId, gameId * 10 + 2, categoryId, "질문게시판") +
                boardLink(gameId, gameId * 10 + 3, categoryId, "공략게시판") +
                "</aside>");

        response.getWriter().println("<div class='post-detail-card'>");
        response.getWriter().println("<h1>" + escapeHtml(post.getTitle()) + "</h1>");
        response.getWriter().println(
                "<div class='info'>작성자: " + escapeHtml(post.getUsername()) +
                        " | 조회수: " + post.getViewCount() +
                        " | 작성일: " + post.getCreatedAt() + "</div>"
        );
        response.getWriter().println("<div class='content'>" + escapeHtml(post.getContent()) + "</div>");
        response.getWriter().println(
                "<div class='like-area'>" +
                        "<button type='button' class='like-button' onclick=\"recommendPost('LIKE')\">👍 좋아요 <span id='likeCount' class='count'>" + likeCount + "</span></button>" +
                        "<button type='button' class='dislike-button' onclick=\"recommendPost('DISLIKE')\">👎 나빠요 <span id='dislikeCount' class='count'>" + dislikeCount + "</span></button>" +
                        "</div>"
        );

        response.getWriter().println("""
                    <div class="buttons">
                        <button type="button" class="btn btn-outline-secondary" onclick="history.back()">목록으로</button>
                    </div>
                    </div>
                    </div>
                </main>

                <script src="js/common.js"></script>
                <script src="js/comment.js"></script>
                <script>
                    function recommendPost(type) {
                        const postId = new URLSearchParams(location.search).get("postId");
                        if (!postId) { alert("게시글 번호가 없습니다."); return; }
                        fetch("post-like", {
                            method: "POST",
                            headers: { "Content-Type": "application/x-www-form-urlencoded" },
                            body: "postId=" + encodeURIComponent(postId) + "&likeType=" + encodeURIComponent(type)
                        })
                        .then(response => {
                            if (response.status === 401) { alert("로그인이 필요합니다."); location.href = "login.html"; return null; }
                            return response.json();
                        })
                        .then(data => {
                            if (!data) return;
                            if (data.success) {
                                document.getElementById("likeCount").textContent = data.likeCount;
                                document.getElementById("dislikeCount").textContent = data.dislikeCount;
                                alert(type === "LIKE" ? "좋아요를 눌렀습니다." : "나빠요를 눌렀습니다.");
                            } else alert(data.message);
                        })
                        .catch(error => { console.error(error); alert("추천 처리 중 오류가 발생했습니다."); });
                    }
                </script>
                </body>
                </html>
                """);
    }

    private String boardLink(long gameId, long boardId, long currentCategoryId, String name) {
        String activeClass = boardId == currentCategoryId ? " active" : "";
        return "<a class='post-board-link" + activeClass + "' href='game.html?gameId=" + gameId + "&categoryId=" + boardId + "'>" + escapeHtml(name) + "</a>";
    }

    private String getGameName(long gameId) {
        return switch ((int) gameId) {
            case 110 -> "리니지";
            case 120 -> "블레이드앤소울";
            case 130 -> "메이플스토리";
            case 140 -> "로스트아크";
            case 210 -> "서든어택";
            case 220 -> "오버워치";
            case 230 -> "발로란트";
            case 240 -> "배틀그라운드";
            case 310 -> "리그 오브 레전드";
            case 320 -> "도타 2";
            case 410 -> "FC 온라인";
            case 420 -> "eFootball";
            case 430 -> "NBA 2K";
            case 510 -> "스타크래프트";
            case 520 -> "문명 VI";
            case 530 -> "에이지 오브 엠파이어 IV";
            case 610 -> "심즈 4";
            case 620 -> "시티즈: 스카이라인 II";
            case 630 -> "유로 트럭 시뮬레이터 2";
            case 910 -> "마인크래프트";
            case 920 -> "GTA V";
            case 930 -> "철권 8";
            case 940 -> "포르자 호라이즌 5";
            case 950 -> "데드 바이 데이라이트";
            case 960 -> "몬스터헌터 와일즈";
            default -> "게임 커뮤니티";
        };
    }

    private String getGenreName(long gameId) {
        long genreId = (gameId / 100) * 100;
        return switch ((int) genreId) {
            case 100 -> "RPG";
            case 200 -> "FPS/TPS";
            case 300 -> "MOBA";
            case 400 -> "스포츠";
            case 500 -> "전략";
            case 600 -> "시뮬레이션";
            case 900 -> "그 외 장르";
            default -> "GAME";
        };
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#039;");
    }
}
