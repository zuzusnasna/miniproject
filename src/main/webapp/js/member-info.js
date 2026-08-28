function injectMemberInfoStyles() {
    if (document.getElementById("member-info-style")) return;
    const style = document.createElement("style");
    style.id = "member-info-style";
    style.textContent = `
        .common-member-info { position:fixed; width:240px; box-sizing:border-box; background:#fff; border:1px solid #ddd; border-radius:8px; box-shadow:0 2px 8px rgba(0,0,0,.1); overflow:hidden; z-index:9999; }
        .common-member-info .member-title { display:flex; align-items:center; justify-content:space-between; gap:10px; padding:12px 15px; background:#fff; cursor:grab; user-select:none; font-weight:700; }
        .common-member-info .member-toggle { display:flex; align-items:center; justify-content:center; width:26px; height:26px; padding:0; border:0; border-radius:4px; background:transparent; color:#555; font-size:22px; line-height:1; cursor:pointer; flex:0 0 26px; }
        .common-member-info .member-toggle:hover { background:#f1f1f1; color:#111; }
        .common-member-info #commonMemberContent { padding:0 15px 15px; }
        .common-member-info #commonMemberContent[hidden] { display:none !important; }
        .common-member-info.collapsed { width:200px; }
        .common-member-info.collapsed .member-title { padding:9px 12px; }
    `;
    document.head.appendChild(style);
}

function initMemberInfoBox() {
    injectMemberInfoStyles();

    let wrapper = document.getElementById("commonMemberInfo");
    if (!wrapper) {
        wrapper = document.createElement("div");
        wrapper.id = "commonMemberInfo";
        wrapper.innerHTML = `
            <div class="common-member-info">
                <div class="member-title">
                    <span>👤 회원정보</span>
                    <button type="button" class="member-toggle" aria-label="회원정보 접기">−</button>
                </div>
                <div id="commonMemberContent">회원 정보를 불러오는 중...</div>
            </div>`;
        document.body.appendChild(wrapper);
    }

    const memberBox = wrapper.querySelector(".common-member-info");
    const button = memberBox.querySelector(".member-toggle");
    const content = memberBox.querySelector("#commonMemberContent");

    if (button && content && button.dataset.bound !== "true") {
        button.dataset.bound = "true";
        const saved = localStorage.getItem("gameCommunity_memberBoxCollapsed") === "true";
        button.onclick = function(event) {
            event.preventDefault();
            event.stopPropagation();
            const collapsed = content.hidden === false;
            content.hidden = collapsed;
            button.textContent = collapsed ? "+" : "−";
            button.setAttribute("aria-label", collapsed ? "회원정보 펼치기" : "회원정보 접기");
            localStorage.setItem("gameCommunity_memberBoxCollapsed", String(collapsed));
            memberBox.classList.toggle("collapsed", collapsed);
            keepMemberBoxInsideViewport(memberBox);
        };
        content.hidden = saved;
        button.textContent = saved ? "+" : "−";
        button.setAttribute("aria-label", saved ? "회원정보 펼치기" : "회원정보 접기");
        memberBox.classList.toggle("collapsed", saved);
    }

    restoreMemberBoxPosition(memberBox);
    makeMemberBoxDraggable(memberBox);
    keepMemberBoxInsideViewport(memberBox);
    loadMemberInfo();
}

