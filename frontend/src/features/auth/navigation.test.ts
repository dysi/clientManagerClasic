import { afterEach, describe, expect, it } from "vitest";
import { router } from "../../router";
import { session } from "./session";
import { returnPath } from "./navigation";

afterEach(() => {
  session.user = null;
});
describe("Navegación de acceso", () => {
  it("envía la entrada anónima al login y vuelve a la pantalla principal con sesión", async () => {
    await router.push("/");
    expect(router.currentRoute.value.path).toBe("/login");
    session.user = { username: "admin", roles: ["ROLE_ADMIN"] };
    await router.replace(
      returnPath(router.currentRoute.value.query.redirect, router),
    );
    await router.push("/login");
    expect(router.currentRoute.value.path).toBe("/licenses");
  });
  it("conserva la pantalla protegida y sus filtros al iniciar sesión", async () => {
    await router.push("/payments?licenseId=7");
    expect(router.currentRoute.value.path).toBe("/login");
    expect(router.currentRoute.value.query.redirect).toBe(
      "/payments?licenseId=7",
    );
    session.user = { username: "admin", roles: ["ROLE_ADMIN"] };
    await router.replace(
      returnPath(router.currentRoute.value.query.redirect, router),
    );
    expect(router.currentRoute.value.fullPath).toBe("/payments?licenseId=7");
  });
  it.each([
    "https://example.com",
    "//example.com",
    "/login",
    "/no-existe",
    undefined,
    ["/users"],
  ])("rechaza destinos externos, inválidos o circulares: %s", (value) => {
    expect(returnPath(value, router)).toBe("/licenses");
  });
});
