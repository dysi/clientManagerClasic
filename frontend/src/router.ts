import { createRouter, createWebHashHistory } from "vue-router";
import { session } from "./features/auth/session";
import { loginLocation, returnPath } from "./features/auth/navigation";
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", redirect: "/clients" },
    {
      path: "/login",
      component: () => import("./features/auth/LoginPage.vue"),
    },
    {
      path: "/clients",
      component: () => import("./features/clients/ClientsPage.vue"),
    },
    {
      path: "/clients/new",
      component: () => import("./features/clients/ClientFormPage.vue"),
    },
    {
      path: "/clients/:id/edit",
      component: () => import("./features/clients/ClientFormPage.vue"),
    },
    {
      path: "/clients/:id",
      component: () => import("./features/clients/ClientDetailPage.vue"),
    },
    {
      path: "/licenses",
      component: () => import("./features/licenses/LicensesPage.vue"),
    },
    {
      path: "/licenses/new",
      component: () => import("./features/licenses/LicenseFormPage.vue"),
    },
    {
      path: "/licenses/:id/edit",
      component: () => import("./features/licenses/LicenseFormPage.vue"),
    },
    {
      path: "/payments",
      component: () => import("./features/payments/PaymentsPage.vue"),
    },
    {
      path: "/payments/new",
      component: () => import("./features/payments/PaymentFormPage.vue"),
    },
    {
      path: "/payments/:id/edit",
      component: () => import("./features/payments/PaymentFormPage.vue"),
    },
    {
      path: "/users",
      component: () => import("./features/users/UsersPage.vue"),
    },
    {
      path: "/users/new",
      component: () => import("./features/users/UserFormPage.vue"),
    },
    {
      path: "/users/:id/edit",
      component: () => import("./features/users/UserFormPage.vue"),
    },
    {
      path: "/users/:id/password",
      component: () => import("./features/users/PasswordPage.vue"),
    },
    { path: "/:pathMatch(.*)*", redirect: "/clients" },
  ],
});
router.beforeEach((to) => {
  if (to.path !== "/login" && !session.user) return loginLocation(to.fullPath);
  if (to.path === "/login" && session.user)
    return returnPath(to.query.redirect, router);
});
