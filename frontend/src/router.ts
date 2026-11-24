import { createRouter, createWebHashHistory } from "vue-router";
import { session } from "./features/auth/session";
import {
  homePath,
  entryPath,
  sessionHome,
  isPortalPath,
  loginLocation,
  returnPath,
} from "./features/auth/navigation";
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", redirect: entryPath },
    { path: "/portal-access", component: () => import("./features/auth/PortalAccessPage.vue") },
    {
      path: "/access",
      component: () => import("./features/auth/LoginPage.vue"),
      props: { documentMode: true },
    },
    { path: "/portal", component: () => import("./features/portal/PortalWelcomePage.vue") },
    { path: "/portal/payments/new", component: () => import("./features/portal/PortalPaymentPage.vue") },
    { path: "/portal/payments/:id", component: () => import("./features/portal/PortalPaymentDetailPage.vue") },
    { path: "/portal/payments/:id/comments", component: () => import("./features/portal/PortalCommentsPage.vue") },
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
      path: "/license-query-log",
      component: () => import("./features/licenses/LicenseQueryLogPage.vue"),
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
      path: "/payments/:id",
      component: () => import("./features/payments/PaymentReviewPage.vue"),
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
    { path: "/parameters", component: () => import("./features/parameters/ParametersPage.vue") },
    { path: "/parameters/banks", component: () => import("./features/parameters/BanksPage.vue") },
    { path: "/parameters/banks/new", component: () => import("./features/parameters/BankFormPage.vue") },
    { path: "/parameters/banks/:id/edit", component: () => import("./features/parameters/BankFormPage.vue") },
    { path: "/parameters/bank-accounts", component: () => import("./features/parameters/BankAccountsPage.vue") },
    { path: "/parameters/bank-accounts/new", component: () => import("./features/parameters/BankAccountFormPage.vue") },
    { path: "/parameters/bank-accounts/:id/edit", component: () => import("./features/parameters/BankAccountFormPage.vue") },
    { path: "/:pathMatch(.*)*", redirect: entryPath },
  ],
});
router.beforeEach((to) => {
  if (to.path === "/portal-access") return;
  const publicEntry = to.path === "/login" || to.path === "/access";
  if (!publicEntry && !session.user) return loginLocation(to.fullPath);
  if (publicEntry && session.user)
    return returnPath(to.query.redirect, router);
  if (session.user?.access === "PORTAL" && !isPortalPath(to.path)) return sessionHome();
  if (session.user?.access === "ADMIN" && isPortalPath(to.path)) return homePath;
});
