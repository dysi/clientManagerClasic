<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import Tag from "primevue/tag";
import Message from "primevue/message";
import PageHeader from "../../components/PageHeader.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import FormField from "../../components/FormField.vue";
import EntitySelect from "../../components/EntitySelect.vue";
import DateInput from "../../components/DateInput.vue";
import { usePagedList } from "../../composables/usePagedList";
import { displayDateTime, money } from "../../services/format";
import { paymentCounts, type Payment, type PaymentCounts } from "../../services/admin";
import { paymentLabels } from "./review";
const route = useRoute(), router = useRouter();
const licenseId = route.query.licenseId ? Number(route.query.licenseId) : undefined;
const clientId = ref<number | null>(route.query.clientId ? Number(route.query.clientId) : null);
const clientLabel = ref(typeof route.query.clientName === "string" ? route.query.clientName : "");
const bankAccountId = ref<number | null>(route.query.bankAccountId ? Number(route.query.bankAccountId) : null);
const bankAccountLabel = ref(typeof route.query.bankAccountName === "string" ? route.query.bankAccountName : "");
const states = [
  { label: "Pendientes", value: "PENDIENTE", count: "pending", severity: "warn" },
  { label: "Aprobados", value: "APROBADO", count: "approved", severity: "success" },
  { label: "Rechazados", value: "RECHAZADO", count: "rejected", severity: "danger" },
] as const;
const statusQuery = typeof route.query.status === "string" ? route.query.status.split(",") : [];
const selected = ref<string[]>(states.filter(state => statusQuery.includes(state.value)).map(state => state.value));
const from = ref<string | null>(typeof route.query.from === "string" ? route.query.from : null);
const to = ref<string | null>(typeof route.query.to === "string" ? route.query.to : null);
const { rows, loading, error, page, size, total, load } = usePagedList<Payment>("/web/api/payments", () => ({ status: selected.value.join(","), licenseId, clientId: clientId.value, bankAccountId: bankAccountId.value, from: from.value, to: to.value }));
page.value = /^\d+$/.test(String(route.query.page ?? "0")) ? Number(route.query.page ?? 0) : 0;
size.value = [25,50,100].includes(Number(route.query.size)) ? Number(route.query.size) : 50;
const counts = ref<PaymentCounts | null>(null);
const countsLoading = ref(false), countsError = ref("");
let countsSequence = 0;
onUnmounted(() => { countsSequence++; });
async function loadCounts() {
  const sequence = ++countsSequence;
  countsLoading.value = true;
  countsError.value = "";
  counts.value = null;
  const params = new URLSearchParams();
  if (licenseId != null) params.set("licenseId", String(licenseId));
  if (clientId.value != null) params.set("clientId", String(clientId.value));
  if (bankAccountId.value != null) params.set("bankAccountId", String(bankAccountId.value));
  if (from.value) params.set("from", from.value);
  if (to.value) params.set("to", to.value);
  try {
    const result = await paymentCounts(params);
    if (sequence === countsSequence) counts.value = result;
  } catch (failure) {
    if (sequence === countsSequence) countsError.value = failure instanceof Error ? failure.message : "No se pudieron cargar las cantidades.";
  } finally {
    if (sequence === countsSequence) countsLoading.value = false;
  }
}
onMounted(loadCounts);
function toggleStatus(value: string) {
  selected.value = selected.value.includes(value) ? selected.value.filter(state => state !== value) : [...selected.value, value];
  return navigate();
}
async function navigate(next = 0, count = size.value) {
  const query: Record<string, string> = {};
  if (bankAccountId.value != null) { query.bankAccountId = String(bankAccountId.value); query.bankAccountName = bankAccountLabel.value; }
  if (selected.value.length) query.status = states.filter(state => selected.value.includes(state.value)).map(state => state.value).join(",");
  if (clientId.value != null) { query.clientId = String(clientId.value); query.clientName = clientLabel.value; }
  if (licenseId != null) { query.licenseId = String(licenseId); if (typeof route.query.companyName === "string") query.companyName = route.query.companyName; }
  if (from.value) query.from = from.value; if (to.value) query.to = to.value;
  if (next) query.page = String(next); if (count !== 50) query.size = String(count);
  const target = router.resolve({ path: "/payments", query });
  if (target.fullPath === route.fullPath) { page.value = next; size.value = count; await Promise.all([load(), loadCounts()]); }
  else await router.replace(target.fullPath);
}
</script>
<template>
  <PageHeader title="Pagos" description="Consulta y revisión administrativa de pagos informados." />
  <Message v-if="licenseId" severity="info" class="mb-4">Pagos de {{ route.query.companyName || `la licencia #${licenseId}` }}. <RouterLink to="/payments" class="underline">Ver todos</RouterLink></Message>
  <Message v-if="error" severity="error" role="alert" class="mb-4">{{ error }}</Message>
  <Card><template #content>
    <div role="group" aria-label="Estados del pago" :aria-busy="countsLoading" class="mt-4 flex flex-wrap gap-2">
      <Button type="button" :label="`Todos (${counts?.total ?? '…'})`" severity="secondary" :outlined="selected.length > 0" :aria-pressed="selected.length === 0" @click="selected = []; navigate()" />
      <Button v-for="state in states" :key="state.value" type="button" :label="`${state.label} (${counts?.[state.count] ?? '…'})`" :severity="state.severity" :outlined="!selected.includes(state.value)" :aria-pressed="selected.includes(state.value)" @click="toggleStatus(state.value)" />
    </div>
    <p class="mt-2 text-sm text-muted-color">Podés combinar estados. Sin estados seleccionados se muestran todos.</p>
    <Message v-if="countsError" severity="error" role="alert" class="mt-2">{{ countsError }} <Button type="button" label="Reintentar cantidades" text size="small" @click="loadCounts" /></Message>
    <div class="my-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <FormField id="paymentClient" label="Cliente"><EntitySelect input-id="paymentClient" v-model="clientId" :label="clientLabel" kind="clients" @selected="clientLabel = $event?.label ?? ''" @update:model-value="navigate()" /><Button v-if="clientId != null" type="button" label="Quitar filtro de cliente" text size="small" @click="clientId = null; clientLabel = ''; navigate()" /></FormField>
      <FormField id="paymentBankAccount" label="Cuenta bancaria"><EntitySelect input-id="paymentBankAccount" v-model="bankAccountId" :label="bankAccountLabel" kind="payment-bank-accounts" @selected="bankAccountLabel = $event?.label ?? ''" @update:model-value="navigate()" /><Button v-if="bankAccountId != null" type="button" label="Quitar filtro de cuenta" text size="small" @click="bankAccountId = null; bankAccountLabel = ''; navigate()" /></FormField>
      <FormField id="paymentFrom" label="Emisión desde"><DateInput input-id="paymentFrom" v-model="from" @update:model-value="navigate()" /></FormField>
      <FormField id="paymentTo" label="Emisión hasta"><DateInput input-id="paymentTo" v-model="to" @update:model-value="navigate()" /></FormField>
    </div>
    <DataTable class="min-w-0 max-w-full" :value="rows" :loading="loading" data-key="id" row-hover striped-rows scrollable>
      <template #empty>No se encontraron pagos con estos filtros.</template>
      <Column field="id" header="Referencia" /><Column header="Cliente"><template #body="{ data }: { data: Payment }"><RouterLink :to="'/clients/' + data.clientId" class="text-primary underline">{{ data.clientName }}</RouterLink></template></Column>
      <Column header="Cuenta bancaria"><template #body="{ data }: { data: Payment }"><div>{{ data.destinationBank }} · {{ data.destinationHolder }}</div><div class="text-sm text-muted-color">{{ data.destinationAlias || data.destinationCbu }}</div></template></Column>
      <Column header="Licencia"><template #body="{ data }">{{ data.companyName || 'Sin licencia asociada' }}</template></Column>
      <Column header="Importe"><template #body="{ data }">{{ money(data.amount) }}</template></Column>
      <Column header="Emisión"><template #body="{ data }">{{ displayDateTime(data.issuedAt) }}</template></Column>
      <Column header="Estado"><template #body="{ data }: { data: Payment }"><Tag :value="paymentLabels[data.status]" :severity="data.status === 'APROBADO' ? 'success' : data.status === 'RECHAZADO' ? 'danger' : 'warn'" /></template></Column>
      <Column header="Resuelto por"><template #body="{ data }: { data: Payment }">{{ data.status === "PENDIENTE" ? "—" : data.decidedBy || "Sin información" }}</template></Column>
      <Column header="Sincronización"><template #body="{ data }: { data: Payment }"><Tag v-if="data.angryboxSynchronizedAt" value="Sincronizado" severity="success" icon="pi pi-bolt" :title="`sincronizado: ${displayDateTime(data.angryboxSynchronizedAt)}`" /></template></Column>
      <Column header="Detalle"><template #body="{ data }"><Button as="router-link" :to="{ path: '/payments/' + data.id, query: { returnTo: route.fullPath } }" label="Revisar pago" icon="pi pi-eye" text size="small" /></template></Column>
    </DataTable>
    <ListPaginator :page="page" :size="size" :total="total" @change="navigate" />
  </template></Card>
</template>
