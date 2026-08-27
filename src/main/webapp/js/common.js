document.addEventListener("DOMContentLoaded", function () {
    // 1. 공통 헤더/네비바 및 레이아웃 컨테이너 생성
    initLayout();

    // 2. 게임 카테고리 드롭다운 생성
    const categoryList = document.querySelector(".category-list");
    if (categoryList) {
        loadGameCategories(categoryList);
    }

    // 3. 회원정보 드래그 박스 생성
    initMemberInfoBox();
});

function initLayout() {
    injectCustomStyles();

    if (document.querySelector(".site-header-wrapper")) {
        return;
    }

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
            <div class="custom-container category-list">
                <a href="javascript:void(0)">RPG ▾</a>
                <a href="javascript:void(0)">FPS/TPS ▾</a>
                <a href="javascript:void(0)">MOBA ▾</a>
                <a href="javascript:void(0)">스포츠 ▾</a>
                <a href="javascript:void(0)">전략 ▾</a>
                <a href="javascript:void(0)">시뮬레이션 ▾</a>
                <a href="javascript:void(0)">그 외 장르 ▾</a>
            </div>
        </nav>
    `;
    document.body.prepend(headerWrapper);

    const currentPath = window.location.pathname;

    if (currentPath.includes("post-write") || currentPath.includes("post.html")) {
        return;
    }

    const mainContent = document.getElementById("main-content");
    if (mainContent && !mainContent.closest(".layout-grid")) {
        const layoutContainer = document.createElement("div");
        layoutContainer.className = "custom-container layout-grid";
        mainContent.parentNode.insertBefore(layoutContainer, mainContent);
        mainContent.classList.add("content-left");
        layoutContainer.appendChild(mainContent);

        const sidebar = document.createElement("aside");
        sidebar.className = "content-right";
        sidebar.innerHTML = `
            <div class="sidebar-card shadow-sm border-0 rounded-3 p-3 bg-white">
                <p class="mb-2 text-muted fw-bold">커뮤니티를 더 즐겁게 이용해보세요!</p>
                <a href="login.html" class="btn btn-primary w-100 fw-bold">로그인 하기</a>
            </div>
        `;
        layoutContainer.appendChild(sidebar);
    }
}

function injectCustomStyles() {
    if (document.getElementById("custom-layout-style")) return;
    const style = document.createElement("style");
    style.id = "custom-layout-style";
    style.innerHTML = `
        body .custom-container, body .site-header-wrapper .custom-container, body .header-inner, body .category-list {
            width:100% !important; max-width:1280px !important; margin:0 auto !important; padding:0 16px !important; box-sizing:border-box !important;
        }
        body #main-content, body .content-left, body .container, body .container-sm, body .container-md, body .container-lg, body .container-xl {
            width:100% !important; max-width:1280px !important; margin-left:auto !important; margin-right:auto !important;
        }
        body .layout-grid { display:flex !important; flex-direction:row !important; gap:24px !important; width:100% !important; max-width:1280px !important; margin:24px auto !important; align-items:flex-start !important; }
        body .content-left { flex:1 1 0% !important; min-width:0 !important; }
        body .content-right { width:300px !important; flex-shrink:0 !important; }
    `;
    document.head.appendChild(style);
}

// =========================================================
// 게임 카테고리 네비게이션
// =========================================================

async function loadGameCategories(categoryList) {
    try {
        const response = await fetch("categories?depth=2");

        if (!response.ok) {
            throw new Error("카테고리 조회 실패 : " + response.status);
        }

        const games = await response.json();
        const menuData = makeMenuData(games);
        renderGameNav(categoryList, menuData);
    } catch (error) {
        console.error("게임 카테고리 불러오기 실패:", error);
    }
}

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

    Object.values(groups).forEach(group => {
        group.games.sort((a, b) => {
            return (a.sortOrder || 0) - (b.sortOrder || 0);
        });
    });

    return Object.values(groups);
}

function renderGameNav(categoryList, menuData) {
    categoryList.innerHTML = menuData.map(group => `
        <div class="game-nav-item">
            <a class="game-nav-trigger" href="javascript:void(0)">
                ${escapeGameNav(group.label)} ▾
            </a>

            <div class="game-nav-dropdown">
                ${group.games.map(game => `
                    <a class="game-nav-link" href="game.html?gameId=${game.categoryId}">
                        <span class="game-nav-game">
                            <img
                                class="game-nav-icon"
                                src="${escapeGameNav(game.iconUrl)}"
                                alt="${escapeGameNav(game.categoryName)}"
                                onerror="this.style.display='none'"
                            >
                            <span>${escapeGameNav(game.categoryName)}</span>
                        </span>
                        <small>커뮤니티 →</small>
                    </a>
                `).join("")}
            </div>
        </div>
    `).join("");

    injectGameNavStyles();
}

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
            gap: 20px !important;
            overflow-x: visible !important;
            overflow-y: visible !important;
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
            width: 340px;
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
            gap: 16px;
            padding: 9px 14px;
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

function escapeGameNav(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// =========================================================
// 회원정보
// =========================================================

function initMemberInfoBox() {
    let wrapper = document.getElementById("commonMemberInfo");
    if (!wrapper) {
        wrapper = document.createElement("div");
        wrapper.id = "commonMemberInfo";
        wrapper.innerHTML = `<div class="common-member-info"><div class="member-title">👤 회원정보</div><div id="commonMemberContent">회원 정보를 불러오는 중...</div></div>`;
        document.body.appendChild(wrapper);
    }
    const memberBox = wrapper.querySelector(".common-member-info");
    restoreMemberBoxPosition(memberBox);
    makeMemberBoxDraggable(memberBox);
    loadMemberInfo();
}

function loadMemberInfo() {
    const content = document.getElementById("commonMemberContent");
    fetch("member-info", { method:"GET", credentials:"include" })
        .then(response => { if (response.status === 401 || !response.ok) throw new Error("로그인이 필요합니다."); return response.json(); })
        .then(member => {
            const authLink = document.getElementById("navAuthLink") || document.querySelector('a[href*="login.html"]');
            if (authLink) { authLink.textContent="로그아웃"; authLink.href="logout"; }
            const sidebarCard = document.querySelector(".content-right .sidebar-card");
            if (sidebarCard) {
                sidebarCard.className="sidebar-card shadow-sm border-0 rounded-3 p-3 bg-white";
                sidebarCard.innerHTML=`<div class="text-center"><p class="mb-3 fw-bold text-dark fs-6">👋 <span class="text-primary">${escapeHtml(member.name)}</span>님 환영합니다!</p><a href="post-write.html" class="btn btn-primary w-100 mb-2 fw-bold py-2">✏️ 글쓰기</a><a href="logout" class="btn btn-outline-danger w-100 btn-sm fw-bold">로그아웃</a></div>`;
            }
            if (content) {
                const likeCount=Number(member.receivedLikeCount)||0, dislikeCount=Number(member.receivedDislikeCount)||0;
                const level=Math.floor(likeCount/10)+1, actualLevel=Math.min(level,10); let currentLikes=likeCount%10; if(actualLevel>=10)currentLikes=10;
                const progress=Math.max(0,Math.min(100,currentLikes*10)), remaining=actualLevel>=10?0:10-currentLikes;
                content.innerHTML=`<div class="member-row">이름: <strong>${escapeHtml(member.name)}</strong></div><div class="member-row">아이디: <strong>${escapeHtml(member.username)}</strong></div><div class="member-row">회원번호: <strong>${member.memberNo||'-'}</strong></div><div class="member-row member-level">⭐ 레벨 <strong>${actualLevel}</strong></div><div class="level-section"><div class="level-progress-info"><span>Lv.${actualLevel}</span><span>${actualLevel>=10?'MAX':'Lv.'+(actualLevel+1)}</span></div><div class="level-progress"><div class="level-progress-fill" style="width:${progress}%;"></div></div><div class="level-progress-text"><span>${currentLikes} / 10 좋아요</span><span>${Math.round(progress)}%</span></div><div class="level-progress-remaining">${actualLevel>=10?'🎉 최고 레벨입니다!':`다음 레벨까지 <strong>${remaining}</strong>개`}</div></div><div class="member-row like">👍 받은 좋아요: <strong>${likeCount}</strong></div><div class="member-row dislike">👎 받은 나빠요: <strong>${dislikeCount}</strong></div>`;
            }
        })
        .catch(error => { console.debug("비로그인 상태:",error.message); if(content) content.innerHTML=`<div class="member-error">로그인하면 회원정보가 표시됩니다.</div>`; });
}

function restoreMemberBoxPosition(memberBox) {
    if(!memberBox)return; const savedLeft=localStorage.getItem("gameCommunity_memberBoxLeft"), savedTop=localStorage.getItem("gameCommunity_memberBoxTop");
    if(savedLeft!==null&&savedTop!==null){memberBox.style.position="fixed";memberBox.style.left=savedLeft+"px";memberBox.style.top=savedTop+"px";memberBox.style.right="auto";memberBox.style.bottom="auto";memberBox.style.zIndex="9999";}
}
function makeMemberBoxDraggable(memberBox) {
    if(!memberBox||memberBox.dataset.draggable==="true")return; const dragHandle=memberBox.querySelector(".member-title"); if(!dragHandle)return;
    memberBox.dataset.draggable="true";dragHandle.style.cursor="grab";dragHandle.style.userSelect="none";let isDragging=false,offsetX=0,offsetY=0;
    dragHandle.addEventListener("mousedown",e=>{e.preventDefault();isDragging=true;const rect=memberBox.getBoundingClientRect();offsetX=e.clientX-rect.left;offsetY=e.clientY-rect.top;memberBox.style.position="fixed";memberBox.style.left=rect.left+"px";memberBox.style.top=rect.top+"px";memberBox.style.right="auto";memberBox.style.bottom="auto";memberBox.style.zIndex="9999";dragHandle.style.cursor="grabbing";});
    document.addEventListener("mousemove",e=>{if(!isDragging)return;let left=e.clientX-offsetX,top=e.clientY-offsetY;const maxLeft=Math.max(0,window.innerWidth-memberBox.offsetWidth),maxTop=Math.max(0,window.innerHeight-memberBox.offsetHeight);left=Math.max(0,Math.min(left,maxLeft));top=Math.max(0,Math.min(top,maxTop));memberBox.style.left=left+"px";memberBox.style.top=top+"px";});
    document.addEventListener("mouseup",()=>{if(!isDragging)return;isDragging=false;dragHandle.style.cursor="grab";const rect=memberBox.getBoundingClientRect();localStorage.setItem("gameCommunity_memberBoxLeft",Math.round(rect.left));localStorage.setItem("gameCommunity_memberBoxTop",Math.round(rect.top));});
}
function escapeHtml(value){if(value==null)return"";return String(value).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#039;");}
