document.addEventListener('DOMContentLoaded', () => {
    const card = document.querySelector('.post-detail-card');
    if (!card) return;

    const postId = new URLSearchParams(location.search).get('postId');
    if (!postId) return;

    const style = document.createElement('style');
    style.textContent = `
        .comment-section{margin-top:28px;padding-top:22px;border-top:1px solid #e5e7eb;font-size:.92rem;color:#333}
        .comment-title{font-size:1.05rem;font-weight:800;margin:0 0 14px}
        .comment-form{display:flex;gap:8px;margin-bottom:18px}
        .comment-input{flex:1;min-height:42px;max-height:110px;padding:10px 12px;border:1px solid #d9dce1;border-radius:7px;resize:vertical;font:inherit}
        .comment-submit{align-self:flex-end;height:42px;padding:0 15px;border:0;border-radius:7px;background:#6941c6;color:#fff;font-weight:700}
        .comment-item{padding:13px 4px;border-top:1px solid #edf0f2}
        .comment-item.reply{margin-left:34px;padding-left:13px;border-left:2px solid #ece8f8}
        .comment-head{display:flex;align-items:center;gap:8px;margin-bottom:6px;color:#777;font-size:.8rem}
        .comment-author{font-weight:800;color:#333}
        .comment-body{white-space:pre-wrap;word-break:break-word;line-height:1.55}
        .comment-body.deleted{color:#999;font-style:italic}
        .comment-bottom{display:flex;align-items:center;justify-content:space-between;margin-top:7px;min-height:25px}
        .comment-actions{display:flex;gap:8px}
        .comment-text-btn{padding:0;border:0;background:none;color:#777;font-size:.78rem;cursor:pointer}
        .comment-text-btn:hover{color:#6941c6}
        .comment-reactions{display:flex;align-items:center;gap:5px;margin-left:auto}
        .reaction-btn{display:inline-flex;align-items:center;gap:2px;padding:2px 4px;border:0;background:transparent;cursor:pointer;font-size:.9rem;line-height:1;color:#666}
        .reaction-btn:hover{transform:scale(1.08)}
        .reaction-count{font-size:.72rem;color:#777;min-width:10px}
        .reply-form{display:flex;gap:7px;margin-top:9px;margin-left:34px}
        .reply-form .comment-input{min-height:36px;font-size:.86rem}
        .reply-form .comment-submit{height:36px;padding:0 12px;font-size:.82rem}
        .comment-empty{padding:20px 0;text-align:center;color:#999}
        @media(max-width:600px){.comment-item.reply,.reply-form{margin-left:18px}}
    `;
    document.head.appendChild(style);

    const section = document.createElement('section');
    section.className = 'comment-section';
    section.innerHTML = `
        <h3 class="comment-title">댓글 <span id="commentCount">0</span></h3>
        <form id="commentForm" class="comment-form">
            <textarea id="commentInput" class="comment-input" maxlength="1000" placeholder="댓글을 입력하세요" required></textarea>
            <button class="comment-submit" type="submit">등록</button>
        </form>
        <div id="commentList"><div class="comment-empty">댓글을 불러오는 중입니다.</div></div>
    `;
    const buttons = card.querySelector('.buttons');
    if (buttons) card.insertBefore(section, buttons); else card.appendChild(section);

    document.getElementById('commentForm').addEventListener('submit', async e => {
        e.preventDefault();
        const input = document.getElementById('commentInput');
        const content = input.value.trim();
        if (!content) return;
        const data = await request('comments', 'POST', {postId, content});
        if (data?.success) { input.value=''; await loadComments(); }
        else if (data) alert(data.message || '댓글 등록에 실패했습니다.');
    });

    async function loadComments(){
        try{
            const res=await fetch(`comments?postId=${encodeURIComponent(postId)}`);
            const data=await res.json();
            render(data.comments || []);
        }catch(e){
            console.error(e);
            document.getElementById('commentList').innerHTML='<div class="comment-empty">댓글을 불러오지 못했습니다.</div>';
        }
    }

    function render(comments){
        const list=document.getElementById('commentList');
        document.getElementById('commentCount').textContent=comments.filter(c=>!c.deleted).length;
        if(!comments.length){list.innerHTML='<div class="comment-empty">첫 댓글을 남겨보세요.</div>';return;}
        list.innerHTML='';
        comments.forEach(c=>list.appendChild(makeComment(c)));
    }

    function makeComment(c){
        const item=document.createElement('article');
        item.className='comment-item'+(c.parentCommentId!==null?' reply':'');
        item.dataset.commentId=c.commentId;

        const head=document.createElement('div');
        head.className='comment-head';
        const author=document.createElement('span'); author.className='comment-author'; author.textContent=c.username;
        const date=document.createElement('span'); date.textContent=formatDate(c.createdAt)+(c.updatedAt?' · 수정됨':'');
        head.append(author,date);

        const body=document.createElement('div');
        body.className='comment-body'+(c.deleted?' deleted':'');
        body.textContent=c.content;

        const bottom=document.createElement('div'); bottom.className='comment-bottom';
        const actions=document.createElement('div'); actions.className='comment-actions';
        if(!c.deleted && c.parentCommentId===null) actions.append(textButton('답글',()=>showReply(item,c.commentId)));
        if(!c.deleted && c.mine){
            actions.append(textButton('수정',()=>editComment(c)));
            actions.append(textButton('삭제',()=>deleteComment(c.commentId)));
        }
        bottom.appendChild(actions);

        if(!c.deleted){
            const reactions=document.createElement('div'); reactions.className='comment-reactions';
            reactions.append(reactionButton('👍',c.likeCount,()=>react(c.commentId,'LIKE')));
            reactions.append(reactionButton('👎',c.dislikeCount,()=>react(c.commentId,'DISLIKE')));
            bottom.appendChild(reactions);
        }
        item.append(head,body,bottom);
        return item;
    }

    function textButton(label,onClick){
        const b=document.createElement('button'); b.type='button'; b.className='comment-text-btn'; b.textContent=label; b.onclick=onClick; return b;
    }
    function reactionButton(icon,count,onClick){
        const b=document.createElement('button'); b.type='button'; b.className='reaction-btn'; b.title=icon==='👍'?'좋아요':'나빠요';
        const i=document.createElement('span'); i.textContent=icon;
        const n=document.createElement('span'); n.className='reaction-count'; n.textContent=count;
        b.append(i,n); b.onclick=onClick; return b;
    }

    function showReply(item,parentId){
        document.querySelectorAll('.reply-form').forEach(f=>f.remove());
        const form=document.createElement('form'); form.className='reply-form';
        form.innerHTML='<textarea class="comment-input" maxlength="1000" placeholder="답글을 입력하세요" required></textarea><button class="comment-submit" type="submit">답글</button>';
        form.onsubmit=async e=>{
            e.preventDefault(); const input=form.querySelector('textarea'); const content=input.value.trim(); if(!content)return;
            const data=await request('comments','POST',{postId,parentCommentId:parentId,content});
            if(data?.success) await loadComments(); else if(data) alert(data.message||'답글 등록에 실패했습니다.');
        };
        item.appendChild(form); form.querySelector('textarea').focus();
    }

    async function editComment(c){
        const content=prompt('댓글을 수정하세요.',c.content);
        if(content===null)return;
        const value=content.trim(); if(!value){alert('댓글 내용을 입력해주세요.');return;}
        const data=await request(`comments?commentId=${encodeURIComponent(c.commentId)}&content=${encodeURIComponent(value)}`,'PUT');
        if(data?.success) await loadComments(); else if(data) alert(data.message||'수정에 실패했습니다.');
    }

    async function deleteComment(commentId){
        if(!confirm('댓글을 삭제할까요?'))return;
        const data=await request(`comments?commentId=${encodeURIComponent(commentId)}`,'DELETE');
        if(data?.success) await loadComments(); else if(data) alert(data.message||'삭제에 실패했습니다.');
    }

    async function react(commentId,likeType){
        const data=await request('comment-like','POST',{commentId,likeType});
        if(data?.success) await loadComments(); else if(data) alert(data.message||'추천 처리에 실패했습니다.');
    }

    async function request(url,method,params){
        const options={method,headers:{'Content-Type':'application/x-www-form-urlencoded'}};
        if(params) options.body=new URLSearchParams(params).toString();
        const res=await fetch(url,options);
        if(res.status===401){alert('로그인이 필요합니다.');location.href='login.html';return null;}
        try{return await res.json();}catch(e){console.error(e);return {success:false,message:'서버 응답을 확인해주세요.'};}
    }

    function formatDate(value){
        if(!value)return '';
        const d=new Date(value.replace(' ','T'));
        return Number.isNaN(d.getTime())?value:d.toLocaleString('ko-KR',{month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'});
    }

    loadComments();
});
