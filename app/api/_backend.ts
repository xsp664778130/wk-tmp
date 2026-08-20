import { getChatGPTUser } from "../chatgpt-auth";

export async function backendRequest(request: Request, path: string, init?: RequestInit) {
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "请先登录。" }, { status: 401 });

  const baseUrl = process.env.SKILLPORT_BACKEND_URL?.replace(/\/$/, "");
  const gatewayKey = process.env.SKILLPORT_GATEWAY_KEY;
  if (!baseUrl || !gatewayKey) {
    return Response.json({ error: "SkillPort Java 服务尚未配置。" }, { status: 503 });
  }

  const headers = new Headers(init?.headers);
  headers.set("X-SkillPort-Gateway-Key", gatewayKey);
  headers.set("X-SkillPort-User-Id", user.userId);
  headers.set("X-SkillPort-User-Email", user.email);

  try {
    const upstream = await fetch(`${baseUrl}${path}`, { ...init, headers });
    const responseHeaders = new Headers(upstream.headers);
    responseHeaders.delete("connection");
    responseHeaders.delete("content-encoding");
    responseHeaders.delete("transfer-encoding");
    return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
  } catch {
    return Response.json({ error: "暂时无法连接 SkillPort Java 服务。" }, { status: 502 });
  }
}

export async function proxyMultipart(request: Request, path: string) {
  const contentType = request.headers.get("content-type") ?? "application/octet-stream";
  return backendRequest(request, path, {
    method: "POST",
    headers: { "content-type": contentType },
    body: await request.arrayBuffer(),
  });
}
