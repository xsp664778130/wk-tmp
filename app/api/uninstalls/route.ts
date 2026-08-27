import { backendRequest } from "../_backend";

export async function POST(request: Request) {
  const payload = (await request.json()) as { skillId?: string; deviceId?: string; targets?: string[] };
  return backendRequest(request, "/api/v1/installations/uninstall", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ skillId: payload.skillId, deviceId: payload.deviceId, targets: payload.targets }),
  });
}
