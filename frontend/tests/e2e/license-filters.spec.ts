import { test, expect } from "@playwright/test";

test("licencias combina cliente, texto y estado; pagina 25/50/100 y resalta filas", async ({
  page,
  context,
  baseURL,
}) => {
  test.setTimeout(90_000);
  if (!baseURL || !process.env.E2E_USER || !process.env.E2E_PASSWORD)
    throw new Error("Configurar el entorno E2E aislado.");
  await page.goto(baseURL);
  await page.getByRole("link", { name: "Ingreso para usuarios", exact: true }).click();
  await page.getByLabel(/^Usuario/).fill(process.env.E2E_USER);
  await page.getByLabel(/^Contraseña/).fill(process.env.E2E_PASSWORD);
  await page
    .getByRole("button", { name: "Iniciar sesión", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Licencias", exact: true }),
  ).toBeVisible();
  const api = new URL("../web/", baseURL).href;
  const csrf = await (await context.request.get(api + "session/csrf")).json();
  const headers = { [csrf.headerName]: csrf.token };
  async function create(resource: string, data: unknown) {
    const response = await context.request.post(api + "api/" + resource, {
      headers,
      data,
    });
    expect(response.status()).toBe(201);
    return response.json();
  }
  const first = await create("clients", {
    firstname: "Filtro Alfa",
    lastname: "Prueba",
    email: "",
    phone: "",
    cuil: "",
  });
  const second = await create("clients", {
    firstname: "Filtro Beta",
    lastname: "Prueba",
    email: "",
    phone: "",
    cuil: "",
  });
  for (const [index, client] of [first, first, second, second].entries()) {
    const license = await create("licenses", {
      clientId: client.id,
      companyId: 91001 + index,
      companyName: `Filtro licencia ${index + 1}`,
      description: "",
      initialActivity: "2026-01-01",
      lockDate: "2099-12-31",
      prePayment: true,
    });
    if (index % 2 === 1)
      expect(
        (
          await context.request.put(api + `api/licenses/${license.id}/lock`, {
            headers,
            data: { locked: true },
          })
        ).ok(),
      ).toBe(true);
  }
  await page
    .getByLabel("Buscar registros", { exact: true })
    .fill("Filtro licencia");
  await page.getByRole("button", { name: "Buscar", exact: true }).click();
  const rows = page.locator("tbody tr");
  await expect(rows).toHaveCount(4);
  const pageSize = page.getByRole("combobox", {
    name: "Registros por página",
    exact: true,
  });
  await expect(pageSize).toHaveText("50");
  await page
    .getByRole("combobox", { name: "Cliente", exact: true })
    .fill("Filtro Alfa");
  await page
    .getByRole("option", { name: "Filtro Alfa Prueba", exact: true })
    .click();
  await expect(rows).toHaveCount(2);
  await page
    .getByRole("combobox", { name: "Estado de licencia", exact: true })
    .click();
  await page.getByRole("option", { name: "Bloqueadas", exact: true }).click();
  await expect(rows).toHaveCount(1);
  await expect(rows.first()).toContainText("Filtro licencia 2");
  await page
    .getByRole("button", { name: "Limpiar cliente", exact: true })
    .click();
  await expect(rows).toHaveCount(2);
  await expect(
    page.getByLabel("Buscar registros", { exact: true }),
  ).toHaveValue("Filtro licencia");
  for (const size of [25, 100, 50]) {
    const response = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        url.pathname.endsWith("/web/api/licenses") &&
        url.searchParams.get("size") === String(size) &&
        url.searchParams.get("status") === "BLOCKED" &&
        url.searchParams.get("keyword") === "Filtro licencia" &&
        !url.searchParams.has("clientId")
      );
    });
    await pageSize.click();
    await page.getByRole("option", { name: String(size), exact: true }).click();
    expect((await (await response).json()).size).toBe(size);
    await expect(rows).toHaveCount(2);
  }
  await page.mouse.move(0, 0);
  const background = await rows
    .first()
    .evaluate((row) => getComputedStyle(row).backgroundColor);
  await rows.first().hover();
  await expect
    .poll(() =>
      rows.first().evaluate((row) => getComputedStyle(row).backgroundColor),
    )
    .not.toBe(background);
  await page.mouse.move(0, 0);
  await page.screenshot({
    path: "../target/license-client-filters-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "../target/license-client-filters-mobile.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 1440, height: 900 });
  for (const [index, days] of [-1, 0, 1, 15, 16, 35, 35].entries()) {
    const date = new Date();
    date.setDate(date.getDate() + days);
    const lockDate = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    const license = await create("licenses", {
      clientId: first.id,
      companyId: 92001 + index,
      companyName: `Estado prueba ${index}`,
      description: "",
      initialActivity: "2026-01-01",
      lockDate,
      prePayment: true,
    });
    if (index === 6)
      expect(
        (
          await context.request.put(api + `api/licenses/${license.id}/lock`, {
            headers,
            data: { locked: true },
          })
        ).ok(),
      ).toBe(true);
  }
  await page
    .getByRole("combobox", { name: "Estado de licencia", exact: true })
    .click();
  await page.getByRole("option", { name: "Todas", exact: true }).click();
  await page
    .getByLabel("Buscar registros", { exact: true })
    .fill("Estado prueba");
  await page.getByRole("button", { name: "Buscar", exact: true }).click();
  await expect(rows).toHaveCount(7);
  const stateRow = (index: number) =>
    rows.filter({ hasText: `Estado prueba ${index}` });
  await expect(stateRow(5)).toContainText("(35 días)");
  await expect(stateRow(6)).not.toContainText("(35 días)");
  await expect(stateRow(0)).toContainText("venció hace 1 día");
  await expect(stateRow(1)).toContainText("vence hoy");
  for (const index of [2, 3])
    await expect(
      stateRow(index)
        .locator('[data-pc-name="tag"]')
        .filter({ hasText: "Por vencer" }),
    ).toHaveClass(/p-tag-danger/);
  await expect(stateRow(4).getByText("Activa", { exact: true })).toBeVisible();
  await expect(
    stateRow(0).locator('[data-pc-name="tag"]').filter({ hasText: "Vencida" }),
  ).toHaveClass(/p-tag-warn/);
  const palette = await page.evaluate(() => {
    const color = (className: string) => {
      const probe = document.createElement("div");
      probe.className = className;
      document.body.append(probe);
      const value = getComputedStyle(probe).backgroundColor;
      probe.remove();
      return value;
    };
    return { danger: color("bg-red-100!"), warning: color("bg-amber-100!") };
  });
  await page.mouse.move(0, 0);
  await expect
    .poll(() =>
      stateRow(6).evaluate((row) => getComputedStyle(row).backgroundColor),
    )
    .toBe(palette.danger);
  for (const index of [0, 1])
    await expect
      .poll(() =>
        stateRow(index).evaluate(
          (row) => getComputedStyle(row).backgroundColor,
        ),
      )
      .toBe(palette.warning);
  await page.screenshot({
    path: "../target/license-alerts-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "../target/license-alerts-mobile.png",
    fullPage: true,
  });
});
