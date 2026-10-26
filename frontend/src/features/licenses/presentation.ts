import type { License } from "../../services/admin";
import { displayDate } from "../../services/format";

type Timing = Pick<License, "lockDate" | "locked" | "daysUntilLock">;

export function lockDateLabel(license: Timing) {
  const date = displayDate(license.lockDate);
  const days = license.daysUntilLock;
  if (license.locked || days == null) return date;
  if (days === 0) return `${date} (vence hoy)`;
  const count = Math.abs(days);
  const duration = `${count} ${count === 1 ? "día" : "días"}`;
  return days > 0
    ? `${date} (${duration})`
    : `${date} (venció hace ${duration})`;
}

export function licenseRowClass(license: Pick<License, "status">) {
  if (license.status === "BLOCKED") return "bg-red-100! hover:bg-red-200!";
  if (license.status === "EXPIRED") return "bg-amber-100! hover:bg-amber-200!";
  return undefined;
}
