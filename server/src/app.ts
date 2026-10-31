import { Hono } from "hono";
import { bodyLimit } from "hono/body-limit";
import { HTTPException } from "hono/http-exception";
import { logger } from "hono/logger";
import { secureHeaders } from "hono/secure-headers";
import { auth } from "./auth";
import { env } from "./env";
import { apiError, type AppEnv } from "./http";
import { requireSession } from "./middleware";
import { expenseRoutes } from "./routes/expenses";
import { meRoutes } from "./routes/me";

export const app = new Hono<AppEnv>();

if (env.NODE_ENV !== "test") app.use(logger());

app.use(secureHeaders());
app.use(
  bodyLimit({
    maxSize: 16 * 1024,
    onError: () => {
      throw apiError(413, "payload_too_large", "Request body is too large");
    },
  }),
);

app.get("/health", (c) => c.json({ ok: true }));

app.on(["GET", "POST"], "/api/auth/*", (c) => auth.handler(c.req.raw));

app.use("/api/*", requireSession);
app.route("/api", expenseRoutes);
app.route("/api", meRoutes);

app.notFound(() => {
  throw apiError(404, "not_found", "Route not found");
});

app.onError((error, c) => {
  if (error instanceof HTTPException) {
    if (error.res) return error.getResponse();
    const status = error.status;
    const code = status === 401 ? "unauthorized" : status === 404 ? "not_found" : "bad_request";
    return c.json({ error: { code, message: error.message || "Invalid request" } }, status);
  }
  console.error(error);
  return c.json({ error: { code: "internal_error", message: "Something went wrong" } }, 500);
});
