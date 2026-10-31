import { describe, expect, test } from "bun:test";
import { call, expenseBody, signUp } from "./helpers";

describe("expenses CRUD", () => {
  test("create, read, update and delete an expense", async () => {
    const { token } = await signUp();

    const created = await call("POST", "/api/expenses", { token, body: expenseBody() });
    expect(created.status).toBe(201);
    const id = created.json.expense.id;
    expect(created.json.expense).toMatchObject({
      amount: 25_000,
      category: "food",
      note: "Lunch",
      spentOn: "2026-10-15",
    });

    const fetched = await call("GET", `/api/expenses/${id}`, { token });
    expect(fetched.json.expense.id).toBe(id);

    const updated = await call("PATCH", `/api/expenses/${id}`, {
      token,
      body: { amount: 30_050, note: "  Dinner  " },
    });
    expect(updated.status).toBe(200);
    expect(updated.json.expense.amount).toBe(30_050);
    expect(updated.json.expense.note).toBe("Dinner");
    expect(updated.json.expense.category).toBe("food");

    const amountOnly = await call("PATCH", `/api/expenses/${id}`, { token, body: { amount: 1 } });
    expect(amountOnly.json.expense.note).toBe("Dinner");

    const deleted = await call("DELETE", `/api/expenses/${id}`, { token });
    expect(deleted.status).toBe(204);
    expect((await call("GET", `/api/expenses/${id}`, { token })).status).toBe(404);
    expect((await call("DELETE", `/api/expenses/${id}`, { token })).status).toBe(404);
  });

  test("note defaults to empty string", async () => {
    const { token } = await signUp();
    const { note: _, ...body } = expenseBody();
    const res = await call("POST", "/api/expenses", { token, body });
    expect(res.status).toBe(201);
    expect(res.json.expense.note).toBe("");
  });

  test.each([
    ["zero amount", { amount: 0 }],
    ["negative amount", { amount: -100 }],
    ["fractional paise", { amount: 10.5 }],
    ["amount above limit", { amount: 100_000_001 }],
    ["string amount", { amount: "100" }],
    ["unknown category", { category: "crypto" }],
    ["note too long", { note: "x".repeat(121) }],
    ["bad date", { spentOn: "15-10-2026" }],
    ["impossible date", { spentOn: "2026-02-30" }],
    ["date out of range", { spentOn: "1999-12-31" }],
    ["unknown field", { userId: "someone-else" }],
  ])("rejects %s", async (_, override) => {
    const { token } = await signUp();
    const res = await call("POST", "/api/expenses", { token, body: expenseBody(override) });
    expect(res.status).toBe(400);
    expect(res.json.error.code).toBe("bad_request");
  });

  test("rejects an empty update", async () => {
    const { token } = await signUp();
    const created = await call("POST", "/api/expenses", { token, body: expenseBody() });
    const res = await call("PATCH", `/api/expenses/${created.json.expense.id}`, {
      token,
      body: {},
    });
    expect(res.status).toBe(400);
  });

  test("rejects malformed ids", async () => {
    const { token } = await signUp();
    const res = await call("GET", "/api/expenses/not-a-uuid", { token });
    expect(res.status).toBe(400);
  });

  test("rejects invalid JSON", async () => {
    const { token } = await signUp();
    const res = await call("POST", "/api/expenses", {
      token,
      headers: { "Content-Type": "application/json" },
    });
    expect(res.status).toBe(400);
  });
});

describe("isolation between users", () => {
  test("users cannot see, edit or delete each other's expenses", async () => {
    const alice = await signUp({ name: "Alice" });
    const bob = await signUp({ name: "Bob" });

    const created = await call("POST", "/api/expenses", {
      token: alice.token,
      body: expenseBody(),
    });
    const id = created.json.expense.id;

    expect((await call("GET", `/api/expenses/${id}`, { token: bob.token })).status).toBe(404);
    expect(
      (await call("PATCH", `/api/expenses/${id}`, { token: bob.token, body: { amount: 1 } })).status,
    ).toBe(404);
    expect((await call("DELETE", `/api/expenses/${id}`, { token: bob.token })).status).toBe(404);

    const bobList = await call("GET", "/api/expenses", { token: bob.token });
    expect(bobList.json.expenses).toEqual([]);

    const bobSummary = await call("GET", "/api/summary?month=2026-10", { token: bob.token });
    expect(bobSummary.json.total).toBe(0);

    const stillThere = await call("GET", `/api/expenses/${id}`, { token: alice.token });
    expect(stillThere.json.expense.amount).toBe(25_000);
  });
});

