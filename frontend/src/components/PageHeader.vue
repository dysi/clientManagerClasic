<script setup lang="ts">
import Toolbar from "primevue/toolbar";
defineProps<{ title: string; description?: string }>();
</script>
<template>
  <Toolbar class="mb-6 gap-4 border-0 bg-transparent p-0">
    <template #start
      ><div>
        <h1 class="text-2xl font-semibold text-color">{{ title }}</h1>
        <p v-if="description" class="mt-2 text-muted-color">
          {{ description }}
        </p>
      </div></template
    >
    <template #end
      ><div class="flex flex-wrap gap-2"><slot /></div
    ></template>
  </Toolbar>
</template>
