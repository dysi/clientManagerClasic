import { test, expect } from "@playwright/test";

test("entrada por el puerto, bienvenida, destino solicitado y sesión vencida", async ({
  page,
  baseURL,
}) => {
  test.setTimeout(60_000);
  page.setDefaultTimeout(10_000);
  const username = process.env.E2E_USER;
  const password = process.env.E2E_PASSWORD;
  if (!username || !password || !baseURL)
    throw new Error(
      "Configurar E2E_BASE_URL, E2E_USER y E2E_PASSWORD con una base aislada.",
    );
  const entry = new URL(baseURL).origin + "/";
  const welcome = page.getByRole("heading", {
    name: "Te damos la bienvenida",
    exact: true,
  });
  const login = async () => {
    await page.getByLabel(/^Usuario/).fill(username);
    await page.getByLabel(/^Contraseña/).fill(password);
    await page
      .getByRole("button", { name: "Iniciar sesión", exact: true })
      .click();
  };
  await page.goto(entry);
  await expect(welcome).toBeVisible();
  await expect(page).toHaveURL(/\/ui\/#\/access$/);
  await page.getByRole("link", { name: "Ingreso para usuarios", exact: true }).click();
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.screenshot({
    path: "../target/portal-login-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: "../target/portal-login-mobile.png",
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page
    .getByRole("button", { name: "¿Necesitás ayuda para ingresar?" })
    .click();
  await expect(page.getByText(/Si olvidaste tu contraseña/)).toBeVisible();
  await page.setViewportSize({ width: 1440, height: 1000 });

  await page.goto(baseURL + "#/licenses?clientId=100");
  await expect(welcome).toBeVisible();
  await expect(page).toHaveURL(/redirect=/);
  const requestedLogin = page.url();
  await page.getByLabel(/^Usuario/).fill(username);
  await page.getByLabel(/^Contraseña/).fill("incorrect-password");
  await page
    .getByRole("button", { name: "Iniciar sesión", exact: true })
    .click();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page).toHaveURL(requestedLogin);
  await login();
  await expect(
    page.getByRole("heading", { name: "Licencias", exact: true }),
  ).toBeVisible();
  await expect(page).toHaveURL(/#\/licenses\?clientId=100$/);
  await page.goto(entry);
  await expect(
    page.getByRole("heading", { name: "Licencias", exact: true }),
  ).toBeVisible();
  await page.goto(baseURL + "#/login");
  await expect(
    page.getByRole("heading", { name: "Licencias", exact: true }),
  ).toBeVisible();

  // Simulate server-side expiration while the browser is on a protected page.
  await page.goto(baseURL + "#/users");
  await expect(
    page.getByRole("heading", { name: "Usuarios", exact: true }),
  ).toBeVisible();
  const loggedOut = await page.evaluate(async () => {
    const context = location.pathname.replace(/\/ui\/$/, "");
    const csrf = await (await fetch(context + "/web/session/csrf")).json();
    return (
      await fetch(context + "/web/session/logout", {
        method: "POST",
        headers: { [csrf.headerName]: csrf.token },
      })
    ).status;
  });
  expect(loggedOut).toBe(204);
  await page.getByRole("button", { name: "Buscar", exact: true }).click();
  await expect(welcome).toBeVisible();
  await expect(page).toHaveURL(/redirect=.*users/);
  await login();
  await expect(
    page.getByRole("heading", { name: "Usuarios", exact: true }),
  ).toBeVisible();
});
