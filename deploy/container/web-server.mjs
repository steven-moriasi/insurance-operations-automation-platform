import { createReadStream, statSync } from "node:fs";
import { createServer, request as proxyRequest } from "node:http";
import { extname, join, normalize } from "node:path";

const port = Number.parseInt(process.env.PORT ?? "8080", 10);
const publicRoot = "/app/public";
const bffUrl = new URL(process.env.BFF_URL ?? "http://web-bff:8080");
const forwardedProto = process.env.FORWARDED_PROTO ?? "https";
const proxyPrefixes = ["/api/", "/login", "/logout", "/oauth2/"];
const contentTypes = new Map([
  [".css", "text/css; charset=utf-8"],
  [".html", "text/html; charset=utf-8"],
  [".js", "text/javascript; charset=utf-8"],
  [".json", "application/json; charset=utf-8"],
  [".svg", "image/svg+xml"],
]);

function setSecurityHeaders(response) {
  response.setHeader("X-Content-Type-Options", "nosniff");
  response.setHeader("Referrer-Policy", "no-referrer");
}

function proxy(request, response) {
  const upstream = proxyRequest(
    {
      protocol: bffUrl.protocol,
      hostname: bffUrl.hostname,
      port: bffUrl.port,
      method: request.method,
      path: request.url,
      headers: {
        ...request.headers,
        host: request.headers.host,
        "x-forwarded-for": request.socket.remoteAddress,
        "x-forwarded-proto": request.headers["x-forwarded-proto"] ?? forwardedProto,
      },
    },
    (upstreamResponse) => {
      response.writeHead(upstreamResponse.statusCode ?? 502, upstreamResponse.headers);
      upstreamResponse.pipe(response);
    },
  );
  upstream.on("error", () => {
    response.writeHead(502, { "content-type": "application/json" });
    response.end('{"error":"BFF unavailable"}');
  });
  request.pipe(upstream);
}

function serveStatic(request, response) {
  const requestedPath = new URL(request.url ?? "/", "http://localhost").pathname;
  const relativePath = normalize(decodeURIComponent(requestedPath)).replace(/^(\.\.(\/|\\|$))+/, "");
  let filePath = join(publicRoot, relativePath);

  try {
    if (statSync(filePath).isDirectory()) {
      filePath = join(filePath, "index.html");
    }
  } catch {
    filePath = join(publicRoot, "index.html");
  }

  setSecurityHeaders(response);
  response.writeHead(200, {
    "content-type": contentTypes.get(extname(filePath)) ?? "application/octet-stream",
  });
  createReadStream(filePath).pipe(response);
}

createServer((request, response) => {
  if (proxyPrefixes.some((prefix) => request.url?.startsWith(prefix))) {
    proxy(request, response);
    return;
  }
  serveStatic(request, response);
}).listen(port, "0.0.0.0");
