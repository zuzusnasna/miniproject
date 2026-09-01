/*
 * GameHub 공통 레이아웃
 *
 * 이 파일의 핵심 역할은 여러 페이지에서 반복되는
 * Header / 카테고리 메뉴 / 우측 광고 영역을 한 번만 생성하는 것이다.
 *
 * 공부 포인트:
 * - DOM API로 공통 HTML을 동적으로 생성할 수 있다.
 * - className / classList를 이용해 CSS와 연결한다.
 * - 페이지마다 HTML을 복붙하지 않고 하나의 JS로 재사용한다.
 */
function initLayout() {
    // 공통 테마 CSS를 아직 로드하지 않았다면 한 번만 추가한다.
    loadGameHubTheme();

    // Header는 페이지마다 중복 생성되지 않도록 먼저 존재 여부를 확인한다.
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

    // main 영역을 공통 2단 레이아웃 안으로 이동한다.
    // 이미 layout-grid 안에 있다면 다시 감싸지 않는다.
    const mainContent = document.getElementById("main-content") || document.querySelector("main") || document.querySelector(".board-container");
    if (mainContent && !mainContent.closest(".layout-grid")) {
        const layoutContainer = document.createElement("div");
        layoutContainer.className = "custom-container layout-grid";
        mainContent.parentNode.insertBefore(layoutContainer, mainContent);
        mainContent.classList.add("content-left");
        layoutContainer.appendChild(mainContent);

        // 오른쪽 광고 영역도 공통 레이아웃에서 한 번만 생성한다.
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

    // gameId가 있는 게임 페이지에서만 대표 배너 이미지를 적용한다.
    applyGameBanner();
}

function loadGameHubTheme() {
    // 같은 CSS를 여러 번 <link>로 추가하지 않도록 id로 확인한다.
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

    // URL의 gameId를 숫자로 변환해 게임별 이미지와 연결한다.
    const gameId = Number(new URLSearchParams(location.search).get("gameId"));

    /*
     * 게임별 대표 키아트/배너.
     * 객체의 key를 gameId로 사용하면 긴 if/else 문 없이
     * 원하는 이미지를 바로 찾을 수 있다.
     */
    const banners = {
        // RPG
        110: "https://shared.steamstatic.com/store_item_assets/steam/apps/216150/44dd4c4c8a5cc5bb860c4fafb317424f1f0db7aa/hero_capsule_2x.jpg",
        120: "https://shared.steamstatic.com/store_item_assets/steam/apps/1956040/header.jpg",
        130: "https://nxl.nxfs.nexon.com/media/10086/newage-main_card.jpg",
        140: "https://images.indianexpress.com/2022/02/lost-ark-featured.jpg",
        // FPS / TPS
        210: "https://www.gamerevolution.com/wp-content/uploads/sites/2/2022/10/co2hvp.jpg",
        220: "https://vg24.gr/wp-content/uploads/2022/06/overwatch-2-key-art.jpg",
        230: "https://image.gameapps.hk/images/202105/05/riot-games-valorant-release-00.jpg",
        240: "https://cdn.cloudflare.steamstatic.com/steam/apps/578080/header.jpg",
        // MOBA
        310: "https://i.gzn.jp/img/2017/09/29/league-of-legends/00.jpg",
        320: "https://images.gamewatcherstatic.com/image/file/4/5e/126384/Dota-2-Key-Art-1.jpg",
        // 스포츠
        410: "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1600&q=85",
        420: "https://images.unsplash.com/photo-1556056504-5c7696c4c28d?auto=format&fit=crop&w=1600&q=85",
        430: "https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=1600&q=85",
        // 전략
        510: "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=1600&q=85",
        520: "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=1600&q=85",
        530: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1600&q=85",
        // 시뮬레이션
        610: "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1600&q=85",
        620: "https://images.unsplash.com/photo-1448630360428-65456885c650?auto=format&fit=crop&w=1600&q=85",
        630: "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=1600&q=85",
        // 그 외
        910: "https://congngheviet.com/wp-content/uploads/2025/05/minecraft-key-art.webp",
        920: "https://cdn.cloudflare.steamstatic.com/steam/apps/271590/header.jpg",
        930: "https://cdn.cloudflare.steamstatic.com/steam/apps/1778820/header.jpg",
        940: "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1600&q=85",
        950: "https://images.unsplash.com/photo-1509248961158-e54f6934749c?auto=format&fit=crop&w=1600&q=85",
        960: "https://images.unsplash.com/photo-1542751110-97427bbecf20?auto=format&fit=crop&w=1600&q=85"
    };

    const image = banners[gameId];
    if (image) {
        // CSS 변수에 이미지를 넣고, 실제 표시 여부는 CSS class로 제어한다.
        banner.style.setProperty("--game-banner-image", `url("${image}")`);
        banner.classList.add("has-game-image");
    }
}
