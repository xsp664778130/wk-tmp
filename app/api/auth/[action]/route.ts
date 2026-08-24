import { SESSION_COOKIE_NAME, sessionTokenFromRequest } from "../../../session-auth";

type AuthAction = "login" | "register" | "logout" | "me";

export async function GET(request: Request, context: { params: Promise<{ action: string }> }) {
  const { action } = await context.params;
  if (action === "wecom") return startWeComLogin(request);
  if (action !== "me") return Response.json({ error: "不支持的操作。" }, { status: 405 });
  return authenticatedRequest(request, "me", "GET");
}

function startWeComLogin(request: Request) {
  const requestedMode = new URL(request.url).searchParams.get("mode");
  const target = new URL("/api/auth/wecom", "https://www.jmuyuer.com");
  target.searchParams.set("mode", requestedMode === "auto" ? "auto" : "qr");
  return Response.redirect(target, 307);
}

export async function POST(request: Request, context: { params: Promise<{ action: string }> }) {
  const { action } = await context.params;
  if (!isAuthAction(action) || action === "me") {
    return Response.json({ error: "不支持的操作。" }, { status: 405 });
  }
  if (action === "logout") return logout(request);
  return createSession(request, action);
}

async function createSession(request: Request, action: "login" | "register") {
  const configuration = backendConfiguration();
  if (!configuration) return unavailable();

  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return Response.json({ error: "请求格式不正确。" }, { status: 400 });
  }

  try {
    const upstream = await fetch(`${configuration.baseUrl}/api/v1/auth/${action}`, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "X-SkillPort-Gateway-Key": configuration.gatewayKey,
      },
      body: JSON.stringify(payload),
    });
    const body = await readJson(upstream);
    if (!upstream.ok) return authError(upstream.status, body);

    const token = typeof body.token === "string" ? body.token : "";
    const user = body.user;
    if (!token || !user) return Response.json({ error: "登录服务返回异常。" }, { status: 502 });

    const headers = new Headers({ "content-type": "application/json", "cache-control": "no-store" });
    headers.append("set-cookie", sessionCookie(token, body.expiresAt, request));
    return new Response(JSON.stringify({ user }), { status: action === "register" ? 201 : 200, headers });
  } catch {
    return unavailable();
  }
}

async function logout(request: Request) {
  const token = sessionTokenFromRequest(request);
  const configuration = backendConfiguration();
  if (token && configuration) {
    try {
      await fetch(`${configuration.baseUrl}/api/v1/auth/logout`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "X-SkillPort-Gateway-Key": configuration.gatewayKey,
        },
      });
    } catch {
      // The browser session is still cleared when the backend is temporarily unavailable.
    }
  }
  return new Response(null, { status: 204, headers: { "set-cookie": expiredSessionCookie(request) } });
}

async function authenticatedRequest(request: Request, action: AuthAction, method: string) {
  const token = sessionTokenFromRequest(request);
  const configuration = backendConfiguration();
  if (!token) return Response.json({ error: "请先登录。" }, { status: 401 });
  if (!configuration) return unavailable();
  try {
    const upstream = await fetch(`${configuration.baseUrl}/api/v1/auth/${action}`, {
      method,
      headers: {
        Authorization: `Bearer ${token}`,
        "X-SkillPort-Gateway-Key": configuration.gatewayKey,
      },
    });
    const body = await readJson(upstream);
    if (!upstream.ok) return authError(upstream.status, body);
    return Response.json(body, { headers: { "cache-control": "no-store" } });
  } catch {
    return unavailable();
  }
}

function backendConfiguration() {
  const baseUrl = process.env.SKILLPORT_BACKEND_URL?.replace(/\/$/, "");
  const gatewayKey = process.env.SKILLPORT_GATEWAY_KEY;
  return baseUrl && gatewayKey ? { baseUrl, gatewayKey } : null;
}

async function readJson(response: Response): Promise<Record<string, unknown>> {
  try {
    return (await response.json()) as Record<string, unknown>;
  } catch {
    return {};
  }
}

function authError(status: number, body: Record<string, unknown>) {
  const fallback = status === 409
    ? "该邮箱已经注册。"
    : status === 401
      ? "邮箱或密码错误。"
      : "请检查填写内容。";
  const message = typeof body.detail === "string" ? body.detail : fallback;
  return Response.json({ error: message }, { status });
}

function sessionCookie(token: string, expiresAt: unknown, request: Request) {
  const expires = typeof expiresAt === "string" ? new Date(expiresAt) : new Date(Date.now() + 30 * 86400_000);
  const maxAge = Math.max(0, Math.floor((expires.getTime() - Date.now()) / 1000));
  return `${SESSION_COOKIE_NAME}=${encodeURIComponent(token)}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${maxAge}${secureSuffix(request)}`;
}

function expiredSessionCookie(request: Request) {
  return `${SESSION_COOKIE_NAME}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0${secureSuffix(request)}`;
}

function secureSuffix(request: Request) {
  return new URL(request.url).protocol === "https:" ? "; Secure" : "";
}

function unavailable() {
  return Response.json({ error: "暂时无法连接 SkillPort 登录服务。" }, { status: 503 });
}

function isAuthAction(value: string): value is AuthAction {
  return value === "login" || value === "register" || value === "logout" || value === "me";
}
