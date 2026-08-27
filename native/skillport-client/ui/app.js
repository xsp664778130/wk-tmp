const token=document.querySelector('meta[name="skillport-local-token"]').content;
const app=document.querySelector('#app');
let state=null,mode='private',query='',modal=null,busy=false;
const labels={codex:'Codex',qoder:'Qoder',opencode:'OpenCode',claude:'Claude Code'};
const marks={codex:'CX',qoder:'Q',opencode:'OC',claude:'CC'};

async function api(path,options={}){
  const response=await fetch(path,{...options,headers:{'content-type':'application/json','X-SkillPort-Local':token,...(options.headers||{})}});
  if(response.status===204)return null;
  const data=await response.json().catch(()=>({}));
  if(!response.ok)throw new Error(data.error||'操作没有完成');
  return data;
}

async function refresh(){
  try{state=await api('/api/state');render();}catch(error){app.innerHTML=`<div class="boot"><span>!</span><h1>客户端暂时不可用</h1><p>${escapeHTML(error.message)}</p></div>`;}
}

function render(){if(!state?.authenticated){renderLogin();return}renderWorkspace()}

function renderLogin(){
  app.className='';
  app.innerHTML=`<section class="login-shell"><div class="login-panel"><div class="login-card"><div class="login-brand"><span class="brand-mark">SP</span><div><b>SkillPort Client</b><small>本机 Skill 管理器</small></div></div><h1>登录你的空间</h1><p class="login-lead">使用与网页版相同的账号，数据仍按用户完全隔离。</p><form class="login-form"><label>邮箱<input name="email" type="email" autocomplete="email" required placeholder="name@example.com"></label><label>密码<input name="password" type="password" autocomplete="current-password" minlength="8" required placeholder="至少 8 位"></label><p class="login-error" aria-live="polite"></p><button class="primary" type="submit">登录并打开我的 Skill</button></form></div></div><div class="login-visual"><div class="visual-card"><h2>不用常驻 Bridge，也能随时安装 Skill</h2><p>打开客户端时才运行，下载完成后直接写入 Codex、Qoder、OpenCode 或 Claude Code 的标准目录。</p><div class="visual-steps"><div><span>1</span><p><b>使用现有账号登录</b><small>云端个人空间与公有池保持同步</small></p></div><div><span>2</span><p><b>选择目标 AI 工具</b><small>自动校验文件并安装到正确目录</small></p></div><div><span>3</span><p><b>随时重新加载或卸载</b><small>卸载不留备份，云端原件仍保留</small></p></div></div></div></div></section>`;
  const form=app.querySelector('form');form.addEventListener('submit',async event=>{event.preventDefault();const button=form.querySelector('button'),error=form.querySelector('.login-error');button.disabled=true;button.textContent='正在登录…';error.textContent='';try{await api('/api/login',{method:'POST',body:JSON.stringify({email:form.email.value,password:form.password.value})});await refresh()}catch(reason){error.textContent=reason.message;button.disabled=false;button.textContent='登录并打开我的 Skill'}})
}

function renderWorkspace(){
  app.className='';const current=mode==='private'?state.skills:state.publicSkills;const filtered=current.filter(skill=>!query||`${skill.name} ${skill.description} ${skill.category}`.toLowerCase().includes(query.toLowerCase()));
  app.innerHTML=`<div class="shell"><aside class="sidebar"><div class="brand"><span class="brand-mark">SP</span>SkillPort</div><p class="nav-label">SKILL 空间</p><nav class="nav"><button data-mode="private" class="${mode==='private'?'active':''}"><i>⌂</i>我的 Skill</button><button data-mode="public" class="${mode==='public'?'active':''}"><i>◎</i>Skill 公有池</button></nav><div class="side-bottom"><b>客户端 ${escapeHTML(state.version)}</b><small>${state.os==='windows'?'Windows':'macOS'} · 按需运行</small></div></aside><section class="workspace"><header class="topbar"><input class="search" value="${escapeHTML(query)}" placeholder="搜索名称、描述或分类…"><div class="user-chip"><b>${escapeHTML(state.user.displayName)}</b><small>${escapeHTML(state.user.email)}</small></div><button class="logout">退出登录</button></header><div class="content"><section class="hero"><div><h1>${mode==='private'?'我的 Skill':'Skill 公有池'}</h1><p>${mode==='private'?'直接下载安装到本机 AI 工具，云端原件保持不变。':'先拉取独立副本，再安装到本机。'}</p></div><div class="tool-status">${state.tools.map(tool=>`<span class="tool-pill ${tool.detected?'detected':''}">${escapeHTML(tool.name)} · ${tool.detected?'已检测':'可安装'}</span>`).join('')}</div></section><section class="grid">${filtered.length?filtered.map(cardHTML).join(''):`<div class="empty"><span>⌕</span><h3>没有找到 Skill</h3><p>换一个关键词，或切换到另一个空间。</p></div>`}</section></div></section></div>${modal?modalHTML():''}`;
  bindWorkspace();
}

