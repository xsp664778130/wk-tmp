"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { DragEvent, FormEvent } from "react";
import {
  createMacInstaller,
  createMacInstallerArchive,
  createWindowsInstaller,
  installPaths,
  installerTargetRoots,
  resolveSkillName,
  slugifySkillName,
} from "./installer-utils";

const skillCategories = ["编程技能", "测试技能", "排查技能", "日志技能"] as const;

const releaseNotes = [
  {
    version: "1.0.24",
    date: "2026-08-28",
    title: "上传界面主题适配",
    changes: [
      "上传弹窗的标题、说明、字段标签与辅助文字统一跟随当前配色主题。",
      "文件选择区改为柔和的主题色层次，移除暗色主题下突兀的纯白色块。",
      "输入框、结构样例、头像区域与保存按钮补齐悬停、聚焦和禁用状态。",
    ],
  },
  {
    version: "1.0.23",
    date: "2026-08-27",
    title: "Skill 保存接口修复",
    changes: [
      "修复 K3s 网页编辑 Skill 分类时返回 Method Not Allowed 的问题。",
      "统一 K3s 与 Sites 的 Skill 分类、详细说明和使用步骤保存地址。",
      "补齐网页上传时详细说明和使用步骤在服务端的接收与保存。",
    ],
  },
  {
    version: "1.0.22",
    date: "2026-08-27",
    title: "暗色界面可读性优化",
    changes: [
      "提升左侧品牌、导航文字和分组标题的亮度与字重，暗色背景下更清晰。",
      "右侧本机工具区域改为统一的深色卡片层级，移除突兀的纯白色块。",
      "设备选择、快速导入、状态标签和操作按钮统一适配全部配色主题。",
    ],
  },
  {
    version: "1.0.21",
    date: "2026-08-27",
    title: "Skill 分类保存修复",
    changes: [
      "我的 Skill 分类改为选择后立即自动保存，不再需要额外点击保存按钮。",
      "分类保存过程会显示明确状态，失败时自动恢复原分类。",
      "已分享 Skill 的分类保存成功后同步刷新公有池记录。",
    ],
  },
  {
    version: "1.0.20",
    date: "2026-08-27",
    title: "Skill 详情与使用步骤",
    changes: [
      "上传和编辑 Skill 时可维护完整详细说明与最多 20 个使用步骤。",
      "Skill 卡片新增详情摘要和步骤数量，点击后按编号查看完整使用方法。",
      "已分享 Skill 的名称、描述、详细说明和使用步骤会自动同步到公有池。",
    ],
  },
  {
    version: "1.0.19",
    date: "2026-08-27",
    title: "多主题配色",
    changes: [
      "新增深夜紫、曜石黑、海湾蓝与晨雾白 4 套界面配色，可在顶部随时切换。",
      "默认采用参考图风格的深夜紫暗色界面，卡片、弹窗、输入框和侧栏统一适配。",
      "主题选择保存在当前设备，关闭网页或重启客户端后仍会自动恢复。",
    ],
  },
  {
    version: "1.0.18",
    date: "2026-08-25",
    title: "Skill 详情操作区焕新",
    changes: [
      "客户端 Skill 详情页重新划分主操作、次级操作与危险操作，按钮统一尺寸和对齐。",
      "安装到本机作为唯一主按钮，卸载与公有池操作使用等宽次级按钮。",
      "分类改为选择后自动保存，保存备注回归内容区域，删除操作降低视觉干扰。",
    ],
  },
  {
    version: "1.0.17",
    date: "2026-08-25",
    title: "一键客户端自动更新",
    changes: [
      "客户端发现云端新版本时，版本弹窗显示“立即更新到最新版”按钮。",
      "点击后在客户端内下载匹配当前系统的安装包，并实时显示下载进度。",
      "下载完成后自动启动 Windows 或 macOS 安装程序，不再跳转浏览器手动查找文件。",
    ],
  },
  {
    version: "1.0.16",
    date: "2026-08-25",
    title: "Skill 分类同步编辑",
    changes: [
      "我的 Skill 详情新增分类编辑，可以随时切换统一分类。",
      "已分享到公有池的 Skill 会在同一事务中同步修改公开分类。",
      "网页与桌面客户端都会立即刷新私人空间和公有池分类。",
    ],
  },
  {
    version: "1.0.15",
    date: "2026-08-25",
    title: "公开意见墙与分页",
    changes: [
      "意见信箱升级为公开意见墙，未登录访客也可以浏览大家提交的意见。",
      "每条意见显示提交人昵称、意见类型与准确提交时间。",
      "列表采用服务端 MySQL 分页，支持逐页浏览并保留传真提交动画。",
    ],
  },
  {
    version: "1.0.14",
    date: "2026-08-25",
    title: "Cursor Skills 支持",
    changes: [
      "本机 AI 工具新增 Cursor，并显示实际的 ~/.cursor/skills 目录。",
      "网页 Bridge 与桌面客户端均可把 Skill 安装或卸载到 Cursor。",
      "macOS 与 Windows 会根据 Cursor 应用或命令进行真实识别。",
    ],
  },
  {
    version: "1.0.13",
    date: "2026-08-25",
    title: "意见信箱与传真动画",
    changes: [
      "网页与桌面客户端新增账号专属的意见信箱入口。",
      "提交时播放纸张扫描、信号传输和送达回执的传真动画。",
      "意见安全写入 MySQL，并与提交用户账号绑定。",
    ],
  },
  {
    version: "1.0.12",
    date: "2026-08-25",
    title: "最近 5 个版本记录",
    changes: [
      "版本更新弹窗新增固定滚动区域和始终可见的滚动条。",
      "按发布时间从新到旧展示最近 5 个版本的完整更新内容。",
      "发现云端新版本时自动置顶，并保留最近历史记录。",
    ],
  },
  {
    version: "1.0.11",
    date: "2026-08-25",
    title: "本机 Skill 目录可见",
    changes: [
      "已识别的 AI 工具卡片直接显示实际本机 Skills 目录。",
      "目录路径使用独立文件夹样式，长路径自动折行并保留完整提示。",
      "未检测到的工具不展示目录，避免把预设路径误认为已安装目录。",
    ],
  },
  {
    version: "1.0.10",
    date: "2026-08-25",
    title: "客户端在线更新",
    changes: [
      "客户端自动检查云端最新版本，并准确比较当前版本。",
      "发现新版本时显示更新提示、版本号和红点提醒。",
      "更新弹窗按当前系统提供 macOS 或 Windows 的立即更新按钮。",
      "已是最新版时明确显示当前版本，不再持续显示误导性的红点。",
    ],
  },
  {
    version: "1.0.9",
    date: "2026-08-24",
    title: "上传兼容与草稿保护",
    changes: [
      "压缩包内的 SKILL.md 文件名支持大小写兼容，并显示实际检查到的文件路径。",
      "上传弹窗不再因点击遮罩意外关闭，未提交的名称、描述和分类会自动恢复。",
      "移除 OpenAI 工具项，新增 OpenCode 与 Claude Code 的识别、安装和卸载支持。",
      "重新设计删除按钮，避免窄卡片中出现文字竖排。",
    ],
  },
  {
    version: "1.0.8",
    date: "2026-08-24",
    title: "我的 Skill 快捷管理升级",
    changes: [
      "我的 Skill 卡片新增删除按钮，无需先打开详情页。",
      "删除成功后网页与客户端列表立即同步刷新。",
      "macOS 与 Windows 客户端重新设计卡片和详情页按钮排布。",
    ],
  },
  {
    version: "1.0.7",
    date: "2026-08-24",
    title: "版本通知全端同步",
    changes: [
      "K3s 托管网页同步显示右上角版本更新入口。",
      "macOS 与 Windows 客户端在登录页和主界面显示版本更新入口。",
      "点击版本更新可直接查看当前版本的新增、优化与修复内容。",
    ],
  },
  {
    version: "1.0.6",
    date: "2026-08-24",
    title: "Skill 元数据同步升级",
    changes: [
      "上传 Skill 时可以自定义名称、描述和分类。",
      "分享至公有池时，名称、描述和分类保持一致。",
      "网页拖拽上传会先进入信息确认，不再直接使用文件名保存。",
      "macOS 与 Windows 客户端同步支持新的上传信息。",
    ],
  },
] as const;

const currentRelease = releaseNotes[0];
const releaseHistoryLimit = 5;
const releaseSeenStorageKey = "skillport.release-seen";
const uploadDraftStorageKey = "skillport.upload-draft.v1";
const themeStorageKey = "skillport.ui-theme.v1";

const themeOptions = [
  { id: "midnight", name: "深夜紫", description: "参考图同款暗色", colors: ["#090c13", "#8168ff", "#b9ff6a"] },
  { id: "graphite", name: "曜石黑", description: "中性克制的黑灰", colors: ["#141517", "#d69a6a", "#dce6ef"] },
  { id: "ocean", name: "海湾蓝", description: "沉静的蓝青配色", colors: ["#07141c", "#35b8d8", "#71e5c1"] },
  { id: "daylight", name: "晨雾白", description: "明亮柔和的浅色", colors: ["#f6f5f1", "#7457e8", "#c9f55f"] },
] as const;

type UiTheme = (typeof themeOptions)[number]["id"];

function isUiTheme(value: string | null): value is UiTheme {
  return themeOptions.some((option) => option.id === value);
}

type SkillCategory = (typeof skillCategories)[number];

type SkillUploadMetadata = {
  name: string;
  description: string;
  detail: string;
  usageSteps: string[];
  category: SkillCategory;
};

type Skill = {
  id: string;
  name: string;
  description: string;
  detail: string;
  usageSteps: string[];
  category: SkillCategory;
  accent: string;
  icon: string;
  author: string;
  uses: string;
  note?: string;
  uploaded?: boolean;
  fileName?: string;
  compatible: string[];
  scope?: "private" | "public";
  shared?: boolean;
  pulled?: boolean;
  avatarUrl?: string;
  ownedByCurrentUser?: boolean;
  sourceSkillId?: string;
};

type User = { id: string; name: string; email: string } | null;
type Device = {
  id: string;
  name: string;
  os: string;
  arch: string;
  status: string;
  installedTools?: string[];
  toolsDetectedAt?: string;
  lastSeenAt?: string;
};
type DashboardStatistics = {
  mySkills: number;
  sharedSkills: number;
  totalInstalls: number;
  connectedDevices: number;
  onlineDevices: number;
};
type InstallTask = {
  id: string;
  skillId: string;
  targets: string[];
  operation: "INSTALL" | "UNINSTALL";
  status: string;
  createdAt: string;
};

function browserDeviceStorageKey(userId: string) {
  return `skillport.browser-device.${userId}`;
}

const categories = [
  ["全部技能", "▦"],
  ["编程技能", "</>"],
  ["测试技能", "✓"],
  ["排查技能", "⌕"],
  ["日志技能", "≡"],
] as const;

function normalizeSkillCategory(value: unknown): SkillCategory {
  const category = String(value || "").trim();
  const legacyCategories: Record<string, SkillCategory> = {
    编程开发: "编程技能",
    测试工具: "测试技能",
    排查工具: "排查技能",
    日志报告: "日志技能",
  };
  const normalized = legacyCategories[category] || category;
  return skillCategories.includes(normalized as SkillCategory)
    ? (normalized as SkillCategory)
    : "编程技能";
}

