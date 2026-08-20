import { backendRequest } from "../_backend";

export async function GET(request: Request) {
  return backendRequest(request, "/api/v1/installations");
}

export async function POST(request: Request) {
  const payload = (await request.json()) as { skillId?: string; deviceId?: string; targets?: string[] };
  return backendRequest(request, "/api/v1/installations", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ skillId: payload.skillId, deviceId: payload.deviceId, targets: payload.targets }),
  });
}
