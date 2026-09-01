/* =========================================================
   카테고리 관리자 / 게시판 생성 신청 관리
   ========================================================= */

/**
 * 마이페이지에서 관리자 관련 기능을 초기화합니다.
 *
 * 1. 카테고리 관리자 : 내 게임의 게시판 생성 요청
 * 2. 시스템 관리자   : 게시판 생성 요청 승인 / 거절
 * 3. 시스템 관리자   : 카테고리 관리자 권한 승인 / 거절
 */
function initCategoryManagerRequestAdmin() {
    initCategoryBoardCreateSection();
    initAdminCategoryApproval();
    initCategoryManagerApproval();
}


/* =========================================================
   카테고리 관리자 - 내 게임 게시판 생성
   ========================================================= */

/**
 * 현재 로그인한 사용자가 카테고리 관리자라면
 * 마이페이지의 '게시판 생성' 영역을 보여줍니다.
 *
 * GET /category-create
 * -> 현재 사용자의 관리자 여부와 관리 중인 게임 정보를 조회합니다.
 */
async function initCategoryBoardCreateSection() {
    const section = document.getElementById("categoryManagerSection");

    // 해당 영역이 없는 페이지에서는 아무 작업도 하지 않습니다.
    if (!section) return;

    try {
        const response = await fetch("category-create", {
            credentials: "include"
        });

        if (!response.ok) return;

        const data = await response.json();

        // 카테고리 관리자가 아니면 생성 영역을 표시하지 않습니다.
        if (!data.isManager) return;

        section.style.display = "block";

        // 관리 중인 게임 이름을 화면에 표시합니다.
        const label = document.getElementById("managedGameLabel");

        if (label) {
            label.textContent = data.gameName || "현재 관리 게임";
        }
    } catch (error) {
        console.debug("게시판 생성 영역을 표시하지 않습니다.", error);
    }
}

/**
 * 마이페이지의 '생성 요청' 버튼에서 호출됩니다.
 *
 * 사용자가 입력한 게시판 이름을 서버로 전달합니다.
 * CategoryCreateServlet에서는 승인 대기 상태(IS_ACTIVE='N')로 저장합니다.
 */
async function requestCreateCategory() {
    const input = document.getElementById("newCategoryName");
    const message = document.getElementById("createCategoryMsg");
    const button = document.querySelector("#categoryManagerSection button");

    if (!input) return;

    const categoryName = input.value.trim();

    // 게시판 이름은 필수입니다.
    if (!categoryName) {
        setCreateCategoryMessage("게시판 이름을 입력해주세요.", false);
        input.focus();
        return;
    }

    // 중복 요청을 막기 위해 요청 중에는 버튼을 비활성화합니다.
    if (button) button.disabled = true;

    setCreateCategoryMessage("생성 요청을 처리하는 중입니다...", null);

    try {
        const response = await fetch("category-create", {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({
                categoryName
            })
        });

        const data = await response.json();

        if (!response.ok || !data.success) {
            setCreateCategoryMessage(
                data.message || "게시판 생성 요청에 실패했습니다.",
                false
            );
            return;
        }

        // 성공하면 입력창을 비웁니다.
        input.value = "";

        setCreateCategoryMessage(
            data.message || "게시판 생성 요청이 완료되었습니다. 시스템 관리자 승인 후 공개됩니다.",
            true
        );
    } catch (error) {
        console.error("게시판 생성 요청 실패:", error);
        setCreateCategoryMessage("서버 통신 오류가 발생했습니다.", false);
    } finally {
        // 성공 / 실패와 관계없이 버튼을 다시 활성화합니다.
        if (button) button.disabled = false;
    }
}

/**
 * 게시판 생성 요청 결과 메시지의 문구와 색상을 변경합니다.
 *
 * success
 * - true  : 성공
 * - false : 실패
 * - null  : 처리 중
 */
function setCreateCategoryMessage(text, success) {
    const message = document.getElementById("createCategoryMsg");

    if (!message) return;

    message.textContent = text;
    message.className = "small mt-1";

    if (success === true) {
        message.classList.add("text-success");
    } else if (success === false) {
        message.classList.add("text-danger");
    } else {
        message.classList.add("text-muted");
    }
}


