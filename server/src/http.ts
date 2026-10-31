import { HTTPException } from "hono/http-exception";
import type { auth } from "./auth";

type SessionResult = NonNullable<Awaited<ReturnType<typeof auth.api.getSession>>>;

export type AppEnv = {
  Variables: {
    user: SessionResult["user"];
    session: SessionResult["session"];
  };
};

export type ErrorCode =
  | "bad_request"
  | "unauthorized"
  | "not_found"
  | "payload_too_large"
  | "internal_error";

export function apiError(status: 400 | 401 | 404 | 413 | 500, code: ErrorCode, message: string) {
  return new HTTPException(status, {
    res: Response.json({ error: { code, message } }, { status }),
  });
}
