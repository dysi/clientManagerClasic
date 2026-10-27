<script setup lang="ts">
import { useRoute } from "vue-router";
import Menu from "primevue/menu";
defineProps<{ collapsed?: boolean }>();
const route = useRoute();
const navigation = [
  { label: "Clientes", icon: "pi pi-address-book", route: "/clients" },
  { label: "Licencias", icon: "pi pi-key", route: "/licenses" },
  { label: "Pagos", icon: "pi pi-wallet", route: "/payments" },
  { label: "Usuarios", icon: "pi pi-users", route: "/users" },
  { label: "Parámetros", icon: "pi pi-cog", route: "/parameters" },
];
</script>

<template>
  <nav aria-label="Navegación administrativa">
    <Menu :model="navigation" aria-label="Secciones administrativas" class="w-full min-w-0 border-0 bg-transparent">
      <template #item="{ item, props }">
        <RouterLink :to="item.route" v-bind="props.action"
          :aria-current="route.path.startsWith(item.route) ? 'page' : undefined"
          :title="collapsed && typeof item.label === 'string' ? item.label : undefined"
          class="rounded-border"
          :class="[collapsed ? 'justify-center' : '', route.path.startsWith(item.route) ? 'bg-primary-50 font-semibold text-primary' : '']">
          <i :class="item.icon" aria-hidden="true" />
          <span :class="collapsed ? 'sr-only' : undefined">{{ item.label }}</span>
        </RouterLink>
      </template>
    </Menu>
  </nav>
</template>