/* =========================================================
   시스템 관리자 - 게시판 생성 승인 / 거절
   ========================================================= */

/**
 * 시스템 관리자에게 승인 대기 중인 게시판 목록을 보여줍니다.
 *
 * GET /admin/categories
 * -> 승인 대기 상태인 게시판 목록을 조회합니다.
 */
async function initAdminCategoryApproval() {
    const section = document.getElementById("adminApprovalSection");

    if (!section) return;

    try {
        const response = await fetch("admin/categories", {
            credentials: "include"
        });

        // 일반 사용자에게는 관리자 영역을 표시하지 않습니다.
        if (response.status === 401 || response.status === 403) {
            return;
        }

        if (!response.ok) return;

        const list = await response.json();

        section.style.display = "block";
        renderPendingCategories(list);
    } catch (error) {
        console.debug("게시판 승인 영역을 표시하지 않습니다.", error);
    }
}

/**
 * 승인 대기 게시판 목록을 테이블에 출력합니다.
 */
function renderPendingCategories(list) {
    const tbody = document.getElementById("pendingTableBody");

    if (!tbody) return;

    tbody.innerHTML = "";

    // 승인 대기 목록이 없을 때 안내 문구를 출력합니다.
    if (!Array.isArray(list) || list.length === 0) {
        tbody.innerHTML =
            '<tr><td colspan="4" class="text-muted">승인 대기 중인 게시판이 없습니다.</td></tr>';
        return;
    }

    list.forEach(category => {
        const row = document.createElement("tr");

        row.innerHTML = `
            <td class="fw-bold">
                ${escapeHtml(category.parentCategoryName || "미지정")}
            </td>
            <td class="fw-bold">
                ${escapeHtml(category.categoryName)}
            </td>
            <td>
                ${escapeHtml(category.createdAt || "-")}
            </td>
            <td>
                <button
                    type="button"
                    class="btn btn-sm btn-success me-1"
                    onclick="processCategoryApproval(${Number(category.categoryId)}, 'approve')">
                    승인
                </button>
                <button
                    type="button"
                    class="btn btn-sm btn-danger"
                    onclick="processCategoryApproval(${Number(category.categoryId)}, 'reject')">
                    거절
                </button>
            </td>
        `;

        tbody.appendChild(row);
    });
}

/**
 * 게시판 생성 요청을 승인하거나 거절합니다.
 *
 * POST /admin/categories
 * - categoryId : 처리할 게시판 ID
 * - action     : approve 또는 reject
 */
async function processCategoryApproval(categoryId, action) {
    const message = action === "approve"
        ? "이 게시판 생성 요청을 승인할까요?"
        : "이 게시판 생성 요청을 거절할까요?";

    if (!confirm(message)) return;

    try {
        const response = await fetch("admin/categories", {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({
                categoryId: String(categoryId),
                action
            })
        });

        const data = await response.json();

        if (!response.ok || !data.success) {
            alert(data.message || "게시판 승인 처리에 실패했습니다.");
            return;
        }

        alert(
            action === "approve"
                ? "게시판 생성을 승인했습니다."
                : "게시판 생성 요청을 거절했습니다."
        );

        // 처리 후 승인 대기 목록을 다시 조회합니다.
        initAdminCategoryApproval();
    } catch (error) {
        console.error("게시판 승인 처리 실패:", error);
        alert("서버 통신 오류가 발생했습니다.");
    }
}


/* =========================================================
   시스템 관리자 - 카테고리 관리자 권한 신청
   ========================================================= */

/**
 * 시스템 관리자가 승인할 수 있는
 * 카테고리 관리자 권한 신청 목록을 조회합니다.
 */