function dashboardStatisticsFromApi(value: unknown): DashboardStatistics | null {
  if (!value || typeof value !== "object") return null;
  const input = value as Record<string, unknown>;
  const keys = ["mySkills", "sharedSkills", "totalInstalls", "connectedDevices", "onlineDevices"] as const;
  const counts = keys.map((key) => Number(input[key]));
  if (counts.some((count) => !Number.isSafeInteger(count) || count < 0)) return null;
  return Object.fromEntries(keys.map((key, index) => [key, counts[index]])) as DashboardStatistics;
}

function installTaskFromApi(value: unknown): InstallTask | null {
  if (!value || typeof value !== "object") return null;
  const task = value as Record<string, unknown>;
  if (!task.id || !task.skillId || !task.createdAt) return null;
  return {
    id: String(task.id),
    skillId: String(task.skillId),
    targets: Array.isArray(task.targets) ? task.targets.map(String) : [],
    operation: task.operation === "UNINSTALL" ? "UNINSTALL" : "INSTALL",
    status: String(task.status || "PENDING"),
    createdAt: String(task.createdAt),
  };
}

function activityTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "时间未知";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

const accents = ["coral", "lime", "violet", "blue", "yellow", "pink"];

function normalizeToolCompatibility(value: unknown) {
  const requested = (Array.isArray(value) ? value.map(String) : String(value || "").split(","))
    .map((item) => item.trim())
    .filter(Boolean);
  const migrated = requested.flatMap((item) => item === "openai" ? ["opencode", "claude"] : [item]);
  const supported = ["codex", "qoder", "opencode", "claude", "cursor"];
  const normalized = supported.filter((item) => migrated.includes(item));
  return normalized.length ? normalized : supported;
}

function privateSkillFromApi(skill: Record<string, unknown>, index: number): Skill {
  return {
    id: String(skill.id),
    name: String(skill.name),
    description: String(skill.description || ""),
    detail: String(skill.detail || skill.description || ""),
    usageSteps: Array.isArray(skill.usageSteps) ? skill.usageSteps.map(String).filter(Boolean) : [],
    category: normalizeSkillCategory(skill.category),
    accent: accents[index % accents.length],
    icon: skill.sourcePublicSkillId ? "↓" : "↑",
    author: skill.sourcePublicSkillId ? "公有池拉取" : "我的上传",
    uses: "私有",
    note: String(skill.note || ""),
    uploaded: true,
    fileName: String(skill.fileName || "skill.zip"),
    compatible: normalizeToolCompatibility(skill.toolCompatibility),
    scope: "private",
    shared: Boolean(skill.shared),
    avatarUrl: skill.avatarUrl ? String(skill.avatarUrl) : undefined,
  };
}

function publicSkillFromApi(skill: Record<string, unknown>, index: number): Skill {
  const compatible = normalizeToolCompatibility(skill.compatible || skill.toolCompatibility);
  return {
    id: String(skill.id),
    name: String(skill.name),
    description: String(skill.description || ""),
    detail: String(skill.detail || skill.description || ""),
    usageSteps: Array.isArray(skill.usageSteps) ? skill.usageSteps.map(String).filter(Boolean) : [],
    category: normalizeSkillCategory(skill.category),
    accent: accents[index % accents.length],
    icon: "↗",
    author: String(skill.author || "SkillPort 用户"),
    uses: `${Number(skill.pullCount || 0)} 次拉取`,
    fileName: String(skill.fileName || "skill.zip"),
    compatible,
    scope: "public",
    pulled: Boolean(skill.pulled),
    avatarUrl: skill.avatarUrl ? String(skill.avatarUrl) : undefined,
    ownedByCurrentUser: Boolean(skill.ownedByCurrentUser),
    sourceSkillId: skill.sourceSkillId ? String(skill.sourceSkillId) : undefined,
  };
}

function SkillAvatar({ skill, large = false }: { skill: Skill; large?: boolean }) {
  return (
    <span className={`skill-icon ${large ? "large " : ""}${skill.accent}`}>
      {skill.avatarUrl ? <img src={skill.avatarUrl} alt={`${skill.name} 头像`}/> : skill.icon}
    </span>
  );
}

const toolMeta = {
  codex: { name: "Codex", mark: "CX", color: "dark" },
  qoder: { name: "Qoder", mark: "Q", color: "blue" },
  opencode: { name: "OpenCode", mark: "OC", color: "green" },
  claude: { name: "Claude Code", mark: "CC", color: "violet" },
  cursor: { name: "Cursor", mark: "CU", color: "dark" },
} as const;

function displayToolPath(id: keyof typeof installerTargetRoots, windows: boolean, slug?: string) {
  const relative = slug ? `${installerTargetRoots[id]}/${slug}` : installerTargetRoots[id];
  return windows ? `%USERPROFILE%\\${relative.replaceAll("/", "\\")}` : `~/${relative}`;
}

type ClientPlatform = "macos" | "windows";

const clientDownloads: Record<ClientPlatform, { label: string; url: string }> = {
  macos: {
    label: "macOS 客户端",
    url: "https://www.jmuyuer.com/bridge/client/SkillPort-Bridge.pkg?v=1.0.21",
  },
  windows: {
    label: "Windows 客户端",
    url: "https://www.jmuyuer.com/bridge/client/SkillPort-Setup.exe?v=1.0.21",
  },
};

function detectClientPlatform(userAgent: string): ClientPlatform | null {
  const normalized = userAgent.toLowerCase();
  if (normalized.includes("windows")) return "windows";
  if ((normalized.includes("macintosh") || normalized.includes("mac os x"))
      && !normalized.includes("iphone") && !normalized.includes("ipad")) return "macos";
  return null;
}

function startClientDownload(platform: ClientPlatform) {
  const link = document.createElement("a");
  link.href = clientDownloads[platform].url;
  link.download = "";
  document.body.appendChild(link);
  link.click();
  link.remove();
}

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

function normalizeHttpUrl(value: string) {
  const trimmed = value.trim();
  const markdownLink = trimmed.match(/^\[[^\]]+\]\((https?:\/\/[^)]+)\)$/i);
  const candidate = markdownLink?.[1] ?? trimmed;
  try {
    const url = new URL(candidate);
    if (url.protocol !== "https:" && url.protocol !== "http:") return "";
    return url.toString().replace(/\/$/, "");
  } catch {
    return "";
  }
}

function powershellUrlExpression(value: string) {
  const normalized = normalizeHttpUrl(value);
  const separatorIndex = normalized.indexOf("://");
  if (separatorIndex < 0) return "''";
  const scheme = normalized.slice(0, separatorIndex).replaceAll("'", "''");
  const remainder = normalized.slice(separatorIndex + 3).replaceAll("'", "''");
  // Splitting the scheme prevents rich-text tools from rewriting URLs as [url](url).
  return `'${scheme}:'+'//${remainder}'`;
}

