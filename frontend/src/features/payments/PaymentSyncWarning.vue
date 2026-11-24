<script setup lang="ts">
import Message from "primevue/message";
defineProps<{ synchronizedAt?: string | null }>();
</script>
<template>
  <Message v-if="synchronizedAt" severity="warn" :closable="false" role="alert">Este pago ya fue sincronizado con AngryBox. Los cambios que realices en este pago no se reflejarán en el sistema remoto.</Message>
</template>
