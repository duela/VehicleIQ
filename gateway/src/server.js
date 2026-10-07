"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const node_crypto_1 = __importDefault(require("node:crypto"));
const express_1 = __importDefault(require("express"));
const express_rate_limit_1 = __importDefault(require("express-rate-limit"));
const helmet_1 = __importDefault(require("helmet"));
const jsonwebtoken_1 = __importDefault(require("jsonwebtoken"));
const http_proxy_middleware_1 = require("http-proxy-middleware");
const app = (0, express_1.default)();
const port = Number(process.env.PORT ?? 4000);
const coreUrl = process.env.CORE_URL ?? "http://localhost:8080";
const secret = process.env.JWT_SECRET ?? "local-development-secret-change-before-deploy-vehicleiq";
const demoApiKey = process.env.DEMO_API_KEY ?? "viq_demo_local_only_7f0c3e91";
const demoEnabled = process.env.ENABLE_DEMO_AUTH !== "false" && process.env.NODE_ENV !== "production";
let partnerCalls = 0;
app.disable("x-powered-by");
app.use((0, helmet_1.default)({ contentSecurityPolicy: false }));
app.use((req, res, next) => {
    const requestId = req.header("x-request-id") ?? node_crypto_1.default.randomUUID();
    res.setHeader("x-request-id", requestId);
    res.setHeader("x-content-type-options", "nosniff");
    next();
});
app.get("/health", (_req, res) => res.json({ status: "ok", service: "vehicleiq-gateway" }));
app.get("/metrics", (_req, res) => res.json({ service: "gateway", partnerRequests: partnerCalls, demoAuthEnabled: demoEnabled }));
app.post("/api/auth/demo", express_1.default.json(), (req, res) => {
    if (!demoEnabled)
        return res.status(404).json({ error: "Demo sign-in is disabled." });
    const role = req.body?.role;
    if (!["DEALER", "ADMIN", "PARTNER"].includes(role))
        return res.status(400).json({ error: "Choose a supported demo workspace." });
    const isAdmin = role === "ADMIN";
    const tenantId = isAdmin ? "platform" : role === "PARTNER" ? "tenant-partner-sandbox" : "tenant-demo";
    const token = jsonwebtoken_1.default.sign({ tenant_id: tenantId, roles: [role], name: isAdmin ? "VehicleIQ Admin" : role === "PARTNER" ? "Partner Sandbox" : "Samuel Carter" }, secret, { algorithm: "HS256", subject: isAdmin ? "user-admin-demo" : role === "PARTNER" ? "client-demo-partner" : "user-dealer-demo", expiresIn: "6h", issuer: "vehicleiq-local" });
    res.json({ accessToken: token, tokenType: "Bearer", expiresIn: 21600, user: { name: isAdmin ? "VehicleIQ Admin" : role === "PARTNER" ? "Partner Sandbox" : "Samuel Carter", role, tenantId }, localDemo: true });
});
// Browser app and core API. Spring remains the source of truth for tenant authorization.
app.use((0, http_proxy_middleware_1.createProxyMiddleware)({
    pathFilter: (path) => path.startsWith("/api/v1"),
    target: coreUrl,
    changeOrigin: true,
    pathRewrite: (path) => path.replace(/^\/api/, ""),
    on: { proxyReq: (proxyReq, req) => { proxyReq.setHeader("x-request-id", req.headers["x-request-id"] ?? node_crypto_1.default.randomUUID()); } }
}));
// Private API product sandbox. The configured demo key is compared in constant time and is never returned by this endpoint.
function apiKeyMatches(candidate) {
    const digest = (value) => node_crypto_1.default.createHash("sha256").update(value).digest();
    return node_crypto_1.default.timingSafeEqual(digest(candidate), digest(demoApiKey));
}
app.use("/partner/v1", (0, express_rate_limit_1.default)({ windowMs: 60_000, limit: 60, standardHeaders: true, legacyHeaders: false }), (req, res, next) => {
    const key = req.header("x-api-key") ?? "";
    const bearer = req.header("authorization");
    if (key && apiKeyMatches(key)) {
        partnerCalls += 1;
        res.locals.partnerToken = jsonwebtoken_1.default.sign({ tenant_id: "tenant-partner-sandbox", roles: ["PARTNER"], name: "Partner Sandbox" }, secret, { algorithm: "HS256", subject: "client-demo-partner", expiresIn: "5m", issuer: "vehicleiq-local" });
    }
    else if (!bearer?.startsWith("Bearer ")) {
        return res.status(401).json({ error: { code: "INVALID_CREDENTIAL", message: "Supply a valid API key or OAuth bearer token." } });
    }
    next();
}, (0, http_proxy_middleware_1.createProxyMiddleware)({
    target: coreUrl,
    changeOrigin: true,
    pathRewrite: (path) => `/v1/partner${path}`,
    on: { proxyReq: (proxyReq, req) => {
            const response = req.res;
            if (response.locals.partnerToken)
                proxyReq.setHeader("authorization", `Bearer ${response.locals.partnerToken}`);
            proxyReq.removeHeader("x-api-key");
            proxyReq.setHeader("x-request-id", req.headers["x-request-id"] ?? node_crypto_1.default.randomUUID());
        } }
}));
app.use((err, _req, res, _next) => {
    console.error(JSON.stringify({ level: "error", message: err.message }));
    res.status(500).json({ error: { code: "GATEWAY_ERROR", message: "The request could not be completed." } });
});
app.listen(port, () => console.log(JSON.stringify({ level: "info", service: "gateway", port, coreUrl, demoAuthEnabled: demoEnabled })));
