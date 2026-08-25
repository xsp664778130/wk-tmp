import { backendRequest } from "../_backend";

export async function POST(request: Request) {
  return backendRequest(request, "/api/v1/feedback", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: await request.text(),
  });
}
