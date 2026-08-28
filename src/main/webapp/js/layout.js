function initLayout() {
    injectCustomStyles();

    if (!document.querySelector(".site-header-wrapper")) {
        const headerWrapper = document.createElement("header");
        headerWrapper.className = "site-header-wrapper";
        headerWrapper.innerHTML = `
            <div class="main-header">
                <div class="custom-container header-inner">
                    <a href="home.html" class="logo">Game Hub</a>
                    <div class="user-menu">
                        <a href="login.html" id="navAuthLink">로그인</a>
                        <a href="mypage.html">마이페이지</a>
                    </div>
                </div>
            </div>
            <nav class="category-nav">
                <div class="custom-container category-list"></div>
            </nav>`;
        document.body.prepend(headerWrapper);
    }

    const mainContent = document.getElementById("main-content") || document.querySelector("main") || document.querySelector(".board-container");
    if (mainContent && !mainContent.closest(".layout-grid")) {
        const layoutContainer = document.createElement("div");
        layoutContainer.className = "custom-container layout-grid";
        mainContent.parentNode.insertBefore(layoutContainer, mainContent);
        mainContent.classList.add("content-left");
        layoutContainer.appendChild(mainContent);

        const adSidebar = document.createElement("aside");
        adSidebar.className = "content-right ad-sidebar";
        adSidebar.innerHTML = `
    <div class="ad-slot" aria-label="광고 영역">
        <span class="ad-label">ADVERTISEMENT</span>

        <video class="ad-video" autoplay muted loop playsinline>
            <source src="images/gamehub-ad.mp4" type="video/mp4">
        </video>
    </div>
`;
        layoutContainer.appendChild(adSidebar);
    }
}

function injectCustomStyles() {
    if (document.getElementById("custom-layout-style")) return;
    const style = document.createElement("style");
    style.id = "custom-layout-style";
    style.textContent = `
        body .custom-container, body .site-header-wrapper .custom-container, body .header-inner, body .category-list { width:100% !important; max-width:1280px !important; margin:0 auto !important; padding:0 16px !important; box-sizing:border-box !important; }
        body #main-content, body .content-left, body .container, body .container-sm, body .container-md, body .container-lg, body .container-xl { width:100% !important; margin-left:auto !important; margin-right:auto !important; }
        body .layout-grid { display:flex !important; flex-direction:row !important; gap:24px !important; width:100% !important; max-width:1480px !important; margin:24px auto !important; padding:0 16px !important; box-sizing:border-box !important; align-items:flex-start !important; }
        body .content-left { flex:1 1 0% !important; min-width:0 !important; }
        body .content-right { width:160px !important; flex:0 0 160px !important; }
        body .ad-sidebar { position:relative; }
        body .ad-video {
    width:100%;
    height:570px;
    object-fit:cover;
    border-radius:9px;
    display:block;
}
        body .ad-slot { min-height:600px; padding:6px; border:1px solid rgba(105,65,198,.28); border-radius:12px; background:#f8f6fd; color:#6941c6; display:flex; flex-direction:column; align-items:center; justify-content:center; text-align:center; box-sizing:border-box; }
        body .ad-label { position:absolute; top:14px; font-size:.62rem; font-weight:800; letter-spacing:.08em; color:#8a7aad; }
        @media (max-width:980px) { body .layout-grid { flex-direction:column !important; } body .content-right { width:100% !important; flex:0 0 auto !important; } body .ad-slot { min-height:140px; } }
    `;
    document.head.appendChild(style);
}
