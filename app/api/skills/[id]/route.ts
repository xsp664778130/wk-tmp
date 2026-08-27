import { backendRequest } from "../../_backend";

export async function DELETE(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  return backendRequest(request, `/api/v1/skills/${encodeURIComponent(id)}`, { method: "DELETE" });
}

export async function PATCH(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  const payload = (await request.json()) as {
    category?: string;
    name?: string;
    description?: string;
    detail?: string;
    usageSteps?: string[];
  };
  if (payload.detail !== undefined || payload.usageSteps !== undefined) {
    if (!payload.name?.trim() || !payload.description?.trim() || !payload.detail?.trim()) {
      return Response.json({ error: "请完整填写 Skill 名称、描述和详细说明。" }, { status: 400 });
    }
    return backendRequest(request, `/api/v1/skills/${encodeURIComponent(id)}/details`, {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        name: payload.name.trim(),
        description: payload.description.trim(),
        detail: payload.detail.trim(),
        usageSteps: Array.isArray(payload.usageSteps) ? payload.usageSteps : [],
      }),
    });
  }
  if (!payload.category) return Response.json({ error: "请选择 Skill 分类。" }, { status: 400 });
  return backendRequest(request, `/api/v1/skills/${encodeURIComponent(id)}/category`, {
    method: "PATCH",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ category: payload.category }),
  });
}