function loadMemberInfo() {
    const content = document.getElementById("commonMemberContent");
    fetch("member-info", {method:"GET", credentials:"include"})
        .then(response => { if (!response.ok) throw new Error("로그인이 필요합니다."); return response.json(); })
        .then(member => {
            const authLink = document.getElementById("navAuthLink") || document.querySelector('a[href*="login.html"]');
            if (authLink) { authLink.textContent = "로그아웃"; authLink.href = "logout"; }
            if (!content) return;
            const likeCount = Number(member.receivedLikeCount) || 0;
            const dislikeCount = Number(member.receivedDislikeCount) || 0;
            const actualLevel = Math.min(Math.floor(likeCount / 10) + 1, 10);
            const currentLikes = actualLevel >= 10 ? 10 : likeCount % 10;
            const progress = Math.max(0, Math.min(100, currentLikes * 10));
            const remaining = actualLevel >= 10 ? 0 : 10 - currentLikes;
            content.innerHTML = `
                <div class="member-row">이름: <strong>${escapeHtml(member.name)}</strong></div>
                <div class="member-row">아이디: <strong>${escapeHtml(member.username)}</strong></div>
                <div class="member-row member-level">⭐ 레벨 <strong>${actualLevel}</strong></div>
                <div class="level-section">
                    <div class="level-progress-info"><span>Lv.${actualLevel}</span><span>${actualLevel >= 10 ? "MAX" : "Lv." + (actualLevel + 1)}</span></div>
                    <div class="level-progress"><div class="level-progress-fill" style="width:${progress}%;"></div></div>
                    <div class="level-progress-text"><span>${currentLikes} / 10 좋아요</span><span>${Math.round(progress)}%</span></div>
                    <div class="level-progress-remaining">${actualLevel >= 10 ? "🎉 최고 레벨입니다!" : `다음 레벨까지 <strong>${remaining}</strong>개`}</div>
                </div>
                <div class="member-row like">👍 받은 좋아요: <strong>${likeCount}</strong></div>
                <div class="member-row dislike">👎 받은 나빠요: <strong>${dislikeCount}</strong></div>`;
        })
        .catch(error => {
            console.debug("비로그인 상태:", error.message);
            if (content) content.innerHTML = `<div class="member-error">로그인하면 회원정보가 표시됩니다.</div>`;
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

function keepMemberBoxInsideViewport(memberBox) {
    if (!memberBox) return;
    const rect = memberBox.getBoundingClientRect();
    const maxLeft = Math.max(0, window.innerWidth - memberBox.offsetWidth);
    const maxTop = Math.max(0, window.innerHeight - memberBox.offsetHeight);
    const left = Math.max(0, Math.min(rect.left, maxLeft));
    const top = Math.max(0, Math.min(rect.top, maxTop));
    memberBox.style.left = left + "px";
    memberBox.style.top = top + "px";
    memberBox.style.right = "auto";
    memberBox.style.bottom = "auto";
    localStorage.setItem("gameCommunity_memberBoxLeft", Math.round(left));
    localStorage.setItem("gameCommunity_memberBoxTop", Math.round(top));
}

function makeMemberBoxDraggable(memberBox) {
    if (!memberBox || memberBox.dataset.draggable === "true") return;
    const dragHandle = memberBox.querySelector(".member-title");
    if (!dragHandle) return;
    memberBox.dataset.draggable = "true";
    let isDragging = false, offsetX = 0, offsetY = 0;
    dragHandle.addEventListener("mousedown", function(event) {
        if (event.target.closest(".member-toggle")) return;
        event.preventDefault();
        isDragging = true;
        const rect = memberBox.getBoundingClientRect();
        offsetX = event.clientX - rect.left;
        offsetY = event.clientY - rect.top;
        memberBox.style.position = "fixed";
        memberBox.style.left = rect.left + "px";
        memberBox.style.top = rect.top + "px";
        memberBox.style.right = "auto";
        memberBox.style.bottom = "auto";
    });
    document.addEventListener("mousemove", function(event) {
        if (!isDragging) return;
        let left = event.clientX - offsetX;
        let top = event.clientY - offsetY;
        const maxLeft = Math.max(0, window.innerWidth - memberBox.offsetWidth);
        const maxTop = Math.max(0, window.innerHeight - memberBox.offsetHeight);
        memberBox.style.left = Math.max(0, Math.min(left, maxLeft)) + "px";
        memberBox.style.top = Math.max(0, Math.min(top, maxTop)) + "px";
    });
    document.addEventListener("mouseup", function() {
        if (!isDragging) return;
        isDragging = false;
        keepMemberBoxInsideViewport(memberBox);
    });
}

window.addEventListener("resize", function() {
    const memberBox = document.querySelector(".common-member-info");
    if (memberBox) keepMemberBoxInsideViewport(memberBox);
});

function escapeHtml(value) {
    if (value == null) return "";
    return String(value).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/\"/g,"&quot;").replace(/'/g,"&#039;");
}
