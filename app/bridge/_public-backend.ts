export async function publicBackendRequest(path: string, init?: RequestInit) {
  const baseUrl = process.env.SKILLPORT_BACKEND_URL?.replace(/\/$/, "");
  if (!baseUrl) {
    return Response.json({ error: "SkillPort Java 服务尚未配置。" }, { status: 503 });
  }

  try {
    const upstream = await fetch(`${baseUrl}${path}`, init);
    const headers = new Headers(upstream.headers);
    headers.delete("connection");
    headers.delete("content-encoding");
    headers.delete("transfer-encoding");
    return new Response(upstream.body, { status: upstream.status, headers });
  } catch {
    return Response.json({ error: "暂时无法连接 SkillPort Java 服务。" }, { status: 502 });
  }
}
