import { eq } from "drizzle-orm";
import { Hono } from "hono";
import { db } from "../db/client";
import { user } from "../db/schema";
import type { AppEnv } from "../http";
import { validate } from "../middleware";
import { profileUpdate } from "../validation";

type ProfileSource = Pick<typeof user.$inferSelect, "id" | "name" | "email" | "createdAt"> & {
  monthlyBudget?: number | null;
};

function toProfile(row: ProfileSource) {
  return {
    id: row.id,
    name: row.name,
    email: row.email,
    monthlyBudget: row.monthlyBudget ?? 0,
    createdAt: new Date(row.createdAt).toISOString(),
  };
}

export const meRoutes = new Hono<AppEnv>()
  .get("/me", (c) => c.json({ profile: toProfile(c.get("user")) }))

  .patch("/me", validate("json", profileUpdate), async (c) => {
    const [row] = await db
      .update(user)
      .set(c.req.valid("json"))
      .where(eq(user.id, c.get("user").id))
      .returning();
    return c.json({ profile: toProfile(row!) });
  });
