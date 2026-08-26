document.addEventListener("DOMContentLoaded", function () {
    const categoryList = document.querySelector(".category-list");
    if (!categoryList) return;

    const menuData = {
        "롤플레잉(RPG)": [
            { name: "리니지", gameId: 110 },
            { name: "블레이드앤소울", gameId: 120 }
        ],
        "슈팅(FPS)": [
            { name: "서든어택", gameId: 210 },
            { name: "오버워치", gameId: 220 }
        ]
    };

    Array.from(categoryList.querySelectorAll(":scope > a")).forEach(link => {
        const label = link.textContent.replace("▾", "").trim();
        const games = menuData[label];
        if (!games) return;

        const item = document.createElement("div");
        item.className = "game-nav-item";

        const trigger = link.cloneNode(true);
        trigger.href = "javascript:void(0)";
        trigger.className = "game-nav-trigger";

        const dropdown = document.createElement("div");
        dropdown.className = "game-nav-dropdown";
        dropdown.innerHTML = games.map(game => `
            <a class="game-nav-link" href="game.html?gameId=${game.gameId}">
                <span>${escapeGameNav(game.name)}</span>
                <small>커뮤니티 →</small>
            </a>
        `).join("");

        item.appendChild(trigger);
        item.appendChild(dropdown);
        link.replaceWith(item);
    });

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
            color: #111;
            font-weight: 600;
            font-size: .95rem;
            text-decoration: none;
            cursor: default;
        }

        .game-nav-trigger:hover {
            color: var(--primary-color);
        }

        .game-nav-dropdown {
            position: absolute;
            z-index: 10000;
            top: calc(100% + 10px);
            left: 50%;
            width: 230px;
            padding: 9px;
            border: 1px solid rgba(105,65,198,.18);
            border-radius: 12px;
            background: rgba(255,255,255,.94);
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
            top: -12px;
            height: 12px;
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
