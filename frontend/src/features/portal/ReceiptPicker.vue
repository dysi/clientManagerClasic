<script setup lang="ts">
import { computed, ref, watch } from "vue";
import Button from "primevue/button";
import FileUpload from "primevue/fileupload";
import Message from "primevue/message";
import Textarea from "primevue/textarea";
import type { ReceiptSelection } from "../../services/portal";
const props = defineProps<{ modelValue: ReceiptSelection[]; maxReceipts: number; maxFileBytes: number; disabled?: boolean }>();
const emit = defineEmits<{ "update:modelValue": [ReceiptSelection[]] }>();
const generation = ref(0);
const sizeMb = computed(() => (props.maxFileBytes / 1048576).toFixed(0));
const identity = (file: File) => JSON.stringify([file.name, file.size, file.lastModified]);
watch(() => props.modelValue.length, (count, previous) => { if (count === 0 && previous > 0) generation.value++; });
function select(value: unknown) {
  const files = Array.isArray(value) ? value.filter((file: unknown): file is File => file instanceof File) : [];
  for (const file of files) if ("objectURL" in file && typeof file.objectURL === "string") URL.revokeObjectURL(file.objectURL);
  emit("update:modelValue", files.map(file => ({ file, description: props.modelValue.find(item => identity(item.file) === identity(file))?.description ?? "" })));
}
function describe(file: File, description: string) {
  emit("update:modelValue", props.modelValue.map(item => identity(item.file) === identity(file) ? { ...item, description } : item));
}
</script>
<template>
  <p class="mb-3 text-sm text-muted-color">Hasta {{ maxReceipts }} archivos por envío. PDF, JPG o PNG de hasta {{ sizeMb }} MB cada uno. Descripción opcional de hasta 1000 caracteres por archivo.</p>
  <FileUpload :key="generation" name="receipts" accept=".pdf,.jpg,.jpeg,.png" multiple custom-upload
    :max-file-size="maxFileBytes" :file-limit="maxReceipts" :disabled="disabled" :show-upload-button="false" :show-cancel-button="false"
    choose-label="Elegir comprobantes" :invalid-file-size-message="'{0}: el archivo no puede superar los ' + sizeMb + ' MB.'"
    invalid-file-type-message="{0}: solo se admiten PDF, JPG y PNG." :invalid-file-limit-message="'No podés enviar más de ' + maxReceipts + ' comprobantes por vez. Quitá los excedentes.'"
    @select="select($event.files)" @remove="select($event.files)" @clear="select([])">
    <template #content="{ files, messages, removeFileCallback }">
      <Message v-for="message in messages" :key="message" severity="error" :closable="false" role="alert" class="mb-3">{{ message }}</Message>
      <Message v-if="files.length > maxReceipts" severity="error" :closable="false" role="alert" class="mb-3">Quitá los archivos excedentes: el máximo por envío es {{ maxReceipts }}.</Message>
      <ul class="flex flex-col gap-4">
        <li v-for="(file, index) in files" :key="identity(file)" class="min-w-0">
          <div class="flex min-w-0 items-center justify-between gap-3">
            <span class="min-w-0 break-all">{{ file.name }} <span class="text-sm text-muted-color">({{ (file.size / 1048576).toFixed(2) }} MB)</span></span>
            <Button type="button" icon="pi pi-times" :aria-label="'Quitar ' + file.name" text severity="secondary" :disabled="disabled" @click="removeFileCallback(index)" />
          </div>
          <Message v-if="file.size === 0" severity="error" :closable="false" role="alert">{{ file.name }} está vacío. Quitalo y elegí otro archivo.</Message>
          <label :for="'receipt-description-' + index" class="mb-1 block text-sm">Descripción de {{ file.name }} (opcional)</label>
          <Textarea :id="'receipt-description-' + index" :model-value="modelValue.find(item => identity(item.file) === identity(file))?.description ?? ''"
            rows="2" maxlength="1000" :disabled="disabled" fluid @update:model-value="describe(file, $event ?? '')" />
        </li>
      </ul>
      <p v-if="!files.length && !messages?.length" class="text-sm text-muted-color">Elegí los comprobantes o arrastralos hasta acá.</p>
    </template>
  </FileUpload>
</template>