export function SkillWorkspace({ initialUser }: { initialUser: User }) {
  const [user, setUser] = useState<User>(initialUser);
  const [activeCategory, setActiveCategory] = useState("全部技能");
  const [query, setQuery] = useState("");
  const [libraryMode, setLibraryMode] = useState<"public" | "private">("public");
  const [privateSkills, setPrivateSkills] = useState<Skill[]>([]);
  const [publicSkills, setPublicSkills] = useState<Skill[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [statistics, setStatistics] = useState<DashboardStatistics | null>(null);
  const [installTasks, setInstallTasks] = useState<InstallTask[]>([]);
  const [selected, setSelected] = useState<Skill | null>(null);
  const [installer, setInstaller] = useState<Skill | null>(null);
  const [uninstaller, setUninstaller] = useState<Skill | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [pendingUploadFile, setPendingUploadFile] = useState<File | null>(null);
  const [releaseOpen, setReleaseOpen] = useState(false);
  const [theme, setTheme] = useState<UiTheme>("midnight");
  const [themeOpen, setThemeOpen] = useState(false);
  const [releaseSeen, setReleaseSeen] = useState(true);
  const [pairOpen, setPairOpen] = useState(false);
  const [clientPlatform, setClientPlatform] = useState<ClientPlatform | null>(null);
  const [clientDownloadOpen, setClientDownloadOpen] = useState(false);
  const [authOpen, setAuthOpen] = useState(false);
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [shareCandidate, setShareCandidate] = useState<Skill | null>(null);
  const [shareBusy, setShareBusy] = useState(false);
  const [actionCandidate, setActionCandidate] = useState<{ skill: Skill; action: "delete" | "unpublish" } | null>(null);
  const [actionBusy, setActionBusy] = useState(false);
  const [pullingId, setPullingId] = useState<string | null>(null);
  const [scanningDeviceId, setScanningDeviceId] = useState<string | null>(null);
  const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const automaticToolScanAt = useRef(new Map<string, number>());

  useEffect(() => {
    const timer = window.setTimeout(() => {
      try {
        setReleaseSeen(window.localStorage.getItem(releaseSeenStorageKey) === currentRelease.version);
        const savedTheme = window.localStorage.getItem(themeStorageKey);
        if (isUiTheme(savedTheme)) setTheme(savedTheme);
      } catch {
        setReleaseSeen(false);
      }
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    document.documentElement.dataset.skillportTheme = theme;
    try {
      window.localStorage.setItem(themeStorageKey, theme);
    } catch {
      // 浏览器禁止本地存储时，本次会话内仍然可以切换主题。
    }
    return () => { delete document.documentElement.dataset.skillportTheme; };
  }, [theme]);

  useEffect(() => {
    if (!releaseOpen) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setReleaseOpen(false);
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [releaseOpen]);

  useEffect(() => {
    let active = true;
    queueMicrotask(() => {
      if (!active) return;
      if (!user) {
        setSelectedDeviceId(null);
        return;
      }
      try {
        setSelectedDeviceId(window.localStorage.getItem(browserDeviceStorageKey(user.id)) || null);
      } catch {
        setSelectedDeviceId(null);
      }
    });
    return () => { active = false; };
  }, [user]);

  const bindBrowserDevice = useCallback((deviceId: string | null) => {
    setSelectedDeviceId(deviceId);
    if (!user) return;
    try {
      const key = browserDeviceStorageKey(user.id);
      if (deviceId) window.localStorage.setItem(key, deviceId);
      else window.localStorage.removeItem(key);
    } catch {
      // Private browsing may block localStorage; the in-memory selection remains isolated.
    }
  }, [user]);

  useEffect(() => {
    const timer = window.setTimeout(() => setClientPlatform(detectClientPlatform(navigator.userAgent)), 0);
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (user || !/wxwork/i.test(navigator.userAgent)) return;
    const attemptKey = "skillport.wecom-auto-attempted";
    try {
      if (window.sessionStorage.getItem(attemptKey)) return;
      window.sessionStorage.setItem(attemptKey, "1");
    } catch {
      // Continue with one automatic authorization attempt when sessionStorage is unavailable.
    }
    window.location.assign("/api/auth/wecom?mode=auto");
  }, [user]);

  useEffect(() => {
    const url = new URL(window.location.href);
    const errorCode = url.searchParams.get("wecom_error");
    if (!errorCode) return;
    const messages: Record<string, string> = {
      not_configured: "企业微信登录尚未完成管理员配置。",
      invalid_state: "企业微信登录校验已过期，请重新扫码。",
      denied: "你取消了企业微信授权。",
      unavailable: "企业微信登录暂时不可用，请稍后重试。",
    };
    let active = true;
    queueMicrotask(() => {
      if (!active) return;
      setAuthOpen(true);
      setToast(messages[errorCode] || "企业微信登录没有完成，请重新尝试。");
      url.searchParams.delete("wecom_error");
      window.history.replaceState(null, "", `${url.pathname}${url.search}${url.hash}`);
    });
    return () => { active = false; };
  }, []);

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
      fetch("/api/stats", { cache: "no-store" }).then((response) => (response.ok ? response.json() : null)),
      fetch("/api/installs", { cache: "no-store" }).then((response) => (response.ok ? response.json() : null)),
    ])
      .then(([skillData, deviceData, publicSkillData, statisticsData, installData]) => {
        if (cancelled) return;
        const uploaded = (Array.isArray(skillData?.skills) ? skillData.skills : [])
          .map(privateSkillFromApi);
        const published = (Array.isArray(publicSkillData?.skills) ? publicSkillData.skills : [])
          .map(publicSkillFromApi);
        setPrivateSkills(uploaded);
        setPublicSkills(published);
        setDevices(Array.isArray(deviceData?.devices) ? deviceData.devices : []);
        setStatistics(dashboardStatisticsFromApi(statisticsData));
        setInstallTasks((Array.isArray(installData?.tasks) ? installData.tasks : [])
          .map(installTaskFromApi)
          .filter((task: InstallTask | null): task is InstallTask => task !== null));
      })
      .catch(() => setStatistics(null));
    return () => { cancelled = true; };
  }, [user]);

  useEffect(() => {
    if (!user) return;
    let cancelled = false;

    async function pollDevices() {
      try {
        const response = await fetch("/api/devices", { cache: "no-store" });
        if (!response.ok) return;
        const data = await response.json();
        if (!cancelled) setDevices(Array.isArray(data?.devices) ? data.devices : []);
      } catch {
        // Keep the last known device state while the network is temporarily unavailable.
      }
    }

    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") void pollDevices();
    };
    void pollDevices();
    const timer = window.setInterval(() => void pollDevices(), 3000);
    window.addEventListener("focus", pollDevices);
    document.addEventListener("visibilitychange", refreshWhenVisible);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
      window.removeEventListener("focus", pollDevices);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
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
      const searchMatch = !normalized || `${skill.name} ${skill.description} ${skill.detail} ${skill.usageSteps.join(" ")} ${skill.category}`.toLowerCase().includes(normalized);
      return categoryMatch && searchMatch;
    });
  }, [activeCategory, libraryMode, privateSkills, publicSkills, query]);

  function guardAccount(action: () => void) {
    if (user) return action();
    setAuthOpen(true);
    setToast("请先登录 SkillPort 账户");
  }

  function downloadClient() {
    if (!clientPlatform) {
      setClientDownloadOpen(true);
      return;
    }
    startClientDownload(clientPlatform);
    setToast(`${clientDownloads[clientPlatform].label}已开始下载`);
  }

  function toggleReleaseNotes() {
    const nextOpen = !releaseOpen;
    setReleaseOpen(nextOpen);
    if (!nextOpen) return;
    setReleaseSeen(true);
    try {
      window.localStorage.setItem(releaseSeenStorageKey, currentRelease.version);
    } catch {
      // 浏览器禁止本地存储时仍可正常查看更新内容。
    }
  }

  async function refreshStatistics() {
    if (!user) {
      setStatistics(null);
      return;
    }
    try {
      const response = await fetch("/api/stats", { cache: "no-store" });
      setStatistics(response.ok ? dashboardStatisticsFromApi(await response.json()) : null);
    } catch {
      setStatistics(null);
    }
  }

  async function refreshInstallTasks() {
    if (!user) {
      setInstallTasks([]);
      return;
    }
    try {
      const response = await fetch("/api/installs", { cache: "no-store" });
      const data = response.ok ? await response.json() : null;
      setInstallTasks((Array.isArray(data?.tasks) ? data.tasks : [])
        .map(installTaskFromApi)
        .filter((task: InstallTask | null): task is InstallTask => task !== null));
    } catch {
      setInstallTasks([]);
    }
  }

  const refreshLocalTools = useCallback(async (device: Device) => {
    if (device.status !== "ONLINE" || scanningDeviceId) return;
    setScanningDeviceId(device.id);
    const previousDetection = device.toolsDetectedAt || "";
    try {
      const requested = await fetch(`/api/devices/${encodeURIComponent(device.id)}/scan-tools`, {
        method: "POST",
      });
      const responseBody = await requested.json().catch(() => ({}));
      if (!requested.ok) throw new Error(String(responseBody?.error || "识别请求没有完成"));

      for (let attempt = 0; attempt < 15; attempt += 1) {
        await new Promise((resolve) => window.setTimeout(resolve, 700));
        const response = await fetch("/api/devices", { cache: "no-store" });
        if (!response.ok) continue;
        const data = await response.json();
        const nextDevices: Device[] = Array.isArray(data?.devices) ? data.devices : [];
        setDevices(nextDevices);
        const refreshed = nextDevices.find((candidate) => candidate.id === device.id);
        if (refreshed?.toolsDetectedAt && refreshed.toolsDetectedAt !== previousDetection) {
          const count = Array.isArray(refreshed.installedTools) ? refreshed.installedTools.length : 0;
          setToast(`识别完成，发现 ${count} 个本机 AI 工具`);
          return;
        }
      }
      setToast("Bridge 已收到请求，但识别结果暂未返回");
    } catch (error) {
      setToast(error instanceof Error ? error.message : "重新识别没有完成");
    } finally {
      setScanningDeviceId(null);
    }
  }, [scanningDeviceId]);

  useEffect(() => {
    if (!selectedDeviceId) return;
    const device = devices.find((candidate) => candidate.id === selectedDeviceId);
    if (!device || device.status !== "ONLINE") {
      automaticToolScanAt.current.delete(selectedDeviceId);
      return;
    }
    const lastRequestedAt = automaticToolScanAt.current.get(device.id) ?? 0;
    if (scanningDeviceId || Date.now() - lastRequestedAt < 60_000) return;
    automaticToolScanAt.current.set(device.id, Date.now());
    const timer = window.setTimeout(() => void refreshLocalTools(device), 0);
    return () => window.clearTimeout(timer);
  }, [devices, refreshLocalTools, scanningDeviceId, selectedDeviceId]);

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" }).catch(() => undefined);
    setUser(null);
    setPrivateSkills([]);
    setPublicSkills([]);
    setLibraryMode("public");
    setDevices([]);
    setSelectedDeviceId(null);
    setStatistics(null);
    setInstallTasks([]);
    setSelected(null);
    setInstaller(null);
    setUninstaller(null);
    setToast("已安全退出 SkillPort");
  }

  function onFile(file?: File) {
    if (!file) return;
    guardAccount(() => {
      setPendingUploadFile(file);
      setUploadOpen(true);
    });
  }

  async function uploadFile(file: File, metadata: SkillUploadMetadata, avatar?: File) {
    if (avatar && (!(["image/png", "image/jpeg", "image/webp", "image/gif"].includes(avatar.type))
        || avatar.size > 2 * 1024 * 1024)) {
      throw new Error("头像仅支持 PNG、JPEG、WebP、GIF，且不能超过 2MB");
    }
    const form = new FormData();
    form.append("file", file);
    form.append("name", metadata.name.trim());
    form.append("description", metadata.description.trim());
    form.append("detail", metadata.detail.trim());
    form.append("usageSteps", metadata.usageSteps.join("\n"));
    form.append("category", metadata.category);
    if (avatar) form.append("avatar", avatar);
    const response = await fetch("/api/skills", { method: "POST", body: form });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(String(data?.detail || data?.error || data?.message || "上传没有完成，请稍后再试"));
    }
    const created = data.skill ?? data;
    const uploaded = privateSkillFromApi(created, 0);
    setPrivateSkills((current) => [uploaded, ...current]);
    setLibraryMode("private");
    setUploadOpen(false);
    setPendingUploadFile(null);
    setToast(`“${uploaded.name}”已通过结构检查并保存`);
    void refreshStatistics();
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
      void refreshStatistics();
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
      void refreshStatistics();
    } catch {
      setToast("拉取没有完成，请稍后再试");
    } finally {
      setPullingId(null);
    }
  }

  async function updateSkillCategory(skill: Skill, category: SkillCategory) {
    try {
      const response = await fetch(`/api/skills/${encodeURIComponent(skill.id)}`, {
        method: "PATCH",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ category }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(String(data?.detail || data?.error || "分类修改没有完成"));
      const normalizedCategory = normalizeSkillCategory(data?.category || category);
      setPrivateSkills((current) => current.map((item) => item.id === skill.id
        ? { ...item, category: normalizedCategory }
        : item));
      setPublicSkills((current) => current.map((item) => item.sourceSkillId === skill.id
        ? { ...item, category: normalizedCategory }
        : item));
      setSelected((current) => current?.id === skill.id
        ? { ...current, category: normalizedCategory }
        : current);
      setToast(skill.shared ? "分类已保存，并同步到 Skill 公有池" : "Skill 分类已保存");
      return true;
    } catch (error) {
      setToast(error instanceof Error ? error.message : "分类修改没有完成，请稍后再试");
      return false;
    }
  }

  async function updateSkillDetails(skill: Skill, values: { name: string; description: string; detail: string; usageSteps: string[] }) {
    try {
      const response = await fetch(`/api/skills/${encodeURIComponent(skill.id)}`, {
        method: "PATCH",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(values),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(String(data?.detail || data?.error || "详情修改没有完成"));
      const next = {
        name: String(data?.name || values.name),
        description: String(data?.description || values.description),
        detail: String(data?.detail || values.detail),
        usageSteps: Array.isArray(data?.usageSteps) ? data.usageSteps.map(String) : values.usageSteps,
      };
      setPrivateSkills((current) => current.map((item) => item.id === skill.id ? { ...item, ...next } : item));
      setPublicSkills((current) => current.map((item) => item.sourceSkillId === skill.id ? { ...item, ...next } : item));
      setSelected((current) => current?.id === skill.id ? { ...current, ...next } : current);
      setToast(skill.shared ? "Skill 详情已保存，并同步到公有池" : "Skill 详情已保存");
      return true;
    } catch (error) {
      setToast(error instanceof Error ? error.message : "详情修改没有完成，请稍后再试");
      return false;
    }
  }

  async function confirmSkillAction() {
    if (!actionCandidate || !user) return;
    const { skill, action } = actionCandidate;
    setActionBusy(true);
    try {
      const path = action === "delete"
        ? `/api/skills/${encodeURIComponent(skill.id)}`
        : skill.scope === "public"
          ? `/api/public-skills/${encodeURIComponent(skill.id)}`
          : `/api/public-skills/source/${encodeURIComponent(skill.id)}`;
      const response = await fetch(path, { method: "DELETE" });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(String(data?.error || "操作没有完成"));
      if (action === "delete") {
        setPrivateSkills((current) => current.filter((item) => item.id !== skill.id));
        setPublicSkills((current) => current.filter((item) => item.sourceSkillId !== skill.id));
        setToast("Skill 及其私人文件已删除");
      } else {
        setPublicSkills((current) => current.filter((item) => item.id !== skill.id && item.sourceSkillId !== skill.id));
        setPrivateSkills((current) => current.map((item) => item.id === (skill.sourceSkillId || skill.id)
          ? { ...item, shared: false }
          : item));
        setToast("Skill 已从公有池下架，私人原件仍保留");
      }
      setSelected(null);
      setActionCandidate(null);
      void refreshStatistics();
    } catch (error) {
      setToast(error instanceof Error ? error.message : "操作没有完成，请稍后再试");
    } finally {
      setActionBusy(false);
    }
  }

  const displayName = user?.name?.includes("@") ? user.name.split("@")[0] : user?.name || "访客";
  const selectedDevice = selectedDeviceId
    ? devices.find((device) => device.id === selectedDeviceId) ?? null
    : null;
  const onlineDevice = selectedDevice?.status === "ONLINE" ? selectedDevice : null;
  const detectedTools = new Set(onlineDevice?.installedTools || []);
  const hasToolDetection = Boolean(onlineDevice?.toolsDetectedAt);
  const scanningTools = onlineDevice?.id === scanningDeviceId;

  return (
    <div className="app-shell" data-theme={theme}>
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
            <div className="bridge-top"><span className={onlineDevice ? "status-dot" : "status-dot offline"}/><span>SkillPort Bridge</span><b>{onlineDevice ? "在线" : selectedDevice ? "离线" : "未选择"}</b></div>
            <p>{selectedDevice ? selectedDevice.name : "请先选择当前浏览器对应的电脑"}</p>
            <div className="mini-tools"><span>CX</span><span>Q</span><span>AI</span></div>
          </div>
          <button className="settings-row feedback-entry" onClick={() => setFeedbackOpen(true)}><span>✉</span> 公开意见墙 <span>›</span></button>
          <button className="settings-row" onClick={() => setThemeOpen((open) => !open)}><span>◐</span> 配色主题 <span>›</span></button>
        </div>
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div className="search-wrap">
            <span className="search-icon">⌕</span>
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索技能、分类或用途..." aria-label="搜索技能" />
            <kbd>⌘ K</kbd>
          </div>
          <div className="theme-switcher">
            <button className="theme-trigger" aria-label="切换界面配色" aria-expanded={themeOpen} onClick={() => setThemeOpen((open) => !open)}>
              <span className={`theme-orb ${theme}`}/><span>主题</span><b>{themeOptions.find((option) => option.id === theme)?.name}</b>
            </button>
            {themeOpen && (
              <section className="theme-panel" aria-label="选择界面配色">
                <div className="theme-panel-head"><div><span>APPEARANCE</span><h2>配色主题</h2></div><button onClick={() => setThemeOpen(false)} aria-label="关闭主题选择">×</button></div>
                <div className="theme-options">
                  {themeOptions.map((option) => (
                    <button key={option.id} className={theme === option.id ? "theme-option active" : "theme-option"} onClick={() => { setTheme(option.id); setThemeOpen(false); }}>
                      <span className="theme-swatches">{option.colors.map((color) => <i key={color} style={{ backgroundColor: color }}/>)}</span>
                      <span><b>{option.name}</b><small>{option.description}</small></span>
                      <em>{theme === option.id ? "✓" : ""}</em>
                    </button>
                  ))}
                </div>
                <p>主题只保存在当前设备，不影响账户数据。</p>
              </section>
            )}
          </div>
          <div className="release-update">
            <button className="version-update-button" aria-label={`查看 ${currentRelease.version} 版本更新`} aria-expanded={releaseOpen} onClick={toggleReleaseNotes}>
              <span className="version-update-icon">✦</span><span>版本更新</span><b>v{currentRelease.version}</b>
              {!releaseSeen && <i className="notification-dot" aria-label="有新版本内容"/>}
            </button>
            {releaseOpen && (
              <section className="release-panel" aria-label="版本更新内容">
                <div className="release-panel-head"><div><span>LATEST UPDATE</span><h2>版本更新</h2></div><button onClick={() => setReleaseOpen(false)} aria-label="关闭版本更新">×</button></div>
                {releaseNotes.slice(0, releaseHistoryLimit).map((release, index) => (
                  <article className="release-note" key={release.version}>
                    <div className="release-version"><b>v{release.version}</b>{index === 0 && <em>当前版本</em>}<time dateTime={release.date}>{release.date}</time></div>
                    <h3>{release.title}</h3>
                    <ul>{release.changes.map((change) => <li key={change}>{change}</li>)}</ul>
                  </article>
                ))}
                <p className="release-tip">以后每次发布都会在这里记录本次新增、优化和修复内容。</p>
              </section>
            )}
          </div>
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
            <div className="welcome-actions">
              <button className="client-download-button" onClick={downloadClient} title="下载 SkillPort 桌面客户端"><span>↓</span> {clientPlatform ? `下载 ${clientDownloads[clientPlatform].label}` : "下载客户端"}</button>
              <button className="primary-button" onClick={() => guardAccount(() => { setPendingUploadFile(null); setUploadOpen(true); })}><span>＋</span> 上传 Skill</button>
            </div>
          </section>

          <section className="stats-strip" aria-label="技能统计">
            <div><span className="stat-icon purple">▦</span><p><b>{statistics?.mySkills ?? "—"}</b><small>我的 Skills</small></p><em>{statistics ? `${statistics.sharedSkills} 个已分享` : "登录后查看"}</em></div>
            <div><span className="stat-icon orange">◎</span><p><b>{statistics?.totalInstalls ?? "—"}</b><small>累计加载任务</small></p><em>{statistics ? "MySQL 实时统计" : "登录后查看"}</em></div>
            <div><span className="stat-icon green">⌁</span><p><b>{statistics?.connectedDevices ?? "—"}</b><small>已连接设备</small></p><em className="neutral">{statistics ? `${statistics.onlineDevices} 台在线` : "Bridge 实时状态"}</em></div>
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
                    <SkillAvatar skill={skill}/>
                    {skill.scope === "private" && skill.shared ? <span className="shared-badge">已公开</span> : <button className="more-button" aria-label={`${skill.name} 更多操作`} onClick={(event) => { event.stopPropagation(); setSelected(skill); }}>•••</button>}
                  </div>
                  <span className="category-pill">{skill.category}</span>
                  <h3>{skill.name}</h3>
                  <p className="skill-description">{skill.description}</p>
                  <div className="skill-detail-preview">
                    <span>详情</span>
                    <p>{skill.detail || skill.description}</p>
                  </div>
                  <div className="skill-step-summary"><span>☷</span>{skill.usageSteps.length ? `${skill.usageSteps.length} 个使用步骤` : "查看完整使用说明"}</div>
                  {skill.note && <p className="note-preview"><span>✎</span>{skill.note}</p>}
                  <div className="card-footer">
                    <span className="author-avatar">{skill.author.slice(0, 1)}</span><span className="author-name">{skill.author}</span>
                    <span className="usage">↓ {skill.uses}</span>
                  </div>
                  {skill.scope === "public" ? (
                    <button className={skill.pulled ? "install-card-button pulled" : "install-card-button"} disabled={skill.pulled || pullingId === skill.id} onClick={(event) => { event.stopPropagation(); void pullSkill(skill); }}>
                      {pullingId === skill.id ? "正在拉取…" : skill.pulled ? "已在我的空间" : "拉取到我的空间"} <span>{skill.pulled ? "✓" : "→"}</span>
                    </button>
                  ) : (
                    <div className="local-card-actions">
                      <button className="delete-card-button" title="删除云端 Skill" aria-label={`删除 ${skill.name}`} onClick={(event) => { event.stopPropagation(); setActionCandidate({ skill, action: "delete" }); }}><span aria-hidden="true">⌫</span><span>删除</span></button>
                      <button className="uninstall-card-button" onClick={(event) => { event.stopPropagation(); setUninstaller(skill); }}>卸载</button>
                      <button className="install-card-button" onClick={(event) => { event.stopPropagation(); setInstaller(skill); }}>加载到本机 <span>→</span></button>
                    </div>
                  )}
                </article>
              ))}
            </section>
          ) : (
            <div className="empty-state"><span>{libraryMode === "public" ? "◎" : "⌕"}</span><h3>{libraryMode === "public" ? "公有池还没有这个分类的 Skill" : "私人空间里还没有 Skill"}</h3><p>{libraryMode === "public" ? "你可以成为第一个分享者，个人备注不会公开。" : "拖动上传，或先从公有池拉取一份。"}</p><button onClick={() => { setQuery(""); setActiveCategory("全部技能"); if (libraryMode === "public") guardAccount(() => setLibraryMode("private")); else setLibraryMode("public"); }}>{libraryMode === "public" ? "去分享 Skill" : "浏览公有池"}</button></div>
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
          <div className="rail-title"><div><h2>本机工具</h2><p>{onlineDevice ? hasToolDetection ? `${onlineDevice.name} · 已识别 ${detectedTools.size} 个工具` : `${onlineDevice.name} · 等待首次识别` : selectedDevice ? `${selectedDevice.name} · 当前离线，不采用历史结果` : "本浏览器尚未绑定电脑"}</p></div><span className={onlineDevice ? "live-pill" : "live-pill offline"}><i/>{onlineDevice ? "在线" : selectedDevice ? "离线" : "未选择"}</span></div>
          <label className="device-picker">
            <span>这个浏览器对应</span>
            <select value={selectedDeviceId || ""} onChange={(event) => bindBrowserDevice(event.target.value || null)}>
              <option value="">请选择当前这台电脑</option>
              {devices.map((device) => <option key={device.id} value={device.id}>{device.status === "ONLINE" ? "●" : "○"} {device.name} · {device.os}</option>)}
            </select>
            <small>{selectedDevice ? "工具识别、安装和卸载只会发送到这台设备。" : "不会自动借用账号中其他电脑的识别结果。"}</small>
          </label>
          <div className="tool-list">
            {Object.entries(toolMeta).map(([id, tool]) => (
              <div className="tool-row" key={id}><span className={`tool-logo ${tool.color}`}>{tool.mark}</span><p><b>{tool.name}</b><small>{selectedDevice ? selectedDevice.os : "macOS · Windows"}</small></p><span className={hasToolDetection ? detectedTools.has(id) ? "connected" : "connected missing" : "connected pending"}>{hasToolDetection ? detectedTools.has(id) ? "已安装" : "未检测到" : onlineDevice ? "待识别" : selectedDevice ? "设备离线" : "未选择设备"}</span></div>
            ))}
          </div>
          <div className="tool-actions">
            <button className="refresh-tools" disabled={!onlineDevice || scanningTools} onClick={() => onlineDevice && void refreshLocalTools(onlineDevice)}>{scanningTools ? "正在识别…" : "重新识别"} <span>↻</span></button>
            <button className="manage-tools" onClick={() => guardAccount(() => setPairOpen(true))}>{onlineDevice ? "管理连接" : selectedDevice ? "重新连接" : "配对新设备"} <span>→</span></button>
          </div>
        </section>

        <section className="rail-section activity-section">
          <div className="rail-title"><div><h2>最近动态</h2><p>你的 Skill 使用记录</p></div><button>查看全部</button></div>
          <div className="timeline">
            {installTasks.slice(0, 3).map((task) => {
              const skillName = privateSkills.find((skill) => skill.id === task.skillId)?.name || "已删除的 Skill";
              const targetNames = task.targets.map((target) => toolMeta[target as keyof typeof toolMeta]?.name || target).join("、");
              const status = task.status === "COMPLETED" ? "已完成" : task.status === "FAILED" ? "失败" : "处理中";
              const uninstall = task.operation === "UNINSTALL";
              return <div key={task.id}><span className={`timeline-dot ${uninstall ? "violet" : "coral"}`}>{uninstall ? "−" : "↗"}</span><p><b>{uninstall ? "卸载了" : "加载了"} {skillName}</b><small>{targetNames || "未记录目标"} · {status} · {activityTime(task.createdAt)}</small></p></div>;
            })}
            {!installTasks.length && <div><span className="timeline-dot violet">○</span><p><b>{user ? "暂无加载记录" : "登录后查看使用记录"}</b><small>{user ? "完成首次 Bridge 加载后会显示在这里" : "数据按账号隔离"}</small></p></div>}
          </div>
        </section>
      </aside>

      {selected && <DetailModal skill={selected} pulling={pullingId === selected.id} onClose={() => setSelected(null)} onInstall={() => { setInstaller(selected); setSelected(null); }} onUninstall={() => { setUninstaller(selected); setSelected(null); }} onPull={() => void pullSkill(selected)} onShare={() => setShareCandidate(selected)} onDelete={() => setActionCandidate({ skill: selected, action: "delete" })} onUnpublish={() => setActionCandidate({ skill: selected, action: "unpublish" })} onSaveCategory={(category) => updateSkillCategory(selected, category)} onSaveDetails={(values) => updateSkillDetails(selected, values)} onSaveNote={(note) => {
        setPrivateSkills((current) => current.map((skill) => skill.id === selected.id ? { ...skill, note } : skill));
        setSelected((current) => current ? { ...current, note } : current);
        if (selected.uploaded && user) fetch("/api/skills", { method: "PATCH", headers: { "content-type": "application/json" }, body: JSON.stringify({ id: selected.id, note }) }).catch(() => undefined);
        setToast("备注已保存，仅你自己可见");
      }}/>}
      {shareCandidate && <ShareConfirmModal skill={shareCandidate} busy={shareBusy} onClose={() => !shareBusy && setShareCandidate(null)} onConfirm={() => void shareSkill()}/>}
      {actionCandidate && <SkillActionConfirmModal skill={actionCandidate.skill} action={actionCandidate.action} busy={actionBusy} onClose={() => !actionBusy && setActionCandidate(null)} onConfirm={() => void confirmSkillAction()}/>}
      {installer && (
        <InstallModal skill={installer} signedIn={Boolean(user)} onRequireSignIn={() => { setInstaller(null); setAuthOpen(true); }} onConnectBridge={() => { setInstaller(null); setPairOpen(true); }} onlineDevice={onlineDevice ?? null} onClose={() => setInstaller(null)} onDone={(message) => { setInstaller(null); setToast(message); void refreshStatistics(); void refreshInstallTasks(); }}/>
      )}
      {uninstaller && (
        <UninstallModal skill={uninstaller} onlineDevice={onlineDevice ?? null} onConnectBridge={() => { setUninstaller(null); setPairOpen(true); }} onClose={() => setUninstaller(null)} onDone={(message) => { setUninstaller(null); setToast(message); void refreshInstallTasks(); }}/>
      )}
      {uploadOpen && <UploadModal initialFile={pendingUploadFile} onClose={() => { setUploadOpen(false); setPendingUploadFile(null); }} onUpload={uploadFile}/>}
      {pairOpen && <PairDeviceModal connectedDevice={onlineDevice ?? null} knownDevices={devices} onDevicePaired={(deviceId) => { bindBrowserDevice(deviceId); setPairOpen(false); setToast("新设备已绑定到这个浏览器，正在刷新本机工具"); }} onClose={() => setPairOpen(false)}/>}
      {clientDownloadOpen && <ClientDownloadModal
        onClose={() => setClientDownloadOpen(false)}
        onDownload={(platform) => { startClientDownload(platform); setClientDownloadOpen(false); setToast(`${clientDownloads[platform].label}已开始下载`); }}
      />}
      {feedbackOpen && <FeedbackMailboxModal
        signedIn={Boolean(user)}
        onClose={() => setFeedbackOpen(false)}
        onRequireSignIn={() => { setFeedbackOpen(false); setAuthOpen(true); }}
        onSubmitted={() => setToast("意见已公开送达，感谢你的反馈")}
      />}
      {authOpen && <AuthModal
        onClose={() => setAuthOpen(false)}
        onAuthenticated={(authenticatedUser) => { setUser(authenticatedUser); setAuthOpen(false); setToast("欢迎进入你的 SkillPort 私人空间"); }}
      />}
      {toast && <div className="toast"><span>✓</span>{toast}</div>}
    </div>
  );
}