function cardHTML(skill){const isPublic=mode==='public';return `<article class="card"><div class="card-top"><span class="skill-icon">${isPublic?'↗':'↓'}</span><span class="category">${escapeHTML(skill.category||'编程技能')}</span></div><h3>${escapeHTML(skill.name)}</h3><p class="description">${escapeHTML(skill.description||'暂无描述')}</p>${!isPublic&&skill.note?`<p class="note">✎ ${escapeHTML(skill.note)}</p>`:''}<div class="meta"><span>${isPublic?escapeHTML(skill.author||'社区用户'):'云端个人空间'}</span><span>${formatSize(skill.sizeBytes)}</span></div><div class="actions">${isPublic?`<button class="pull" data-pull="${skill.id}" ${skill.pulled?'disabled':''}>${skill.pulled?'已在我的空间':'拉取到我的空间'}</button>`:`<button class="uninstall" data-uninstall="${skill.id}">卸载</button><button class="install" data-install="${skill.id}">安装到本机</button>`}</div></article>`}

function bindWorkspace(){
  app.querySelectorAll('[data-mode]').forEach(button=>button.onclick=()=>{mode=button.dataset.mode;query='';modal=null;render()});
  app.querySelector('.search').oninput=event=>{query=event.target.value;const position=query.length;render();const next=app.querySelector('.search');next.focus();next.setSelectionRange(position,position)};
  app.querySelector('.logout').onclick=async()=>{try{await api('/api/logout',{method:'POST'});await refresh()}catch(error){toast(error.message)}};
  app.querySelectorAll('[data-pull]').forEach(button=>button.onclick=()=>pull(button.dataset.pull));
  app.querySelectorAll('[data-install]').forEach(button=>button.onclick=()=>openModal('install',button.dataset.install));
  app.querySelectorAll('[data-uninstall]').forEach(button=>button.onclick=()=>openModal('uninstall',button.dataset.uninstall));
  if(modal){app.querySelector('.close').onclick=()=>{modal=null;render()};app.querySelectorAll('.target').forEach(button=>button.onclick=()=>{const id=button.dataset.target;modal.targets=modal.targets.includes(id)?modal.targets.filter(value=>value!==id):[...modal.targets,id];render()});app.querySelector('.modal .primary').onclick=runModalAction}
}

async function pull(id){if(busy)return;busy=true;try{await api('/api/pull',{method:'POST',body:JSON.stringify({publicSkillId:id})});toast('已拉取到你的个人空间');await refresh()}catch(error){toast(error.message)}finally{busy=false}}
function openModal(action,id){const skill=state.skills.find(item=>item.id===id);modal={action,skill,targets:['codex']};render()}
function modalHTML(){const removing=modal.action==='uninstall';return `<div class="backdrop"><section class="modal" role="dialog" aria-modal="true"><button class="close">×</button><h2>${removing?'从本机卸载':'安装到本机'} ${escapeHTML(modal.skill.name)}</h2><p>${removing?'只删除所选工具中的本机副本，云端 Skill 和备注不会删除。':'客户端会下载云端原件、校验 SHA-256，并写入所选工具目录。'}</p><div class="targets">${state.tools.map(tool=>`<button class="target ${modal.targets.includes(tool.id)?'checked':''}" data-target="${tool.id}"><span>${marks[tool.id]||'?'}</span><p><b>${escapeHTML(tool.name)}</b><small>${escapeHTML(tool.directory)}</small></p><span class="check">${modal.targets.includes(tool.id)?'✓':''}</span></button>`).join('')}</div>${removing?'<div class="warning"><b>本机副本会被永久删除</b><br>不保留备份，需要时可以重新从云端安装。</div>':''}<button class="primary" ${!modal.targets.length||busy?'disabled':''}>${busy?'正在处理…':removing?'确认永久卸载':'下载并安装'}</button></section></div>`}
async function runModalAction(){if(busy||!modal.targets.length)return;busy=true;render();try{const path=modal.action==='install'?'/api/install':'/api/uninstall';const body=modal.action==='install'?{skillId:modal.skill.id,targets:modal.targets}:{skillName:modal.skill.name,targets:modal.targets};const result=await api(path,{method:'POST',body:JSON.stringify(body)});modal=null;render();toast(result.message)}catch(error){busy=false;render();toast(error.message)}finally{busy=false}}
function toast(message){document.querySelector('.toast')?.remove();const node=document.createElement('div');node.className='toast';node.textContent=message;document.body.appendChild(node);setTimeout(()=>node.remove(),3600)}
function formatSize(value){if(!value)return'—';if(value<1024*1024)return`${Math.ceil(value/1024)} KB`;return`${(value/1024/1024).toFixed(1)} MB`}
function escapeHTML(value){return String(value??'').replace(/[&<>'"]/g,character=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[character]))}

refresh();
