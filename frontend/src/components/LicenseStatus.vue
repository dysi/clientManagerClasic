<script setup lang="ts">
import Tag from "primevue/tag";
defineProps<{ status: string }>();
const states: Record<
  string,
  { label: string; severity: "success" | "warn" | "danger" | "secondary" }
> = {
  ACTIVE: { label: "Activa", severity: "success" },
  EXPIRING: { label: "Por vencer", severity: "danger" },
  EXPIRED: { label: "Vencida", severity: "warn" },
  BLOCKED: { label: "Bloqueada", severity: "danger" },
  UNKNOWN: { label: "Sin fecha", severity: "secondary" },
};
</script>
<template>
  <Tag
    :value="(states[status] || states.UNKNOWN)!.label"
    :severity="(states[status] || states.UNKNOWN)!.severity"
    :dt="{
      colorScheme: {
        light: { warn: { background: '{amber.200}', color: '{amber.950}' } },
      },
    }"
  />
</template>
