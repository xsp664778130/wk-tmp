import { backendRequest } from "../../../_backend";

export async function DELETE(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  return backendRequest(request, `/api/v1/public-skills/source/${encodeURIComponent(id)}`, { method: "DELETE" });
}
