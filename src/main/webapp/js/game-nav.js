document.addEventListener("DOMContentLoaded", function () {
    const categoryList = document.querySelector(".category-list");
    if (!categoryList) return;

    const menuData = [
        {
            label: "RPG",
            games: [
                { name: "리니지", gameId: 110 },
                { name: "블레이드앤소울", gameId: 120 },
                { name: "메이플스토리", gameId: 130 },
                { name: "로스트아크", gameId: 140 }
            ]
        },
        {
            label: "FPS/TPS",
            games: [
                { name: "서든어택", gameId: 210 },
                { name: "오버워치", gameId: 220 },
                { name: "발로란트", gameId: 230 },
                { name: "배틀그라운드", gameId: 240 }
            ]
        },
        {
            label: "MOBA",
            games: [
                { name: "리그 오브 레전드", gameId: 310 },
                { name: "도타 2", gameId: 320 }
            ]
        },
        {
            label: "스포츠",
            games: [
                { name: "FC 온라인", gameId: 410 },
                { name: "eFootball", gameId: 420 },
                { name: "NBA 2K", gameId: 430 }
            ]
        },
        {
            label: "전략",
            games: [
                { name: "스타크래프트", gameId: 510 },
                { name: "문명 VI", gameId: 520 },
                { name: "에이지 오브 엠파이어 IV", gameId: 530 }
            ]
        },
        {
            label: "시뮬레이션",
            games: [
                { name: "심즈 4", gameId: 610 },
                { name: "시티즈: 스카이라인 II", gameId: 620 },
                { name: "유로 트럭 시뮬레이터 2", gameId: 630 }
            ]
        },
        {
            label: "그 외 장르",
            games: [
                { name: "마인크래프트", gameId: 910 },
                { name: "GTA V", gameId: 920 },
                { name: "철권 8", gameId: 930 },
                { name: "포르자 호라이즌 5", gameId: 940 },
                { name: "데드 바이 데이라이트", gameId: 950 },
                { name: "몬스터헌터 와일즈", gameId: 960 }
            ]
        }
    ];

    categoryList.innerHTML = menuData.map(group => `
        <div class="game-nav-item">
            <a class="game-nav-trigger" href="javascript:void(0)">${escapeGameNav(group.label)} ▾</a>
            <div class="game-nav-dropdown">
                ${group.games.map(game => `
                    <a class="game-nav-link" href="game.html?gameId=${game.gameId}">
                        <span>${escapeGameNav(game.name)}</span>
                        <small>커뮤니티 →</small>
                    </a>
                `).join("")}
            </div>
        </div>
    `).join("");

    injectGameNavStyles();
});

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
            transition: opacity .16s ease, transform .16s ease, visibility .16s ease;
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
            padding: 11px 12px;
            border-radius: 8px;
            color: #29252f !important;
            text-decoration: none;
            font-weight: 700 !important;
            transition: background .15s ease, color .15s ease;
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
    `;
    document.head.appendChild(style);
}

function escapeGameNav(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
