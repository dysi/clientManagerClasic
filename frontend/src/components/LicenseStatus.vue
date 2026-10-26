<script setup lang="ts">
import Tag from "primevue/tag";
defineProps<{ status: string }>();
const states: Record<
  string,
  { label: string; severity: "success" | "warn" | "danger" | "secondary" }
> = {
  ACTIVE: { label: "Activa", severity: "success" },
  EXPIRING: { label: "Por vencer", severity: "warn" },
  EXPIRED: { label: "Vencida", severity: "danger" },
  BLOCKED: { label: "Bloqueada", severity: "danger" },
  UNKNOWN: { label: "Sin fecha", severity: "secondary" },
};
</script>
<template>
  <Tag
    :value="(states[status] || states.UNKNOWN)!.label"
    :severity="(states[status] || states.UNKNOWN)!.severity"
  />
</template>
