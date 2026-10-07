"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const ioredis_1 = __importDefault(require("ioredis"));
const mode = process.env.EVENT_MODE ?? "local";
const redisUrl = process.env.REDIS_URL ?? "redis://localhost:6379";
const coreUrl = process.env.CORE_URL ?? "http://localhost:8080";
const workerToken = process.env.WORKER_TOKEN ?? "local-worker-token-change-before-deploy";
const stream = "vehicleiq:events";
const group = "vehicleiq-integration-workers";
const consumer = `worker-${process.pid}`;
if (mode !== "redis") {
    console.log(JSON.stringify({ level: "info", service: "integration-worker", mode, message: "Redis stream worker is idle; core local profile processes outbox events." }));
    setInterval(() => undefined, 60_000);
}
else {
    const redis = new ioredis_1.default(redisUrl, { maxRetriesPerRequest: null });
    async function start() {
        try {
            await redis.xgroup("CREATE", stream, group, "0", "MKSTREAM");
        }
        catch (error) {
            if (!(error instanceof Error) || !error.message.includes("BUSYGROUP"))
                throw error;
        }
        console.log(JSON.stringify({ level: "info", service: "integration-worker", stream, group, consumer }));
        while (true) {
            const batch = await redis.xreadgroup("GROUP", group, consumer, "COUNT", 10, "BLOCK", 5000, "STREAMS", stream, ">");
            if (!batch)
                continue;
            for (const [, messages] of batch)
                for (const [id, values] of messages) {
                    const event = Object.fromEntries(Array.from({ length: values.length / 2 }, (_, i) => [values[i * 2], values[i * 2 + 1]]));
                    try {
                        if (event.type === "analysis.requested.v1") {
                            // This mock adapter intentionally contributes no external vehicle-history facts.
                            // The core still computes from user-entered evidence and records source limitations.
                            const response = await fetch(`${coreUrl}/v1/internal/analysis/${event.aggregateId}/complete`, { method: "POST", headers: { "x-worker-token": workerToken } });
                            if (!response.ok)
                                throw new Error(`Core returned ${response.status}`);
                        }
                        await redis.xack(stream, group, id);
                    }
                    catch (error) {
                        console.error(JSON.stringify({ level: "error", eventId: id, error: String(error) }));
                        await new Promise((resolve) => setTimeout(resolve, 1500));
                    }
                }
        }
    }
    start().catch((error) => { console.error(error); process.exitCode = 1; });
}
