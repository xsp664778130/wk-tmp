import { backendRequest } from "../../_backend";

export async function DELETE(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  return backendRequest(request, `/api/v1/skills/${encodeURIComponent(id)}`, { method: "DELETE" });
}

export async function PATCH(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  const payload = (await request.json()) as { category?: string };
  if (!payload.category) return Response.json({ error: "请选择 Skill 分类。" }, { status: 400 });
  return backendRequest(request, `/api/v1/skills/${encodeURIComponent(id)}/category`, {
    method: "PATCH",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ category: payload.category }),
  });
}
