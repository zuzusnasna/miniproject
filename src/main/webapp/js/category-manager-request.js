function initCategoryManagerRequestAdmin() {
    const adminSection = document.getElementById("adminApprovalSection");
    if (!adminSection) return;

    fetch("admin/category-manager-requests", { credentials: "include" })
        .then(res => {
            if (!res.ok) return null;
            return res.json();
        })
        .then(list => {
            if (!list) return;

            const section = document.createElement("div");
            section.id = "categoryManagerRequestAdminSection";
            section.className = "mt-5 pt-4 border-top";
            section.innerHTML = `
                <h4 class="fw-bold text-dark mb-3">🎮 카테고리 관리자 권한 신청</h4>
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
            adminSection.parentNode.insertBefore(section, adminSection.nextSibling);
            renderCategoryManagerRequests(list);
        })
        .catch(() => {});
}

function loadCategoryManagerRequests() {
    fetch("admin/category-manager-requests", { credentials: "include" })
        .then(res => res.ok ? res.json() : null)
        .then(list => { if (list) renderCategoryManagerRequests(list); })
        .catch(() => {});
}

function renderCategoryManagerRequests(list) {
    const tbody = document.getElementById("categoryManagerRequestBody");
    if (!tbody) return;
    tbody.innerHTML = "";
    if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-muted">카테고리 관리자 권한 신청이 없습니다.</td></tr>';
        return;
    }
    list.forEach(r => {
        tbody.innerHTML += `
            <tr>
                <td class="fw-bold">${escapeHtml(r.nickname || r.username)}</td>
                <td class="fw-bold text-primary">${r.receivedLikeCount}</td>
                <td class="fw-bold">${escapeHtml(r.categoryName)}</td>
                <td>${escapeHtml(r.requestedAt)}</td>
                <td>
                    <button class="btn btn-sm btn-success me-1" onclick="processCategoryManagerRequest(${r.requestId}, 'approve')">승인</button>
                    <button class="btn btn-sm btn-danger" onclick="processCategoryManagerRequest(${r.requestId}, 'reject')">거절</button>
                </td>
            </tr>`;
    });
}

function processCategoryManagerRequest(requestId, action) {
    const message = action === "approve" ? "이 회원에게 해당 게임의 카테고리 관리자 권한을 승인할까요?" : "카테고리 관리자 권한 신청을 거절할까요?";
    if (!confirm(message)) return;

    fetch("admin/category-manager-requests", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `requestId=${encodeURIComponent(requestId)}&action=${encodeURIComponent(action)}`
    })
        .then(res => res.json())
        .then(data => {
            if (!data.success) {
                alert(data.message || "처리에 실패했습니다.");
                return;
            }
            alert(action === "approve" ? "카테고리 관리자 권한을 승인했습니다." : "신청을 거절했습니다.");
            loadCategoryManagerRequests();
        })
        .catch(() => alert("서버 통신 오류가 발생했습니다."));
}

function escapeHtml(value) {
    if (value == null) return "";
    return String(value).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/\"/g,"&quot;").replace(/'/g,"&#039;");
}
