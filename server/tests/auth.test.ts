import { describe, expect, test } from "bun:test";
import { call, signUp } from "./helpers";

describe("auth", () => {
  test("sign up returns a signed bearer token and the user", async () => {
    const { token, user, credentials } = await signUp();
    expect(token).toContain(".");
    expect(user.email).toBe(credentials.email);
    expect(user.monthlyBudget).toBe(0);
    expect(user).not.toHaveProperty("password");
  });

  test("sign up rejects a duplicate email", async () => {
    const { credentials } = await signUp();
    const res = await call("POST", "/api/auth/sign-up/email", { body: credentials });
    expect(res.status).toBe(422);
  });

  test("sign up treats email case-insensitively", async () => {
    const { credentials } = await signUp();
    const res = await call("POST", "/api/auth/sign-up/email", {
      body: { ...credentials, email: credentials.email.toUpperCase() },
    });
    expect(res.status).toBe(422);
  });

  test("sign up rejects short passwords", async () => {
    const res = await call("POST", "/api/auth/sign-up/email", {
      body: { name: "A", email: "short@example.com", password: "1234567" },
    });
    expect(res.status).toBe(400);
  });

  test("sign up rejects invalid email", async () => {
    const res = await call("POST", "/api/auth/sign-up/email", {
      body: { name: "A", email: "not-an-email", password: "long-enough-pass" },
    });
    expect(res.status).toBe(400);
  });

  test("clients cannot set monthlyBudget during sign up", async () => {
    const res = await call("POST", "/api/auth/sign-up/email", {
      body: {
        name: "Sneaky",
        email: "sneaky@example.com",
        password: "long-enough-pass",
        monthlyBudget: 999,
      },
    });
    if (res.status === 200) expect(res.json.user.monthlyBudget).toBe(0);
    else expect(res.status).toBe(400);
  });

  test("sign in works with correct credentials", async () => {
    const { credentials } = await signUp();
    const res = await call("POST", "/api/auth/sign-in/email", {
      body: { email: credentials.email, password: credentials.password },
    });
    expect(res.status).toBe(200);
    expect(res.headers.get("set-auth-token")).toBeTruthy();
  });

  test("sign in fails with a wrong password", async () => {
    const { credentials } = await signUp();
    const res = await call("POST", "/api/auth/sign-in/email", {
      body: { email: credentials.email, password: "wrong-password" },
    });
    expect(res.status).toBe(401);
    expect(res.headers.get("set-auth-token")).toBeNull();
  });

  test("sign in fails for an unknown user", async () => {
    const res = await call("POST", "/api/auth/sign-in/email", {
      body: { email: "nobody@example.com", password: "whatever-password" },
    });
    expect(res.status).toBe(401);
  });

  test("get-session works with the bearer token", async () => {
    const { token, credentials } = await signUp();
    const res = await call("GET", "/api/auth/get-session", { token });
    expect(res.status).toBe(200);
    expect(res.json.user.email).toBe(credentials.email);
  });

  test("an unsigned raw token is rejected", async () => {
    const { token } = await signUp();
    const raw = token.split(".")[0]!;
    const res = await call("GET", "/api/me", { token: raw });
    expect(res.status).toBe(401);
  });

  test("a tampered token is rejected", async () => {
    const { token } = await signUp();
    const [raw, signature] = token.split(".");
    const res = await call("GET", "/api/me", { token: `${raw}x.${signature}` });
    expect(res.status).toBe(401);
  });

  test("sign out revokes the session", async () => {
    const { token } = await signUp();
    expect((await call("GET", "/api/me", { token })).status).toBe(200);
    const out = await call("POST", "/api/auth/sign-out", { token, body: {} });
    expect(out.status).toBe(200);
    expect((await call("GET", "/api/me", { token })).status).toBe(401);
  });
});

describe("protected routes", () => {
  test.each([
    ["GET", "/api/me"],
    ["PATCH", "/api/me"],
    ["GET", "/api/expenses"],
    ["POST", "/api/expenses"],
    ["GET", "/api/summary?month=2026-10"],
  ])("%s %s requires a session", async (method, path) => {
    const res = await call(method, path);
    expect(res.status).toBe(401);
    expect(res.json.error.code).toBe("unauthorized");
  });

  test("garbage bearer tokens are rejected", async () => {
    const res = await call("GET", "/api/me", { token: "abc.def" });
    expect(res.status).toBe(401);
  });
});

describe("misc", () => {
  test("health check", async () => {
    const res = await call("GET", "/health");
    expect(res.json).toEqual({ ok: true });
  });

  test("unknown routes return a JSON 404", async () => {
    const { token } = await signUp();
    const res = await call("GET", "/api/nope", { token });
    expect(res.status).toBe(404);
    expect(res.json.error.code).toBe("not_found");
  });

  test("security headers are present", async () => {
    const res = await call("GET", "/health");
    expect(res.headers.get("x-content-type-options")).toBe("nosniff");
    expect(res.headers.get("x-frame-options")).toBeTruthy();
  });

  test("oversized bodies are rejected", async () => {
    const { token } = await signUp();
    const res = await call("POST", "/api/expenses", {
      token,
      body: { note: "x".repeat(20_000) },
    });
    expect(res.status).toBe(413);
  });
});
