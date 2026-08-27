import { backendRequest } from "../../../_backend";

export async function GET(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  return backendRequest(request, `/api/v1/skills/${encodeURIComponent(id)}/avatar`);
}
