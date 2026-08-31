function initLayout() {
    loadGameHubTheme();
    injectCustomStyles();

    if (!document.querySelector(".site-header-wrapper")) {
        const headerWrapper = document.createElement("header");
        headerWrapper.className = "site-header-wrapper";
        headerWrapper.innerHTML = `
            <div class="main-header">
                <div class="custom-container header-inner">
                    <a href="home.html" class="logo">GAMEHUB</a>
                    <div class="user-menu">
                        <a href="login.html" id="navAuthLink">로그인</a>
                        <a href="mypage.html">마이페이지</a>
                        <button type="button" class="btn-noti-icon" aria-label="알림" onclick="alert('새로운 알림이 없습니다.')">
                            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true">
                                <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z"/>
                            </svg>
                        </button>
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
            </div>`;
        layoutContainer.appendChild(adSidebar);
    }

    applyGameBanner();
}

function loadGameHubTheme() {
    if (document.getElementById("gamehub-theme-css")) return;
    const link = document.createElement("link");
    link.id = "gamehub-theme-css";
    link.rel = "stylesheet";
    link.href = "css/gamehub-theme.css?v=3";
    document.head.appendChild(link);
}

function applyGameBanner() {
    const banner = document.querySelector(".game-banner");
    if (!banner) return;

    const gameId = Number(new URLSearchParams(location.search).get("gameId"));

    const banners = {
        110: "https://cdn.cloudflare.steamstatic.com/steam/apps/550/header.jpg",
        120: "https://cdn.cloudflare.steamstatic.com/steam/apps/1085660/header.jpg",
        130: "https://cdn.cloudflare.steamstatic.com/steam/apps/1817070/header.jpg",
        140: "https://cdn.cloudflare.steamstatic.com/steam/apps/1599340/header.jpg",
        210: "https://cdn.cloudflare.steamstatic.com/steam/apps/240/header.jpg",
        220: "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1600&q=80",
        230: "https://images.unsplash.com/photo-1547394765-185e1e68f34e?auto=format&fit=crop&w=1600&q=80",
        240: "https://cdn.cloudflare.steamstatic.com/steam/apps/578080/header.jpg",
        310: "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1600&q=80",
        320: "https://images.unsplash.com/photo-1511882150382-421056c89033?auto=format&fit=crop&w=1600&q=80",
        410: "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1600&q=80",
        420: "https://images.unsplash.com/photo-1556056504-5c7696c4c28d?auto=format&fit=crop&w=1600&q=80",
        430: "https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=1600&q=80",
        510: "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=1600&q=80",
        520: "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=1600&q=80",
        530: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1600&q=80",
        610: "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1600&q=80",
        620: "https://images.unsplash.com/photo-1448630360428-65456885c650?auto=format&fit=crop&w=1600&q=80",
        630: "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=1600&q=80",
        910: "https://images.unsplash.com/photo-1603481546238-487240415921?auto=format&fit=crop&w=1600&q=80",
        920: "https://cdn.cloudflare.steamstatic.com/steam/apps/271590/header.jpg",
        930: "https://cdn.cloudflare.steamstatic.com/steam/apps/1778820/header.jpg",
        940: "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1600&q=80",
        950: "https://images.unsplash.com/photo-1509248961158-e54f6934749c?auto=format&fit=crop&w=1600&q=80",
        960: "https://images.unsplash.com/photo-1542751110-97427bbecf20?auto=format&fit=crop&w=1600&q=80"
    };

    const image = banners[gameId];
    if (image) {
        banner.style.setProperty("--game-banner-image", `url("${image}")`);
        banner.classList.add("has-game-image");
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
        body .ad-video { width:100%; height:570px; object-fit:cover; border-radius:9px; display:block; }
        body .ad-slot { min-height:600px; padding:6px; border:1px solid rgba(105,65,198,.28); border-radius:12px; background:#f8f6fd; color:#6941c6; display:flex; flex-direction:column; align-items:center; justify-content:center; text-align:center; box-sizing:border-box; }
        body .ad-label { position:absolute; top:14px; font-size:.62rem; font-weight:800; letter-spacing:.08em; color:#8a7aad; }
        @media (max-width:980px) { body .layout-grid { flex-direction:column !important; } body .content-right { width:100% !important; flex:0 0 auto !important; } body .ad-slot { min-height:140px; } }
    `;
    document.head.appendChild(style);
}
