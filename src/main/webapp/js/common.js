document.addEventListener("DOMContentLoaded", function () {
    initLayout();
    initMemberInfoBox();
});

function initLayout() {
    // 1버전 프론트의 상단 헤더/카테고리/사이드바 구조를 유지한다.
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
                    <a href="login.html">로그인</a>
                    <a href="signup.html">회원가입</a>
                    <a href="mypage.html">마이페이지</a>
                </div>
            </div>
        </div>
        <nav class="category-nav">
            <div class="custom-container category-list">
                <a href="post.html">격투</a>
                <a href="post.html">레이싱</a>
                <a href="post.html">롤플레잉(RPG) ▾</a>
                <a href="post.html">보드</a>
                <a href="post.html">슈팅(FPS) ▾</a>
                <a href="post.html">스포츠 ▾</a>
                <a href="post.html">시뮬레이션</a>
                <a href="post.html">아케이드</a>
                <a href="post.html">어드벤처</a>
                <a href="post.html">전략 ▾</a>
                <a href="post.html">퍼즐</a>
            </div>
        </nav>
    `;
    document.body.prepend(headerWrapper);

    const mainContent = document.getElementById("main-content");
    if (mainContent && !mainContent.closest(".layout-grid")) {
        const layoutContainer = document.createElement("div");
        layoutContainer.className = "custom-container layout-grid";

        mainContent.classList.add("content-left");
        layoutContainer.appendChild(mainContent);

        const sidebar = document.createElement("aside");
        sidebar.className = "content-right";
        sidebar.innerHTML = `
            <div class="sidebar-card">
                <p class="mb-2 text-muted fw-bold">커뮤니티를 더 즐겁게 이용해보세요!</p>
                <a href="login.html" class="btn btn-primary-custom w-100">로그인 하기</a>
            </div>
        `;
        layoutContainer.appendChild(sidebar);
        headerWrapper.after(layoutContainer);
    }
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
    if (!content) return;

    fetch("member-info", {
        method: "GET",
        credentials: "include"
    })
        .then(response => {
            if (response.status === 401) {
                throw new Error("로그인이 필요합니다.");
            }
            if (!response.ok) {
                throw new Error("회원정보를 불러오지 못했습니다.");
            }
            return response.json();
        })
        .then(member => {
            const likeCount = Number(member.receivedLikeCount) || 0;
            const dislikeCount = Number(member.receivedDislikeCount) || 0;

            const level = Math.floor(likeCount / 10) + 1;
            const actualLevel = Math.min(level, 5);
            let currentLikes = likeCount % 10;
            if (actualLevel >= 5) currentLikes = 10;

            const progress = Math.max(0, Math.min(100, currentLikes * 10));
            const remaining = actualLevel >= 5 ? 0 : 10 - currentLikes;

            content.innerHTML = `
                <div class="member-row">이름: <strong>${escapeHtml(member.name)}</strong></div>
                <div class="member-row">아이디: <strong>${escapeHtml(member.username)}</strong></div>
                <div class="member-row member-level">⭐ 레벨 <strong>${actualLevel}</strong></div>
                <div class="level-section">
                    <div class="level-progress-info">
                        <span>Lv.${actualLevel}</span>
                        <span>${actualLevel >= 5 ? "MAX" : "Lv." + (actualLevel + 1)}</span>
                    </div>
                    <div class="level-progress">
                        <div class="level-progress-fill" style="width: ${progress}%;"></div>
                    </div>
                    <div class="level-progress-text">
                        <span>${currentLikes} / 10 좋아요</span>
                        <span>${Math.round(progress)}%</span>
                    </div>
                    <div class="level-progress-remaining">
                        ${actualLevel >= 5 ? "🎉 최고 레벨입니다!" : `다음 레벨까지 <strong>${remaining}</strong>개`}
                    </div>
                </div>
                <div class="member-row like">👍 받은 좋아요: <strong>${likeCount}</strong></div>
                <div class="member-row dislike">👎 받은 나빠요: <strong>${dislikeCount}</strong></div>
            `;
        })
        .catch(error => {
            console.debug("회원정보 미표시:", error.message);
            content.innerHTML = `<div class="member-error">로그인하면 회원정보가 표시됩니다.</div>`;
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
