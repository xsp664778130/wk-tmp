"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import type { DragEvent, FormEvent } from "react";
import {
  createMacInstaller,
  createMacInstallerArchive,
  createWindowsInstaller,
  installPaths,
  resolveSkillName,
  slugifySkillName,
} from "./installer-utils";

type Skill = {
  id: string;
  name: string;
  description: string;
  category: string;
  accent: string;
  icon: string;
  author: string;
  uses: string;
  note?: string;
  uploaded?: boolean;
  fileName?: string;
  compatible: string[];
};

type User = { id: string; name: string; email: string } | null;
type Device = { id: string; name: string; os: string; arch: string; status: string; lastSeenAt?: string };

const sampleSkills: Skill[] = [
  {
    id: "api-architect",
    name: "API Architect",
    description: "从需求快速生成结构清晰的 API 设计、接口约定与错误码规范。",
    category: "编程开发",
    accent: "coral",
    icon: "</>",
    author: "SkillPort Labs",
    uses: "8.4k",
    compatible: ["codex", "qoder", "openai"],
  },
  {
    id: "data-storyteller",
    name: "Data Storyteller",
    description: "把复杂数据变成有说服力的洞察、图表建议和汇报大纲。",
    category: "数据分析",
    accent: "lime",
    icon: "↗",
    author: "Mia Chen",
    uses: "6.1k",
    compatible: ["codex", "openai"],
  },
  {
    id: "ux-writing",
    name: "UX Writing Kit",
    description: "统一产品文案语气，覆盖空状态、表单、通知与错误提示。",
    category: "创意设计",
    accent: "violet",
    icon: "Aa",
    author: "Paper Studio",
    uses: "4.8k",
    compatible: ["qoder", "openai"],
  },
  {
    id: "meeting-synth",
    name: "Meeting Synth",
    description: "提炼会议记录，自动整理决策、行动项、负责人和截止时间。",
    category: "效率工具",
    accent: "blue",
    icon: "≡",
    author: "Noon AI",
    uses: "12.7k",
    compatible: ["codex", "qoder", "openai"],
  },
  {
    id: "market-radar",
    name: "Market Radar",
    description: "搭建行业研究框架，追踪竞品动态并输出机会判断。",
    category: "商业研究",
    accent: "yellow",
    icon: "◎",
    author: "Northstar",
    uses: "3.9k",
    compatible: ["codex", "openai"],
  },
  {
    id: "workflow-pilot",
    name: "Workflow Pilot",
    description: "拆解重复工作并生成可靠的自动化流程与异常处理策略。",
    category: "自动化",
    accent: "pink",
    icon: "⌁",
    author: "Kite Works",
    uses: "5.3k",
    compatible: ["codex", "qoder"],
  },
];

const categories = [
  ["全部技能", "▦"],
  ["编程开发", "</>"],
  ["数据分析", "↗"],
  ["创意设计", "✦"],
  ["效率工具", "◫"],
  ["商业研究", "◎"],
  ["自动化", "⌁"],
] as const;

const toolMeta = {
  codex: { name: "Codex", mark: "CX", color: "dark" },
  qoder: { name: "Qoder", mark: "Q", color: "blue" },
  openai: { name: "OpenAI", mark: "AI", color: "green" },
} as const;

