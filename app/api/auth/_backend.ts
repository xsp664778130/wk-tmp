import { SESSION_COOKIE_NAME, sessionTokenFromRequest } from "../../session-auth";

export function authBackendConfiguration() {
  const baseUrl = process.env.SKILLPORT_BACKEND_URL?.replace(/\/$/, "");
  const gatewayKey = process.env.SKILLPORT_GATEWAY_KEY;
  return baseUrl && gatewayKey ? { baseUrl, gatewayKey } : null;
}

export async function proxyAuthJson(request: Request, upstreamPath: string, authenticated: boolean) {
  const configuration = authBackendConfiguration();
  if (!configuration) return Response.json({ error: "暂时无法连接 SkillPort 账户服务。" }, { status: 503 });
  const token = authenticated ? sessionTokenFromRequest(request) : null;
  if (authenticated && !token) return Response.json({ error: "请先登录。" }, { status: 401 });
  let body: string | undefined;
  if (request.method !== "GET" && request.method !== "HEAD") body = await request.text();
  try {
    const upstream = await fetch(`${configuration.baseUrl}${upstreamPath}`, {
      method: request.method,
      headers: {
        accept: "application/json",
        ...(body ? { "content-type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        "X-SkillPort-Gateway-Key": configuration.gatewayKey,
      },
      body,
    });
    if (upstream.status === 204) return new Response(null, { status: 204 });
    const data = await upstream.json().catch(() => ({})) as Record<string, unknown>;
    if (!upstream.ok) {
      const message = typeof data.detail === "string" ? data.detail
        : typeof data.error === "string" ? data.error : "账户操作没有完成，请稍后重试。";
      return Response.json({ error: message }, { status: upstream.status });
    }
    return Response.json(data, { status: upstream.status, headers: { "cache-control": "no-store" } });
  } catch {
    return Response.json({ error: "暂时无法连接 SkillPort 账户服务。" }, { status: 503 });
  }
}

export function expiredSessionCookie(request: Request) {
  const secure = new URL(request.url).protocol === "https:" ? "; Secure" : "";
  return `${SESSION_COOKIE_NAME}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0${secure}`;
}
