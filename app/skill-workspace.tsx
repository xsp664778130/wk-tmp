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

const skillCategories = ["编程开发", "测试工具", "排查工具", "日志报告"] as const;

type SkillCategory = (typeof skillCategories)[number];

type Skill = {
  id: string;
  name: string;
  description: string;
  category: SkillCategory;
  accent: string;
  icon: string;
  author: string;
  uses: string;
  note?: string;
  uploaded?: boolean;
  fileName?: string;
  compatible: string[];
  scope?: "private" | "public" | "sample";
  shared?: boolean;
  pulled?: boolean;
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
    category: "日志报告",
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
    category: "测试工具",
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
    category: "日志报告",
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
    category: "排查工具",
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
    category: "排查工具",
    accent: "pink",
    icon: "⌁",
    author: "Kite Works",
    uses: "5.3k",
    compatible: ["codex", "qoder"],
  },
];

sampleSkills.forEach((skill) => { skill.scope = "sample"; });

const categories = [
  ["全部技能", "▦"],
  ["编程开发", "</>"],
  ["测试工具", "✓"],
  ["排查工具", "⌕"],
  ["日志报告", "≡"],
] as const;

function normalizeSkillCategory(value: unknown): SkillCategory {
  const category = String(value || "").trim() as SkillCategory;
  return skillCategories.includes(category) ? category : "编程开发";
}

const accents = ["coral", "lime", "violet", "blue", "yellow", "pink"];

function privateSkillFromApi(skill: Record<string, unknown>, index: number): Skill {
  return {
    id: String(skill.id),
    name: String(skill.name),
    description: String(skill.description || ""),
    category: normalizeSkillCategory(skill.category),
    accent: accents[index % accents.length],
    icon: skill.sourcePublicSkillId ? "↓" : "↑",
    author: skill.sourcePublicSkillId ? "公有池拉取" : "我的上传",
    uses: "私有",
    note: String(skill.note || ""),
    uploaded: true,
    fileName: String(skill.fileName || "skill.zip"),
    compatible: String(skill.toolCompatibility || "codex,qoder,openai").split(","),
    scope: "private",
    shared: Boolean(skill.shared),
  };
}

function publicSkillFromApi(skill: Record<string, unknown>, index: number): Skill {
  const compatible = Array.isArray(skill.compatible)
    ? skill.compatible.map(String)
    : String(skill.toolCompatibility || "codex,qoder,openai").split(",");
  return {
    id: String(skill.id),
    name: String(skill.name),
    description: String(skill.description || ""),
    category: normalizeSkillCategory(skill.category),
    accent: accents[index % accents.length],
    icon: "↗",
    author: String(skill.author || "SkillPort 用户"),
    uses: `${Number(skill.pullCount || 0)} 次拉取`,
    fileName: String(skill.fileName || "skill.zip"),
    compatible,
    scope: "public",
    pulled: Boolean(skill.pulled),
  };
}

const toolMeta = {
  codex: { name: "Codex", mark: "CX", color: "dark" },
  qoder: { name: "Qoder", mark: "Q", color: "blue" },
  openai: { name: "OpenAI", mark: "AI", color: "green" },
} as const;

async function copyText(value: string) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(value);
    return;
  }
  const textarea = document.createElement("textarea");
  textarea.value = value;
  textarea.setAttribute("readonly", "");
  textarea.style.position = "fixed";
  textarea.style.opacity = "0";
  document.body.appendChild(textarea);
  textarea.select();
  const copied = document.execCommand("copy");
  textarea.remove();
  if (!copied) throw new Error("copy failed");
}