export function SkillWorkspace({ initialUser }: { initialUser: User }) {
  const [user, setUser] = useState<User>(initialUser);
  const [activeCategory, setActiveCategory] = useState("全部技能");
  const [query, setQuery] = useState("");
  const [skills, setSkills] = useState<Skill[]>(sampleSkills);
  const [devices, setDevices] = useState<Device[]>([]);
  const [selected, setSelected] = useState<Skill | null>(null);
  const [installer, setInstaller] = useState<Skill | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [pairOpen, setPairOpen] = useState(false);
  const [authOpen, setAuthOpen] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let cancelled = false;
    if (!user) return;
    Promise.all([
      fetch("/api/skills").then((response) => (response.ok ? response.json() : null)),
      fetch("/api/devices").then((response) => (response.ok ? response.json() : null)),
    ])
      .then(([skillData, deviceData]) => {
        if (cancelled) return;
        const uploaded = (Array.isArray(skillData?.skills) ? skillData.skills : []).map((skill: Record<string, unknown>, index: number): Skill => ({
          id: String(skill.id),
          name: String(skill.name),
          description: String(skill.description),
          category: String(skill.category),
          accent: ["coral", "lime", "violet", "blue"][index % 4],
          icon: "↑",
          author: "我的上传",
          uses: "私有",
          note: String(skill.note || ""),
          uploaded: true,
          fileName: String(skill.fileName || "skill.zip"),
          compatible: String(skill.toolCompatibility || "codex,qoder,openai").split(","),
        }));
        setSkills([...uploaded, ...sampleSkills]);
        setDevices(Array.isArray(deviceData?.devices) ? deviceData.devices : []);
      })
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, [user]);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(null), 2800);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return skills.filter((skill) => {
      const categoryMatch = activeCategory === "全部技能" || skill.category === activeCategory;
      const searchMatch = !normalized || `${skill.name} ${skill.description} ${skill.category}`.toLowerCase().includes(normalized);
      return categoryMatch && searchMatch;
    });
  }, [activeCategory, query, skills]);

  function guardAccount(action: () => void) {
    if (user) return action();
    setAuthOpen(true);
    setToast("请先登录 SkillPort 账户");
  }

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" }).catch(() => undefined);
    setUser(null);
    setSkills(sampleSkills);
    setDevices([]);
    setSelected(null);
    setInstaller(null);
    setToast("已安全退出 SkillPort");
  }

  function onFile(file?: File) {
    if (!file) return;
    guardAccount(() => { void uploadFile(file); });
  }

  async function uploadFile(file: File) {
    try {
      const extension = file.name.split(".").pop()?.toLowerCase() || "md";
      const payload = new Uint8Array(await file.arrayBuffer());
      const detectedName = resolveSkillName(payload, extension, file.name.replace(/\.(zip|skill|md)$/i, ""));
      setUploadOpen(false);
      const preview: Skill = {
        id: `pending-${Date.now()}`,
        name: detectedName,
        description: "刚刚上传，等待补充更详细的 Skill 描述。",
        category: "效率工具",
        accent: "lime",
        icon: "↑",
        author: "我的上传",
        uses: "私有",
        uploaded: true,
        fileName: file.name,
        compatible: ["codex", "qoder", "openai"],
      };
      setSkills((current) => [preview, ...current]);
      const form = new FormData();
      form.append("file", file);
      form.append("name", preview.name);
      form.append("category", preview.category);
      fetch("/api/skills", { method: "POST", body: form })
        .then(async (response) => {
          if (!response.ok) throw new Error();
          const data = await response.json();
          const created = data.skill ?? data;
          setSkills((current) => current.map((skill) => skill.id === preview.id ? { ...preview, id: created.id, fileName: created.fileName || file.name } : skill));
          setToast("Skill 已安全保存到你的私人空间");
        })
        .catch(() => {
          setSkills((current) => current.filter((skill) => skill.id !== preview.id));
          setToast("上传没有完成，请稍后再试");
        });
    } catch {
      setToast("无法读取 Skill 文件，请检查文件是否完整");
    }
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragging(false);
    onFile(event.dataTransfer.files?.[0]);
  }

  const displayName = user?.name?.includes("@") ? user.name.split("@")[0] : user?.name || "访客";
  const onlineDevice = devices.find((device) => device.status === "ONLINE");

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">S</span><span>skillport<span className="brand-dot">.</span></span></div>
        <nav className="side-nav" aria-label="技能分类">
          <p className="nav-label">探索</p>
          {categories.map(([name, icon]) => (
            <button key={name} className={activeCategory === name ? "nav-item active" : "nav-item"} onClick={() => setActiveCategory(name)}>
              <span className="nav-icon">{icon}</span><span>{name}</span>
              {name === "全部技能" && <span className="nav-count">128</span>}
            </button>
          ))}
          <p className="nav-label nav-label-space">个人空间</p>
          <button className="nav-item" onClick={() => { setActiveCategory("全部技能"); setQuery("我的上传"); }}><span className="nav-icon">↑</span><span>我的上传</span><span className="nav-count">{skills.filter((s) => s.uploaded).length}</span></button>
          <button className="nav-item"><span className="nav-icon">♡</span><span>收藏夹</span></button>
          <button className="nav-item"><span className="nav-icon">↺</span><span>安装记录</span></button>
        </nav>
        <div className="sidebar-bottom">
          <div className="bridge-card">
            <div className="bridge-top"><span className={onlineDevice ? "status-dot" : "status-dot offline"}/><span>SkillPort Bridge</span><b>{onlineDevice ? "在线" : "离线"}</b></div>
            <p>{onlineDevice ? onlineDevice.name : "可使用安装脚本兜底"}</p>
            <div className="mini-tools"><span>CX</span><span>Q</span><span>AI</span></div>
          </div>
          <button className="settings-row"><span>⚙</span> 设置与帮助 <span>›</span></button>
        </div>
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div className="search-wrap">
            <span className="search-icon">⌕</span>
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索技能、分类或用途..." aria-label="搜索技能" />
            <kbd>⌘ K</kbd>
          </div>
          <button className="round-button" aria-label="通知">○<span className="notification-dot"/></button>
          {user ? (
            <div className="account"><span className="avatar">{displayName.slice(0, 1).toUpperCase()}</span><span className="account-copy"><b>{displayName}</b><small>数据库账户</small></span><button className="logout-button" onClick={logout} aria-label="退出登录">退出</button></div>
          ) : (
            <button className="login-button" onClick={() => setAuthOpen(true)}>登录 / 注册 <span>→</span></button>
          )}
        </header>

        <div className="content-scroll">
          <section className="welcome-row">
            <div>
              <p className="eyebrow">YOUR AI TOOLBOX</p>
              <h1>{user ? `晚上好，${displayName}` : "把好用的 Skill，装进每个 AI"}<span className="wave">✦</span></h1>
              <p>统一管理、随时备注，一键分发到你的本机 AI 工具。</p>
            </div>
            <button className="primary-button" onClick={() => guardAccount(() => setUploadOpen(true))}><span>＋</span> 上传 Skill</button>
          </section>

          <section className="stats-strip" aria-label="技能统计">
            <div><span className="stat-icon purple">▦</span><p><b>{skills.length + 18}</b><small>我的 Skills</small></p><em>+3 本周</em></div>
            <div><span className="stat-icon orange">↗</span><p><b>46</b><small>累计加载</small></p><em>↑ 12%</em></div>
            <div><span className="stat-icon green">⌁</span><p><b>3</b><small>已连接工具</small></p><em className="neutral">全部在线</em></div>
          </section>

          <section className="library-heading">
            <div><h2>{activeCategory === "全部技能" ? "为你推荐" : activeCategory}</h2><p>精选实用 Skill，让你的 AI 更懂工作。</p></div>
            <div className="view-actions"><button className="sort-button">最近更新　⌄</button><button className="view-button active">▦</button><button className="view-button">☷</button></div>
          </section>

          {filtered.length ? (
            <section className="skill-grid">
              {filtered.map((skill) => (
                <article className="skill-card" key={skill.id} onClick={() => setSelected(skill)}>
                  <div className="card-top">
                    <span className={`skill-icon ${skill.accent}`}>{skill.icon}</span>
                    <button className="more-button" aria-label={`${skill.name} 更多操作`} onClick={(event) => event.stopPropagation()}>•••</button>
                  </div>
                  <span className="category-pill">{skill.category}</span>
                  <h3>{skill.name}</h3>
                  <p className="skill-description">{skill.description}</p>
                  {skill.note && <p className="note-preview"><span>✎</span>{skill.note}</p>}
                  <div className="card-footer">
                    <span className="author-avatar">{skill.author.slice(0, 1)}</span><span className="author-name">{skill.author}</span>
                    <span className="usage">↓ {skill.uses}</span>
                  </div>
                  <button className="install-card-button" onClick={(event) => { event.stopPropagation(); setInstaller(skill); }}>加载到本机 <span>→</span></button>
                </article>
              ))}
            </section>
          ) : (
            <div className="empty-state"><span>⌕</span><h3>没有找到匹配的 Skill</h3><p>换个关键词，或浏览其他分类。</p><button onClick={() => { setQuery(""); setActiveCategory("全部技能"); }}>查看全部技能</button></div>
          )}
        </div>
      </main>

      <aside className="right-rail">
        <section className="rail-section">
          <div className="rail-title"><div><h2>快速导入</h2><p>添加你的专属 Skill</p></div><span>＋</span></div>
          <div className={dragging ? "drop-zone dragging" : "drop-zone"} onDragOver={(event) => { event.preventDefault(); setDragging(true); }} onDragLeave={() => setDragging(false)} onDrop={handleDrop} onClick={() => fileInput.current?.click()}>
            <input ref={fileInput} hidden type="file" accept=".zip,.skill,.md" onChange={(event) => onFile(event.target.files?.[0])}/>
            <span className="upload-mark">↑</span><b>拖动文件到这里</b><p>或 <u>点击选择文件</u></p><small>支持 .zip · .skill · SKILL.md</small>
          </div>
        </section>

        <section className="rail-section tools-section">
          <div className="rail-title"><div><h2>本机工具</h2><p>{onlineDevice ? `${onlineDevice.name} · ${onlineDevice.os}` : "等待 Bridge 客户端连接"}</p></div><span className={onlineDevice ? "live-pill" : "live-pill offline"}><i/>{onlineDevice ? "在线" : "离线"}</span></div>
          <div className="tool-list">
            {Object.entries(toolMeta).map(([id, tool]) => (
              <div className="tool-row" key={id}><span className={`tool-logo ${tool.color}`}>{tool.mark}</span><p><b>{tool.name}</b><small>macOS · Windows</small></p><span className="connected">可加载</span></div>
            ))}
          </div>
          <button className="manage-tools" onClick={() => guardAccount(() => setPairOpen(true))}>{onlineDevice ? "管理连接" : "配对新设备"} <span>→</span></button>
        </section>

        <section className="rail-section activity-section">
          <div className="rail-title"><div><h2>最近动态</h2><p>你的 Skill 使用记录</p></div><button>查看全部</button></div>
          <div className="timeline">
            <div><span className="timeline-dot coral">↗</span><p><b>加载了 API Architect</b><small>发送到 Codex · 12 分钟前</small></p></div>
            <div><span className="timeline-dot lime">↑</span><p><b>上传了 Brand Voice</b><small>个人空间 · 昨天 18:42</small></p></div>
            <div><span className="timeline-dot violet">✎</span><p><b>更新了一条备注</b><small>UX Writing Kit · 8月17日</small></p></div>
          </div>
        </section>
      </aside>

      {selected && <DetailModal skill={selected} onClose={() => setSelected(null)} onInstall={() => { setInstaller(selected); setSelected(null); }} onSaveNote={(note) => {
        setSkills((current) => current.map((skill) => skill.id === selected.id ? { ...skill, note } : skill));
        setSelected((current) => current ? { ...current, note } : current);
        if (selected.uploaded && user) fetch("/api/skills", { method: "PATCH", headers: { "content-type": "application/json" }, body: JSON.stringify({ id: selected.id, note }) }).catch(() => undefined);
        setToast("备注已保存，仅你自己可见");
      }}/>} 
      {installer && (
        <InstallModal skill={installer} signedIn={Boolean(user)} onRequireSignIn={() => { setInstaller(null); setAuthOpen(true); }} onlineDevice={onlineDevice ?? null} onClose={() => setInstaller(null)} onDone={(message) => { setInstaller(null); setToast(message); }}/>
      )}
      {uploadOpen && <UploadModal onClose={() => setUploadOpen(false)} onFile={onFile}/>}
      {pairOpen && <PairDeviceModal onClose={() => setPairOpen(false)}/>}
      {authOpen && <AuthModal onClose={() => setAuthOpen(false)} onAuthenticated={(authenticatedUser) => { setUser(authenticatedUser); setAuthOpen(false); setToast("欢迎进入你的 SkillPort 私人空间"); }}/>}
      {toast && <div className="toast"><span>✓</span>{toast}</div>}
    </div>
  );
}