function initCategoryManagerApproval() {
    const adminSection = document.getElementById("adminApprovalSection");

    if (!adminSection) return;

    fetch("admin/category-manager-requests", {
        credentials: "include"
    })
        .then(res => {
            if (!res.ok) return null;
            return res.json();
        })
        .then(list => {
            if (!list) return;

            // 이미 생성된 테이블이 있으면 데이터만 갱신합니다.
            if (document.getElementById("categoryManagerRequestAdminSection")) {
                renderCategoryManagerRequests(list);
                return;
            }

            // 기존 관리자 승인 영역 바로 아래에 관리자 권한 신청 영역을 추가합니다.
            const section = document.createElement("div");

            section.id = "categoryManagerRequestAdminSection";
            section.className = "mt-5 pt-4 border-top";
            section.innerHTML = `
                <h4 class="fw-bold text-dark mb-3">
                    🎮 카테고리 관리자 권한 신청
                </h4>

                <div class="table-responsive">
                    <table class="table table-bordered align-middle text-center">
                        <thead class="table-light">
                            <tr>
                                <th>신청자</th>
                                <th>받은 좋아요</th>
                                <th>관리 게임</th>
                                <th>신청 일시</th>
                                <th>관리</th>
                            </tr>
                        </thead>
                        <tbody id="categoryManagerRequestBody"></tbody>
                    </table>
                </div>
            `;

            adminSection.parentNode.insertBefore(
                section,
                adminSection.nextSibling
            );

            renderCategoryManagerRequests(list);
        })
        .catch(() => {});
}

/**
 * 카테고리 관리자 권한 신청 목록을 다시 조회합니다.
 * 승인 / 거절 처리 후 화면을 갱신할 때 사용합니다.
 */
function loadCategoryManagerRequests() {
    fetch("admin/category-manager-requests", {
        credentials: "include"
    })
        .then(res => res.ok ? res.json() : null)
        .then(list => {
            if (list) {
                renderCategoryManagerRequests(list);
            }
        })
        .catch(() => {});
}

/**
 * 카테고리 관리자 권한 신청 목록을 테이블에 출력합니다.
 */
function renderCategoryManagerRequests(list) {
    const tbody = document.getElementById("categoryManagerRequestBody");

    if (!tbody) return;

    tbody.innerHTML = "";

    if (!list.length) {
        tbody.innerHTML =
            '<tr><td colspan="5" class="text-muted">카테고리 관리자 권한 신청이 없습니다.</td></tr>';
        return;
    }

    list.forEach(r => {
        tbody.innerHTML += `
            <tr>
                <td class="fw-bold">
                    ${escapeHtml(r.nickname || r.username)}
                </td>
                <td class="fw-bold text-primary">
                    ${r.receivedLikeCount}
                </td>
                <td class="fw-bold">
                    ${escapeHtml(r.categoryName)}
                </td>
                <td>
                    ${escapeHtml(r.requestedAt)}
                </td>
                <td>
                    <button
                        class="btn btn-sm btn-success me-1"
                        onclick="processCategoryManagerRequest(${r.requestId}, 'approve')">
                        승인
                    </button>
                    <button
                        class="btn btn-sm btn-danger"
                        onclick="processCategoryManagerRequest(${r.requestId}, 'reject')">
                        거절
                    </button>
                </td>
            </tr>
        `;
    });
}

/**
 * 카테고리 관리자 권한 신청을 승인하거나 거절합니다.
 */
function processCategoryManagerRequest(requestId, action) {
    const message = action === "approve"
        ? "이 회원에게 해당 게임의 카테고리 관리자 권한을 승인할까요?"
        : "카테고리 관리자 권한 신청을 거절할까요?";

    if (!confirm(message)) return;

    fetch("admin/category-manager-requests", {
        method: "POST",
        credentials: "include",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: `requestId=${encodeURIComponent(requestId)}&action=${encodeURIComponent(action)}`
    })
        .then(res => res.json())
        .then(data => {
            if (!data.success) {
                alert(data.message || "처리에 실패했습니다.");
                return;
            }

            alert(
                action === "approve"
                    ? "카테고리 관리자 권한을 승인했습니다."
                    : "신청을 거절했습니다."
            );

            loadCategoryManagerRequests();
        })
        .catch(() => alert("서버 통신 오류가 발생했습니다."));
}


/* =========================================================
   공통 - HTML 특수문자 이스케이프
   ========================================================= */

/**
 * 서버에서 받은 문자열을 HTML에 넣기 전에 특수문자로 변환합니다.
 *
 * 예:
 * < -> &lt;
 * > -> &gt;
 * & -> &amp;
 * " -> &quot;
 * ' -> &#039;
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
