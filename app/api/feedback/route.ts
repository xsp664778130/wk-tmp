import { backendRequest, publicBackendRequest } from "../_backend";

export async function GET(request: Request) {
  const query = new URL(request.url).searchParams.toString();
  return publicBackendRequest(`/api/v1/feedback${query ? `?${query}` : ""}`, {
    method: "GET",
    headers: { accept: "application/json" },
  });
}

export async function POST(request: Request) {
  return backendRequest(request, "/api/v1/feedback", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: await request.text(),
  });
}