function DetailModal({ skill, onClose, onInstall, onSaveNote }: { skill: Skill; onClose: () => void; onInstall: () => void; onSaveNote: (note: string) => void }) {
  const [note, setNote] = useState(skill.note || "");
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal detail-modal" role="dialog" aria-modal="true" aria-label={`${skill.name} 详情`} onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" onClick={onClose}>×</button>
        <div className="detail-hero"><span className={`skill-icon large ${skill.accent}`}>{skill.icon}</span><div><span className="category-pill">{skill.category}</span><h2>{skill.name}</h2><p>by {skill.author} · {skill.uses} 次加载</p></div></div>
        <p className="detail-description">{skill.description}</p>
        <div className="compatibility"><b>兼容工具</b><div>{skill.compatible.map((id) => <span key={id}>{toolMeta[id as keyof typeof toolMeta]?.name}</span>)}</div></div>
        <label className="note-field"><span><b>我的备注</b><small>仅你自己可见</small></span><textarea value={note} onChange={(event) => setNote(event.target.value)} placeholder="记录使用方法、适用项目或注意事项…" maxLength={1000}/><em>{note.length}/1000</em></label>
        <div className="modal-actions"><button className="secondary-button" onClick={() => onSaveNote(note)}>保存备注</button><button className="primary-button" onClick={onInstall}>加载到本机 <span>→</span></button></div>
      </div>
    </div>
  );
}

