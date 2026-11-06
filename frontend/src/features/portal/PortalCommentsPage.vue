<script setup lang="ts">
import { ref } from "vue";
import { useRoute } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Message from "primevue/message";
import Skeleton from "primevue/skeleton";
import Textarea from "primevue/textarea";
import FormField from "../../components/FormField.vue";
import PageHeader from "../../components/PageHeader.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import { useDraft } from "../../composables/useDraft";
import { usePagedList } from "../../composables/usePagedList";
import { request } from "../../services/http";
import { displayDateTime } from "../../services/format";
import type { PortalComment } from "../../services/portal";
const props = defineProps<{ paymentId?: number }>();
const emit = defineEmits<{ changed: [] }>();
const id = props.paymentId ?? Number(useRoute().params.id);
const endpoint = `/web/portal/payments/${id}/comments`;
const { rows, loading, error: listError, page, size, total, load, paginate } = usePagedList<PortalComment>(endpoint);
const form = ref({ text: "" });
const { saving, error, fields, save, reset } = useDraft(form);
async function submit() {
  const result = await save(() => request<PortalComment>(endpoint, { method: "POST", body: JSON.stringify({ text: form.value.text.trim() }) }), "Se agregó el comentario público.");
  if (result) { form.value.text = ""; reset(); await load(); emit("changed"); }
}
</script>
<template>
  <PageHeader v-if="paymentId == null" :title="`Comentarios del pago #${id}`" description="Solo se muestra la información pública del pago.">
    <Button as="router-link" to="/portal" label="Volver al portal" text />
  </PageHeader>
  <Message v-if="listError" severity="error" role="alert">{{ listError }}</Message>
  <template v-else>
    <Skeleton v-if="loading" height="8rem" />
    <div v-else class="flex flex-col gap-4">
      <Message v-if="!rows.length" severity="info" :closable="false">Todavía no hay comentarios públicos.</Message>
      <Card v-for="comment in rows" :key="comment.id"><template #subtitle>{{ displayDateTime(comment.occurredAt) }}</template>
        <template #content><p class="whitespace-pre-wrap break-words">{{ comment.text }}</p></template>
      </Card>
      <ListPaginator :page="page" :size="size" :total="total" @change="paginate" />
    </div>
    <Card class="mt-6"><template #title>Agregar comentario público</template><template #content>
      <form class="flex flex-col gap-4" :aria-busy="saving" @submit.prevent="submit">
        <Message v-if="error" severity="error" role="alert">{{ error }}</Message>
        <FormField id="text" label="Comentario" required hint="No podrás editarlo ni borrarlo después de enviarlo." :error="fields.text" v-slot="field">
          <Textarea id="text" v-model="form.text" rows="4" maxlength="4000" required :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
        </FormField>
        <div class="flex justify-end"><Button type="submit" label="Enviar comentario" :loading="saving" :disabled="!form.text.trim()" /></div>
      </form>
    </template></Card>
  </template>
</template>
