import { backendRequest } from "../../../../_backend";

function target(request: Request) {
  const url = new URL(request.url);
  return {
    tool: url.searchParams.get("tool")?.trim() ?? "",
    slug: url.searchParams.get("slug")?.trim() ?? "",
  };
}

export async function GET(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  const selection = target(request);
  if (!selection.tool || !selection.slug) {
    return Response.json({ error: "缺少本机 Skill 信息。" }, { status: 400 });
  }
  return backendRequest(request, `/api/v1/devices/${encodeURIComponent(id)}/local-skills/environment/read`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(selection),
  });
}

export async function PATCH(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  const selection = target(request);
  const payload = (await request.json().catch(() => null)) as { values?: Record<string, string> } | null;
  if (!selection.tool || !selection.slug || !payload?.values || !Object.keys(payload.values).length) {
    return Response.json({ error: "请至少修改一个 env.properties 值。" }, { status: 400 });
  }
  return backendRequest(request, `/api/v1/devices/${encodeURIComponent(id)}/local-skills/environment`, {
    method: "PATCH",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ ...selection, values: payload.values }),
  });
}
