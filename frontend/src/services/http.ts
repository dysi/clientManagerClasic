export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public fields: Record<string, string> = {},
  ) {
    super(message);
  }
}

// The production path is supplied by Spring's context, not a hard-coded hostname.
export const contextPath = import.meta.env.DEV
  ? "/clientmanager"
  : window.location.pathname.replace(/\/ui(?:\/index\.html)?\/?$/, "");
let csrf: { headerName: string; token: string } | null = null;

export async function refreshCsrf() {
  const response = await fetch(`${contextPath}/web/session/csrf`, {
    credentials: "same-origin",
    cache: "no-store",
  });
  if (!response.ok)
    throw new ApiError(
      response.status,
      "No se pudo preparar la sesión. Volvé a intentar.",
    );
  csrf = await response.json();
}

export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  // El navegador genera el boundary de multipart; JSON conserva el contrato anterior.
  if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
  if (
    !["GET", "HEAD", "OPTIONS"].includes(
      (options.method ?? "GET").toUpperCase(),
    )
  ) {
    if (!csrf) await refreshCsrf();
    headers.set(csrf!.headerName, csrf!.token);
  }
  let response: Response;
  try {
    response = await fetch(`${contextPath}${path}`, {
      ...options,
      credentials: "same-origin",
      headers,
      cache: "no-store",
    });
  } catch {
    throw new ApiError(
      0,
      "No hay conexión con el servidor. Tus cambios siguen en el formulario.",
    );
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    if (response.status === 401)
      window.dispatchEvent(new Event("session-expired"));
    throw new ApiError(
      response.status,
      error.message ||
        (response.status === 403
          ? "No tenés permiso para esta operación. Actualizá la página si venció la sesión."
          : "No se pudo completar la operación."),
      error.fields || {},
    );
  }
  return response.status === 204 ? (undefined as T) : response.json();
}
