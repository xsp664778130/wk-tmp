import { publicBackendRequest } from "../../../../bridge/_public-backend";

export async function POST(request: Request) {
  const contentType = request.headers.get("content-type") || "application/json";
  return publicBackendRequest("/api/v1/bridge/pair", {
    method: "POST",
    headers: { "content-type": contentType },
    body: await request.arrayBuffer(),
  });
}
