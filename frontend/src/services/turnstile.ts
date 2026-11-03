interface TurnstileOptions {
  sitekey: string;
  action: string;
  language: string;
  size: "flexible";
  callback: (token: string) => void;
  "expired-callback": () => void;
  "error-callback": () => void;
}
export interface TurnstileApi {
  render(container: HTMLElement, options: TurnstileOptions): string;
  remove(id: string): void;
}
declare global {
  interface Window { turnstile?: TurnstileApi }
}
let pending: Promise<TurnstileApi> | undefined;

/** Carga explícita compartida por las dos rutas de ingreso de la SPA. */
export function loadTurnstile(): Promise<TurnstileApi> {
  if (window.turnstile) return Promise.resolve(window.turnstile);
  if (pending) return pending;
  pending = new Promise<TurnstileApi>((resolve, reject) => {
    const script = document.createElement("script");
    const timeout = window.setTimeout(() => fail(), 15000);
    function fail() {
      window.clearTimeout(timeout);
      script.remove();
      pending = undefined;
      reject(new Error("No se pudo cargar la verificación de seguridad. Volvé a intentar."));
    }
    script.src = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";
    script.async = true;
    script.onload = () => {
      window.clearTimeout(timeout);
      if (window.turnstile) resolve(window.turnstile);
      else fail();
    };
    script.onerror = fail;
    document.head.append(script);
  });
  return pending;
}
