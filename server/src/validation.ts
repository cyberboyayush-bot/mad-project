import { z } from "zod";
import { CATEGORIES } from "./categories";

/** ₹10,00,000 expressed in paise. */
export const MAX_AMOUNT_PAISE = 100_000_000;

const isoDate = z.iso.date().refine((value) => {
  const year = Number(value.slice(0, 4));
  return year >= 2000 && year <= 2100;
}, "Date must be between the years 2000 and 2100");

export const monthParam = z
  .string()
  .regex(/^\d{4}-(0[1-9]|1[0-2])$/, "Month must look like YYYY-MM");

const expenseFields = z.strictObject({
  amount: z.int().positive().max(MAX_AMOUNT_PAISE),
  category: z.enum(CATEGORIES),
  note: z.string().trim().max(120),
  spentOn: isoDate,
});

export const expenseInput = expenseFields.extend({ note: expenseFields.shape.note.default("") });

export const expenseUpdate = expenseFields.partial().refine(
  (value) => Object.keys(value).length > 0,
  "Provide at least one field to update",
);

export const expenseQuery = z.object({
  month: monthParam.optional(),
  category: z.enum(CATEGORIES).optional(),
  q: z.string().trim().max(60).optional(),
});

export const summaryQuery = z.object({ month: monthParam });

export const profileUpdate = z
  .strictObject({
    name: z.string().trim().min(1).max(60),
    monthlyBudget: z.int().min(0).max(MAX_AMOUNT_PAISE),
  })
  .partial()
  .refine((value) => Object.keys(value).length > 0, "Provide at least one field to update");

export const idParam = z.object({ id: z.uuid() });

/** Returns [first day of month, first day of next month] as YYYY-MM-DD strings. */
export function monthRange(month: string): [string, string] {
  const [year, mon] = month.split("-").map(Number) as [number, number];
  const next = mon === 12 ? `${year + 1}-01` : `${year}-${String(mon + 1).padStart(2, "0")}`;
  return [`${month}-01`, `${next}-01`];
}

export function previousMonth(month: string): string {
  const [year, mon] = month.split("-").map(Number) as [number, number];
  return mon === 1 ? `${year - 1}-12` : `${year}-${String(mon - 1).padStart(2, "0")}`;
}
