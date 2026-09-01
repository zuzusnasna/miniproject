/* =========================================================
   댓글 기능
   ========================================================= */

document.addEventListener("DOMContentLoaded", () => {
    // 댓글은 게시글 상세 카드가 있는 페이지에서만 초기화합니다.
    const card = document.querySelector(".post-detail-card");
    if (!card) return;

    const postId = new URLSearchParams(location.search).get("postId");
    if (!postId) return;

    // 댓글 전용 스타일을 페이지에 추가합니다.
    injectCommentStyles();

    // 댓글 영역을 생성하고 게시글에 추가합니다.
    const section = createCommentSection(card);

    // 댓글 등록 이벤트를 연결합니다.
    bindCommentForm(section, postId);

    // 댓글 목록을 최초 1회 조회합니다.
    loadComments();

    /**
     * 댓글 영역 전용 CSS를 추가합니다.
     */
    function injectCommentStyles() {
        const style = document.createElement("style");

        style.textContent = `
            .comment-section {
                margin-top: 28px;
                padding-top: 22px;
                border-top: 1px solid #e5e7eb;
                font-size: .92rem;
                color: #333;
            }

            .comment-title {
                font-size: 1.05rem;
                font-weight: 800;
                margin: 0 0 14px;
            }

            .comment-form {
                display: flex;
                gap: 8px;
                margin-bottom: 18px;
            }

            .comment-input {
                flex: 1;
                min-height: 42px;
                max-height: 110px;
                padding: 10px 12px;
                border: 1px solid #d9dce1;
                border-radius: 7px;
                resize: vertical;
                font: inherit;
            }

            .comment-submit {
                align-self: flex-end;
                height: 42px;
                padding: 0 15px;
                border: 0;
                border-radius: 7px;
                background: #6941c6;
                color: #fff;
                font-weight: 700;
            }

            .comment-item {
                padding: 13px 4px;
                border-top: 1px solid #edf0f2;
            }

            .comment-item.reply {
                margin-left: 34px;
                padding-left: 13px;
                border-left: 2px solid #ece8f8;
            }

            .comment-head {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 6px;
                color: #777;
                font-size: .8rem;
            }

            .comment-author {
                font-weight: 800;
                color: #333;
            }

            .comment-body {
                white-space: pre-wrap;
                word-break: break-word;
                line-height: 1.55;
            }

            .comment-body.deleted {
                color: #999;
                font-style: italic;
            }

            .comment-bottom {
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin-top: 7px;
                min-height: 25px;
            }

            .comment-actions {
                display: flex;
                gap: 8px;
            }

            .comment-text-btn {
                padding: 0;
                border: 0;
                background: none;
                color: #777;
                font-size: .78rem;
                cursor: pointer;
            }

            .comment-text-btn:hover {
                color: #6941c6;
            }

            .comment-reactions {
                display: flex;
                align-items: center;
                gap: 5px;
                margin-left: auto;
            }

            .reaction-btn {
                display: inline-flex;
                align-items: center;
                gap: 2px;
                padding: 2px 4px;
                border: 0;
                background: transparent;
                cursor: pointer;
                font-size: .9rem;
                line-height: 1;
                color: #666;
            }

            .reaction-btn:hover {
                transform: scale(1.08);
            }

            .reaction-count {
                font-size: .72rem;
                color: #777;
                min-width: 10px;
            }

            .reply-form {
                display: flex;
                gap: 7px;
                margin-top: 9px;
                margin-left: 34px;
            }

            .reply-form .comment-input {
                min-height: 36px;
                font-size: .86rem;
            }

            .reply-form .comment-submit {
                height: 36px;
                padding: 0 12px;
                font-size: .82rem;
            }

            .comment-edit-form {
                margin-top: 7px;
            }

            .comment-edit-input {
                width: 100%;
                min-height: 64px;
                max-height: 150px;
                padding: 9px 10px;
                border: 1px solid #cfc7e6;
                border-radius: 7px;
                resize: vertical;
                font: inherit;
                box-sizing: border-box;
                outline: none;
            }

            .comment-edit-input:focus {
                border-color: #6941c6;
                box-shadow: 0 0 0 2px rgba(105,65,198,.08);
            }

            .comment-edit-actions {
                display: flex;
                justify-content: flex-end;
                gap: 7px;
                margin-top: 7px;
            }

            .comment-edit-save,
            .comment-edit-cancel {
                height: 32px;
                padding: 0 12px;
                border-radius: 6px;
                font-size: .78rem;
                font-weight: 700;
                cursor: pointer;
            }

            .comment-edit-save {
                border: 0;
                background: #6941c6;
                color: #fff;
            }

            .comment-edit-cancel {
                border: 1px solid #d9dce1;
                background: #fff;
                color: #666;
            }

            .comment-empty {
                padding: 20px 0;
                text-align: center;
                color: #999;
            }

            @media (max-width: 600px) {
                .comment-item.reply,
                .reply-form {
                    margin-left: 18px;
                }
            }
        `;

        document.head.appendChild(style);
    }

    /**
     * 댓글 목록과 댓글 입력창을 포함하는 영역을 생성합니다.
     */
    function createCommentSection(card) {
        const section = document.createElement("section");
        section.className = "comment-section";
        section.innerHTML = `
            <h3 class="comment-title">
                댓글 <span id="commentCount">0</span>
            </h3>

            <form id="commentForm" class="comment-form">
                <textarea
                    id="commentInput"
                    class="comment-input"
                    maxlength="1000"
                    placeholder="댓글을 입력하세요"
                    required
                ></textarea>

                <button class="comment-submit" type="submit">
                    등록
                </button>
            </form>

            <div id="commentList">
                <div class="comment-empty">
                    댓글을 불러오는 중입니다.
                </div>
            </div>
        `;

        const buttons = card.querySelector(".buttons");

        if (buttons) {
            card.insertBefore(section, buttons);
        } else {
            card.appendChild(section);
        }

        return section;
    }

    /**
     * 새 댓글 등록 이벤트를 연결합니다.
     */
    function bindCommentForm(section, postId) {
        section.querySelector("#commentForm").addEventListener(
            "submit",
            async event => {
                event.preventDefault();

                const input = section.querySelector("#commentInput");
                const content = input.value.trim();

                if (!content) return;

                const data = await request(
                    "comments",
                    "POST",
                    {
                        postId,
                        content
                    }
                );

                if (data?.success) {
                    input.value = "";
                    await loadComments();
                } else if (data) {
                    alert(
                        data.message ||
                        "댓글 등록에 실패했습니다."
                    );
                }
            }
        );
    }

    /**
     * 서버에서 현재 게시글의 댓글 목록을 조회합니다.
     */
    async function loadComments() {
        try {
            const response = await fetch(
                `comments?postId=${encodeURIComponent(postId)}`
            );
            const data = await response.json();

            render(data.comments || []);
        } catch (error) {
            console.error(error);

            document.getElementById("commentList").innerHTML = `
                <div class="comment-empty">
                    댓글을 불러오지 못했습니다.
                </div>
            `;
        }
    }

    /**
     * 댓글 목록을 화면에 렌더링합니다.
     */
    function render(comments) {
        const list = document.getElementById("commentList");
        const count = comments.filter(comment => !comment.deleted).length;

        document.getElementById("commentCount").textContent = count;

        if (!comments.length) {
            list.innerHTML = `
                <div class="comment-empty">
                    첫 댓글을 남겨보세요.
                </div>
            `;
            return;
        }

        list.innerHTML = "";

        comments.forEach(comment => {
            list.appendChild(makeComment(comment));
        });
    }

    /**
     * 댓글 하나를 DOM 요소로 생성합니다.
     */
    function makeComment(comment) {
        const item = document.createElement("article");

        item.className =
            "comment-item" +
            (comment.parentCommentId !== null ? " reply" : "");

        item.dataset.commentId = comment.commentId;

        // 작성자와 작성일 영역
        const head = document.createElement("div");
        head.className = "comment-head";

        const author = document.createElement("span");
        author.className = "comment-author";
        author.textContent = comment.nickname;

        const date = document.createElement("span");
        date.textContent =
            formatDate(comment.createdAt) +
            (comment.updatedAt ? " · 수정됨" : "");

        head.append(author, date);

        // 댓글 내용
        const body = document.createElement("div");
        body.className =
            "comment-body" +
            (comment.deleted ? " deleted" : "");
        body.textContent = comment.content;

        // 댓글 하단 버튼 영역
        const bottom = document.createElement("div");
        bottom.className = "comment-bottom";

        const actions = document.createElement("div");
        actions.className = "comment-actions";

        if (
            !comment.deleted &&
            comment.parentCommentId === null
        ) {
            actions.append(
                textButton(
                    "답글",
                    () => showReply(item, comment.commentId)
                )
            );
        }

        if (!comment.deleted && comment.mine) {
            actions.append(
                textButton(
                    "수정",
                    () => showEdit(item, comment)
                )
            );

            actions.append(
                textButton(
                    "삭제",
                    () => deleteComment(comment.commentId)
                )
            );
        }

        bottom.appendChild(actions);

        // 삭제되지 않은 댓글만 좋아요/나빠요를 표시합니다.
        if (!comment.deleted) {
            const reactions = document.createElement("div");
            reactions.className = "comment-reactions";

            reactions.append(
                reactionButton(
                    "👍",
                    comment.likeCount,
                    () => react(comment.commentId, "LIKE")
                )
            );

            reactions.append(
                reactionButton(
                    "👎",
                    comment.dislikeCount,
                    () => react(comment.commentId, "DISLIKE")
                )
            );

            bottom.appendChild(reactions);
        }

        item.append(head, body, bottom);

        return item;
    }

    /**
     * 텍스트 형태의 댓글 버튼을 생성합니다.
     */
    function textButton(label, onClick) {
        const button = document.createElement("button");

        button.type = "button";
        button.className = "comment-text-btn";
        button.textContent = label;
        button.onclick = onClick;

        return button;
    }

    /**
     * 좋아요/나빠요 버튼을 생성합니다.
     */
    function reactionButton(icon, count, onClick) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "reaction-btn";
        button.title = icon === "👍" ? "좋아요" : "나빠요";

        const iconElement = document.createElement("span");
        iconElement.textContent = icon;

        const countElement = document.createElement("span");
        countElement.className = "reaction-count";
        countElement.textContent = count;

        button.append(iconElement, countElement);
        button.onclick = onClick;

        return button;
    }

    /**
     * 댓글의 답글 입력창을 표시합니다.
     */
    function showReply(item, parentId) {
        document.querySelectorAll(".reply-form").forEach(form => {
            form.remove();
        });

        const form = document.createElement("form");
        form.className = "reply-form";

        form.innerHTML = `
            <textarea
                class="comment-input"
                maxlength="1000"
                placeholder="답글을 입력하세요"
                required
            ></textarea>
            <button class="comment-submit" type="submit">
                답글
            </button>
        `;

        form.onsubmit = async event => {
            event.preventDefault();

            const input = form.querySelector("textarea");
            const content = input.value.trim();

            if (!content) return;

            const data = await request(
                "comments",
                "POST",
                {
                    postId,
                    parentCommentId: parentId,
                    content
                }
            );

            if (data?.success) {
                await loadComments();
            } else if (data) {
                alert(
                    data.message ||
                    "답글 등록에 실패했습니다."
                );
            }
        };

        item.appendChild(form);
        inputFocus(form);
    }

    /**
     * 댓글 수정 입력창을 표시합니다.
     */
    function showEdit(item, comment) {
        document.querySelectorAll(".comment-edit-form").forEach(form => {
            form.remove();
        });

        const body = item.querySelector(".comment-body");
        const bottom = item.querySelector(".comment-bottom");

        if (!body || !bottom) return;

        body.style.display = "none";
        bottom.style.display = "none";

        const form = document.createElement("form");
        form.className = "comment-edit-form";

        const input = document.createElement("textarea");
        input.className = "comment-edit-input";
        input.maxLength = 1000;
        input.value = comment.content;
        input.required = true;

        const actionWrap = document.createElement("div");
        actionWrap.className = "comment-edit-actions";

        const save = document.createElement("button");
        save.type = "submit";
        save.className = "comment-edit-save";
        save.textContent = "저장";

        const cancel = document.createElement("button");
        cancel.type = "button";
        cancel.className = "comment-edit-cancel";
        cancel.textContent = "취소";

        cancel.onclick = () => {
            form.remove();
            body.style.display = "";
            bottom.style.display = "";
        };

        actionWrap.append(save, cancel);
        form.append(input, actionWrap);
        body.insertAdjacentElement("afterend", form);

        form.onsubmit = async event => {
            event.preventDefault();

            const value = input.value.trim();

            if (!value) {
                alert("댓글 내용을 입력해주세요.");
                input.focus();
                return;
            }

            const data = await request(
                `comments?commentId=${encodeURIComponent(comment.commentId)}&content=${encodeURIComponent(value)}`,
                "PUT"
            );

            if (data?.success) {
                await loadComments();
            } else if (data) {
                alert(data.message || "수정에 실패했습니다.");
            }
        };

        inputFocus(input);
        input.setSelectionRange(input.value.length, input.value.length);
    }

    /**
     * 댓글을 삭제합니다.
     */
    async function deleteComment(commentId) {
        if (!confirm("댓글을 삭제할까요?")) return;

        const data = await request(
            `comments?commentId=${encodeURIComponent(commentId)}`,
            "DELETE"
        );

        if (data?.success) {
            await loadComments();
        } else if (data) {
            alert(data.message || "삭제에 실패했습니다.");
        }
    }

    /**
     * 댓글에 좋아요 또는 나빠요를 등록합니다.
     */
    async function react(commentId, likeType) {
        const data = await request(
            "comment-like",
            "POST",
            {
                commentId,
                likeType
            }
        );

        if (data?.success) {
            await loadComments();
        } else if (data) {
            alert(data.message || "추천 처리에 실패했습니다.");
        }
    }

    /**
     * 댓글 관련 Servlet 요청을 공통 처리합니다.
     */
    async function request(url, method, params) {
        const options = {
            method,
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            }
        };

        if (params) {
            options.body = new URLSearchParams(params).toString();
        }

        const response = await fetch(url, options);

        // 로그인하지 않은 경우 로그인 페이지로 이동합니다.
        if (response.status === 401) {
            alert("로그인이 필요합니다.");
            location.href = "login.html";
            return null;
        }

        try {
            return await response.json();
        } catch (error) {
            console.error(error);

            return {
                success: false,
                message: "서버 응답을 확인해주세요."
            };
        }
    }

    /**
     * 댓글 작성/수정 입력창에 포커스를 이동합니다.
     */
    function inputFocus(target) {
        const input =
            target instanceof HTMLTextAreaElement
                ? target
                : target.querySelector("textarea");

        if (input) {
            input.focus();
        }
    }

    /**
     * 날짜 문자열을 화면에 표시하기 좋은 형태로 변환합니다.
     */
    function formatDate(value) {
        if (!value) return "";

        const date = new Date(value.replace(" ", "T"));

        if (Number.isNaN(date.getTime())) {
            return value;
        }

        return date.toLocaleString("ko-KR", {
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        });
    }
});
