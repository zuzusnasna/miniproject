function injectMemberInfoStyles() {
    // 회원정보 전용 스타일은 CSS 파일에서 관리한다.
}

function initMemberInfoBox() {
    injectMemberInfoStyles();

    let wrapper = document.getElementById("commonMemberInfo");
    const userMenu = document.querySelector(".user-menu");

    if (!wrapper) {
        wrapper = document.createElement("div");
        wrapper.id = "commonMemberInfo";
        wrapper.innerHTML = `
            <div class="common-member-info" hidden>
                <div class="member-title">
                    <span>👤 회원정보</span>
                    <button type="button" class="member-toggle" aria-label="회원정보 접기">−</button>
                </div>
                <div id="commonMemberContent">Not login status</div>
            </div>`;

        // 회원정보 창은 상단바의 회원정보 버튼 바로 아래에서 열리도록 한다.
        if (userMenu) {
            userMenu.appendChild(wrapper);
        } else {
            document.body.appendChild(wrapper);
        }
    }

    const memberBox = wrapper.querySelector(".common-member-info");
    const button = memberBox?.querySelector(".member-toggle");
    const content = memberBox?.querySelector("#commonMemberContent");
    const trigger = document.getElementById("memberMenuTrigger");

    if (!memberBox || !content) return;

    // 기본 상태는 비로그인 상태로 표시한다.
    if (trigger) {
        trigger.disabled = true;
        trigger.textContent = "Not login status";
        trigger.setAttribute("aria-expanded", "false");
        trigger.classList.remove("active");
    }

    // 상단바 회원정보 버튼과 패널을 연결한다.
    if (trigger && trigger.dataset.bound !== "true") {
        trigger.dataset.bound = "true";
        trigger.addEventListener("click", function (event) {
            event.preventDefault();
            if (trigger.disabled) return;

            const isOpen = !memberBox.hidden;
            memberBox.hidden = isOpen;
            trigger.setAttribute("aria-expanded", String(!isOpen));
            trigger.classList.toggle("active", !isOpen);
        });
    }

    // 패널 내부 접기 버튼.
    if (button && button.dataset.bound !== "true") {
        button.dataset.bound = "true";
        button.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();
            memberBox.hidden = true;
            if (trigger) {
                trigger.setAttribute("aria-expanded", "false");
                trigger.classList.remove("active");
            }
        });
    }

    loadMemberInfo();
}

function loadMemberInfo() {
    const content = document.getElementById("commonMemberContent");
    const trigger = document.getElementById("memberMenuTrigger");

    fetch("member-info", { method: "GET", credentials: "include" })
        .then(response => {
            if (!response.ok) throw new Error("로그인이 필요합니다.");
            return response.json();
        })
        .then(member => {
            const authLink = document.getElementById("navAuthLink") || document.querySelector('a[href*="login.html"]');
            if (authLink) {
                authLink.textContent = "로그아웃";
                authLink.href = "logout";
            }

            if (trigger) {
                trigger.disabled = false;
                trigger.innerHTML = '회원정보 <span class="member-menu-arrow">▼</span>';
                trigger.setAttribute("aria-expanded", "false");
            }

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

            loadCategoryManagerRequest(content, likeCount);
        })
        .catch(error => {
            console.debug("비로그인 상태:", error.message);

            const memberBox = document.querySelector("#commonMemberInfo .common-member-info");
            if (memberBox) memberBox.hidden = true;

            if (trigger) {
                trigger.disabled = true;
                trigger.textContent = "Not login status";
                trigger.setAttribute("aria-expanded", "false");
                trigger.classList.remove("active");
            }

            if (content) content.textContent = "Not login status";
        });
}

function loadCategoryManagerRequest(content, likeCount) {
    if (likeCount < 50) return;

    fetch("category-manager-request", { credentials: "include" })
        .then(response => {
            if (!response.ok) throw new Error("카테고리 관리자 신청 정보를 불러오지 못했습니다.");
            return response.json();
        })
        .then(data => {
            if (!data || !data.success || !data.eligible || data.status === "APPROVED") return;

            const box = document.createElement("div");
            box.id = "categoryManagerRequest";

            if (data.status === "PENDING") {
                box.innerHTML = '<strong>🎮 카테고리 관리자</strong><div class="request-message">권한 신청이 승인 대기 중입니다.</div>';
            } else {
                const options = (data.games || [])
                    .map(game => `<option value="${game.categoryId}">${escapeHtml(game.categoryName)}</option>`)
                    .join("");

                box.innerHTML = `
                    <strong>🎮 카테고리 관리자 신청</strong>
                    <div class="request-message">받은 좋아요 ${data.likes}개로 신청할 수 있습니다.</div>
                    <select id="managerGameSelect"><option value="">관리할 게임 선택</option>${options}</select>
                    <button type="button" id="managerRequestButton">신청하기</button>
                    <div id="managerRequestMessage" class="request-message"></div>`;

                box.querySelector("#managerRequestButton").addEventListener("click", submitCategoryManagerRequest);
            }

            content.appendChild(box);
        })
        .catch(error => console.debug("카테고리 관리자 신청 정보:", error.message));
}

function submitCategoryManagerRequest() {
    const select = document.getElementById("managerGameSelect");
    const button = document.getElementById("managerRequestButton");
    const message = document.getElementById("managerRequestMessage");
    if (!select || !button || !message) return;

    if (!select.value) {
        message.textContent = "관리할 게임을 선택해주세요.";
        message.style.color = "#d32f2f";
        return;
    }

    button.disabled = true;
    const body = new URLSearchParams({ categoryId: select.value });

    fetch("category-manager-request", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body
    })
        .then(response => response.json())
        .then(data => {
            message.textContent = data.message || (data.success ? "신청되었습니다." : "신청에 실패했습니다.");
            message.style.color = data.success ? "#198754" : "#d32f2f";
            if (data.success) {
                button.textContent = "승인 대기 중";
            } else {
                button.disabled = false;
            }
        })
        .catch(() => {
            message.textContent = "서버 통신 오류가 발생했습니다.";
            message.style.color = "#d32f2f";
            button.disabled = false;
        });
}

function escapeHtml(value) {
    if (value == null) return "";
    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/\"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
