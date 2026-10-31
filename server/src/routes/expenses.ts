import { and, desc, eq, gte, ilike, lt, sql } from "drizzle-orm";
import { Hono } from "hono";
import { db } from "../db/client";
import { expense, type Expense } from "../db/schema";
import { apiError, type AppEnv } from "../http";
import { validate } from "../middleware";
import {
  expenseInput,
  expenseQuery,
  expenseUpdate,
  idParam,
  monthRange,
  previousMonth,
  summaryQuery,
} from "../validation";

const LIST_LIMIT = 500;

function toDto(row: Expense) {
  return {
    id: row.id,
    amount: row.amount,
    category: row.category,
    note: row.note,
    spentOn: row.spentOn,
    createdAt: row.createdAt.toISOString(),
  };
}

function escapeLike(value: string) {
  return value.replace(/[\\%_]/g, (match) => `\\${match}`);
}

export const expenseRoutes = new Hono<AppEnv>()
  .get("/expenses", validate("query", expenseQuery), async (c) => {
    const { month, category, q } = c.req.valid("query");
    const userId = c.get("user").id;

    const filters = [eq(expense.userId, userId)];
    if (month) {
      const [start, end] = monthRange(month);
      filters.push(gte(expense.spentOn, start), lt(expense.spentOn, end));
    }
    if (category) filters.push(eq(expense.category, category));
    if (q) filters.push(ilike(expense.note, `%${escapeLike(q)}%`));

    const rows = await db
      .select()
      .from(expense)
      .where(and(...filters))
      .orderBy(desc(expense.spentOn), desc(expense.createdAt))
      .limit(LIST_LIMIT);

    return c.json({ expenses: rows.map(toDto) });
  })

  .post("/expenses", validate("json", expenseInput), async (c) => {
    const input = c.req.valid("json");
    const [row] = await db
      .insert(expense)
      .values({ ...input, userId: c.get("user").id })
      .returning();
    return c.json({ expense: toDto(row!) }, 201);
  })

  .get("/expenses/:id", validate("param", idParam), async (c) => {
    const { id } = c.req.valid("param");
    const row = await db.query.expense.findFirst({
      where: and(eq(expense.id, id), eq(expense.userId, c.get("user").id)),
    });
    if (!row) throw apiError(404, "not_found", "Expense not found");
    return c.json({ expense: toDto(row) });
  })

  .patch(
    "/expenses/:id",
    validate("param", idParam),
    validate("json", expenseUpdate),
    async (c) => {
      const { id } = c.req.valid("param");
      const [row] = await db
        .update(expense)
        .set(c.req.valid("json"))
        .where(and(eq(expense.id, id), eq(expense.userId, c.get("user").id)))
        .returning();
      if (!row) throw apiError(404, "not_found", "Expense not found");
      return c.json({ expense: toDto(row) });
    },
  )

  .delete("/expenses/:id", validate("param", idParam), async (c) => {
    const { id } = c.req.valid("param");
    const [row] = await db
      .delete(expense)
      .where(and(eq(expense.id, id), eq(expense.userId, c.get("user").id)))
      .returning({ id: expense.id });
    if (!row) throw apiError(404, "not_found", "Expense not found");
    return c.body(null, 204);
  })

  .get("/summary", validate("query", summaryQuery), async (c) => {
    const { month } = c.req.valid("query");
    const user = c.get("user");
    const [start, end] = monthRange(month);
    const [prevStart, prevEnd] = monthRange(previousMonth(month));

    const inMonth = and(
      eq(expense.userId, user.id),
      gte(expense.spentOn, start),
      lt(expense.spentOn, end),
    );
    const total = sql<number>`coalesce(sum(${expense.amount}), 0)`.mapWith(Number);
    const count = sql<number>`count(*)`.mapWith(Number);

    const [totals, previous, byCategory, byDay] = await Promise.all([
      db.select({ total, count }).from(expense).where(inMonth),
      db
        .select({ total })
        .from(expense)
        .where(
          and(
            eq(expense.userId, user.id),
            gte(expense.spentOn, prevStart),
            lt(expense.spentOn, prevEnd),
          ),
        ),
      db
        .select({ category: expense.category, total, count })
        .from(expense)
        .where(inMonth)
        .groupBy(expense.category)
        .orderBy(desc(total)),
      db
        .select({ date: expense.spentOn, total })
        .from(expense)
        .where(inMonth)
        .groupBy(expense.spentOn)
        .orderBy(expense.spentOn),
    ]);

    return c.json({
      month,
      total: totals[0]?.total ?? 0,
      count: totals[0]?.count ?? 0,
      previousTotal: previous[0]?.total ?? 0,
      budget: user.monthlyBudget ?? 0,
      byCategory,
      byDay,
    });
  });
