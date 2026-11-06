import { test, expect } from "@playwright/test";

test("administración completa con formularios separados y sesión segura", async ({
  page,
  context,
}) => {
  test.setTimeout(120_000);
  page.setDefaultTimeout(15_000);
  const username = process.env.E2E_USER;
  const password = process.env.E2E_PASSWORD;
  if (!username || !password)
    throw new Error(
      "Configurar E2E_USER y E2E_PASSWORD contra una base aislada.",
    );
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  const confirm = async () => {
    await page
      .getByRole("alertdialog")
      .getByRole("button", { name: "Confirmar", exact: true })
      .click();
  };
  const heading = (name: string) =>
    page.getByRole("heading", { name, exact: true });
  const link = (name: string) => page.getByRole("link", { name, exact: true });
  async function captureModule(name: string) {
    await expect(page.locator(".p-toast-message")).toHaveCount(0, {
      timeout: 10_000,
    });
    await expect(page.locator(".p-datatable-loading-overlay")).toHaveCount(0);
    await page.screenshot({
      path: `../target/frontend-${name}.png`,
      fullPage: true,
    });
    await page.setViewportSize({ width: 390, height: 844 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBe(true);
    await page.screenshot({
      path: `../target/frontend-${name}-mobile.png`,
      fullPage: true,
    });
    await page.setViewportSize({ width: 1440, height: 900 });
  }
  await page.goto("./");
  await expect(heading("Te damos la bienvenida")).toBeVisible();
  await link("Ingreso para usuarios").click();
  await page.screenshot({
    path: "../target/frontend-login.png",
    fullPage: true,
  });
  await page.getByLabel(/^Usuario/).fill(username);
  await page.getByLabel(/^Contraseña/).fill(password);
  await page.getByRole("button", { name: "Iniciar sesión" }).click();
  await expect(heading("Licencias")).toBeVisible();
  await page.getByRole("menuitem", { name: "Clientes", exact: true }).click();
  await expect(heading("Clientes")).toBeVisible();
  await page.getByRole("link", { name: "Saltar al contenido" }).focus();
  await page.keyboard.press("Enter");
  await expect(page.locator("#main")).toBeFocused();
  await expect(page).toHaveURL(/#\/clients$/);
  expect(await page.evaluate(() => localStorage.getItem("token"))).toBeNull();
  const cookie = (await context.cookies()).find(
    (cookie) => cookie.name === "JSESSIONID",
  );
  expect(cookie?.httpOnly).toBe(true);
  expect(cookie?.sameSite).toBe("Lax");
  await link("Nuevo cliente").click();
  await page.locator("#firstname").fill("Ana E2E");
  await page.getByLabel(/^Apellido/).fill("Verificación");
  await page
    .getByLabel("Correo electrónico", { exact: true })
    .fill("ana.e2e@example.test");
  await page.getByLabel("Teléfono", { exact: true }).fill("3415551234");
  await page.getByLabel("CUIL / CUIT", { exact: true }).fill("20123456789");
  await page.getByRole("button", { name: "Guardar cliente" }).click();
  await expect(heading("Ana E2E Verificación")).toBeVisible();
  await link("Editar datos").click();
  await page.locator("#firstname").fill("Nombre descartado");
  await page.getByRole("button", { name: "Cancelar", exact: true }).click();
  await confirm();
  await expect(heading("Ana E2E Verificación")).toBeVisible();
  await link("Editar datos").click();
  await page.getByLabel(/^Apellido/).fill("Actualizada");
  await page.getByRole("button", { name: "Guardar cliente" }).click();
  await expect(heading("Ana E2E Actualizada")).toBeVisible();
  await page.reload();
  await expect(heading("Ana E2E Actualizada")).toBeVisible();
  await expect(page.getByText("3415551234", { exact: true })).toBeVisible();
  await page.screenshot({
    path: "../target/frontend-clients-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: "../target/frontend-clients-mobile.png",
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await page.setViewportSize({ width: 1440, height: 900 });

  await link("Nueva licencia").click();
  await expect(
    page.getByRole("combobox", { name: "Cliente", exact: true }),
  ).toHaveValue("Ana E2E Actualizada");
  await page.getByLabel(/^Identificador de empresa/).fill("987654");
  await page.getByLabel(/^Nombre de empresa/).fill("Empresa E2E");
  await page
    .getByLabel("Descripción", { exact: true })
    .fill("Licencia de verificación");
  await page.getByRole("button", { name: "Guardar licencia" }).click();
  await expect(heading("Licencias")).toBeVisible();
  await expect(
    page.getByRole("cell", { name: /Empresa E2E/ }).first(),
  ).toBeVisible();
  await page.getByRole("button", { name: "Bloquear", exact: true }).click();
  await confirm();
  await expect(page.getByText("Bloqueada", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Desbloquear", exact: true }).click();
  await confirm();
  await expect(page.getByText("Activa", { exact: true })).toBeVisible();
  await link("Editar").click();
  await expect(page.getByLabel(/^Nombre de empresa/)).toHaveValue(
    "Empresa E2E",
  );
  await page
    .getByLabel("Descripción", { exact: true })
    .fill("Licencia actualizada");
  await page.getByRole("button", { name: "Guardar licencia" }).click();
  await expect(heading("Licencias")).toBeVisible();
  await captureModule("licenses");

  await page
    .getByRole("table")
    .getByRole("link", { name: "Pagos", exact: true })
    .click();
  await expect(heading("Pagos")).toBeVisible();
  await expect(page.getByRole("group", { name: "Estados del pago", exact: true })).toBeVisible();
  await expect(page.getByLabel("Emisión desde", { exact: true })).toBeVisible();
  await expect(page.getByLabel("Emisión hasta", { exact: true })).toBeVisible();
  await expect(link("Nuevo pago")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Anular", exact: true })).toHaveCount(0);
  await captureModule("payments");

  await page.getByRole("menuitem", { name: "Usuarios", exact: true }).click();
  await link("Nuevo usuario").click();
  await page.getByLabel(/^Nombre de usuario/).fill("e2e-user");
  await page.locator("#firstname").fill("Nombre E2E");
  await page.getByLabel(/^Apellido/).fill("Apellido E2E");
  await page.getByLabel(/^Correo electrónico/).fill("user.e2e@example.test");
  await page.getByLabel("Cliente vinculado", { exact: true }).fill("Ana E2E");
  await page
    .getByRole("option", { name: "Ana E2E Actualizada", exact: true })
    .click();
  await expect(
    page.getByLabel("Cliente vinculado", { exact: true }),
  ).toHaveValue("Ana E2E Actualizada");
  await page.getByLabel(/^Contraseña inicial/).fill("InitialPassword123!");
  await page.getByRole("button", { name: "Guardar usuario" }).click();
  await expect(heading("Usuarios")).toBeVisible();
  let userRow = page.getByRole("row").filter({ hasText: "e2e-user" });
  await expect(
    userRow.getByText("Ana E2E Actualizada", { exact: true }),
  ).toBeVisible();
  await userRow.getByRole("link", { name: "Editar", exact: true }).click();
  await expect(page.locator("#initialPassword")).toHaveCount(0);
  await page.locator("#firstname").fill("Nombre actualizado");
  await page.getByRole("button", { name: "Guardar usuario" }).click();
  await expect(heading("Usuarios")).toBeVisible();
  userRow = page.getByRole("row").filter({ hasText: "e2e-user" });
  await userRow.getByRole("link", { name: "Cambiar contraseña" }).click();
  await page.getByLabel(/^Nueva contraseña/).fill("ReplacedPassword123!");
  await page
    .getByLabel(/^Repetir nueva contraseña/)
    .fill("ReplacedPassword123!");
  await page.getByRole("button", { name: "Actualizar contraseña" }).click();
  await expect(heading("Usuarios")).toBeVisible();
  await captureModule("users");
  await page
    .getByRole("button", { name: "Cerrar sesión", exact: true })
    .click();
  await confirm();
  await expect(heading("Te damos la bienvenida")).toBeVisible();
  await page.getByLabel(/^Usuario/).fill("e2e-user");
  await page.getByLabel(/^Contraseña/).fill("ReplacedPassword123!");
  await page.getByRole("button", { name: "Iniciar sesión" }).click();
  await expect(page).toHaveURL(/#\/portal$/);
  await expect(heading("Cuenta de Ana E2E Actualizada")).toBeVisible();
  await expect(page.getByRole("menuitem", { name: "Usuarios", exact: true })).toHaveCount(0);
  expect(errors).toEqual([]);
});
