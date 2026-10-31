import { eq } from "drizzle-orm";
import { auth } from "../auth";
import type { Category } from "../categories";
import { db, sql } from "./client";
import { expense, user } from "./schema";

const DEMO = { name: "Ayush", email: "demo@pocketledger.app", password: "demo12345" };

type Item = [daysAgo: number, rupees: number, category: Category, note: string];

const ITEMS: Item[] = [
  [0, 180, "food", "Chai & samosa"],
  [0, 649, "entertainment", "Movie night"],
  [1, 1240, "shopping", "Notebook & pens"],
  [1, 85, "transport", "Metro"],
  [2, 420, "food", "Dinner with friends"],
  [3, 2199, "bills", "Broadband"],
  [3, 60, "transport", "Auto"],
  [4, 350, "health", "Pharmacy"],
  [5, 1499, "education", "Online course"],
  [6, 230, "food", "Lunch at canteen"],
  [7, 999, "shopping", "T-shirt"],
  [8, 120, "transport", "Bus pass top-up"],
  [9, 540, "food", "Groceries"],
  [10, 299, "bills", "Mobile recharge"],
  [12, 750, "entertainment", "Concert ticket"],
  [13, 95, "food", "Coffee"],
  [15, 2400, "shopping", "Running shoes"],
  [17, 160, "transport", "Cab to station"],
  [19, 480, "food", "Pizza"],
  [21, 200, "other", "Gift wrap"],
  [24, 640, "health", "Doctor visit"],
  [27, 310, "food", "Groceries"],
  [33, 1800, "bills", "Electricity"],
  [36, 520, "food", "Restaurant"],
  [40, 3200, "shopping", "Headphones"],
  [44, 150, "transport", "Metro"],
  [48, 899, "education", "Books"],
  [52, 260, "food", "Snacks"],
];

function daysAgo(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toLocaleDateString("en-CA");
}

const existing = await db.query.user.findFirst({ where: eq(user.email, DEMO.email) });
if (existing) await db.delete(user).where(eq(user.id, existing.id));

const { user: created } = await auth.api.signUpEmail({ body: DEMO });
await db.update(user).set({ monthlyBudget: 15_000_00 }).where(eq(user.id, created.id));
await db.insert(expense).values(
  ITEMS.map(([days, rupees, category, note]) => ({
    userId: created.id,
    amount: rupees * 100,
    category,
    note,
    spentOn: daysAgo(days),
  })),
);

await sql.end();
console.log(`Seeded ${ITEMS.length} expenses for ${DEMO.email} / ${DEMO.password}`);
