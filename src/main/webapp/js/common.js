document.addEventListener("DOMContentLoaded", async function () {
    const currentPath = window.location.pathname;
    if (isPage("login.html") || isPage("signup.html")) return;

    try {
        await loadCommonModules();
        initLayout();

        const categoryList = document.querySelector(".category-list");
        if (categoryList) loadGameCategories(categoryList);

        initMemberInfoBox();

        // 마이페이지에서는 카테고리 관리자 신청 관리 영역을 초기화합니다.
        if (isPage("mypage.html") && typeof initCategoryManagerRequestAdmin === "function") {
            initCategoryManagerRequestAdmin();
        }

        // 게임 페이지에서는 해당 게임의 카테고리 관리자에게만
        // "게시판 추가 신청" 버튼을 보여줍니다.
        if (isPage("game.html")) {
            initCategoryBoardCreateButton();
        }

        if (isEndpoint("post-detail")) {
            initPostEditButton();
        }
    } catch (error) {
        console.error("공통 JS 모듈 로딩 실패:", error);
    }
});

/**
 * 현재 페이지가 지정한 HTML 페이지인지 확인합니다.
 */
function isPage(pageName) {
    const currentPath = window.location.pathname;
    return currentPath.endsWith("/" + pageName) || currentPath.endsWith(pageName);
}

/**
 * 현재 요청 경로가 지정한 Servlet 엔드포인트인지 확인합니다.
 */
function isEndpoint(endpoint) {
    const currentPath = window.location.pathname;
    return currentPath.endsWith("/" + endpoint) || currentPath.includes("/" + endpoint);
}

function loadCommonModules() {
    const modules = ["js/layout.js", "js/game-nav.js", "js/member-info.js"];

    if (isPage("mypage.html")) {
        modules.push("js/category-manager-request.js");
    }

    return Promise.all(modules.map(loadScript));
}

/**
 * 동일한 스크립트가 이미 로드된 경우 중복 삽입하지 않습니다.
 */
function loadScript(src) {
    return new Promise((resolve, reject) => {
        if ([...document.scripts].some(script => script.src.endsWith(src))) {
            resolve();
            return;
        }

        const script = document.createElement("script");
        script.src = src;
        script.onload = resolve;
        script.onerror = () => reject(new Error(src + " 로딩 실패"));
        document.head.appendChild(script);
    });
}

// =========================================================
// 게임 게시판 추가 신청
// =========================================================
/**
 * 현재 로그인한 사용자가 해당 게임의 카테고리 관리자인지 확인한 뒤
 * 게시판 추가 신청 버튼을 게임 게시판 목록 아래에 표시합니다.
 *
 * /category-create GET
 * -> 로그인 여부 + 현재 관리 중인 게임 ID를 확인합니다.
 *
 * /category-create POST
 * -> 입력한 게시판 이름을 해당 게임의 하위 게시판으로 생성합니다.
 */
async function initCategoryBoardCreateButton() {
    const boardMenu = document.getElementById("boardMenu");
    if (!boardMenu) return;

    const requestedGameId = Number(new URLSearchParams(window.location.search).get("gameId"));
    if (!requestedGameId) return;

    try {
        const response = await fetch("category-create", { credentials: "include" });
        if (!response.ok) return;

        const data = await response.json();

        // 카테고리 관리자가 아니거나 다른 게임의 관리자라면 표시하지 않습니다.
        if (!data.isManager || Number(data.categoryId) !== requestedGameId) return;
        if (document.getElementById("categoryBoardCreateButton")) return;

        const button = document.createElement("button");
        button.type = "button";
        button.id = "categoryBoardCreateButton";
        button.className = "board-menu category-board-create";
        button.textContent = "+ 게시판 추가 신청";
        button.title = `${data.gameName || "현재 게임"}에 게시판 추가`;

        button.addEventListener("click", async function () {
            const categoryName = window.prompt(
                `${data.gameName || "현재 게임"}에 추가할 게시판 이름을 입력해주세요.`
            );

            // 취소했거나 빈 문자열을 입력한 경우 아무 작업도 하지 않습니다.
            if (categoryName === null) return;
            const trimmedName = categoryName.trim();
            if (!trimmedName) {
                alert("게시판 이름을 입력해주세요.");
                return;
            }

            button.disabled = true;

            try {
                const createResponse = await fetch("category-create", {
                    method: "POST",
                    credentials: "include",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded"
                    },
                    body: new URLSearchParams({ categoryName: trimmedName })
                });

                const result = await createResponse.json();

                if (!createResponse.ok || !result.success) {
                    alert(result.message || "게시판 추가 신청에 실패했습니다.");
                    button.disabled = false;
                    return;
                }

                alert(result.message || "게시판 생성 요청을 완료했습니다.");

                // 새 게시판이 DB에 반영되었으므로 목록을 다시 불러옵니다.
                window.location.reload();
            } catch (error) {
                console.error("게시판 추가 신청 실패:", error);
                alert("서버 통신 오류가 발생했습니다.");
                button.disabled = false;
            }
        });

        // DB에서 가져온 게시판 목록 아래에 버튼을 배치합니다.
        boardMenu.appendChild(button);
    } catch (error) {
        // 로그인하지 않았거나 일반 회원인 경우 조용히 종료합니다.
        console.debug("게시판 추가 신청 버튼을 표시하지 않습니다.", error);
    }
}

// =========================================================
// 게시글 수정 버튼
// =========================================================
async function initPostEditButton() {
    const postId = new URLSearchParams(window.location.search).get("postId");
    const buttons = document.querySelector(".buttons");
    if (!postId || !buttons) return;

    try {
        const response = await fetch("post-edit?postId=" + encodeURIComponent(postId));
        if (!response.ok) return;

        const data = await response.json();
        if (!data.success || document.getElementById("editPostButton")) return;

        const editButton = document.createElement("button");
        editButton.type = "button";
        editButton.id = "editPostButton";
        editButton.className = "btn btn-outline-primary";
        editButton.textContent = "수정";
        editButton.style.marginRight = "8px";
        editButton.onclick = function () {
            window.location.href = "post-edit.html?postId=" + encodeURIComponent(postId);
        };

        buttons.insertBefore(editButton, buttons.firstElementChild);
    } catch (error) {
        console.error("게시글 수정 버튼 초기화 실패:", error);
    }
}
