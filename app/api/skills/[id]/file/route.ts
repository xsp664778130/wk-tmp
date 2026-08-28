import { backendRequest } from "../../../_backend";

export async function GET(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  return backendRequest(request, `/api/v1/skills/${encodeURIComponent(id)}/content`);
}

export async function PUT(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  return backendRequest(request, `/api/v1/skills/${encodeURIComponent(id)}/content`, {
    method: "PUT",
    headers: { "content-type": request.headers.get("content-type") ?? "application/octet-stream" },
    body: await request.arrayBuffer(),
  });
}
