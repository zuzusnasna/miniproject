/* =========================================================
   공통 초기화
   ========================================================= */

/**
 * 페이지가 로드되면 공통 모듈과 공통 UI를 초기화합니다.
 *
 * 실행 순서
 * 1. 공통 JS 모듈 로드
 * 2. 공통 레이아웃 생성
 * 3. 게임 카테고리 메뉴 로드
 * 4. 회원 정보 영역 초기화
 * 5. 페이지별 기능 초기화
 */
document.addEventListener("DOMContentLoaded", async function () {
    const currentPath = window.location.pathname;

    // 로그인/회원가입 페이지에서는 공통 레이아웃을 만들지 않습니다.
    if (isPage("login.html") || isPage("signup.html")) return;

    try {
        // 다른 JS 파일에서 사용하는 함수를 먼저 로드합니다.
        await loadCommonModules();

        // 헤더, 카테고리 메뉴, 광고 영역을 생성합니다.
        initLayout();

        // 상단 게임 카테고리를 조회하고 렌더링합니다.
        const categoryList = document.querySelector(".category-list");
        if (categoryList) {
            loadGameCategories(categoryList);
        }

        // 상단 회원 정보 메뉴를 초기화합니다.
        initMemberInfoBox();

        // 마이페이지에서는 카테고리 관리자 신청 관리 영역을 초기화합니다.
        if (
            isPage("mypage.html") &&
            typeof initCategoryManagerRequestAdmin === "function"
        ) {
            initCategoryManagerRequestAdmin();
        }

        /*
         * 게시판 메뉴는 game.html에서 DB 조회 후 다시 렌더링됩니다.
         * 따라서 여기서 버튼을 바로 추가하면 renderMenu()에 의해 사라집니다.
         * game.html에서 게시판 목록 렌더링이 끝난 뒤
         * initCategoryBoardCreateButton()을 호출합니다.
         */

        // 게시글 상세 페이지에서 수정 버튼을 초기화합니다.
        if (isEndpoint("post-detail")) {
            initPostEditButton();
        }
    } catch (error) {
        console.error("공통 JS 모듈 로딩 실패:", error);
    }
});


/* =========================================================
   페이지 / 엔드포인트 확인
   ========================================================= */

/**
 * 현재 페이지가 지정한 HTML 페이지인지 확인합니다.
 */
function isPage(pageName) {
    const currentPath = window.location.pathname;

    return (
        currentPath.endsWith("/" + pageName) ||
        currentPath.endsWith(pageName)
    );
}


/**
 * 현재 요청 경로가 지정한 Servlet 엔드포인트인지 확인합니다.
 */
function isEndpoint(endpoint) {
    const currentPath = window.location.pathname;

    return (
        currentPath.endsWith("/" + endpoint) ||
        currentPath.includes("/" + endpoint)
    );
}


/* =========================================================
   공통 JS 모듈 로딩
   ========================================================= */

/**
 * 공통으로 사용하는 JS 파일을 순서대로 로드합니다.
 */
function loadCommonModules() {
    const modules = [
        "js/layout.js",
        "js/game-nav.js",
        "js/member-info.js"
    ];

    // 마이페이지에서만 관리자 신청 관련 JS를 추가로 로드합니다.
    if (isPage("mypage.html")) {
        modules.push("js/category-manager-request.js");
    }

    return Promise.all(modules.map(loadScript));
}


/**
 * 동일한 스크립트가 이미 로드되어 있다면 다시 삽입하지 않습니다.
 */
function loadScript(src) {
    return new Promise((resolve, reject) => {
        const alreadyLoaded = [...document.scripts].some(
            script => script.src.endsWith(src)
        );

        if (alreadyLoaded) {
            resolve();
            return;
        }

        const script = document.createElement("script");
        script.src = src;
        script.onload = resolve;
        script.onerror = () => {
            reject(new Error(src + " 로딩 실패"));
        };

        document.head.appendChild(script);
    });
}


