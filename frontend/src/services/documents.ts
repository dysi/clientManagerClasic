/** Quita separadores admitidos sin descartar letras. */
export function normalizeDocument(value: string): string {
  return value.replace(/[ .-]/g, "");
}

export function validCuit(value: string): boolean {
  const digits = normalizeDocument(value);
  if (!/^[0-9 .-]+$/.test(value) || !/^(20|23|24|27|30|33|34)[0-9]{9}$/.test(digits)) return false;
  const weights = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];
  const sum = weights.reduce((total, weight, index) => total + Number(digits.charAt(index)) * weight, 0);
  const check = 11 - sum % 11;
  return (check === 11 ? 0 : check) === Number(digits.charAt(10));
}

export function clientCuitError(cuit: string, dni: string | null): string {
  if (!cuit.trim()) return "";
  if (!validCuit(cuit)) return "Ingresá un CUIL / CUIT válido, de once dígitos y con dígito verificador correcto.";
  if (dni && normalizeDocument(cuit).slice(2, 10) !== dni.padStart(8, "0")) {
    return "El CUIL / CUIT debe contener el DNI del cliente en sus ocho dígitos centrales.";
  }
  return "";
}
