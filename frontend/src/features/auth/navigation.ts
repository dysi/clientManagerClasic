import type { Router } from "vue-router";
import { session } from "./session";

export const homePath = "/licenses";

export function isPortalPath(path: string): boolean {
  return path === "/portal" || path === "/portal/payments/new" || /^\/portal\/payments\/[1-9][0-9]*(?:\/comments)?$/.test(path);
}

export function sessionHome(): string {
  return session.user?.access === "PORTAL" ? "/portal" : homePath;
}

export function entryPath(): string {
  return session.user ? sessionHome() : "/access";
}

// Only known internal screens may be used as a post-login destination.
export function returnPath(value: unknown, router: Router): string {
  if (
    typeof value !== "string" ||
    !value.startsWith("/") ||
    value.startsWith("//")
  )
    return sessionHome();
  const destination = router.resolve(value);
  if (
    destination.path === "/login" ||
    destination.path === "/access" ||
    !destination.matched.length ||
    destination.matched.some((record) => record.redirect)
  )
    return sessionHome();
  if (session.user?.access === "PORTAL" && !isPortalPath(destination.path))
    return "/portal";
  if (session.user?.access === "ADMIN" && isPortalPath(destination.path))
    return homePath;
  return destination.fullPath;
}

export function loginLocation(path: string) {
  return {
    path: path.startsWith("/portal") ? "/access" : "/login",
    query: path === homePath ? {} : { redirect: path },
    replace: true,
  };
}