const feedbackKinds = ["功能建议", "问题反馈", "体验优化", "其他"] as const;

type PublicFeedback = { id: string; submitter: string; kind: string; content: string; createdAt: string };
type FeedbackPage = { items: PublicFeedback[]; page: number; size: number; totalElements: number; totalPages: number; hasPrevious: boolean; hasNext: boolean };

function FeedbackMailboxModal({ signedIn, onClose, onRequireSignIn, onSubmitted }: {
  signedIn: boolean;
  onClose: () => void;
  onRequireSignIn: () => void;
  onSubmitted: () => void;
}) {
  const [view, setView] = useState<"wall" | "compose">("wall");
  const [kind, setKind] = useState<(typeof feedbackKinds)[number]>("功能建议");
  const [content, setContent] = useState("");
  const [stage, setStage] = useState<"writing" | "sending" | "sent">("writing");
  const [error, setError] = useState("");
  const [feedbackPage, setFeedbackPage] = useState<FeedbackPage | null>(null);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    if (view !== "wall") return;
    const controller = new AbortController();
    const resetTimer = window.setTimeout(() => {
      if (!controller.signal.aborted) {
        setLoading(true);
        setListError("");
      }
    }, 0);
    fetch(`/api/feedback?page=${page}&size=6`, { signal: controller.signal })
      .then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(String(data?.detail || data?.error || "暂时无法读取公开意见"));
        return data as FeedbackPage;
      })
      .then((data) => setFeedbackPage(data))
      .catch((loadError) => {
        if ((loadError as Error).name !== "AbortError") setListError(loadError instanceof Error ? loadError.message : "暂时无法读取公开意见");
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => { window.clearTimeout(resetTimer); controller.abort(); };
  }, [page, refreshKey, view]);

  async function submitFeedback() {
    const normalized = content.trim();
    if (normalized.length < 5) {
      setError("请至少填写 5 个字，让我们更准确地理解你的想法。");
      return;
    }
    setError("");
    setStage("sending");
    const minimumAnimation = new Promise((resolve) => window.setTimeout(resolve, 1900));
    try {
      const request = fetch("/api/feedback", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ kind, content: normalized }),
      });
      const [response] = await Promise.all([request, minimumAnimation]);
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(String(data?.detail || data?.error || "意见暂时没有送达"));
      setStage("sent");
    } catch (submissionError) {
      setStage("writing");
      setError(submissionError instanceof Error ? submissionError.message : "意见暂时没有送达，请稍后重试。");
    }
  }

  function showWallAfterSubmit() {
    onSubmitted();
    setContent("");
    setStage("writing");
    setPage(1);
    setRefreshKey((value) => value + 1);
    setView("wall");
  }

  function openComposer() {
    if (!signedIn) {
      onRequireSignIn();
      return;
    }
    setView("compose");
  }

  return (
    <div className="modal-backdrop feedback-backdrop" onMouseDown={stage === "sending" ? undefined : onClose}>
      <div className="modal feedback-modal" role="dialog" aria-modal="true" aria-label="意见信箱" onMouseDown={(event) => event.stopPropagation()}>
        {stage !== "sending" && <button className="close-button" onClick={onClose} aria-label="关闭意见信箱">×</button>}
        <span className="step-label">FEEDBACK MAILBOX</span>
        <h2>公开意见墙</h2>
        <p className="feedback-lead">每个人都能浏览真实建议；登录后可以像发传真一样提交新意见。</p>

        {stage === "writing" && <div className="feedback-tabs" role="tablist">
          <button className={view === "wall" ? "active" : ""} onClick={() => setView("wall")}>大家的意见</button>
          <button className={view === "compose" ? "active" : ""} onClick={openComposer}>提交意见</button>
        </div>}

        {stage === "writing" && view === "wall" ? (
          <div className="feedback-wall">
            {loading && <div className="feedback-list-state">正在翻阅大家的意见…</div>}
            {!loading && listError && <div className="feedback-error" role="alert">{listError}<button onClick={() => setRefreshKey((value) => value + 1)}>重试</button></div>}
            {!loading && !listError && feedbackPage?.items.length === 0 && <div className="feedback-empty"><span>✉</span><b>还没有公开意见</b><p>成为第一个留下建议的人吧。</p><button onClick={openComposer}>提交第一条意见</button></div>}
            {!loading && !listError && Boolean(feedbackPage?.items.length) && <div className="feedback-public-list">
              {feedbackPage!.items.map((item) => <article key={item.id}>
                <div><span>{item.kind}</span><time dateTime={item.createdAt}>{formatFeedbackTime(item.createdAt)}</time></div>
                <p>{item.content}</p>
                <footer><i>{item.submitter.trim().slice(0, 1) || "S"}</i><b>{item.submitter || "SkillPort 用户"}</b></footer>
              </article>)}
            </div>}
            {!loading && !listError && feedbackPage && feedbackPage.totalPages > 0 && <div className="feedback-pagination">
              <button disabled={!feedbackPage.hasPrevious} onClick={() => setPage((value) => Math.max(1, value - 1))}>← 上一页</button>
              <span>第 <b>{feedbackPage.page}</b> / {feedbackPage.totalPages} 页 · 共 {feedbackPage.totalElements} 条</span>
              <button disabled={!feedbackPage.hasNext} onClick={() => setPage((value) => value + 1)}>下一页 →</button>
            </div>}
          </div>
        ) : stage === "writing" ? (
          <div className="feedback-form">
            <div className="feedback-public-notice"><span>公开</span>提交后，意见内容、你的昵称和提交时间会展示给所有人。</div>
            <label><span>意见类型</span><select value={kind} onChange={(event) => setKind(event.target.value as (typeof feedbackKinds)[number])}>{feedbackKinds.map((item) => <option value={item} key={item}>{item}</option>)}</select></label>
            <label><span>写下你的想法</span><textarea autoFocus rows={6} maxLength={2000} value={content} onChange={(event) => { setContent(event.target.value); setError(""); }} placeholder="例如：希望可以批量选择多个 Skill，一次安装到 Codex…"/><small>{content.length}/2000</small></label>
            {error && <div className="feedback-error" role="alert">{error}</div>}
            <button className="full-primary feedback-submit" disabled={content.trim().length < 5} onClick={() => void submitFeedback()}><span>发送意见</span><b>→</b></button>
          </div>
        ) : (
          <div className={`fax-stage ${stage}`} aria-live="polite">
            <div className="fax-paper"><span>SKILLPORT</span><i/><i/><i/><b>你的意见</b></div>
            <div className="fax-machine">
              <div className="fax-top"><span className="fax-status-light"/><b>{stage === "sending" ? "TRANSMITTING" : "DELIVERED"}</b></div>
              <div className="fax-slot"><i/></div>
              <div className="fax-controls"><span/><span/><span/></div>
            </div>
            <div className="fax-signal"><i/><i/><i/></div>
            <h3>{stage === "sending" ? "正在传真你的意见…" : "意见已送达"}</h3>
            <p>{stage === "sending" ? "纸张扫描中，请稍候。" : "感谢你认真写下这封信，我们会仔细阅读。"}</p>
            {stage === "sent" && <button className="full-primary feedback-finish" onClick={showWallAfterSubmit}>查看公开意见</button>}
          </div>
        )}
      </div>
    </div>
  );
}

function formatFeedbackTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false,
  }).format(date);
}

function ClientDownloadModal({ onClose, onDownload }: { onClose: () => void; onDownload: (platform: ClientPlatform) => void }) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal client-download-modal" role="dialog" aria-modal="true" aria-label="选择客户端版本" onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" onClick={onClose}>×</button>
        <span className="step-label">DESKTOP CLIENT</span>
        <h2>选择你的电脑系统</h2>
        <p className="install-lead">当前浏览器无法自动识别系统，请选择正确版本。客户端安装一次，以后可以直接接收网页下发的 Skill。</p>
        <div className="client-platform-list">
          <button onClick={() => onDownload("macos")}><span className="platform-mark">⌘</span><p><b>下载 macOS 客户端</b><small>macOS 12+ · 安装到“应用程序”，自动创建桌面图标</small></p><em>↓</em></button>
          <button onClick={() => onDownload("windows")}><span className="platform-mark windows">⊞</span><p><b>下载 Windows 客户端</b><small>安装到当前用户应用目录，自动创建桌面图标</small></p><em>↓</em></button>
        </div>
      </div>
    </div>
  );
}

function DetailModal({ skill, pulling, onClose, onInstall, onUninstall, onPull, onShare, onDelete, onUnpublish, onSaveCategory, onSaveDetails, onSaveNote }: {
  skill: Skill;
  pulling: boolean;
  onClose: () => void;
  onInstall: () => void;
  onUninstall: () => void;
  onPull: () => void;
  onShare: () => void;
  onDelete: () => void;
  onUnpublish: () => void;
  onSaveCategory: (category: SkillCategory) => Promise<boolean>;
  onSaveDetails: (values: { name: string; description: string; detail: string; usageSteps: string[] }) => Promise<boolean>;
  onSaveNote: (note: string) => void;
}) {
  const [note, setNote] = useState(skill.note || "");
  const [category, setCategory] = useState<SkillCategory>(skill.category);
  const [savedCategory, setSavedCategory] = useState<SkillCategory>(skill.category);
  const [categorySaving, setCategorySaving] = useState(false);
  const [categoryStatus, setCategoryStatus] = useState<"idle" | "saved" | "error">("idle");
  const [editingDetails, setEditingDetails] = useState(false);
  const [detailsSaving, setDetailsSaving] = useState(false);
  const [detailError, setDetailError] = useState("");
  const [draftName, setDraftName] = useState(skill.name);
  const [draftDescription, setDraftDescription] = useState(skill.description);
  const [draftDetail, setDraftDetail] = useState(skill.detail || skill.description);
  const [draftSteps, setDraftSteps] = useState<string[]>(skill.usageSteps.length ? skill.usageSteps : [""]);
  const isPublic = skill.scope === "public";

  async function saveCategory(nextCategory: SkillCategory) {
    const previousCategory = savedCategory;
    setCategory(nextCategory);
    setCategoryStatus("idle");
    setCategorySaving(true);
    const saved = await onSaveCategory(nextCategory);
    setCategorySaving(false);
    if (saved) {
      setSavedCategory(nextCategory);
      setCategoryStatus("saved");
      return;
    }
    setCategory(previousCategory);
    setCategoryStatus("error");
  }

  function resetDetailDraft() {
    setDraftName(skill.name);
    setDraftDescription(skill.description);
    setDraftDetail(skill.detail || skill.description);
    setDraftSteps(skill.usageSteps.length ? skill.usageSteps : [""]);
    setDetailError("");
    setEditingDetails(false);
  }

  function updateDraftStep(index: number, value: string) {
    setDraftSteps((current) => current.map((step, stepIndex) => stepIndex === index ? value : step));
    setDetailError("");
  }

  async function saveDetails() {
    const usageSteps = draftSteps.map((step) => step.trim()).filter(Boolean);
    if (!draftName.trim() || !draftDescription.trim() || !draftDetail.trim() || !usageSteps.length) {
      setDetailError("请完整填写名称、描述、详细说明，并至少添加一个使用步骤。");
      return;
    }
    setDetailsSaving(true);
    const saved = await onSaveDetails({
      name: draftName.trim(),
      description: draftDescription.trim(),
      detail: draftDetail.trim(),
      usageSteps,
    });
    setDetailsSaving(false);
    if (saved) setEditingDetails(false);
  }
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal detail-modal" role="dialog" aria-modal="true" aria-label={`${skill.name} 详情`} onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" onClick={onClose}>×</button>
        <div className="detail-hero"><SkillAvatar skill={skill} large/><div><span className="category-pill">{isPublic ? skill.category : category}</span><h2>{skill.name}</h2><p>by {skill.author} · {skill.uses}</p></div></div>
        <p className="detail-description">{skill.description}</p>
        {editingDetails ? (
          <section className="skill-detail-editor" aria-label="编辑 Skill 详情">
            <div className="detail-section-heading"><div><span>EDIT DETAILS</span><h3>编辑 Skill 详情</h3></div><small>{skill.shared ? "保存后同步公有池" : "私人空间信息"}</small></div>
            <label><span>Skill 名称</span><input value={draftName} maxLength={160} disabled={detailsSaving} onChange={(event) => { setDraftName(event.target.value); setDetailError(""); }}/></label>
            <label><span>简短描述</span><textarea value={draftDescription} maxLength={2000} rows={2} disabled={detailsSaving} onChange={(event) => { setDraftDescription(event.target.value); setDetailError(""); }}/><small>{draftDescription.length}/2000</small></label>
            <label><span>详细说明</span><textarea value={draftDetail} maxLength={10000} rows={5} disabled={detailsSaving} onChange={(event) => { setDraftDetail(event.target.value); setDetailError(""); }}/><small>{draftDetail.length}/10000</small></label>
            <div className="usage-step-editor">
              <div className="usage-step-editor-head"><span>使用步骤</span><button type="button" disabled={detailsSaving || draftSteps.length >= 20} onClick={() => setDraftSteps((current) => [...current, ""])}>＋ 添加步骤</button></div>
              {draftSteps.map((step, index) => (
                <div className="usage-step-input" key={index}><b>{index + 1}</b><input value={step} maxLength={500} disabled={detailsSaving} placeholder={`第 ${index + 1} 步`} onChange={(event) => updateDraftStep(index, event.target.value)}/><button type="button" aria-label={`删除第 ${index + 1} 步`} disabled={detailsSaving || draftSteps.length === 1} onClick={() => setDraftSteps((current) => current.filter((_, stepIndex) => stepIndex !== index))}>×</button></div>
              ))}
            </div>
            {detailError && <p className="detail-editor-error" role="alert">{detailError}</p>}
            <div className="detail-editor-actions"><button className="secondary-button" disabled={detailsSaving} onClick={resetDetailDraft}>取消</button><button className="primary-button" disabled={detailsSaving} onClick={() => void saveDetails()}>{detailsSaving ? "保存中…" : "保存详情"}</button></div>
          </section>
        ) : (
          <section className="skill-detail-content">
            <div className="detail-section-heading"><div><span>SKILL DETAILS</span><h3>详细说明</h3></div>{!isPublic && <button onClick={() => setEditingDetails(true)}>编辑详情</button>}</div>
            <p>{skill.detail || skill.description}</p>
            <div className="usage-steps-view"><h3>使用步骤 <span>{skill.usageSteps.length}</span></h3>{skill.usageSteps.length ? <ol>{skill.usageSteps.map((step, index) => <li key={`${index}-${step}`}><b>{index + 1}</b><span>{step}</span></li>)}</ol> : <p className="empty-usage-steps">发布者尚未补充具体步骤。</p>}</div>
          </section>
        )}
        <div className="compatibility"><b>兼容工具</b><div>{skill.compatible.map((id) => <span key={id}>{toolMeta[id as keyof typeof toolMeta]?.name}</span>)}</div></div>
        {isPublic ? (
          <>
            <div className="public-privacy-note"><span>✓</span><p><b>拉取后数据隔离</b><small>会复制一份到你的私人空间；发布者看不到你的文件修改和个人备注。</small></p></div>
            <div className="modal-actions">{skill.ownedByCurrentUser && <button className="danger-button" onClick={onUnpublish}>从公有池下架</button>}<button className="secondary-button" onClick={onClose}>关闭</button><button className="primary-button" disabled={skill.pulled || pulling} onClick={onPull}>{pulling ? "正在拉取…" : skill.pulled ? "已在我的空间" : "拉取到我的空间"} <span>{skill.pulled ? "✓" : "→"}</span></button></div>
          </>
        ) : (
          <>
            <label className="category-edit-field"><span><b>Skill 分类</b><small>{skill.shared ? "选择后自动保存并同步公有池" : "选择后立即保存"}</small></span><div><select value={category} disabled={categorySaving} onChange={(event) => void saveCategory(event.target.value as SkillCategory)}>{skillCategories.map((item) => <option key={item} value={item}>{item}</option>)}</select><span className={`category-save-status ${categoryStatus}`} aria-live="polite">{categorySaving ? "保存中…" : categoryStatus === "saved" ? "✓ 已保存" : categoryStatus === "error" ? "保存失败" : "自动保存"}</span></div></label>
            <label className="note-field"><span><b>我的备注</b><small>仅你自己可见</small></span><textarea value={note} onChange={(event) => setNote(event.target.value)} placeholder="记录使用方法、适用项目或注意事项…" maxLength={1000}/><em>{note.length}/1000</em></label>
            <div className="share-inline"><span>◎</span><p><b>{skill.shared ? "已分享到公有池" : "分享给社区"}</b><small>{skill.shared ? "名称、详细说明和使用步骤会保持同步，个人备注仍然私有。" : "公开名称、描述、详细说明、使用步骤、分类、头像和文件，不公开个人备注。"}</small></p><button className={skill.shared ? "unpublish-inline" : ""} onClick={skill.shared ? onUnpublish : onShare}>{skill.shared ? "下架" : "分享"}</button></div>
            <div className="modal-actions"><button className="danger-button" onClick={onDelete}>删除云端 Skill</button><button className="secondary-button uninstall-detail-button" onClick={onUninstall}>从本机卸载</button><button className="secondary-button" onClick={() => onSaveNote(note)}>保存备注</button><button className="primary-button" onClick={onInstall}>加载到本机 <span>→</span></button></div>
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
        <div className="share-fields"><b>将公开</b><span>名称、描述与详情</span><span>使用步骤与分类</span><span>兼容工具与文件</span></div>
        <div className="share-private"><b>保持私有</b><span>你的个人备注</span><span>账户邮箱与其他 Skill</span></div>
        <div className="modal-actions"><button className="secondary-button" disabled={busy} onClick={onClose}>取消</button><button className="primary-button" disabled={busy} onClick={onConfirm}>{busy ? "正在分享…" : "确认分享到公有池"} <span>→</span></button></div>
      </div>
    </div>
  );
}

function SkillActionConfirmModal({ skill, action, busy, onClose, onConfirm }: {
  skill: Skill;
  action: "delete" | "unpublish";
  busy: boolean;
  onClose: () => void;
  onConfirm: () => void;
}) {
  const deleting = action === "delete";
  return (
    <div className="modal-backdrop share-backdrop" onMouseDown={onClose}>
      <div className="modal share-modal danger-modal" role="alertdialog" aria-modal="true" aria-label={deleting ? "确认删除 Skill" : "确认下架 Skill"} onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" disabled={busy} onClick={onClose}>×</button>
        <span className="step-label">{deleting ? "DELETE PRIVATE SKILL" : "UNPUBLISH FROM POOL"}</span>
        <h2>{deleting ? `确认删除 ${skill.name}？` : `确认下架 ${skill.name}？`}</h2>
        <p className="install-lead">{deleting
          ? `将永久删除你的私人 Skill 文件、头像和备注${skill.shared ? "，并同时从公有池下架" : ""}。其他用户此前拉取的独立副本不会受影响。`
          : "只会删除公有池中的发布记录，你的私人原件、头像和备注都会保留。"}</p>
        <div className="modal-actions"><button className="secondary-button" disabled={busy} onClick={onClose}>取消</button><button className="danger-confirm" disabled={busy} onClick={onConfirm}>{busy ? "正在处理…" : deleting ? "确认永久删除" : "确认下架"}</button></div>
      </div>
    </div>
  );
}

function InstallModal({ skill, signedIn, onRequireSignIn, onConnectBridge, onlineDevice, onClose, onDone }: { skill: Skill; signedIn: boolean; onRequireSignIn: () => void; onConnectBridge: () => void; onlineDevice: Device | null; onClose: () => void; onDone: (message: string) => void }) {
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
    if (!skill.uploaded) return;
    const response = await fetch(`/api/skills/${encodeURIComponent(skill.id)}/file`);
    if (!response.ok) return;
    const payload = new Uint8Array(await response.arrayBuffer());
    const extension = response.headers.get("x-skill-extension") || "zip";

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
      const archive = createMacInstallerArchive(script, scriptFileName);
      const archiveBuffer = archive.buffer.slice(
        archive.byteOffset,
        archive.byteOffset + archive.byteLength,
      ) as ArrayBuffer;
      blob = new Blob([archiveBuffer], { type: "application/zip" });
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
            const targetPath = displayToolPath(id as keyof typeof installerTargetRoots, os === "windows");
            return <button key={id} className={checked ? "target-row checked" : "target-row"} onClick={() => toggleTarget(id)}><span className={`tool-logo ${tool.color}`}>{tool.mark}</span><p><b>{tool.name}</b><small>{targetPath}</small></p><span className="checkbox">{checked ? "✓" : ""}</span></button>;
          })}
        </div>
        <div className={onlineDevice ? "bridge-notice" : "bridge-notice offline"}><span className={onlineDevice ? "status-dot" : "status-dot offline"}/><p><b>{onlineDevice ? `Bridge 已连接：${onlineDevice.name}` : "尚未连接 SkillPort Bridge"}</b><small>{onlineDevice ? "点击后由 Netty 实时推送并回传安装进度" : "Bridge 每台电脑只需安装一次，之后加载任何 Skill 都不再下载单独安装器。"}</small></p></div>
        {skill.fileName?.toLowerCase().endsWith(".md") && <div className="pair-error">当前是单文件 SKILL.md；如 Skill 还包含 scripts、references 或 assets，请重新上传 ZIP。</div>}
        {signedIn ? onlineDevice && skill.uploaded ? (
          <button className="full-primary" disabled={!targets.length} onClick={install}>发送到 Bridge <span>→</span></button>
        ) : (
          <>
            <button className="full-primary" onClick={onConnectBridge}>连接 Bridge（只需一次） <span>→</span></button>
            <details className="offline-installer">
              <summary>临时使用离线安装器</summary>
              <p>仅当当前电脑不能运行 Bridge 时使用；每个 Skill 都需要单独下载。</p>
              <button className="secondary-button" disabled={!targets.length || !skill.uploaded} onClick={install}>下载 {os === "macos" ? "macOS" : "Windows"} 离线安装器</button>
            </details>
          </>
        ) : <button className="full-primary" onClick={onRequireSignIn}>登录后继续 <span>→</span></button>}
      </div>
    </div>
  );
}

