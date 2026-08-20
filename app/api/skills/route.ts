import { backendRequest, proxyMultipart } from "../_backend";

export async function GET(request: Request) {
  return backendRequest(request, "/api/v1/skills");
}

export async function POST(request: Request) {
  return proxyMultipart(request, "/api/v1/skills");
}

export async function PATCH(request: Request) {
  const payload = (await request.json()) as { id?: string; note?: string };
  if (!payload.id) return Response.json({ error: "缺少 Skill ID。" }, { status: 400 });
  return backendRequest(request, `/api/v1/skills/${encodeURIComponent(payload.id)}/note`, {
    method: "PATCH",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ note: payload.note ?? "" }),
  });
}
