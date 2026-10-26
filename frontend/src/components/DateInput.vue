<script setup lang="ts">
import { computed } from "vue";
import DatePicker from "primevue/datepicker";
import { toDate, dateOnly } from "../services/format";
const props = defineProps<{
  modelValue: string | null;
  inputId: string;
  disabled?: boolean;
  invalid?: boolean;
  required?: boolean;
  describedby?: string;
}>();
const emit = defineEmits<{ "update:modelValue": [value: string | null] }>();
const value = computed(() => toDate(props.modelValue));
function update(date: Date | Date[] | (Date | null)[] | undefined | null) {
  emit("update:modelValue", date instanceof Date ? dateOnly(date) : null);
}
</script>
<template>
  <DatePicker
    :input-id="inputId"
    :model-value="value"
    date-format="dd/mm/yy"
    show-icon
    show-button-bar
    :disabled="disabled"
    :invalid="invalid"
    :input-props="{ required, 'aria-describedby': describedby }"
    fluid
    @update:model-value="update"
  />
</template>
