import { backendRequest } from "../../../_backend";

export async function GET(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  return backendRequest(request, `/api/v1/devices/${encodeURIComponent(id)}/local-skills`);
}

export async function POST(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  const payload = (await request.json()) as { tool?: string; slug?: string };
  return backendRequest(request, `/api/v1/devices/${encodeURIComponent(id)}/local-skills/uninstall`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ tool: payload.tool, slug: payload.slug }),
  });
}
