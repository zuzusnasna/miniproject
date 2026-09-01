/* =========================================================
   회원정보 영역
   ========================================================= */

/**
 * 회원정보 전용 스타일을 초기화합니다.
 * 현재 스타일은 CSS 파일에서 관리합니다.
 */
function injectMemberInfoStyles() {
    // 회원정보 전용 스타일은 CSS 파일에서 관리합니다.
}


/**
 * 상단바 회원정보 메뉴와 회원정보 패널을 초기화합니다.
 */
function initMemberInfoBox() {
    injectMemberInfoStyles();

    let wrapper = document.getElementById("commonMemberInfo");
    const userMenu = document.querySelector(".user-menu");

    // 회원정보 영역이 없다면 상단바 안에 생성합니다.
    if (!wrapper) {
        wrapper = document.createElement("div");
        wrapper.id = "commonMemberInfo";
        wrapper.innerHTML = `
            <div class="common-member-info" hidden>
                <div class="member-title">
                    <span>👤 회원정보</span>
                    <button
                        type="button"
                        class="member-toggle"
                        aria-label="회원정보 접기"
                    >
                        −
                    </button>
                </div>
                <div id="commonMemberContent">Not login status</div>
            </div>
        `;

        // 회원정보 패널은 상단바의 회원정보 버튼 아래에서 열립니다.
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

    // 초기 상태는 비로그인 상태입니다.
    if (trigger) {
        trigger.disabled = true;
        trigger.textContent = "Not login status";
        trigger.setAttribute("aria-expanded", "false");
        trigger.classList.remove("active");
    }

    // 상단바 회원정보 버튼과 패널을 연결합니다.
    if (trigger && trigger.dataset.bound !== "true") {
        trigger.dataset.bound = "true";

        trigger.addEventListener("click", function (event) {
            event.preventDefault();

            if (trigger.disabled) return;

            const isOpen = !memberBox.hidden;
            memberBox.hidden = isOpen;

            trigger.setAttribute(
                "aria-expanded",
                String(!isOpen)
            );

            trigger.classList.toggle("active", !isOpen);
        });
    }

    // 패널 내부 접기 버튼입니다.
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

    // 실제 로그인 사용자 정보를 조회합니다.
    loadMemberInfo();
}


/**
 * 현재 로그인한 회원 정보를 조회하고 회원정보 패널에 표시합니다.
 */
function loadMemberInfo() {
    const content = document.getElementById("commonMemberContent");
    const trigger = document.getElementById("memberMenuTrigger");

    fetch("member-info", {
        method: "GET",
        credentials: "include"
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("로그인이 필요합니다.");
            }

            return response.json();
        })
        .then(member => {
            // 로그인 상태에서는 인증 링크를 로그아웃으로 변경합니다.
            const authLink =
                document.getElementById("navAuthLink") ||
                document.querySelector('a[href*="login.html"]');

            if (authLink) {
                authLink.textContent = "로그아웃";
                authLink.href = "logout";
            }

            // 회원정보 메뉴를 활성화합니다.
            if (trigger) {
                trigger.disabled = false;
                trigger.innerHTML =
                    '회원정보 <span class="member-menu-arrow">▼</span>';
                trigger.setAttribute("aria-expanded", "false");
            }

            if (!content) return;

            // 좋아요 수를 기준으로 현재 레벨과 진행률을 계산합니다.
            const likeCount = Number(member.receivedLikeCount) || 0;
            const dislikeCount = Number(member.receivedDislikeCount) || 0;

            const actualLevel = Math.min(
                Math.floor(likeCount / 10) + 1,
                10
            );

            const currentLikes =
                actualLevel >= 10 ? 10 : likeCount % 10;

            const progress = Math.max(
                0,
                Math.min(100, currentLikes * 10)
            );

            const remaining =
                actualLevel >= 10 ? 0 : 10 - currentLikes;

            content.innerHTML = `
                <div class="member-row">
                    이름: <strong>${escapeHtml(member.name)}</strong>
                </div>

                <div class="member-row">
                    아이디: <strong>${escapeHtml(member.username)}</strong>
                </div>

                <div class="member-row member-level">
                    ⭐ 레벨 <strong>${actualLevel}</strong>
                </div>

                <div class="level-section">
                    <div class="level-progress-info">
                        <span>Lv.${actualLevel}</span>
                        <span>
                            ${
                                actualLevel >= 10
                                    ? "MAX"
                                    : "Lv." + (actualLevel + 1)
                            }
                        </span>
                    </div>

                    <div class="level-progress">
                        <div
                            class="level-progress-fill"
                            style="width:${progress}%;"
                        ></div>
                    </div>

                    <div class="level-progress-text">
                        <span>${currentLikes} / 10 좋아요</span>
                        <span>${Math.round(progress)}%</span>
                    </div>

                    <div class="level-progress-remaining">
                        ${
                            actualLevel >= 10
                                ? "🎉 최고 레벨입니다!"
                                : `다음 레벨까지 <strong>${remaining}</strong>개`
                        }
                    </div>
                </div>

                <div class="member-row like">
                    👍 받은 좋아요: <strong>${likeCount}</strong>
                </div>

                <div class="member-row dislike">
                    👎 받은 나빠요: <strong>${dislikeCount}</strong>
                </div>
            `;

            // 관리자 신청 자격이 있다면 신청 영역을 추가합니다.
            loadCategoryManagerRequest(content, likeCount);
        })
        .catch(error => {
            // 로그인하지 않은 경우 비로그인 상태로 되돌립니다.
            console.debug("비로그인 상태:", error.message);

            const memberBox = document.querySelector(
                "#commonMemberInfo .common-member-info"
            );

            if (memberBox) {
                memberBox.hidden = true;
            }

            if (trigger) {
                trigger.disabled = true;
                trigger.textContent = "Not login status";
                trigger.setAttribute("aria-expanded", "false");
                trigger.classList.remove("active");
            }

            if (content) {
                content.textContent = "Not login status";
            }
        });
}