function InstallModal({ skill, signedIn, onRequireSignIn, onlineDevice, onClose, onDone }: { skill: Skill; signedIn: boolean; onRequireSignIn: () => void; onlineDevice: Device | null; onClose: () => void; onDone: (message: string) => void }) {
  const [os, setOs] = useState<"macos" | "windows">("macos");
  const [targets, setTargets] = useState<string[]>(() => skill.compatible.includes("codex") ? ["codex"] : skill.compatible.slice(0, 1));

  function toggleTarget(target: string) {
    setTargets((current) => current.includes(target) ? current.filter((item) => item !== target) : [...current, target]);
  }

  async function install() {
    if (!targets.length) return;
    if (onlineDevice && skill.uploaded) {
      const taskResponse = await fetch("/api/installs", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ skillId: skill.id, deviceId: onlineDevice.id, targets }),
      });
      if (taskResponse.ok) {
        onDone(`安装任务已发送到 ${onlineDevice.name}`);
        return;
      }
    }
    let payload: Uint8Array;
    let extension = "md";

    if (skill.uploaded) {
      const response = await fetch(`/api/skills/${encodeURIComponent(skill.id)}/file`);
      if (!response.ok) return;
      payload = new Uint8Array(await response.arrayBuffer());
      extension = response.headers.get("x-skill-extension") || "zip";
    } else {
      payload = new TextEncoder().encode(`# ${skill.name}\n\n${skill.description}\n\n## 使用说明\n\n请根据当前任务调用本 Skill，并在输出前检查结果是否满足用户要求。\n`);
    }

    let binary = "";
    for (let index = 0; index < payload.length; index += 8192) {
      binary += String.fromCharCode(...payload.subarray(index, index + 8192));
    }
    const base64 = window.btoa(binary);
    const resolvedName = resolveSkillName(payload, extension, skill.name);
    const slug = slugifySkillName(resolvedName);
    const selectedTargets = targets.filter((target) => skill.compatible.includes(target));
    const targetPaths = installPaths(selectedTargets, slug);
    if (!targetPaths.length) return;
    const targetLabel = selectedTargets.join("-");
    const uniqueSuffix = Date.now();
    let blob: Blob;
    let downloadName: string;
    if (os === "macos") {
      const scriptFileName = `install-${slug}.command`;
      const script = createMacInstaller(base64, extension, targetPaths);
      blob = new Blob([createMacInstallerArchive(script, scriptFileName)], { type: "application/zip" });
      downloadName = `skillport-${slug}-${targetLabel}-${uniqueSuffix}.zip`;
    } else {
      const script = createWindowsInstaller(base64, extension, targetPaths);
      blob = new Blob([script], { type: "text/plain;charset=utf-8" });
      downloadName = `install-${slug}-${targetLabel}-${uniqueSuffix}.ps1`;
    }
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = downloadName;
    anchor.click();
    anchor.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    fetch("/api/installs", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ skillId: skill.id, targets, operatingSystem: os }) }).catch(() => undefined);
    onDone(`${os === "macos" ? "macOS ZIP" : "Windows"} 安装器已下载（${selectedTargets.length} 个工具）`);
  }

  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal install-modal" role="dialog" aria-modal="true" aria-label="加载 Skill 到本机" onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" onClick={onClose}>×</button>
        <span className="step-label">LOAD TO DEVICE</span><h2>把 {skill.name} 加载到哪里？</h2><p className="install-lead">SkillPort 会为不同工具转换为正确目录结构，原 Skill 文件保持不变。</p>
        <div className="os-switch"><button className={os === "macos" ? "active" : ""} onClick={() => setOs("macos")}><span>●</span> macOS</button><button className={os === "windows" ? "active" : ""} onClick={() => setOs("windows")}><span>⊞</span> Windows</button></div>
        <div className="target-list">
          {skill.compatible.map((id) => {
            const tool = toolMeta[id as keyof typeof toolMeta];
            const checked = targets.includes(id);
            return <button key={id} className={checked ? "target-row checked" : "target-row"} onClick={() => toggleTarget(id)}><span className={`tool-logo ${tool.color}`}>{tool.mark}</span><p><b>{tool.name}</b><small>{os === "macos" ? `~/.${id}/skills` : `%USERPROFILE%\\.${id}\\skills`}</small></p><span className="checkbox">{checked ? "✓" : ""}</span></button>;
          })}
        </div>
        <div className="bridge-notice"><span className={onlineDevice ? "status-dot" : "status-dot offline"}/><p><b>{onlineDevice ? `Bridge 已连接：${onlineDevice.name}` : "安全安装器已准备"}</b><small>{onlineDevice ? "点击后由 Netty 实时推送并回传安装进度" : os === "macos" ? "下载 ZIP、解压后运行一次；安装前会自动备份同名 Skill" : "下载后运行一次；安装前会自动备份同名 Skill"}</small></p></div>
        {skill.fileName?.toLowerCase().endsWith(".md") && <div className="pair-error">当前是单文件 SKILL.md；如 Skill 还包含 scripts、references 或 assets，请重新上传 ZIP。</div>}
        {signedIn ? <button className="full-primary" disabled={!targets.length} onClick={install}>{onlineDevice && skill.uploaded ? "发送到 Bridge" : `下载 ${os === "macos" ? "macOS" : "Windows"} 安装器`} <span>→</span></button> : <button className="full-primary" onClick={onRequireSignIn}>登录后继续 <span>→</span></button>}
      </div>
    </div>
  );
}

