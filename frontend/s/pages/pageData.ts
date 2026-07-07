export type PageMap = Record<string, unknown>;

export const asMap = (value: unknown): PageMap =>
  value && typeof value === "object" && !Array.isArray(value) ? (value as PageMap) : {};

export const asList = (value: unknown): PageMap[] =>
  Array.isArray(value)
    ? value.filter((item) => item && typeof item === "object").map((item) => item as PageMap)
    : [];

export const asText = (value: unknown, fallback = "-"): string => {
  if (value === null || value === undefined || value === "") return fallback;
  if (Array.isArray(value)) return value.map((item) => asText(item, "")).filter(Boolean).join(" / ");
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
};

export const asNumber = (value: unknown, fallback = 0) =>
  typeof value === "number" && Number.isFinite(value) ? value : fallback;

export const percent = (value: unknown, fallback = "-") => {
  const number = asNumber(value, Number.NaN);
  return Number.isFinite(number) ? `${Math.round(number * 100)}%` : fallback;
};
