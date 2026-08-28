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
    } catch (error) {
        console.error("공통 JS 모듈 로딩 실패:", error);
    }
});

function loadCommonModules() {
    const modules = ["js/layout.js", "js/game-nav.js", "js/member-info.js", "js/category-manager-request.js"];

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