export function SkillWorkspace({ initialUser }: { initialUser: User }) {
  const [user, setUser] = useState<User>(initialUser);
  const [activeCategory, setActiveCategory] = useState("全部技能");
  const [query, setQuery] = useState("");
  const [libraryMode, setLibraryMode] = useState<"public" | "private">("public");
  const [privateSkills, setPrivateSkills] = useState<Skill[]>([]);
  const [publicSkills, setPublicSkills] = useState<Skill[]>(sampleSkills);
  const [devices, setDevices] = useState<Device[]>([]);
  const [selected, setSelected] = useState<Skill | null>(null);
  const [installer, setInstaller] = useState<Skill | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [pairOpen, setPairOpen] = useState(false);
  const [authOpen, setAuthOpen] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [shareCandidate, setShareCandidate] = useState<Skill | null>(null);
  const [shareBusy, setShareBusy] = useState(false);
  const [pullingId, setPullingId] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (initialUser) return;
    let cancelled = false;
    fetch("/api/auth/me")
      .then(async (response) => response.ok ? response.json() : null)
      .then((data) => {
        if (!cancelled && data?.user) {
          setUser({ id: data.user.id, name: data.user.displayName, email: data.user.email });
        }
      })
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, [initialUser]);

  useEffect(() => {
    let cancelled = false;
    if (!user) return;
    Promise.all([
      fetch("/api/skills").then((response) => (response.ok ? response.json() : null)),
      fetch("/api/devices").then((response) => (response.ok ? response.json() : null)),
      fetch("/api/public-skills").then((response) => (response.ok ? response.json() : null)),
    ])
      .then(([skillData, deviceData, publicSkillData]) => {
        if (cancelled) return;
        const uploaded = (Array.isArray(skillData?.skills) ? skillData.skills : [])
          .map(privateSkillFromApi);
        const published = (Array.isArray(publicSkillData?.skills) ? publicSkillData.skills : [])
          .map(publicSkillFromApi);
        setPrivateSkills(uploaded);
        setPublicSkills(published);
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
    const skills = libraryMode === "public" ? publicSkills : privateSkills;
    return skills.filter((skill) => {
      const categoryMatch = activeCategory === "全部技能" || skill.category === activeCategory;
      const searchMatch = !normalized || `${skill.name} ${skill.description} ${skill.category}`.toLowerCase().includes(normalized);
      return categoryMatch && searchMatch;
    });
  }, [activeCategory, libraryMode, privateSkills, publicSkills, query]);

  function guardAccount(action: () => void) {
    if (user) return action();
    setAuthOpen(true);
    setToast("请先登录 SkillPort 账户");
  }

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" }).catch(() => undefined);
    setUser(null);
    setPrivateSkills([]);
    setPublicSkills(sampleSkills);
    setLibraryMode("public");
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
        category: "编程开发",
        accent: "lime",
        icon: "↑",
        author: "我的上传",
        uses: "私有",
        uploaded: true,
        fileName: file.name,
        compatible: ["codex", "qoder", "openai"],
        scope: "private",
      };
      setPrivateSkills((current) => [preview, ...current]);
      setLibraryMode("private");
      const form = new FormData();
      form.append("file", file);
      form.append("name", preview.name);
      form.append("category", preview.category);
      fetch("/api/skills", { method: "POST", body: form })
        .then(async (response) => {
          if (!response.ok) throw new Error();
          const data = await response.json();
          const created = data.skill ?? data;
          setPrivateSkills((current) => current.map((skill) => skill.id === preview.id
            ? { ...preview, id: created.id, fileName: created.fileName || file.name }
            : skill));
          setToast("Skill 已安全保存到你的私人空间");
        })
        .catch(() => {
          setPrivateSkills((current) => current.filter((skill) => skill.id !== preview.id));
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

  async function shareSkill() {
    if (!shareCandidate || !user) return;
    setShareBusy(true);
    try {
      const response = await fetch("/api/public-skills", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ skillId: shareCandidate.id }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(String(data?.error || "分享失败"));
      const publication = publicSkillFromApi(data as Record<string, unknown>, 0);
      setPrivateSkills((current) => current.map((skill) => skill.id === shareCandidate.id
        ? { ...skill, shared: true }
        : skill));
      setPublicSkills((current) => current.some((skill) => skill.id === publication.id)
        ? current
        : [publication, ...current]);
      setSelected((current) => current?.id === shareCandidate.id ? { ...current, shared: true } : current);
      setShareCandidate(null);
      setToast("Skill 已分享到公有池，个人备注没有公开");
    } catch {
      setToast("分享没有完成，请稍后再试");
    } finally {
      setShareBusy(false);
    }
  }

  async function pullSkill(skill: Skill) {
    if (!user) {
      setAuthOpen(true);
      setToast("登录后即可拉取到你的私人空间");
      return;
    }
    if (skill.scope !== "public" || skill.pulled || pullingId) return;
    setPullingId(skill.id);
    try {
      const response = await fetch(`/api/public-skills/${encodeURIComponent(skill.id)}/pull`, { method: "POST" });
      const data = await response.json().catch(() => ({}));
      if (!response.ok || !data?.skill) throw new Error();
      const imported = privateSkillFromApi(data.skill as Record<string, unknown>, 0);
      setPrivateSkills((current) => current.some((item) => item.id === imported.id)
        ? current
        : [imported, ...current]);
      setPublicSkills((current) => current.map((item) => item.id === skill.id
        ? { ...item, pulled: true, uses: data.created ? `${Number.parseInt(item.uses, 10) + 1} 次拉取` : item.uses }
        : item));
      setSelected((current) => current?.id === skill.id ? { ...current, pulled: true } : current);
      setToast(data.created ? "已拉取到你的私人空间" : "这个 Skill 已在你的私人空间");
    } catch {
      setToast("拉取没有完成，请稍后再试");
    } finally {
      setPullingId(null);
    }
  }

  const displayName = user?.name?.includes("@") ? user.name.split("@")[0] : user?.name || "访客";
  const onlineDevice = devices.find((device) => device.status === "ONLINE");

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">S</span><span>skillport<span className="brand-dot">.</span></span></div>
        <nav className="side-nav" aria-label="技能分类">
          <p className="nav-label">探索</p>
          <button className={libraryMode === "public" ? "nav-item active" : "nav-item"} onClick={() => { setLibraryMode("public"); setActiveCategory("全部技能"); setQuery(""); }}>
            <span className="nav-icon">◎</span><span>Skill 公有池</span><span className="nav-count">{publicSkills.length}</span>
          </button>
          {categories.map(([name, icon]) => (
            <button key={name} className={libraryMode === "public" && activeCategory === name ? "nav-item category-active" : "nav-item"} onClick={() => { setLibraryMode("public"); setActiveCategory(name); }}>
              <span className="nav-icon">{icon}</span><span>{name}</span>
              {name === "全部技能" && <span className="nav-count">{publicSkills.length}</span>}
            </button>
          ))}
          <p className="nav-label nav-label-space">个人空间</p>
          <button className={libraryMode === "private" ? "nav-item active" : "nav-item"} onClick={() => { guardAccount(() => { setLibraryMode("private"); setActiveCategory("全部技能"); setQuery(""); }); }}><span className="nav-icon">↑</span><span>我的 Skill</span><span className="nav-count">{privateSkills.length}</span></button>
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
            <div><span className="stat-icon purple">▦</span><p><b>{privateSkills.length}</b><small>我的 Skills</small></p><em>{privateSkills.filter((skill) => skill.shared).length} 个已分享</em></div>
            <div><span className="stat-icon orange">◎</span><p><b>{publicSkills.length}</b><small>公有池 Skills</small></p><em>社区共享</em></div>
            <div><span className="stat-icon green">⌁</span><p><b>{devices.filter((device) => device.status === "ONLINE").length}</b><small>在线设备</small></p><em className="neutral">macOS · Windows</em></div>
          </section>

          {libraryMode === "public" && (
            <section className="pool-banner">
              <span className="pool-mark">◎</span>
              <div><b>Skill 公有池</b><p>发现其他用户公开的 Skill，一键复制到你的私人空间后再备注或加载。</p></div>
              <button onClick={() => guardAccount(() => { setLibraryMode("private"); setActiveCategory("全部技能"); })}>分享我的 Skill <span>→</span></button>
            </section>
          )}

          <section className="library-heading">
            <div><h2>{libraryMode === "public" ? (activeCategory === "全部技能" ? "社区最新分享" : activeCategory) : "我的私人空间"}</h2><p>{libraryMode === "public" ? "拉取后会生成一份属于你的独立副本。" : "只有你可以查看文件、备注和已拉取的副本。"}</p></div>
            <div className="view-actions"><button className="sort-button">最近更新　⌄</button><button className="view-button active">▦</button><button className="view-button">☷</button></div>
          </section>

          {filtered.length ? (
            <section className="skill-grid">
              {filtered.map((skill) => (
                <article className="skill-card" key={skill.id} onClick={() => setSelected(skill)}>
                  <div className="card-top">
                    <span className={`skill-icon ${skill.accent}`}>{skill.icon}</span>
                    {skill.scope === "private" && skill.shared ? <span className="shared-badge">已公开</span> : <button className="more-button" aria-label={`${skill.name} 更多操作`} onClick={(event) => event.stopPropagation()}>•••</button>}
                  </div>
                  <span className="category-pill">{skill.category}</span>
                  <h3>{skill.name}</h3>
                  <p className="skill-description">{skill.description}</p>
                  {skill.note && <p className="note-preview"><span>✎</span>{skill.note}</p>}
                  <div className="card-footer">
                    <span className="author-avatar">{skill.author.slice(0, 1)}</span><span className="author-name">{skill.author}</span>
                    <span className="usage">↓ {skill.uses}</span>
                  </div>
                  {skill.scope === "public" ? (
                    <button className={skill.pulled ? "install-card-button pulled" : "install-card-button"} disabled={skill.pulled || pullingId === skill.id} onClick={(event) => { event.stopPropagation(); void pullSkill(skill); }}>
                      {pullingId === skill.id ? "正在拉取…" : skill.pulled ? "已在我的空间" : "拉取到我的空间"} <span>{skill.pulled ? "✓" : "→"}</span>
                    </button>
                  ) : skill.scope === "sample" ? (
                    <button className="install-card-button" onClick={(event) => { event.stopPropagation(); setAuthOpen(true); }}>登录后拉取 <span>→</span></button>
                  ) : (
                    <button className="install-card-button" onClick={(event) => { event.stopPropagation(); setInstaller(skill); }}>加载到本机 <span>→</span></button>
                  )}
                </article>
              ))}
            </section>
          ) : (
            <div className="empty-state"><span>{libraryMode === "public" ? "◎" : "⌕"}</span><h3>{libraryMode === "public" ? "公有池还没有这个分类的 Skill" : "私人空间里还没有 Skill"}</h3><p>{libraryMode === "public" ? "你可以成为第一个分享者，个人备注不会公开。" : "拖动上传，或先从公有池拉取一份。"}</p><button onClick={() => { setQuery(""); setActiveCategory("全部技能"); libraryMode === "public" ? guardAccount(() => setLibraryMode("private")) : setLibraryMode("public"); }}>{libraryMode === "public" ? "去分享 Skill" : "浏览公有池"}</button></div>
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

      {selected && <DetailModal skill={selected} pulling={pullingId === selected.id} onClose={() => setSelected(null)} onInstall={() => { setInstaller(selected); setSelected(null); }} onPull={() => void pullSkill(selected)} onShare={() => setShareCandidate(selected)} onSaveNote={(note) => {
        setPrivateSkills((current) => current.map((skill) => skill.id === selected.id ? { ...skill, note } : skill));
        setSelected((current) => current ? { ...current, note } : current);
        if (selected.uploaded && user) fetch("/api/skills", { method: "PATCH", headers: { "content-type": "application/json" }, body: JSON.stringify({ id: selected.id, note }) }).catch(() => undefined);
        setToast("备注已保存，仅你自己可见");
      }}/>}
      {shareCandidate && <ShareConfirmModal skill={shareCandidate} busy={shareBusy} onClose={() => !shareBusy && setShareCandidate(null)} onConfirm={() => void shareSkill()}/>}
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

function DetailModal({ skill, pulling, onClose, onInstall, onPull, onShare, onSaveNote }: {
  skill: Skill;
  pulling: boolean;
  onClose: () => void;
  onInstall: () => void;
  onPull: () => void;
  onShare: () => void;
  onSaveNote: (note: string) => void;
}) {
  const [note, setNote] = useState(skill.note || "");
  const isPublic = skill.scope === "public" || skill.scope === "sample";
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal detail-modal" role="dialog" aria-modal="true" aria-label={`${skill.name} 详情`} onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" onClick={onClose}>×</button>
        <div className="detail-hero"><span className={`skill-icon large ${skill.accent}`}>{skill.icon}</span><div><span className="category-pill">{skill.category}</span><h2>{skill.name}</h2><p>by {skill.author} · {skill.uses}</p></div></div>
        <p className="detail-description">{skill.description}</p>
        <div className="compatibility"><b>兼容工具</b><div>{skill.compatible.map((id) => <span key={id}>{toolMeta[id as keyof typeof toolMeta]?.name}</span>)}</div></div>
        {isPublic ? (
          <>
            <div className="public-privacy-note"><span>✓</span><p><b>拉取后数据隔离</b><small>会复制一份到你的私人空间；发布者看不到你的文件修改和个人备注。</small></p></div>
            <div className="modal-actions"><button className="secondary-button" onClick={onClose}>暂不拉取</button><button className="primary-button" disabled={skill.pulled || pulling} onClick={skill.scope === "sample" ? onPull : onPull}>{pulling ? "正在拉取…" : skill.pulled ? "已在我的空间" : "拉取到我的空间"} <span>{skill.pulled ? "✓" : "→"}</span></button></div>
          </>
        ) : (
          <>
            <label className="note-field"><span><b>我的备注</b><small>仅你自己可见</small></span><textarea value={note} onChange={(event) => setNote(event.target.value)} placeholder="记录使用方法、适用项目或注意事项…" maxLength={1000}/><em>{note.length}/1000</em></label>
            <div className="share-inline"><span>◎</span><p><b>{skill.shared ? "已分享到公有池" : "分享给社区"}</b><small>{skill.shared ? "其他用户可以拉取公开副本，备注仍保持私有。" : "只公开名称、描述、分类和 Skill 文件，不公开个人备注。"}</small></p><button disabled={skill.shared} onClick={onShare}>{skill.shared ? "已公开" : "分享"}</button></div>
            <div className="modal-actions"><button className="secondary-button" onClick={() => onSaveNote(note)}>保存备注</button><button className="primary-button" onClick={onInstall}>加载到本机 <span>→</span></button></div>
          </>
        )}
      </div>
    </div>
  );
}

function ShareConfirmModal({ skill, busy, onClose, onConfirm }: { skill: Skill; busy: boolean; onClose: () => void; onConfirm: () => void }) {
  return (
    <div className="modal-backdrop share-backdrop" onMouseDown={onClose}>
      <div className="modal share-modal" role="dialog" aria-modal="true" aria-label="确认分享到公有池" onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" disabled={busy} onClick={onClose}>×</button>
        <span className="step-label">SHARE TO PUBLIC POOL</span><h2>确认分享 {skill.name}？</h2>
        <p className="install-lead">分享后，其他已登录用户可以查看公开信息，并把 Skill 文件复制到自己的私人空间。</p>
        <div className="share-fields"><b>将公开</b><span>名称与描述</span><span>分类与兼容工具</span><span>Skill 文件内容</span></div>
        <div className="share-private"><b>保持私有</b><span>你的个人备注</span><span>账户邮箱与其他 Skill</span></div>
        <div className="modal-actions"><button className="secondary-button" disabled={busy} onClick={onClose}>取消</button><button className="primary-button" disabled={busy} onClick={onConfirm}>{busy ? "正在分享…" : "确认分享到公有池"} <span>→</span></button></div>
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
  const [os, setOs] = useState<"macos" | "windows">("macos");
  const [copied, setCopied] = useState(false);

  async function createPairing() {
    setPairing(null);
    setError("");
    setCopied(false);
    try {
      const response = await fetch("/api/devices", { method: "POST" });
      if (!response.ok) throw new Error();
      setPairing(await response.json());
    } catch {
      setError("暂时无法生成配对码，请稍后再试。");
    }
  }

  useEffect(() => {
    void createPairing();
    const userAgent = navigator.userAgent.toLowerCase();
    if (userAgent.includes("windows")) setOs("windows");
  }, []);

  const scriptUrl = pairing
    ? `${pairing.apiBaseUrl.replace(/\/$/, "")}/bridge/install-${os === "macos" ? "macos.sh" : "windows.ps1"}`
    : "";
  const command = pairing
    ? os === "macos"
      ? `curl -fsSL '${scriptUrl}' | bash -s -- '${pairing.apiBaseUrl}' '${pairing.nettyUrl}' '${pairing.code}'`
      : `powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& ([scriptblock]::Create((Invoke-RestMethod '${scriptUrl}'))) -ApiBaseUrl '${pairing.apiBaseUrl}' -NettyUrl '${pairing.nettyUrl}' -PairingCode '${pairing.code}'"`
    : "";
  const manualCommand = pairing
    ? `java -jar skillport-bridge.jar pair ${pairing.apiBaseUrl} ${pairing.nettyUrl} ${pairing.code} "My Computer"`
    : "";

  async function copyInstallerCommand() {
    try {
      await copyText(command);
      setCopied(true);
    } catch {
      setError("浏览器没有复制权限，请手动选中下面的命令复制。");
    }
  }

  function switchOs(nextOs: "macos" | "windows") {
    setOs(nextOs);
    setCopied(false);
  }

  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal pair-modal" role="dialog" aria-modal="true" aria-label="配对 Bridge" onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" onClick={onClose}>×</button><span className="step-label">QUICK SETUP</span><h2>3 步连接这台电脑</h2>
        <p className="install-lead">安装器会自动准备 Java、下载 Bridge、完成配对并设置开机启动，不需要管理员权限。</p>
        {error ? <div className="pair-error">{error}</div> : pairing ? <>
          <div className="os-switch pair-os-switch" role="tablist" aria-label="选择电脑系统">
            <button className={os === "macos" ? "active" : ""} onClick={() => switchOs("macos")}><span>●</span> macOS</button>
            <button className={os === "windows" ? "active" : ""} onClick={() => switchOs("windows")}><span>⊞</span> Windows</button>
          </div>
          <div className="pair-steps">
            <div><span>1</span><p><b>{os === "macos" ? "打开「终端」" : "打开「终端」或 PowerShell"}</b><small>{os === "macos" ? "按 ⌘ + 空格，搜索“终端”并打开" : "右键开始菜单，选择“终端”（无需管理员）"}</small></p></div>
            <div><span>2</span><p><b>复制命令并粘贴运行</b><small>{os === "macos" ? "在终端按 ⌘ + V，然后按回车" : "在终端按 Ctrl + V，然后按回车"}</small></p></div>
            <div><span>3</span><p><b>看到“安装完成”即可</b><small>回到本页面，设备会自动显示为在线</small></p></div>
          </div>
          <button className={copied ? "full-primary pair-copy copied" : "full-primary pair-copy"} onClick={copyInstallerCommand}>{copied ? "✓ 已复制，现在粘贴到终端" : `复制 ${os === "macos" ? "macOS" : "Windows"} 一键安装命令`} <span>→</span></button>
          <div className="pair-command"><code>{command}</code><button onClick={copyInstallerCommand}>{copied ? "已复制" : "复制"}</button></div>
          <div className="pair-code compact"><small>本次配对码</small><b>{pairing.code}</b><span>有效期至 {new Date(pairing.expiresAt).toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" })} · <button onClick={createPairing}>刷新配对码</button></span></div>
          <div className="bridge-notice"><span className="status-dot"/><p><b>安装来源可校验</b><small>Java 与 Bridge 均进行 SHA-256 校验；设备令牌只保存在本机</small></p></div>
          <details className="pair-advanced">
            <summary>高级选项：查看脚本或手动安装</summary>
            <p><a href={scriptUrl} target="_blank" rel="noreferrer">查看当前系统的公开安装脚本</a></p>
            <code>{manualCommand}</code>
          </details>
        </> : <div className="pair-loading">正在生成安全配对码…</div>}
      </div>
    </div>
  );
}