function AuthModal({ onClose, onAuthenticated }: { onClose: () => void; onAuthenticated: (user: Exclude<User, null>) => void }) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    if (mode === "register" && password !== confirmPassword) {
      setError("两次输入的密码不一致。");
      return;
    }
    setBusy(true);
    try {
      const response = await fetch(`/api/auth/${mode}`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ email, password, displayName }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok || !data?.user) {
        setError(data?.error || "登录没有完成，请稍后再试。");
        return;
      }
      onAuthenticated({ id: data.user.id, email: data.user.email, name: data.user.displayName });
    } catch {
      setError("暂时无法连接登录服务，请稍后再试。");
    } finally {
      setBusy(false);
    }
  }

  function switchMode(nextMode: "login" | "register") {
    setMode(nextMode);
    setError("");
    setPassword("");
    setConfirmPassword("");
  }

  return (
    <div className="modal-backdrop auth-backdrop" onMouseDown={onClose}>
      <div className="modal auth-modal" role="dialog" aria-modal="true" aria-label="SkillPort 账户登录" onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" onClick={onClose}>×</button>
        <div className="auth-brand"><span className="brand-mark">S</span><div><b>欢迎来到 SkillPort</b><small>使用你自己的数据库账户</small></div></div>
        <div className="auth-tabs" role="tablist">
          <button className={mode === "login" ? "active" : ""} onClick={() => switchMode("login")}>登录</button>
          <button className={mode === "register" ? "active" : ""} onClick={() => switchMode("register")}>注册新账户</button>
        </div>
        <form className="auth-form" onSubmit={submit}>
          {mode === "register" && <label><span>显示名称</span><input required maxLength={120} autoComplete="name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} placeholder="例如：Asher"/></label>}
          <label><span>邮箱</span><input required type="email" maxLength={254} autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="name@example.com"/></label>
          <label><span>密码</span><input required type="password" minLength={8} maxLength={72} autoComplete={mode === "login" ? "current-password" : "new-password"} value={password} onChange={(event) => setPassword(event.target.value)} placeholder="至少 8 位字符"/></label>
          {mode === "register" && <label><span>确认密码</span><input required type="password" minLength={8} maxLength={72} autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} placeholder="再次输入密码"/></label>}
          {error && <div className="auth-error" role="alert">{error}</div>}
          <button className="full-primary auth-submit" disabled={busy}>{busy ? "正在安全验证…" : mode === "login" ? "登录 SkillPort" : "创建私人账户"} <span>→</span></button>
        </form>
        <div className="auth-security"><span>✓</span><p><b>密码不会明文保存</b><small>服务端使用 BCrypt 哈希，会话保存在 HttpOnly 安全 Cookie 中。</small></p></div>
      </div>
    </div>
  );
}

