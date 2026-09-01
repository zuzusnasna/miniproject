/*
 * GameHub 공통 레이아웃
 *
 * Header / 카테고리 메뉴 / 우측 광고 영역을 공통으로 생성한다.
 */
function initLayout() {
    loadGameHubTheme();

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
                                <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5S10.5 3.17 10.5 4v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z"/>
                            </svg>
                        </button>
                        <button type="button" class="member-menu-trigger" id="memberMenuTrigger" aria-expanded="false" aria-controls="commonMemberInfo" disabled>
                            Not login status
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
            <div class="ad-slot">
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
    link.href = "css/gamehub-theme.css?v=4";
    document.head.appendChild(link);
}

function applyGameBanner() {
    const banner = document.querySelector(".game-banner");
    if (!banner) return;

    const gameId = Number(new URLSearchParams(location.search).get("gameId"));

    const banners = {
        110: "https://shared.steamstatic.com/store_item_assets/steam/apps/216150/44dd4c4c8a5cc5bb860c4fafb317424f1f0db7aa/hero_capsule_2x.jpg",
        120: "https://shared.steamstatic.com/store_item_assets/steam/apps/1956040/header.jpg",
        130: "https://nxl.nxfs.nexon.com/media/10086/newage-main_card.jpg",
        140: "https://images.indianexpress.com/2022/02/lost-ark-featured.jpg",
        210: "https://www.gamerevolution.com/wp-content/uploads/sites/2/2022/10/co2hvp.jpg",
        220: "https://vg24.gr/wp-content/uploads/2022/06/overwatch-2-key-art.jpg",
        230: "https://image.gameapps.hk/images/202105/05/riot-games-valorant-release-00.jpg",
        240: "https://cdn.cloudflare.steamstatic.com/steam/apps/578080/header.jpg",
        310: "https://i.gzn.jp/img/2017/09/29/league-of-legends/00.jpg",
        320: "https://images.gamewatcherstatic.com/image/file/4/5e/126384/Dota-2-Key-Art-1.jpg",
        410: "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1600&q=85",
        420: "https://images.unsplash.com/photo-1556056504-5c7696c4c28d?auto=format&fit=crop&w=1600&q=85",
        430: "https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=1600&q=85",
        510: "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=1600&q=85",
        520: "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=1600&q=85",
        530: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1600&q=85",
        610: "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1600&q=85",
        620: "https://images.unsplash.com/photo-1448630360428-65456885c650?auto=format&fit=crop&w=1600&q=85",
        630: "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=1600&q=85",
        910: "https://congngheviet.com/wp-content/uploads/2025/05/minecraft-key-art.webp",
        920: "https://cdn.cloudflare.steamstatic.com/steam/apps/271590/header.jpg",
        930: "https://cdn.cloudflare.steamstatic.com/steam/apps/1778820/header.jpg",
        940: "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1600&q=85",
        950: "https://images.unsplash.com/photo-1509248961158-e54f6934749c?auto=format&fit=crop&w=1600&q=85",
        960: "https://images.unsplash.com/photo-1542751110-97427bbecf20?auto=format&fit=crop&w=1600&q=85"
    };

    const image = banners[gameId];
    if (image) {
        banner.style.setProperty("--game-banner-image", `url("${image}")`);
        banner.classList.add("has-game-image");
    }
}