function UninstallModal({ skill, onlineDevice, onConnectBridge, onClose, onDone }: {
  skill: Skill;
  onlineDevice: Device | null;
  onConnectBridge: () => void;
  onClose: () => void;
  onDone: (message: string) => void;
}) {
  const [targets, setTargets] = useState<string[]>(() =>
    skill.compatible.includes("codex") ? ["codex"] : skill.compatible.slice(0, 1));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  function toggleTarget(target: string) {
    setTargets((current) => current.includes(target)
      ? current.filter((item) => item !== target)
      : [...current, target]);
  }

  async function uninstall() {
    if (!onlineDevice || !targets.length || busy) return;
    setBusy(true);
    setError("");
    try {
      const response = await fetch("/api/uninstalls", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ skillId: skill.id, deviceId: onlineDevice.id, targets }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(String(data?.detail || data?.error || data?.message || "卸载任务发送失败"));
      }
      onDone(`卸载任务已发送到 ${onlineDevice.name}`);
    } catch (uninstallError) {
      setError(uninstallError instanceof Error ? uninstallError.message : "卸载任务发送失败");
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal install-modal uninstall-modal" role="dialog" aria-modal="true" aria-label="从本机卸载 Skill" onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" disabled={busy} onClick={onClose}>×</button>
        <span className="step-label">REMOVE FROM DEVICE</span><h2>从本机卸载 {skill.name}？</h2>
        <p className="install-lead">只会从你选择的 AI 工具目录移除本机副本，不会删除云端 Skill、公有池内容或个人备注。</p>
        <div className="target-list">
          {skill.compatible.map((id) => {
            const tool = toolMeta[id as keyof typeof toolMeta];
            const checked = targets.includes(id);
            const windows = onlineDevice?.os.toLowerCase().includes("win") ?? false;
            const path = displayToolPath(
              id as keyof typeof installerTargetRoots,
              windows,
              slugifySkillName(skill.name),
            );
            return <button key={id} className={checked ? "target-row checked uninstall-target" : "target-row uninstall-target"} disabled={busy} onClick={() => toggleTarget(id)}><span className={`tool-logo ${tool.color}`}>{tool.mark}</span><p><b>{tool.name}</b><small>{`从 ${path} 移除`}</small></p><span className="checkbox">{checked ? "✓" : ""}</span></button>;
          })}
        </div>
        <div className="uninstall-safety destructive"><span>!</span><p><b>本机副本会被永久删除</b><small>不保留备份；云端 Skill 不会删除，需要时可以重新加载到本机。</small></p></div>
        <div className={onlineDevice ? "bridge-notice" : "bridge-notice offline"}><span className={onlineDevice ? "status-dot" : "status-dot offline"}/><p><b>{onlineDevice ? `将由 ${onlineDevice.name} 执行` : "Bridge 当前未连接"}</b><small>{onlineDevice ? "卸载进度会显示在最近动态中" : "本机卸载必须由 Bridge 执行，不能使用浏览器直接删除文件。"}</small></p></div>
        {error && <div className="upload-error" role="alert"><b>无法卸载</b><span>{error}</span></div>}
        {onlineDevice
          ? <button className="full-primary uninstall-confirm" disabled={!targets.length || busy} onClick={() => void uninstall()}>{busy ? "正在发送卸载任务…" : `确认从 ${targets.length} 个工具卸载`} <span>→</span></button>
          : <button className="full-primary" onClick={onConnectBridge}>连接 Bridge 后卸载 <span>→</span></button>}
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
        <div className="auth-brand"><span className="brand-mark">S</span><div><b>欢迎来到 SkillPort</b><small>企业微信或邮箱账户均可登录</small></div></div>
        <form className="wecom-auth-form" method="get" action="/api/auth/wecom"><input type="hidden" name="mode" value="qr"/><button className="wecom-auth-button" type="submit"><span>企</span><p><b>企业微信登录 / 注册</b><small>首次授权自动注册，企业微信内打开自动登录</small></p><em>→</em></button></form>
        <div className="auth-divider"><span>或使用邮箱</span></div>
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

function uploadNameFromFile(file: File | null) {
  if (!file) return "";
  return file.name.replace(/\.(zip|skill|md)$/i, "").trim();
}

function readUploadDraft() {
  if (typeof window === "undefined") return null;
  try {
    return JSON.parse(window.localStorage.getItem(uploadDraftStorageKey) || "null") as Partial<{
      name: string; description: string; detail: string; usageSteps: string[]; category: SkillCategory;
    }> | null;
  } catch {
    return null;
  }
}

function UploadModal({ initialFile, onClose, onUpload }: {
  initialFile: File | null;
  onClose: () => void;
  onUpload: (file: File, metadata: SkillUploadMetadata, avatar?: File) => Promise<void>;
}) {
  const [savedDraft] = useState(readUploadDraft);
  const [file, setFile] = useState<File | null>(initialFile);
  const [name, setName] = useState(() => typeof savedDraft?.name === "string" && savedDraft.name.trim()
    ? savedDraft.name
    : uploadNameFromFile(initialFile));
  const [description, setDescription] = useState(() => typeof savedDraft?.description === "string" ? savedDraft.description : "");
  const [detail, setDetail] = useState(() => typeof savedDraft?.detail === "string" ? savedDraft.detail : "");
  const [usageStepsText, setUsageStepsText] = useState(() => Array.isArray(savedDraft?.usageSteps) ? savedDraft.usageSteps.join("\n") : "");
  const [category, setCategory] = useState<SkillCategory>(() => savedDraft?.category && skillCategories.includes(savedDraft.category)
    ? savedDraft.category
    : "编程技能");
  const [avatar, setAvatar] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    try {
      window.localStorage.setItem(uploadDraftStorageKey, JSON.stringify({ name, description, detail, usageSteps: usageStepsText.split("\n"), category }));
    } catch {
      // 浏览器禁用本地存储时仍可正常上传。
    }
  }, [name, description, detail, usageStepsText, category]);

  useEffect(() => () => {
    if (avatarPreview) URL.revokeObjectURL(avatarPreview);
  }, [avatarPreview]);

  function chooseAvatar(next?: File) {
    setError("");
    if (!next) return;
    if (!["image/png", "image/jpeg", "image/webp", "image/gif"].includes(next.type) || next.size > 2 * 1024 * 1024) {
      setError("头像仅支持 PNG、JPEG、WebP、GIF，且不能超过 2MB。");
      return;
    }
    if (avatarPreview) URL.revokeObjectURL(avatarPreview);
    setAvatar(next);
    setAvatarPreview(URL.createObjectURL(next));
  }

  async function submit() {
    if (!file || busy) return;
    const usageSteps = usageStepsText.split("\n").map((step) => step.trim()).filter(Boolean);
    if (!name.trim() || !description.trim() || !detail.trim() || !usageSteps.length) {
      setError("请填写 Skill 名称、描述、详细说明，并至少添加一个使用步骤。");
      return;
    }
    if (usageSteps.length > 20) {
      setError("使用步骤最多 20 步，请合并后再上传。");
      return;
    }
    if (usageSteps.some((step) => step.length > 500)) {
      setError("每个使用步骤最多 500 字，请精简后再上传。");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await onUpload(file, { name: name.trim(), description: description.trim(), detail: detail.trim(), usageSteps, category }, avatar || undefined);
      try { window.localStorage.removeItem(uploadDraftStorageKey); } catch { /* 上传本身已经成功。 */ }
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : "上传没有完成，请稍后再试");
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop">
      <div className="modal upload-modal" role="dialog" aria-modal="true" aria-label="上传 Skill">
        <button className="close-button" disabled={busy} onClick={onClose}>×</button><span className="step-label">PRIVATE UPLOAD</span><h2>上传你的 Skill</h2><p className="install-lead">名称、描述、详细说明、使用步骤和分类都由你设置；服务器仍会检查目录和 SKILL.md，不符合标准的文件不会保存。</p>
        <label className={file ? "large-upload selected" : "large-upload"}><input type="file" disabled={busy} accept=".zip,.skill,.md" onChange={(event) => { const next = event.target.files?.[0] || null; setFile(next); if (!name.trim()) setName(uploadNameFromFile(next)); setError(""); }}/><span>{file ? "✓" : "↑"}</span><b>{file ? file.name : "选择 Skill 文件"}</b><small>{file ? `${(file.size / 1024 / 1024).toFixed(2)} MB · 上传后保留原文件` : ".zip、.skill 或 SKILL.md，最大 25MB"}</small></label>
        <div className="upload-fields">
          <label><span>Skill 名称</span><input value={name} disabled={busy} maxLength={160} placeholder="例如：发布前检查助手" onChange={(event) => { setName(event.target.value); setError(""); }}/><small>显示在个人空间；分享到公有池时同步使用此名称。</small></label>
          <label><span>Skill 描述</span><textarea value={description} disabled={busy} maxLength={2000} rows={3} placeholder="说明这个 Skill 能解决什么问题" onChange={(event) => { setDescription(event.target.value); setError(""); }}/><small>{description.length}/2000 · 分享时会同步到公有池。</small></label>
          <label><span>详细说明</span><textarea value={detail} disabled={busy} maxLength={10000} rows={5} placeholder="详细说明适用场景、工作原理、输入输出和注意事项" onChange={(event) => { setDetail(event.target.value); setError(""); }}/><small>{detail.length}/10000 · 卡片显示摘要，点击后查看完整内容。</small></label>
          <label><span>使用步骤</span><textarea value={usageStepsText} disabled={busy} rows={5} placeholder={"每行填写一步，例如：\n打开需要处理的项目\n选择目标文件并运行 Skill\n检查生成结果"} onChange={(event) => { setUsageStepsText(event.target.value); setError(""); }}/><small>{usageStepsText.split("\n").map((step) => step.trim()).filter(Boolean).length}/20 步 · 每行一步，分享时同步到公有池。</small></label>
          <label><span>分类</span><select value={category} disabled={busy} onChange={(event) => setCategory(event.target.value as SkillCategory)}>{skillCategories.map((item) => <option key={item} value={item}>{item}</option>)}</select><small>个人空间与公有池使用同一分类。</small></label>
        </div>
        <div className="skill-standard">
          <div><b>正确 Skill 结构</b><a href="/examples/skillport-standard-sample.zip" download>下载标准样例 ↓</a></div>
          <code>skill-name/<br/>├── SKILL.md&nbsp;&nbsp;必需<br/>├── scripts/&nbsp;&nbsp;可选<br/>├── references/&nbsp;可选<br/>└── assets/&nbsp;&nbsp;&nbsp;可选</code>
          <small>SKILL.md 必须包含 YAML frontmatter：合法的 name、description，以及实际使用说明。</small>
        </div>
        <label className="avatar-upload"><input type="file" accept="image/png,image/jpeg,image/webp,image/gif" onChange={(event) => chooseAvatar(event.target.files?.[0])}/><span className="avatar-preview">{avatarPreview ? <img src={avatarPreview} alt="头像预览"/> : "＋"}</span><p><b>{avatar ? "更换 Skill 头像" : "添加 Skill 头像（可选）"}</b><small>{avatar ? avatar.name : "PNG、JPEG、WebP 或 GIF，最大 2MB"}</small></p><em>选择图片</em></label>
        {error && <div className="upload-error" role="alert"><b>无法上传</b><span>{error}</span><a href="/examples/skillport-standard-sample.zip" download>下载正确样例重新打包</a></div>}
        <button className="full-primary" disabled={!file || !name.trim() || !description.trim() || !detail.trim() || !usageStepsText.trim() || busy} onClick={() => void submit()}>{busy ? "正在检查结构…" : "检查并保存到我的 Skill"} <span>→</span></button>
      </div>
    </div>
  );
}

