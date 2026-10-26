import { describe, expect, it } from "vitest";
import { dateOnly, toDate, displayDate } from "./format";
describe("Fechas de formularios", () => {
  it.each(["2026-01-01", "2026-10-24", "2028-02-29"])(
    "conserva el día local %s sin conversión UTC",
    (value) => {
      const date = toDate(value)!;
      expect(dateOnly(date)).toBe(value);
      expect(date.getHours()).toBe(0);
    },
  );
  it("representa fechas opcionales vacías", () => {
    expect(toDate(null)).toBeNull();
    expect(displayDate(null)).toBe("Sin fecha");
  });
});
