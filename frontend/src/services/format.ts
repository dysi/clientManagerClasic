export function toDate(value: string | null | undefined) {
  if (!value) return null;
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year!, month! - 1, day!);
}
export function dateOnly(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
}
export function today(offset = 0) {
  const value = new Date();
  value.setDate(value.getDate() + offset);
  return dateOnly(value);
}
export function displayDate(value?: string | null) {
  return value
    ? new Intl.DateTimeFormat("es-AR").format(toDate(value)!)
    : "Sin fecha";
}
export function money(value: number) {
  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
  }).format(value);
}
