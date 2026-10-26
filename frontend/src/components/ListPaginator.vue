<script setup lang="ts">
import Paginator from "primevue/paginator";
defineProps<{ page: number; size: number; total: number }>();
defineEmits<{ change: [page: number, size: number] }>();
</script>
<template>
  <Paginator
    :first="page * size"
    :rows="size"
    :total-records="total"
    :rows-per-page-options="[15, 30, 50]"
    template="PrevPageLink CurrentPageReport NextPageLink RowsPerPageDropdown"
    current-page-report-template="{first}–{last} de {totalRecords}"
    @page="$emit('change', $event.page, $event.rows)"
  />
</template>
