<script setup lang="ts">
import InputText from "primevue/inputtext";
import Button from "primevue/button";
import Toolbar from "primevue/toolbar";
const keyword = defineModel<string>({ required: true });
defineProps<{ loading: boolean; placeholder?: string; fullWidth?: boolean }>();
defineEmits<{ search: [] }>();
</script>
<template>
  <Toolbar class="mb-4 gap-4" :pt="fullWidth ? { start: { class: 'w-full min-w-0' }, end: { class: 'hidden' } } : undefined">
    <template #start
      ><form
        class="flex w-full flex-wrap gap-2"
        role="search"
        @submit.prevent="$emit('search')"
      >
        <label class="sr-only" for="record-search">Buscar registros</label
        ><InputText
          id="record-search"
          v-model="keyword"
          :placeholder="placeholder || 'Buscar…'"
          maxlength="150"
          :class="fullWidth ? 'min-w-0 flex-1' : 'w-full sm:w-72'"
        /><Button
          type="submit"
          label="Buscar"
          icon="pi pi-search"
          :loading="loading"
          :class="{ 'shrink-0': fullWidth }"
        /></form
    ></template>
    <template #end
      ><div class="flex flex-wrap items-center gap-2"><slot /></div
    ></template>
  </Toolbar>
</template>