function UploadModal({ onClose, onFile }: { onClose: () => void; onFile: (file?: File) => void }) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal upload-modal" role="dialog" aria-modal="true" aria-label="上传 Skill" onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" onClick={onClose}>×</button><span className="step-label">PRIVATE UPLOAD</span><h2>上传你的 Skill</h2><p className="install-lead">文件与备注都存放在你的私人空间，其他用户无法看到。</p>
        <label className="large-upload"><input type="file" accept=".zip,.skill,.md" onChange={(event) => onFile(event.target.files?.[0])}/><span>↑</span><b>选择 Skill 文件</b><small>.zip、.skill 或 SKILL.md，最大 25MB</small></label>
      </div>
    </div>
  );
}

function PairDeviceModal({ onClose }: { onClose: () => void }) {
  const [pairing, setPairing] = useState<{ code: string; expiresAt: string; apiBaseUrl: string; nettyUrl: string } | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    fetch("/api/devices", { method: "POST" })
      .then(async (response) => {
        if (!response.ok) throw new Error();
        setPairing(await response.json());
      })
      .catch(() => setError("暂时无法生成配对码，请检查 Java 服务配置。"));
  }, []);

  const command = pairing
    ? `java -jar skillport-bridge.jar pair ${pairing.apiBaseUrl} ${pairing.nettyUrl} ${pairing.code} "My Computer"`
    : "";

  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal pair-modal" role="dialog" aria-modal="true" aria-label="配对 Bridge" onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" onClick={onClose}>×</button><span className="step-label">PAIR A DEVICE</span><h2>连接 SkillPort Bridge</h2>
        <p className="install-lead">在 macOS 或 Windows 客户端运行下面的命令。配对码十分钟内有效，使用一次后立即失效。</p>
        {error ? <div className="pair-error">{error}</div> : pairing ? <>
          <div className="pair-code"><small>设备配对码</small><b>{pairing.code}</b><span>有效期至 {new Date(pairing.expiresAt).toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" })}</span></div>
          <div className="pair-command"><code>{command}</code><button onClick={() => navigator.clipboard.writeText(command)}>复制</button></div>
          <div className="bridge-notice"><span className="status-dot"/><p><b>配对后会自动保持连接</b><small>设备令牌仅保存在本机，服务端只保存哈希</small></p></div>
        </> : <div className="pair-loading">正在生成安全配对码…</div>}
      </div>
    </div>
  );
}
