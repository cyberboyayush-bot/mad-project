export const CATEGORIES = [
  "food",
  "transport",
  "shopping",
  "bills",
  "entertainment",
  "health",
  "education",
  "other",
] as const;

export type Category = (typeof CATEGORIES)[number];
