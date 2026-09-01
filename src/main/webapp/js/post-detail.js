// =========================================================
// 게시글 상세 페이지
// =========================================================

// 페이지가 모두 준비되면 게시글 상세 정보를 불러옵니다.
document.addEventListener("DOMContentLoaded", function () {
    loadPostDetail();
});

// =========================================================
// 게시글 상세 조회
// =========================================================
async function loadPostDetail() {
    const postId = new URLSearchParams(window.location.search).get("postId");

    if (!postId) {
        alert("게시글 번호가 없습니다.");
        return;
    }

    try {
        const response = await fetch(`post-detail?postId=${encodeURIComponent(postId)}&format=json`);

        if (response.status === 401) {
            alert("로그인이 필요합니다.");
            location.href = "login.html";
            return;
        }

        if (!response.ok) {
            throw new Error("게시글 조회 실패 : " + response.status);
        }

        const post = await response.json();
        renderPost(post);
    } catch (error) {
        console.error(error);
        alert("게시글을 불러오는 중 오류가 발생했습니다.");
    }
}

// =========================================================
// 게시글 화면 출력
// =========================================================
function renderPost(post) {
    document.title = `${post.title || "게시글 상세"} - Game Hub`;

    setText("gameName", post.gameName || "게임 커뮤니티");
    setText(
        "gameDescription",
        `${post.gameName || "게임"} 커뮤니티 게시글입니다.`
    );

    setText("postTitle", post.title);
    setText(
        "postInfo",
        `작성자: ${post.nickname || "알 수 없음"} | ` +
        `조회수: ${post.viewCount ?? 0} | ` +
        `작성일: ${post.createdAt || ""}`
    );
    setText("postContent", post.content);

    setText("likeCount", post.likeCount ?? 0);
    setText("dislikeCount", post.dislikeCount ?? 0);

    renderBoardMenu(post);
    setupButtons(post);
}

// =========================================================
// 게시판 목록
// =========================================================
function renderBoardMenu(post) {
    const boardMenu = document.getElementById("boardMenu");
    if (!boardMenu) return;

    const gameId = Number(post.gameId);
    const categoryId = Number(post.categoryId);

    const boards = [
        {
            id: gameId * 10 + 1,
            name: "자유게시판"
        },
        {
            id: gameId * 10 + 2,
            name: "질문게시판"
        },
        {
            id: gameId * 10 + 3,
            name: "공략게시판"
        }
    ];

    // DB에서 조회한 추가 게시판을 함께 표시합니다.
    if (Array.isArray(post.customBoards)) {
        post.customBoards.forEach(function (board) {
            boards.push({
                id: Number(board.categoryId),
                name: board.categoryName
            });
        });
    }

    boardMenu.innerHTML = boards.map(function (board) {
        const activeClass = board.id === categoryId ? " active" : "";

        return `
            <a
                class="post-board-link${activeClass}"
                href="game.html?gameId=${encodeURIComponent(gameId)}&categoryId=${encodeURIComponent(board.id)}"
            >
                ${escapeHtml(board.name)}
            </a>
        `;
    }).join("");
}

// =========================================================
// 버튼 이벤트 설정
// =========================================================
function setupButtons(post) {
    const listButton = document.getElementById("listButton");
    const deleteButton = document.getElementById("deleteButton");
    const likeButton = document.getElementById("likeButton");
    const dislikeButton = document.getElementById("dislikeButton");

    if (listButton) {
        listButton.addEventListener("click", function () {
            location.href =
                `game.html?gameId=${encodeURIComponent(post.gameId)}` +
                `&categoryId=${encodeURIComponent(post.categoryId)}`;
        });
    }

    if (likeButton) {
        likeButton.addEventListener("click", function () {
            recommendPost("LIKE");
        });
    }

    if (dislikeButton) {
        dislikeButton.addEventListener("click", function () {
            recommendPost("DISLIKE");
        });
    }

    if (deleteButton && post.canDelete) {
        deleteButton.hidden = false;
        deleteButton.addEventListener("click", function () {
            deletePost(post.postId);
        });
    }
}

// =========================================================
// 좋아요 / 나빠요
// =========================================================
async function recommendPost(type) {
    const postId = new URLSearchParams(window.location.search).get("postId");

    if (!postId) {
        alert("게시글 번호가 없습니다.");
        return;
    }

    try {
        const response = await fetch("post-like", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body:
                `postId=${encodeURIComponent(postId)}` +
                `&likeType=${encodeURIComponent(type)}`
        });

        if (response.status === 401) {
            alert("로그인이 필요합니다.");
            location.href = "login.html";
            return;
        }

        const data = await response.json();

        if (!data.success) {
            alert(data.message || "추천 처리에 실패했습니다.");
            return;
        }

        setText("likeCount", data.likeCount);
        setText("dislikeCount", data.dislikeCount);

        alert(
            type === "LIKE"
                ? "좋아요를 눌렀습니다."
                : "나빠요를 눌렀습니다."
        );
    } catch (error) {
        console.error(error);
        alert("추천 처리 중 오류가 발생했습니다.");
    }
}

// =========================================================
// 게시글 삭제
// =========================================================
async function deletePost(postId) {
    if (!confirm("정말 이 게시글을 삭제하시겠습니까?")) {
        return;
    }

    try {
        const response = await fetch("post-delete", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: `postId=${encodeURIComponent(postId)}`
        });

        const data = await response.json();

        if (response.status === 401) {
            alert("로그인이 필요합니다.");
            location.href = "login.html";
            return;
        }

        if (response.status === 403) {
            alert("이 게시글을 삭제할 권한이 없습니다.");
            return;
        }

        if (!data.success) {
            alert(data.message || "게시글 삭제에 실패했습니다.");
            return;
        }

        alert(data.message || "게시글이 삭제되었습니다.");
        location.href = "game.html";
    } catch (error) {
        console.error(error);
        alert("게시글 삭제 중 오류가 발생했습니다.");
    }
}

// =========================================================
// 공통 유틸
// =========================================================
function setText(id, value) {
    const element = document.getElementById(id);

    if (element) {
        element.textContent = value ?? "";
    }
}

function escapeHtml(value) {
    if (value == null) return "";

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
