import { backendRequest } from "../_backend";

export async function GET(request: Request) {
  return backendRequest(request, "/api/v1/public-skills");
}

export async function POST(request: Request) {
  let payload: { skillId?: string };
  try {
    payload = (await request.json()) as { skillId?: string };
  } catch {
    return Response.json({ error: "请求格式不正确。" }, { status: 400 });
  }
  if (!payload.skillId) return Response.json({ error: "缺少 Skill ID。" }, { status: 400 });
  return backendRequest(request, "/api/v1/public-skills", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ skillId: payload.skillId }),
  });
}
