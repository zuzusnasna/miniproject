document.addEventListener("DOMContentLoaded", function () {
    // 1. 공통 헤더/네비바 및 레이아웃 컨테이너 생성
    initLayout();

    // 2. 회원정보 드래그 박스 생성
    initMemberInfoBox();
});

function initLayout() {
    // 1280px 고정 스타일 최우선 주입
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
                    <a href="signup.html">회원가입</a>
                    <a href="mypage.html">마이페이지</a>
                </div>
            </div>
        </div>
        <nav class="category-nav">
            <div class="custom-container category-list">
                <a href="post.html?categoryId=1">격투</a>
                <a href="post.html?categoryId=2">레이싱</a>
                <a href="post.html?categoryId=3">롤플레잉(RPG) ▾</a>
                <a href="post.html?categoryId=4">보드</a>
                <a href="post.html?categoryId=5">슈팅(FPS) ▾</a>
                <a href="post.html?categoryId=6">스포츠 ▾</a>
                <a href="post.html?categoryId=7">시뮬레이션</a>
                <a href="post.html?categoryId=8">아케이드</a>
                <a href="post.html?categoryId=9">어드벤처</a>
                <a href="post.html?categoryId=10">전략 ▾</a>
                <a href="post.html?categoryId=11">퍼즐</a>
            </div>
        </nav>
    `;
    document.body.prepend(headerWrapper);

    const currentPath = window.location.pathname;

    // post-write 및 post 페이지에서는 우측 사이드바 생성 제외
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

// 좁아지는 모바일 레이아웃 스타일을 강제로 1280px로 고정하는 CSS 함수
function injectCustomStyles() {
    if (document.getElementById("custom-layout-style")) return;

    const style = document.createElement("style");
    style.id = "custom-layout-style";
    style.innerHTML = `
        /* 헤더 및 전체 컨테이너 폭 1280px 고정 */
        body .custom-container,
        body .site-header-wrapper .custom-container,
        body .header-inner,
        body .category-list {
            width: 100% !important;
            max-width: 1280px !important;
            margin: 0 auto !important;
            padding: 0 16px !important;
            box-sizing: border-box !important;
        }

        /* post.html 등 개별 페이지 기본 container 폭 제한 해제 */
        body #main-content,
        body .content-left,
        body .container,
        body .container-sm,
        body .container-md,
        body .container-lg,
        body .container-xl {
            width: 100% !important;
            max-width: 1280px !important;
            margin-left: auto !important;
            margin-right: auto !important;
        }

        /* 2열 배치 flex 그리드 (메인 홈 전용) */
        body .layout-grid {
            display: flex !important;
            flex-direction: row !important;
            gap: 24px !important;
            width: 100% !important;
            max-width: 1280px !important;
            margin: 24px auto !important;
            align-items: flex-start !important;
        }

        body .content-left {
            flex: 1 1 0% !important;
            min-width: 0 !important;
        }

        body .content-right {
            width: 300px !important;
            flex-shrink: 0 !important;
        }
    `;
    document.head.appendChild(style);
}

function initMemberInfoBox() {
    let wrapper = document.getElementById("commonMemberInfo");

    if (!wrapper) {
        wrapper = document.createElement("div");
        wrapper.id = "commonMemberInfo";
        wrapper.innerHTML = `
            <div class="common-member-info">
                <div class="member-title">👤 회원정보</div>
                <div id="commonMemberContent">회원 정보를 불러오는 중...</div>
            </div>
        `;
        document.body.appendChild(wrapper);
    }

    const memberBox = wrapper.querySelector(".common-member-info");

    restoreMemberBoxPosition(memberBox);
    makeMemberBoxDraggable(memberBox);
    loadMemberInfo();
}

function loadMemberInfo() {
    const content = document.getElementById("commonMemberContent");

    fetch("member-info", {
        method: "GET",
        credentials: "include"
    })
        .then(response => {
            if (response.status === 401 || !response.ok) {
                throw new Error("로그인이 필요합니다.");
            }
            return response.json();
        })
        .then(member => {
            const authLink = document.getElementById("navAuthLink") || document.querySelector('a[href*="login.html"]');
            if (authLink) {
                authLink.textContent = "로그아웃";
                authLink.href = "logout";
            }

            // 사이드바가 있는 페이지에서만 사이드바 카드 업데이트 (오류 발생 방지)
            const sidebarCard = document.querySelector(".content-right .sidebar-card");
            if (sidebarCard) {
                sidebarCard.className = "sidebar-card shadow-sm border-0 rounded-3 p-3 bg-white";
                sidebarCard.innerHTML = `
                    <div class="text-center">
                        <p class="mb-3 fw-bold text-dark fs-6">👋 <span class="text-primary">${escapeHtml(member.name)}</span>님 환영합니다!</p>
                        <a href="post-write.html" class="btn btn-primary w-100 mb-2 fw-bold py-2">✏️ 글쓰기</a>
                        <a href="logout" class="btn btn-outline-danger w-100 btn-sm fw-bold">로그아웃</a>
                    </div>
                `;
            }

            if (content) {
                const likeCount = Number(member.receivedLikeCount) || 0;
                const dislikeCount = Number(member.receivedDislikeCount) || 0;

                const level = Math.floor(likeCount / 10) + 1;
                const actualLevel = Math.min(level, 10);
                let currentLikes = likeCount % 10;
                if (actualLevel >= 10) currentLikes = 10;

                const progress = Math.max(0, Math.min(100, currentLikes * 10));
                const remaining = actualLevel >= 10 ? 0 : 10 - currentLikes;

                content.innerHTML = `
                    <div class="member-row">이름: <strong>${escapeHtml(member.name)}</strong></div>
                    <div class="member-row">아이디: <strong>${escapeHtml(member.username)}</strong></div>
                    <div class="member-row">회원번호: <strong>${member.memberNo || '-'}</strong></div>
                    <div class="member-row member-level">⭐ 레벨 <strong>${actualLevel}</strong></div>
                    <div class="level-section">
                        <div class="level-progress-info">
                            <span>Lv.${actualLevel}</span>
                            <span>${actualLevel >= 10 ? "MAX" : "Lv." + (actualLevel + 1)}</span>
                        </div>
                        <div class="level-progress">
                            <div class="level-progress-fill" style="width: ${progress}%;"></div>
                        </div>
                        <div class="level-progress-text">
                            <span>${currentLikes} / 10 좋아요</span>
                            <span>${Math.round(progress)}%</span>
                        </div>
                        <div class="level-progress-remaining">
                            ${actualLevel >= 10 ? "🎉 최고 레벨입니다!" : `다음 레벨까지 <strong>${remaining}</strong>개`}
                        </div>
                    </div>
                    <div class="member-row like">👍 받은 좋아요: <strong>${likeCount}</strong></div>
                    <div class="member-row dislike">👎 받은 나빠요: <strong>${dislikeCount}</strong></div>
                `;
            }
        })
        .catch(error => {
            console.debug("비로그인 상태:", error.message);
            if (content) {
                content.innerHTML = `<div class="member-error">로그인하면 회원정보가 표시됩니다.</div>`;
            }
        });
}

function restoreMemberBoxPosition(memberBox) {
    if (!memberBox) return;

    const savedLeft = localStorage.getItem("gameCommunity_memberBoxLeft");
    const savedTop = localStorage.getItem("gameCommunity_memberBoxTop");

    if (savedLeft !== null && savedTop !== null) {
        memberBox.style.position = "fixed";
        memberBox.style.left = savedLeft + "px";
        memberBox.style.top = savedTop + "px";
        memberBox.style.right = "auto";
        memberBox.style.bottom = "auto";
        memberBox.style.zIndex = "9999";
    }
}

function makeMemberBoxDraggable(memberBox) {
    if (!memberBox || memberBox.dataset.draggable === "true") return;

    const dragHandle = memberBox.querySelector(".member-title");
    if (!dragHandle) return;

    memberBox.dataset.draggable = "true";
    dragHandle.style.cursor = "grab";
    dragHandle.style.userSelect = "none";

    let isDragging = false;
    let offsetX = 0;
    let offsetY = 0;

    dragHandle.addEventListener("mousedown", function (e) {
        e.preventDefault();
        isDragging = true;

        const rect = memberBox.getBoundingClientRect();
        offsetX = e.clientX - rect.left;
        offsetY = e.clientY - rect.top;

        memberBox.style.position = "fixed";
        memberBox.style.left = rect.left + "px";
        memberBox.style.top = rect.top + "px";
        memberBox.style.right = "auto";
        memberBox.style.bottom = "auto";
        memberBox.style.zIndex = "9999";
        dragHandle.style.cursor = "grabbing";
    });

    document.addEventListener("mousemove", function (e) {
        if (!isDragging) return;

        let left = e.clientX - offsetX;
        let top = e.clientY - offsetY;
        const maxLeft = Math.max(0, window.innerWidth - memberBox.offsetWidth);
        const maxTop = Math.max(0, window.innerHeight - memberBox.offsetHeight);

        left = Math.max(0, Math.min(left, maxLeft));
        top = Math.max(0, Math.min(top, maxTop));

        memberBox.style.left = left + "px";
        memberBox.style.top = top + "px";
    });

    document.addEventListener("mouseup", function () {
        if (!isDragging) return;
        isDragging = false;
        dragHandle.style.cursor = "grab";

        const rect = memberBox.getBoundingClientRect();
        localStorage.setItem("gameCommunity_memberBoxLeft", Math.round(rect.left));
        localStorage.setItem("gameCommunity_memberBoxTop", Math.round(rect.top));
    });
}

function escapeHtml(value) {
    if (value == null) return "";
    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}