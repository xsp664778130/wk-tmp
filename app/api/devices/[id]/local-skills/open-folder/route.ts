import { backendRequest } from "../../../../_backend";

export async function POST(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  const payload = (await request.json()) as { tool?: string; slug?: string };
  return backendRequest(request, `/api/v1/devices/${encodeURIComponent(id)}/local-skills/open-folder`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ tool: payload.tool, slug: payload.slug }),
  });
}