/* =========================================================
   게임 게시판 추가 신청
   ========================================================= */

/**
 * 현재 로그인한 사용자가 해당 게임의 카테고리 관리자인지 확인한 뒤
 * 게임 게시판 목록 아래에 게시판 추가 신청 버튼을 표시합니다.
 *
 * 주의:
 * game.html이 DB에서 게시판 목록을 렌더링한 뒤 호출해야 합니다.
 * renderMenu()가 boardMenu의 HTML을 다시 만들기 때문에
 * 그 전에 버튼을 추가하면 버튼이 사라집니다.
 */
async function initCategoryBoardCreateButton() {
    const boardMenu = document.getElementById("boardMenu");
    if (!boardMenu) return;

    const requestedGameId = Number(
        new URLSearchParams(window.location.search).get("gameId")
    );

    if (!requestedGameId) return;

    try {
        const response = await fetch("category-create", {
            credentials: "include"
        });

        if (!response.ok) return;

        const data = await response.json();

        // 카테고리 관리자가 아니거나 다른 게임의 관리자라면 표시하지 않습니다.
        if (
            !data.isManager ||
            Number(data.categoryId) !== requestedGameId
        ) {
            return;
        }

        // 이미 버튼이 만들어져 있다면 중복 생성하지 않습니다.
        if (document.getElementById("categoryBoardCreateButton")) {
            return;
        }

        const button = document.createElement("button");
        button.type = "button";
        button.id = "categoryBoardCreateButton";
        button.className = "board-menu category-board-create";
        button.textContent = "+ 게시판 추가 신청";
        button.title = `${data.gameName || "현재 게임"}에 게시판 추가`;

        // 신청 버튼 클릭 시 게시판 이름을 입력받습니다.
        button.addEventListener("click", async function () {
            const categoryName = window.prompt(
                `${data.gameName || "현재 게임"}에 추가할 게시판 이름을 입력해주세요.`
            );

            // 취소한 경우 아무 작업도 하지 않습니다.
            if (categoryName === null) return;

            const trimmedName = categoryName.trim();

            // 빈 이름은 허용하지 않습니다.
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
                    body: new URLSearchParams({
                        categoryName: trimmedName
                    })
                });

                const result = await createResponse.json();

                if (!createResponse.ok || !result.success) {
                    alert(
                        result.message ||
                        "게시판 추가 신청에 실패했습니다."
                    );

                    button.disabled = false;
                    return;
                }

                alert(
                    result.message ||
                    "게시판 생성 요청을 완료했습니다."
                );

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
        console.debug(
            "게시판 추가 신청 버튼을 표시하지 않습니다.",
            error
        );
    }
}


/* =========================================================
   게시글 수정 버튼
   ========================================================= */

/**
 * 게시글 상세 페이지에서 수정 버튼을 표시합니다.
 * 서버가 현재 사용자의 수정 권한을 확인합니다.
 */
async function initPostEditButton() {
    const postId = new URLSearchParams(window.location.search).get("postId");
    const buttons = document.querySelector(".buttons");

    if (!postId || !buttons) return;

    try {
        const response = await fetch(
            "post-edit?postId=" + encodeURIComponent(postId)
        );

        if (!response.ok) return;

        const data = await response.json();

        if (
            !data.success ||
            document.getElementById("editPostButton")
        ) {
            return;
        }

        const editButton = document.createElement("button");
        editButton.type = "button";
        editButton.id = "editPostButton";
        editButton.className = "btn btn-outline-primary";
        editButton.textContent = "수정";
        editButton.style.marginRight = "8px";

        editButton.onclick = function () {
            window.location.href =
                "post-edit.html?postId=" +
                encodeURIComponent(postId);
        };

        // 기존 버튼 영역의 가장 앞에 수정 버튼을 추가합니다.
        buttons.insertBefore(
            editButton,
            buttons.firstElementChild
        );
    } catch (error) {
        console.error("게시글 수정 버튼 초기화 실패:", error);
    }
}
