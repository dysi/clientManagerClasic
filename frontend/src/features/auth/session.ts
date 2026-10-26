import { reactive } from "vue";
import { ApiError, refreshCsrf, request } from "../../services/http";

export interface SessionUser {
  username: string;
  roles: string[];
}
export const session = reactive<{ user: SessionUser | null; ready: boolean }>({
  user: null,
  ready: false,
});

export async function loadSession() {
  try {
    session.user = await request<SessionUser>("/web/session");
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) throw error;
    session.user = null;
  } finally {
    session.ready = true;
  }
}

export async function signIn(username: string, password: string) {
  await refreshCsrf();
  session.user = await request<SessionUser>("/web/session/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  await refreshCsrf();
}

export async function signOut() {
  await request("/web/session/logout", { method: "POST" });
  session.user = null;
  await refreshCsrf();
}
