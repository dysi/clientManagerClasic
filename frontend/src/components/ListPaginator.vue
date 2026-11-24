<script setup lang="ts">
import Paginator from "primevue/paginator";
withDefaults(defineProps<{ page: number; size: number; total: number; pageSizes?: number[] }>(), {
  pageSizes: () => [25, 50, 100],
});
defineEmits<{ change: [page: number, size: number] }>();
</script>
<template>
  <Paginator
    :first="page * size"
    :rows="size"
    :total-records="total"
    :rows-per-page-options="pageSizes"
    template="PrevPageLink CurrentPageReport NextPageLink RowsPerPageDropdown"
    current-page-report-template="{first}–{last} de {totalRecords}"
    @page="$emit('change', $event.page, $event.rows)"
  />
</template>
