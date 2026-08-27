import { backendRequest } from "../../../_backend";

export async function POST(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  return backendRequest(request, `/api/v1/devices/${encodeURIComponent(id)}/scan-tools`, {
    method: "POST",
  });
}