describe("listing and filtering", () => {
  async function seed() {
    const { token } = await signUp();
    const items = [
      expenseBody({ amount: 10_000, category: "food", note: "Chai & samosa", spentOn: "2026-10-02" }),
      expenseBody({ amount: 50_000, category: "transport", note: "Metro card", spentOn: "2026-10-05" }),
      expenseBody({ amount: 20_000, category: "food", note: "Pizza 100%", spentOn: "2026-10-05" }),
      expenseBody({ amount: 99_900, category: "shopping", note: "Shoes", spentOn: "2026-09-28" }),
      expenseBody({ amount: 5_000, category: "bills", note: "Recharge", spentOn: "2026-11-01" }),
    ];
    for (const body of items) await call("POST", "/api/expenses", { token, body });
    return token;
  }

  test("lists newest first", async () => {
    const token = await seed();
    const res = await call("GET", "/api/expenses", { token });
    const dates = res.json.expenses.map((e: { spentOn: string }) => e.spentOn);
    expect(dates).toEqual([...dates].sort().reverse());
    expect(res.json.expenses).toHaveLength(5);
  });

  test("filters by month (inclusive start, exclusive end)", async () => {
    const token = await seed();
    const res = await call("GET", "/api/expenses?month=2026-10", { token });
    expect(res.json.expenses).toHaveLength(3);
  });

  test("filters by category", async () => {
    const token = await seed();
    const res = await call("GET", "/api/expenses?category=food", { token });
    expect(res.json.expenses).toHaveLength(2);
  });

  test("searches notes case-insensitively", async () => {
    const token = await seed();
    const res = await call("GET", "/api/expenses?q=METRO", { token });
    expect(res.json.expenses).toHaveLength(1);
  });

  test("treats LIKE wildcards literally", async () => {
    const token = await seed();
    expect((await call("GET", "/api/expenses?q=%25", { token })).json.expenses).toHaveLength(1);
    expect((await call("GET", "/api/expenses?q=_", { token })).json.expenses).toHaveLength(0);
  });

  test("rejects bad query params", async () => {
    const { token } = await signUp();
    expect((await call("GET", "/api/expenses?month=2026-13", { token })).status).toBe(400);
    expect((await call("GET", "/api/expenses?category=nope", { token })).status).toBe(400);
  });

  test("summary aggregates the month", async () => {
    const token = await seed();
    await call("PATCH", "/api/me", { token, body: { monthlyBudget: 500_000 } });

    const res = await call("GET", "/api/summary?month=2026-10", { token });
    expect(res.status).toBe(200);
    expect(res.json).toMatchObject({
      month: "2026-10",
      total: 80_000,
      count: 3,
      previousTotal: 99_900,
      budget: 500_000,
    });
    expect(res.json.byCategory).toEqual([
      { category: "transport", total: 50_000, count: 1 },
      { category: "food", total: 30_000, count: 2 },
    ]);
    expect(res.json.byDay).toEqual([
      { date: "2026-10-02", total: 10_000 },
      { date: "2026-10-05", total: 70_000 },
    ]);
  });

  test("summary handles January → previous December", async () => {
    const { token } = await signUp();
    await call("POST", "/api/expenses", { token, body: expenseBody({ spentOn: "2025-12-31" }) });
    const res = await call("GET", "/api/summary?month=2026-01", { token });
    expect(res.json.total).toBe(0);
    expect(res.json.previousTotal).toBe(25_000);
  });

  test("summary handles large totals without overflow", async () => {
    const { token } = await signUp();
    for (let i = 0; i < 25; i++) {
      await call("POST", "/api/expenses", { token, body: expenseBody({ amount: 100_000_000 }) });
    }
    const res = await call("GET", "/api/summary?month=2026-10", { token });
    expect(res.json.total).toBe(2_500_000_000);
  });

  test("summary requires a month", async () => {
    const { token } = await signUp();
    expect((await call("GET", "/api/summary", { token })).status).toBe(400);
  });
});

describe("profile", () => {
  test("get and update the profile", async () => {
    const { token, credentials } = await signUp();
    const me = await call("GET", "/api/me", { token });
    expect(me.json.profile).toMatchObject({ name: "Ayush", email: credentials.email, monthlyBudget: 0 });

    const updated = await call("PATCH", "/api/me", {
      token,
      body: { name: "  Ayush S  ", monthlyBudget: 1_500_000 },
    });
    expect(updated.json.profile).toMatchObject({ name: "Ayush S", monthlyBudget: 1_500_000 });

    const again = await call("GET", "/api/me", { token });
    expect(again.json.profile.monthlyBudget).toBe(1_500_000);
  });

  test.each([
    ["negative budget", { monthlyBudget: -1 }],
    ["empty name", { name: "   " }],
    ["email change", { email: "new@example.com" }],
    ["empty body", {}],
  ])("rejects %s", async (_, body) => {
    const { token } = await signUp();
    const res = await call("PATCH", "/api/me", { token, body });
    expect(res.status).toBe(400);
  });

  test("deleting a user cascades to their expenses", async () => {
    const { token, user } = await signUp();
    await call("POST", "/api/expenses", { token, body: expenseBody() });
    const { sql } = await import("../src/db/client");
    await sql`DELETE FROM "user" WHERE id = ${user.id}`;
    const rows = await sql`SELECT count(*)::int AS count FROM expense WHERE user_id = ${user.id}`;
    expect(rows[0]?.count).toBe(0);
  });
});
