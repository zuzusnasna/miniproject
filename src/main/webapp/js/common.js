document.addEventListener("DOMContentLoaded", async function () {
    const currentPath = window.location.pathname;
    if (currentPath.includes("login.html") || currentPath.includes("signup.html")) return;

    try {
        await loadCommonModules();
        initLayout();

        const categoryList = document.querySelector(".category-list");
        if (categoryList) loadGameCategories(categoryList);

        initMemberInfoBox();

        if (currentPath.endsWith("/mypage.html") || currentPath.endsWith("mypage.html")) {
            if (typeof initCategoryManagerRequestAdmin === "function") {
                initCategoryManagerRequestAdmin();
            }
        }

        if (currentPath.endsWith("/post-detail") || currentPath.includes("/post-detail")) {
            initPostEditButton();
        }
    } catch (error) {
        console.error("공통 JS 모듈 로딩 실패:", error);
    }
});

function loadCommonModules() {
    const currentPath = window.location.pathname;
    const modules = ["js/layout.js", "js/game-nav.js", "js/member-info.js"];

    if (currentPath.endsWith("/mypage.html") || currentPath.endsWith("mypage.html")) {
        modules.push("js/category-manager-request.js");
    }

    return Promise.all(modules.map(src => new Promise((resolve, reject) => {
        if ([...document.scripts].some(script => script.src.endsWith(src))) {
            resolve();
            return;
        }
        const script = document.createElement("script");
        script.src = src;
        script.onload = resolve;
        script.onerror = () => reject(new Error(src + " 로딩 실패"));
        document.head.appendChild(script);
    })));
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
