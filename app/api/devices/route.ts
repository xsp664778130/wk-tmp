import { backendRequest } from "../_backend";

export async function GET(request: Request) {
  return backendRequest(request, "/api/v1/devices");
}

export async function POST(request: Request) {
  return backendRequest(request, "/api/v1/devices/pairing-codes", { method: "POST" });
}
