import type { Router } from "vue-router";

export const homePath = "/clients";

// Only known internal screens may be used as a post-login destination.
export function returnPath(value: unknown, router: Router): string {
  if (
    typeof value !== "string" ||
    !value.startsWith("/") ||
    value.startsWith("//")
  )
    return homePath;
  const destination = router.resolve(value);
  if (
    destination.path === "/login" ||
    !destination.matched.length ||
    destination.matched.some((record) => record.redirect)
  )
    return homePath;
  return destination.fullPath;
}

export function loginLocation(path: string) {
  return {
    path: "/login",
    query: path === homePath ? {} : { redirect: path },
    replace: true,
  };
}