/* =========================================================
   카테고리 관리자 신청
   ========================================================= */

/**
 * 좋아요 수가 50개 이상인 회원에게
 * 카테고리 관리자 신청 영역을 표시합니다.
 */
function loadCategoryManagerRequest(content, likeCount) {
    if (likeCount < 50) return;

    fetch("category-manager-request", {
        credentials: "include"
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(
                    "카테고리 관리자 신청 정보를 불러오지 못했습니다."
                );
            }

            return response.json();
        })
        .then(data => {
            if (
                !data ||
                !data.success ||
                !data.eligible ||
                data.status === "APPROVED"
            ) {
                return;
            }

            const box = document.createElement("div");
            box.id = "categoryManagerRequest";

            // 이미 신청 중인 경우에는 대기 상태만 표시합니다.
            if (data.status === "PENDING") {
                box.innerHTML = `
                    <strong>🎮 카테고리 관리자</strong>
                    <div class="request-message">
                        권한 신청이 승인 대기 중입니다.
                    </div>
                `;
            } else {
                // 관리할 게임 목록을 select 옵션으로 생성합니다.
                const options = (data.games || [])
                    .map(game => `
                        <option value="${game.categoryId}">
                            ${escapeHtml(game.categoryName)}
                        </option>
                    `)
                    .join("");

                box.innerHTML = `
                    <strong>🎮 카테고리 관리자 신청</strong>

                    <div class="request-message">
                        받은 좋아요 ${data.likes}개로 신청할 수 있습니다.
                    </div>

                    <select id="managerGameSelect">
                        <option value="">관리할 게임 선택</option>
                        ${options}
                    </select>

                    <button
                        type="button"
                        id="managerRequestButton"
                    >
                        신청하기
                    </button>

                    <div
                        id="managerRequestMessage"
                        class="request-message"
                    ></div>
                `;

                box.querySelector(
                    "#managerRequestButton"
                ).addEventListener(
                    "click",
                    submitCategoryManagerRequest
                );
            }

            content.appendChild(box);
        })
        .catch(error => {
            console.debug(
                "카테고리 관리자 신청 정보:",
                error.message
            );
        });
}


/**
 * 선택한 게임의 카테고리 관리자 권한을 신청합니다.
 */
function submitCategoryManagerRequest() {
    const select = document.getElementById("managerGameSelect");
    const button = document.getElementById("managerRequestButton");
    const message = document.getElementById("managerRequestMessage");

    if (!select || !button || !message) return;

    // 관리할 게임을 선택하지 않은 경우 신청하지 않습니다.
    if (!select.value) {
        message.textContent = "관리할 게임을 선택해주세요.";
        message.style.color = "#d32f2f";
        return;
    }

    button.disabled = true;

    const body = new URLSearchParams({
        categoryId: select.value
    });

    fetch("category-manager-request", {
        method: "POST",
        credentials: "include",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body
    })
        .then(response => response.json())
        .then(data => {
            message.textContent =
                data.message ||
                (
                    data.success
                        ? "신청되었습니다."
                        : "신청에 실패했습니다."
                );

            message.style.color = data.success
                ? "#198754"
                : "#d32f2f";

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


/* =========================================================
   HTML 이스케이프
   ========================================================= */

/**
 * 사용자 데이터를 HTML에 삽입하기 전에 특수문자를 변환합니다.
 */
function escapeHtml(value) {
    if (value == null) return "";

    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/\"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
