document.addEventListener("DOMContentLoaded", async function () {
    const currentPath = window.location.pathname;
    if (isPage("login.html") || isPage("signup.html")) return;

    try {
        await loadCommonModules();
        initLayout();

        const categoryList = document.querySelector(".category-list");
        if (categoryList) loadGameCategories(categoryList);

        initMemberInfoBox();

        if (isPage("mypage.html") && typeof initCategoryManagerRequestAdmin === "function") {
            initCategoryManagerRequestAdmin();
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
