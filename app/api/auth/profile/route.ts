import { proxyAuthJson } from "../_backend";

export async function GET(request: Request) {
  return proxyAuthJson(request, "/api/v1/auth/profile", true);
}

export async function PATCH(request: Request) {
  return proxyAuthJson(request, "/api/v1/auth/profile", true);
}
