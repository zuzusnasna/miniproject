console.log("🔥 최신 game-nav.js 실행됨");
document.addEventListener("DOMContentLoaded", function () {
    const categoryList = document.querySelector(".category-list");
    if (!categoryList) return;

    loadGameCategories(categoryList);
});


// =========================================================
// CATEGORY 조회
// =========================================================

async function loadGameCategories(categoryList) {

    try {

        const response = await fetch("categories?depth=2");

        if (!response.ok) {
            throw new Error("카테고리 조회 실패 : " + response.status);
        }

        const games = await response.json();

        console.log("게임 카테고리:", games);

        // DB의 DEPTH 2 데이터를 장르별로 그룹화
        const menuData = makeMenuData(games);

        renderGameNav(categoryList, menuData);

    } catch (error) {

        console.error("게임 카테고리 불러오기 실패:", error);

    }
}


// =========================================================
// 장르별 그룹 만들기
// =========================================================

function makeMenuData(games) {

    const genreMap = {
        100: "RPG",
        200: "FPS/TPS",
        300: "MOBA",
        400: "스포츠",
        500: "전략",
        600: "시뮬레이션",
        900: "그 외 장르"
    };

    const groups = {};

    games.forEach(game => {

        const parentId = game.parentId;

        if (!groups[parentId]) {

            groups[parentId] = {
                label: genreMap[parentId] || "그 외 장르",
                games: []
            };

        }

        groups[parentId].games.push(game);

    });

    // DB의 SORT_ORDER 기준 정렬
    Object.values(groups).forEach(group => {

        group.games.sort((a, b) => {

            return (a.sortOrder || 0) -
                (b.sortOrder || 0);

        });

    });

    return Object.values(groups);
}


// =========================================================
// 화면 출력
// =========================================================

function renderGameNav(categoryList, menuData) {

    categoryList.innerHTML = menuData.map(group => `

        <div class="game-nav-item">

            <a
                class="game-nav-trigger"
                href="javascript:void(0)"
            >
                ${escapeGameNav(group.label)} ▾
            </a>

            <div class="game-nav-dropdown">

                ${group.games.map(game => `

                    <a
                        class="game-nav-link"
                        href="game.html?gameId=${game.categoryId}"
                    >

                        <span class="game-nav-game">

                            <img
                                class="game-nav-icon"
                                src="${game.iconUrl}"
                                alt=""
                                onerror="this.style.display='none'"
                            >

                            <span>
                                ${escapeGameNav(game.categoryName)}
                            </span>

                        </span>

                        <small>
                            커뮤니티 →
                        </small>

                    </a>

                `).join("")}

            </div>

        </div>

    `).join("");

    injectGameNavStyles();
}


// =========================================================
// CSS
// =========================================================

function injectGameNavStyles() {

    if (document.getElementById("game-nav-style")) return;

    const style = document.createElement("style");

    style.id = "game-nav-style";

    style.textContent = `

        .category-nav,
        .category-list {
            overflow: visible !important;
        }

        .category-list {
            align-items: center;
            justify-content: space-between;
            gap: 18px !important;
        }

        .game-nav-item {
            position: relative;
            display: flex;
            align-items: center;
            align-self: stretch;
        }

        .game-nav-trigger {
            display: flex;
            align-items: center;
            height: 100%;
            padding: 12px 4px;
            color: #111 !important;
            font-weight: 700 !important;
            font-size: .95rem !important;
            text-decoration: none;
            cursor: default;
        }

        .game-nav-trigger:hover {
            color: var(--primary-color) !important;
        }

        .game-nav-dropdown {
            position: absolute;
            z-index: 10000;
            top: calc(100% + 2px);
            left: 50%;
            width: 255px;
            max-height: 420px;
            overflow-y: auto;
            padding: 9px;
            border: 1px solid rgba(105,65,198,.18);
            border-radius: 12px;
            background: rgba(255,255,255,.95);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            box-shadow: 0 14px 34px rgba(35,22,68,.18);
            opacity: 0;
            visibility: hidden;
            pointer-events: none;
            transform: translate(-50%, -7px);
            transition:
                opacity .16s ease,
                transform .16s ease,
                visibility .16s ease;
        }

        .game-nav-dropdown::before {
            content: "";
            position: absolute;
            left: 0;
            right: 0;
            top: -10px;
            height: 10px;
        }

        .game-nav-item:hover .game-nav-dropdown,
        .game-nav-item:focus-within .game-nav-dropdown {
            opacity: 1;
            visibility: visible;
            pointer-events: auto;
            transform: translate(-50%, 0);
        }

        .game-nav-link {
            display: flex !important;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
            padding: 8px 12px;
            border-radius: 8px;
            color: #29252f !important;
            text-decoration: none;
            font-weight: 700 !important;
            transition:
                background .15s ease,
                color .15s ease;
        }

        .game-nav-link:hover {
            background: #f1ecfb;
            color: var(--primary-color) !important;
        }

        .game-nav-link small {
            color: #999;
            font-size: .72rem;
            font-weight: 600;
        }


        /* =========================
           게임 아이콘
           ========================= */

        .game-nav-game {
        display: flex;
        align-items: center;
        gap: 10px;
        }

        .game-nav-icon {
        width: 32px;
        height: 32px;
        object-fit: cover;
        border-radius: 6px;
        flex-shrink: 0;
        }

        `;

    document.head.appendChild(style);
}


// =========================================================
// HTML escape
// =========================================================

function escapeGameNav(value) {

    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");

}