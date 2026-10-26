<script setup lang="ts">
import Message from "primevue/message";
defineProps<{
  id: string;
  label: string;
  error?: string;
  hint?: string;
  required?: boolean;
}>();
</script>
<template>
  <div class="flex min-w-0 flex-col gap-2">
    <label :for="id" class="font-medium text-color"
      >{{ label }}<span v-if="required" aria-hidden="true"> *</span></label
    >
    <slot
      :invalid="!!error"
      :describedby="error ? `${id}-error` : hint ? `${id}-hint` : undefined"
    />
    <Message
      v-if="error"
      :id="`${id}-error`"
      severity="error"
      size="small"
      variant="simple"
      >{{ error }}</Message
    >
    <small v-else-if="hint" :id="`${id}-hint`" class="text-muted-color">{{
      hint
    }}</small>
  </div>
</template>
