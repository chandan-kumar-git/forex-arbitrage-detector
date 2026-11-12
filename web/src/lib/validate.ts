import { BASE_CODE, type Currency } from "../engine/types";

export type FieldErrors = Partial<Record<"code" | "name" | "price", string>>;

/** The same rules as CurrencyValidator.java. */
export function validateCurrency(
  input: { code: string; name: string; price: string; active: boolean },
  existing: Currency[],
  editing: string | null,
): { currency?: Currency; errors: FieldErrors } {
  const errors: FieldErrors = {};
  const code = input.code.trim();
  const name = input.name.trim();
  const price = Number(input.price.trim());
  if (!/^[A-Z]{3}$/.test(code)) errors.code = "Use exactly 3 uppercase letters, like AUD";
  else if (!editing && existing.some((c) => c.code === code)) errors.code = "Code already exists";
  if (name.length < 2 || name.length > 40) errors.name = "Name must be 2 to 40 characters";
  if (input.price.trim() === "" || !Number.isFinite(price) || price <= 0) errors.price = "Price must be a number above 0";
  if (code === BASE_CODE && !input.active) errors.code = "USD is the base currency and stays active";
  if (Object.keys(errors).length) return { errors };
  return { currency: { code, name, usdPrice: price, active: input.active }, errors };
}
