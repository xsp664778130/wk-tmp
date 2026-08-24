import { backendRequest } from "../_backend";

export async function GET(request: Request) {
  return backendRequest(request, "/api/v1/stats");
}
