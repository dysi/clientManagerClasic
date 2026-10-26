import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, request, refreshCsrf } from "./http";

describe("Cliente HTTP con sesión", () => {
  beforeEach(() => vi.unstubAllGlobals());
  it("envía CSRF en las escrituras y no utiliza un Bearer del navegador", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf-test" }),
        ),
      )
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 1 })));
    vi.stubGlobal("fetch", fetch);
    await refreshCsrf();
    await request("/web/api/clients", { method: "POST", body: "{}" });
    const options = fetch.mock.calls[1]![1] as RequestInit;
    expect((options.headers as Headers).get("X-CSRF-TOKEN")).toBe("csrf-test");
    expect((options.headers as Headers).has("Authorization")).toBe(false);
    expect(options.credentials).toBe("same-origin");
  });
  it("distingue el vencimiento de sesión de un fallo de red", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ message: "Sesión vencida" }), {
          status: 401,
        }),
      ),
    );
    const expired = vi.fn();
    window.addEventListener("session-expired", expired);
    await expect(request("/web/session")).rejects.toMatchObject({
      status: 401,
    });
    expect(expired).toHaveBeenCalledOnce();
    window.removeEventListener("session-expired", expired);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new TypeError("Network error")),
    );
    await expect(request("/web/api/clients")).rejects.toBeInstanceOf(ApiError);
    await expect(request("/web/api/clients")).rejects.toMatchObject({
      status: 0,
    });
  });
});
