import { backendRequest } from "../../../_backend";

export async function GET(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  return backendRequest(request, `/api/v1/skills/${encodeURIComponent(id)}/environment`);
}

export async function PATCH(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  const payload = (await request.json().catch(() => null)) as { values?: Record<string, string> } | null;
  if (!payload?.values || !Object.keys(payload.values).length) {
    return Response.json({ error: "请至少修改一个 env.properties 值。" }, { status: 400 });
  }
  return backendRequest(request, `/api/v1/skills/${encodeURIComponent(id)}/environment`, {
    method: "PATCH",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ values: payload.values }),
  });
}
