async function loadGameCategories(categoryList) {
    try {
        const response = await fetch("categories?depth=2");
        if (!response.ok) throw new Error("카테고리 조회 실패 : " + response.status);
        renderGameNav(categoryList, makeMenuData(await response.json()));
    } catch (error) {
        console.error("게임 카테고리 불러오기 실패:", error);
    }
}

function makeMenuData(games) {
    const genreMap = {100:"RPG",200:"FPS/TPS",300:"MOBA",400:"스포츠",500:"전략",600:"시뮬레이션",900:"그 외 장르"};
    const groups = {};
    games.forEach(game => {
        const parentId = game.parentId;
        if (!groups[parentId]) groups[parentId] = {label:genreMap[parentId] || "그 외 장르", games:[]};
        groups[parentId].games.push(game);
    });
    Object.values(groups).forEach(group => group.games.sort((a,b)=>(a.sortOrder||0)-(b.sortOrder||0)));
    return Object.values(groups);
}

function renderGameNav(categoryList, menuData) {
    categoryList.innerHTML = menuData.map(group => `
        <div class="game-nav-item">
            <a class="game-nav-trigger" href="javascript:void(0)">${escapeGameNav(group.label)} ▾</a>
            <div class="game-nav-dropdown">
                ${group.games.map(game => `
                    <a class="game-nav-link" href="game.html?gameId=${game.categoryId}">
                        <span class="game-nav-game"><img class="game-nav-icon" src="${escapeGameNav(resolveGameIconPath(game.iconUrl))}" alt="${escapeGameNav(game.categoryName)}" onerror="this.style.display='none'"><span>${escapeGameNav(game.categoryName)}</span></span>
                        <small>커뮤니티 →</small>
                    </a>`).join("")}
            </div>
        </div>`).join("");
    injectGameNavStyles();
}

function injectGameNavStyles() {
    if (document.getElementById("game-nav-style")) return;
    const style = document.createElement("style");
    style.id = "game-nav-style";
    style.textContent = `
        .category-nav,.category-list{overflow:visible!important}
        .category-list{align-items:center;justify-content:space-between;gap:20px!important}
        .game-nav-item{position:relative;display:flex;align-items:center;align-self:stretch}
        .game-nav-trigger{display:flex;align-items:center;height:100%;padding:12px 4px;color:#111!important;font-weight:700!important;font-size:.95rem!important;text-decoration:none;cursor:default}
        .game-nav-trigger:hover{color:var(--primary-color)!important}
        .game-nav-dropdown{position:absolute;z-index:10000;top:calc(100% + 2px);left:50%;width:340px;max-height:420px;overflow-y:auto;padding:9px;border:1px solid rgba(105,65,198,.18);border-radius:12px;background:rgba(255,255,255,.95);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);box-shadow:0 14px 34px rgba(35,22,68,.18);opacity:0;visibility:hidden;pointer-events:none;transform:translate(-50%,-7px);transition:opacity .16s ease,transform .16s ease,visibility .16s ease}
        .game-nav-item:hover .game-nav-dropdown,.game-nav-item:focus-within .game-nav-dropdown{opacity:1;visibility:visible;pointer-events:auto;transform:translate(-50%,0)}

        /* 🔥 [추가] 첫 번째 메뉴(RPG 등)는 왼쪽 정렬로 열려 우측으로 펼쳐짐 */
        .game-nav-item:first-child .game-nav-dropdown { left: 0 !important; transform: translate(0, 0) !important; }
        
        /* 🔥 [추가] 마지막 메뉴(그 외 장르 등)는 오른쪽 정렬로 열려 좌측으로 펼쳐짐 */
        .game-nav-item:last-child .game-nav-dropdown { left: auto !important; right: 0 !important; transform: translate(0, 0) !important; }

        .game-nav-link{display:flex!important;justify-content:space-between;align-items:center;gap:16px;padding:9px 14px;border-radius:8px;color:#29252f!important;text-decoration:none;font-weight:700!important}
        .game-nav-link:hover{background:#f1ecfb;color:var(--primary-color)!important}
        .game-nav-link small{color:#999;font-size:.72rem;font-weight:600}
        .game-nav-game{display:flex;align-items:center;gap:10px}
        .game-nav-icon{width:32px;height:32px;object-fit:cover;border-radius:6px;flex-shrink:0}
    `;
    document.head.appendChild(style);
}

function resolveGameIconPath(iconUrl) {
    if (!iconUrl) return "";

    // DB에 저장된 기존 /gamecommunity_ 경로를
    // 현재 애플리케이션 Context Path로 변환
    return iconUrl;
}

function escapeGameNav(value) { return String(value ?? "").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/\"/g,"&quot;").replace(/'/g,"&#039;"); }
