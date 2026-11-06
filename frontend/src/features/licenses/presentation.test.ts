import { describe, expect, it } from "vitest";
import { lockDateLabel } from "./presentation";

describe("Fecha de bloqueo", () => {
  it.each([
    [35, "(35 días)"],
    [1, "(1 día)"],
    [0, "(vence hoy)"],
    [-1, "(venció hace 1 día)"],
    [-12, "(venció hace 12 días)"],
  ])("describe el plazo de %i días", (days, text) => {
    expect(
      lockDateLabel({
        lockDate: "2026-11-29",
        locked: false,
        daysUntilLock: days,
      }),
    ).toContain(text);
  });
  it("no anuncia días disponibles para una licencia ya bloqueada o sin fecha", () => {
    expect(
      lockDateLabel({
        lockDate: "2026-11-29",
        locked: true,
        daysUntilLock: 35,
      }),
    ).not.toContain("días");
    expect(
      lockDateLabel({ lockDate: null, locked: false, daysUntilLock: null }),
    ).toBe("Sin fecha");
  });
});