function PairDeviceModal({ connectedDevice, knownDevices, onDevicePaired, onClose }: {
  connectedDevice: Device | null;
  knownDevices: Device[];
  onDevicePaired: (deviceId: string) => void;
  onClose: () => void;
}) {
  const [pairing, setPairing] = useState<{ code: string; expiresAt: string; apiBaseUrl: string; nettyUrl: string } | null>(null);
  const [error, setError] = useState("");
  const [os, setOs] = useState<"macos" | "windows">(() =>
    typeof navigator !== "undefined" && navigator.userAgent.toLowerCase().includes("windows")
      ? "windows"
      : "macos");
  const [copied, setCopied] = useState(false);
  const initialDeviceIds = useRef(new Set(knownDevices.map((device) => device.id)));

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
    if (connectedDevice) return;
    const timer = window.setTimeout(() => void createPairing(), 0);
    return () => window.clearTimeout(timer);
  }, [connectedDevice]);

  useEffect(() => {
    if (!pairing || connectedDevice) return;
    let cancelled = false;
    async function detectPairedDevice() {
      try {
        const response = await fetch("/api/devices", { cache: "no-store" });
        if (!response.ok) return;
        const data = await response.json();
        const currentDevices: Device[] = Array.isArray(data?.devices) ? data.devices : [];
        const pairedDevice = currentDevices.find((device) => !initialDeviceIds.current.has(device.id));
        if (!cancelled && pairedDevice) onDevicePaired(pairedDevice.id);
      } catch {
        // The normal device polling retries while this modal remains open.
      }
    }
    const timer = window.setInterval(() => void detectPairedDevice(), 1500);
    void detectPairedDevice();
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [connectedDevice, onDevicePaired, pairing]);

  const apiBaseUrl = pairing ? normalizeHttpUrl(pairing.apiBaseUrl) : "";
  const nettyUrl = pairing ? normalizeHttpUrl(pairing.nettyUrl) : "";
  const pairApiBaseUrl = apiBaseUrl === "https://skillport-ai-workspace.mcbbss.chatgpt.site"
    ? "https://www.jmuyuer.com"
    : apiBaseUrl;
  const scriptUrl = pairing
    ? `${pairApiBaseUrl}/bridge/install-${os === "macos" ? "macos.sh" : "windows.ps1"}`
    : "";
  const command = pairing
    ? os === "macos"
      ? `tmp="$(mktemp -t skillport-installer.XXXXXX)"; curl -A 'Mozilla/5.0 SkillPort-Installer' -fL --retry 3 --connect-timeout 15 '${scriptUrl}' -o "$tmp" && bash "$tmp" '${pairApiBaseUrl}' '${nettyUrl}' '${pairing.code}'; rc=$?; rm -f "$tmp"; (exit $rc)`
      : `$api=${powershellUrlExpression(pairApiBaseUrl)}; $netty=${powershellUrlExpression(nettyUrl)}; $installer=$api+'/bridge/install-windows.ps1'; $temp=Join-Path ([IO.Path]::GetTempPath()) ('skillport-installer-'+[Guid]::NewGuid().ToString('N')+'.ps1'); try { $curl=Get-Command curl.exe -ErrorAction SilentlyContinue; if ($curl) { & curl.exe -fL --retry 3 --connect-timeout 15 -A 'Mozilla/5.0 SkillPort-Installer' $installer -o $temp; if ($LASTEXITCODE -ne 0) { throw '安装脚本下载失败，请检查网络后重试。' } } else { $client=New-Object Net.WebClient; $client.Headers['User-Agent']='Mozilla/5.0 SkillPort-Installer'; $client.DownloadFile($installer,$temp) }; if (-not (Test-Path $temp) -or (Get-Item $temp).Length -eq 0) { throw '安装脚本下载结果为空，请检查网络后重试。' }; $script=[IO.File]::ReadAllText($temp,[Text.Encoding]::UTF8); & ([scriptblock]::Create($script)) -ApiBaseUrl $api -NettyUrl $netty -PairingCode '${pairing.code}' } finally { Remove-Item $temp -Force -ErrorAction SilentlyContinue }`
    : "";
  const manualCommand = pairing
    ? `java -jar skillport-bridge.jar pair ${pairApiBaseUrl} ${nettyUrl} ${pairing.code} "My Computer"`
    : "";
  const bridgeUpdateBaseUrl = "https://www.jmuyuer.com";
  const bridgeUpdateCommand = os === "macos"
    ? `tmp="$(mktemp -t skillport-update.XXXXXX)"; curl -A 'Mozilla/5.0 SkillPort-Updater' -fL --retry 3 '${bridgeUpdateBaseUrl}/bridge/update-macos.sh' -o "$tmp" && bash "$tmp" '${bridgeUpdateBaseUrl}'; rc=$?; rm -f "$tmp"; (exit $rc)`
    : `$api='${bridgeUpdateBaseUrl}'; $script=$api+'/bridge/update-windows.ps1'; $temp=Join-Path ([IO.Path]::GetTempPath()) ('skillport-update-'+[Guid]::NewGuid().ToString('N')+'.ps1'); try { $client=New-Object Net.WebClient; $client.Headers['User-Agent']='Mozilla/5.0 SkillPort-Updater'; $client.DownloadFile($script,$temp); & ([scriptblock]::Create([IO.File]::ReadAllText($temp,[Text.Encoding]::UTF8))) -ApiBaseUrl $api } finally { Remove-Item $temp -Force -ErrorAction SilentlyContinue }`;

  async function copyInstallerCommand() {
    try {
      await copyText(command);
      setCopied(true);
    } catch {
      setError("浏览器没有复制权限，请手动选中下面的命令复制。");
    }
  }

  async function copyBridgeUpdateCommand() {
    try {
      await copyText(bridgeUpdateCommand);
      setCopied(true);
    } catch {
      setError("浏览器没有复制权限，请手动选中下面的更新命令复制。");
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
        {connectedDevice ? <>
          <div className="pair-connected"><span>✓</span><p><b>{connectedDevice.name} 已连接</b><small>加载功能可以直接使用；首次使用“从本机卸载”时，请更新一次 Bridge。</small></p></div>
          <div className="os-switch pair-os-switch bridge-update-os" role="tablist" aria-label="选择电脑系统">
            <button className={os === "macos" ? "active" : ""} onClick={() => switchOs("macos")}><span>●</span> macOS</button>
            <button className={os === "windows" ? "active" : ""} onClick={() => switchOs("windows")}><span>⊞</span> Windows</button>
          </div>
          <button className={copied ? "full-primary pair-copy copied" : "full-primary pair-copy"} onClick={copyBridgeUpdateCommand}>{copied ? "✓ 更新命令已复制" : "复制一键更新 Bridge 命令"} <span>→</span></button>
          <div className="pair-command"><code>{bridgeUpdateCommand}</code><button onClick={copyBridgeUpdateCommand}>{copied ? "已复制" : "复制"}</button></div>
          <p className="bridge-update-note">更新不会重新配对，也不会修改已经安装的 Skill；Bridge 会自动重启并重新连接。</p>
          <button className="secondary-button bridge-update-close" onClick={onClose}>关闭</button>
        </> : error ? <div className="pair-error">{error}</div> : pairing ? <>
          <div className="os-switch pair-os-switch" role="tablist" aria-label="选择电脑系统">
            <button className={os === "macos" ? "active" : ""} onClick={() => switchOs("macos")}><span>●</span> macOS</button>
            <button className={os === "windows" ? "active" : ""} onClick={() => switchOs("windows")}><span>⊞</span> Windows</button>
          </div>
          <div className="pair-steps">
            <div><span>1</span><p><b>{os === "macos" ? "打开「终端」" : "打开 PowerShell"}</b><small>{os === "macos" ? "按 ⌘ + 空格，搜索“终端”并打开" : "右键开始菜单，选择 Windows PowerShell（无需管理员）"}</small></p></div>
            <div><span>2</span><p><b>复制命令并粘贴运行</b><small>{os === "macos" ? "在终端按 ⌘ + V，然后按回车" : "直接粘贴到 PS 提示符后运行，不要再套 powershell.exe -Command"}</small></p></div>
            <div><span>3</span><p><b>看到“安装完成”即可</b><small>回到本页面，设备会自动显示为在线</small></p></div>
          </div>
          <button className={copied ? "full-primary pair-copy copied" : "full-primary pair-copy"} onClick={copyInstallerCommand}>{copied ? `✓ 已复制，现在粘贴到${os === "macos" ? "终端" : " PowerShell"}` : `复制 ${os === "macos" ? "macOS" : "Windows PowerShell"} 一键安装命令`} <span>→</span></button>
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
