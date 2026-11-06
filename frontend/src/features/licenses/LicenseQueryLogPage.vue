<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Column from "primevue/column";
import DataTable from "primevue/datatable";
import InputNumber from "primevue/inputnumber";
import InputText from "primevue/inputtext";
import Message from "primevue/message";
import Select from "primevue/select";
import Tag from "primevue/tag";
import PageHeader from "../../components/PageHeader.vue";
import FormField from "../../components/FormField.vue";
import DateInput from "../../components/DateInput.vue";
import ListToolbar from "../../components/ListToolbar.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import { usePagedList } from "../../composables/usePagedList";
import { licenseQueryLog, type LicenseQueryEntry, type LicenseQuerySummary, type LicenseQueryOrigin } from "../../services/licenseQueryLog";

const route = useRoute();
const initialCompany = Number(route.query.companyId);
const companyId = ref<number | null>(Number.isSafeInteger(initialCompany) && initialCompany > 0 ? initialCompany : null);
const ip = ref("");
const from = ref<string | null>(null);
const to = ref<string | null>(null);
const result = ref("ALL");
const version = ref("ALL");
const searchDraft = ref("");
const results = [{ label: "Todos", value: "ALL" }, { label: "Exitosos (2xx)", value: "SUCCESS" }, { label: "Errores (4xx/5xx)", value: "ERROR" }];
const versions = [{ label: "Todas", value: "ALL" }, { label: "Actual (v1)", value: "v1" }, { label: "Con metadatos (v2)", value: "v2" }];
const currentFilters = () => ({ companyId: companyId.value, ip: ip.value.trim(), from: from.value, to: to.value, result: result.value, version: version.value });
const appliedFilters = ref(currentFilters());
const filters = () => appliedFilters.value;
const { rows, keyword, page, size, total, loading, error, search, paginate } = usePagedList<LicenseQueryEntry>("/web/api/license-query-log", filters);
const summary = ref<LicenseQuerySummary | null>(null);
const origins = ref<LicenseQueryOrigin[]>([]);
const analysisLoading = ref(false);
const analysisError = ref("");
const expanded = ref<Record<string, boolean>>({});
let analysisSequence = 0;
const originRows = computed(() => origins.value.map(origin => ({ ...origin,
  key: JSON.stringify([origin.companyId, origin.remoteIp, origin.authenticatedUsername, origin.installationId, origin.hostname]),
})));
const cards = computed(() => summary.value ? [
  { label: "Consultas", value: summary.value.total },
  { label: "Errores HTTP", value: summary.value.errors },
  { label: "IP distintas", value: summary.value.distinctIps },
  { label: "Instalaciones declaradas", value: summary.value.distinctInstallations },
] : []);

async function loadAnalysis() {
  const current = ++analysisSequence;
  analysisLoading.value = true;
  analysisError.value = "";
  summary.value = null;
  origins.value = [];
  const params = new URLSearchParams({ keyword: keyword.value.trim() });
  Object.entries(filters()).forEach(([key, value]) => { if (value != null && value !== "") params.set(key, String(value)); });
  try {
    const [counts, groups] = await Promise.all([licenseQueryLog.summary(params), licenseQueryLog.origins(params)]);
    if (current === analysisSequence) { summary.value = counts; origins.value = groups; }
  } catch (failure) {
    if (current === analysisSequence) analysisError.value = failure instanceof Error ? failure.message : "No se pudo cargar el análisis.";
  } finally {
    if (current === analysisSequence) analysisLoading.value = false;
  }
}
function refresh() {
  appliedFilters.value = currentFilters();
  keyword.value = searchDraft.value;
  expanded.value = {};
  void search();
  void loadAnalysis();
}
function clearFilters() {
  companyId.value = null; ip.value = ""; from.value = null; to.value = null;
  result.value = "ALL"; version.value = "ALL"; searchDraft.value = "";
  refresh();
}
function focusOrigin(origin: LicenseQueryOrigin) {
  companyId.value = origin.companyId;
  ip.value = origin.remoteIp;
  refresh();
}
const timeZone = computed(() => summary.value?.timeZone ?? "America/Argentina/Buenos_Aires");
function timestamp(value: string | number | null) {
  if (value == null) return "Sin datos";
  return new Intl.DateTimeFormat("es-AR", { dateStyle: "short", timeStyle: "medium", timeZone: timeZone.value }).format(new Date(value));
}
onMounted(loadAnalysis);
</script>

<template>
  <PageHeader title="Bitácora de seguimiento" description="Consultas recibidas desde AngryBox y actividad por origen.">
    <Button type="button" label="Actualizar" icon="pi pi-refresh" :loading="loading || analysisLoading" @click="refresh" />
  </PageHeader>
  <Message severity="info" class="mb-4">
    Una IP puede ser compartida o cambiar. Varias IP o instalaciones son indicios para investigar, no una confirmación de clonación.
    Los datos del servidor en v2 son declarados. La versión actual no informa identidad de instalación ni MAC.
  </Message>
  <Message v-if="error" severity="error" class="mb-4">{{ error }}</Message>
  <Card class="mb-4"><template #content>
    <ListToolbar v-model="searchDraft" :loading="loading || analysisLoading" placeholder="Empresa, usuario, IP o instalación" @search="refresh">
      <Button type="button" label="Limpiar filtros" icon="pi pi-filter-slash" text @click="clearFilters" />
    </ListToolbar>
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <FormField id="log-company" label="ID de empresa solicitado">
        <InputNumber v-model="companyId" input-id="log-company" :min="1" :max="Number.MAX_SAFE_INTEGER" :use-grouping="false" fluid />
      </FormField>
      <FormField id="log-ip" label="IP exacta"><InputText id="log-ip" v-model="ip" maxlength="64" fluid /></FormField>
      <FormField id="log-result" label="Resultado HTTP"><Select v-model="result" input-id="log-result" :options="results" option-label="label" option-value="value" fluid /></FormField>
      <FormField id="log-from" label="Desde"><DateInput v-model="from" input-id="log-from" /></FormField>
      <FormField id="log-to" label="Hasta (inclusive)"><DateInput v-model="to" input-id="log-to" /></FormField>
      <FormField id="log-version" label="Versión de API"><Select v-model="version" input-id="log-version" :options="versions" option-label="label" option-value="value" fluid /></FormField>
    </div>
    <div class="mt-4 flex flex-wrap items-center gap-3">
      <Button type="button" label="Aplicar filtros" icon="pi pi-filter" :loading="loading || analysisLoading" @click="refresh" />
      <span class="text-sm text-muted-color">Fechas y horarios en {{ timeZone }}. Aplicá los filtros para actualizar el listado y el análisis.</span>
    </div>
  </template></Card>

  <section aria-label="Análisis de las consultas filtradas" :aria-busy="analysisLoading" class="mb-6">
    <h2 class="mb-3 text-xl font-semibold">Actividad del período filtrado</h2>
    <Message v-if="analysisError" severity="error" class="mb-3">{{ analysisError }}</Message>
    <p v-if="analysisLoading" role="status" class="mb-3">Calculando actividad…</p>
    <template v-if="summary">
      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <Card v-for="card in cards" :key="card.label"><template #content>
          <p class="text-sm text-muted-color">{{ card.label }}</p><p class="mt-2 text-3xl font-semibold">{{ card.value.toLocaleString('es-AR') }}</p>
        </template></Card>
      </div>
      <p class="my-3 text-sm text-muted-color">Primera consulta: {{ timestamp(summary.firstSeen) }} · Última: {{ timestamp(summary.lastSeen) }}.
        Retención: {{ summary.retentionDays }} días; limpieza programada. El resumen incluye todos los registros filtrados, no solo esta página.</p>
      <Message v-if="appliedFilters.companyId && summary.distinctIps > 1" severity="warn" class="mb-3">
        Se observaron varias IP con estos filtros. Compará fechas y usuarios para revisar cambios de red o posibles servidores adicionales.
      </Message>
    </template>
    <Card v-if="summary"><template #title>Orígenes observados</template><template #subtitle>
      Hasta 50 grupos con actividad más reciente, por empresa solicitada, IP, cuenta autenticada, instalación y hostname.
      Los intentos rechazados también pueden indicar una empresa sin haber validado su licencia.
    </template><template #content>
      <div class="min-w-0 overflow-x-auto">
        <DataTable :value="originRows" data-key="key" scrollable>
          <template #empty>No hay orígenes con estos filtros.</template>
          <Column header="Empresa / cuenta"><template #body="{ data }">
            <p>{{ data.companyId == null ? 'Sin empresa' : `Empresa #${data.companyId}` }}</p>
            <p class="text-sm text-muted-color">{{ data.authenticatedUsername || 'Sin autenticación' }}</p>
          </template></Column>
          <Column field="remoteIp" header="IP observada" />
          <Column header="Instalación declarada"><template #body="{ data }">
            <p class="max-w-64 break-words">{{ data.installationId || 'No informada' }}</p><p class="max-w-64 break-words text-sm text-muted-color">{{ data.hostname }}</p>
          </template></Column>
          <Column field="queries" header="Consultas" /><Column field="errors" header="Errores" />
          <Column header="Primera / última"><template #body="{ data }"><p>{{ timestamp(data.firstSeen) }}</p><p>{{ timestamp(data.lastSeen) }}</p></template></Column>
          <Column header="Acciones"><template #body="{ data }"><Button type="button" label="Ver consultas" text size="small" @click="focusOrigin(data)" /></template></Column>
        </DataTable>
      </div>
    </template></Card>
  </section>

  <Card><template #title>Consultas registradas</template><template #subtitle>
    Desplegá una fila para ver el resultado entregado y los datos adicionales disponibles. La IP es la dirección remota reportada por el servidor HTTP; puede corresponder a un proxy.
  </template><template #content>
    <div class="min-w-0 overflow-x-auto">
      <DataTable v-model:expanded-rows="expanded" :value="rows" :loading="loading" data-key="id" scrollable striped-rows>
        <template #empty>No se encontraron consultas con estos filtros.</template>
        <Column expander header="Detalle" :pt="{ rowToggleButton: { 'aria-label': 'Ver u ocultar detalle de consulta' } }" />
        <Column header="Recibida"><template #body="{ data }">{{ timestamp(data.receivedAt) }}</template></Column>
        <Column header="Empresa / licencia"><template #body="{ data }">
          <p class="max-w-64 break-words">{{ data.companyName || 'Sin licencia validada' }}</p>
          <p class="text-sm text-muted-color">Empresa solicitada: {{ data.companyId ?? 'Sin datos' }} · Licencia: {{ data.licenseId ?? 'Sin validar' }}</p>
        </template></Column>
        <Column header="Usuarios"><template #body="{ data }">
          <p class="max-w-52 break-words">Autenticado: {{ data.authenticatedUsername || 'Ninguno' }}</p>
          <p class="max-w-52 break-words text-sm text-muted-color">Solicitado: {{ data.requestedUsername || 'Sin datos' }}</p>
        </template></Column>
        <Column field="remoteIp" header="IP observada" />
        <Column header="Resultado"><template #body="{ data }">
          <Tag :value="`HTTP ${data.httpStatus}`" :severity="data.httpStatus >= 400 ? 'danger' : 'success'" />
          <p class="mt-1 text-sm text-muted-color">{{ data.apiVersion }} · {{ data.durationMs }} ms</p>
        </template></Column>
        <template #expansion="{ data }">
          <dl class="grid max-w-5xl gap-4 p-3 sm:grid-cols-2 lg:grid-cols-3">
            <div><dt class="font-semibold">Instalación declarada</dt><dd class="break-words">{{ data.installationId || 'No informada' }}</dd></div>
            <div><dt class="font-semibold">Proceso / arranque declarado</dt><dd class="break-words">{{ data.processId || 'No informado' }}</dd></div>
            <div><dt class="font-semibold">Hostname declarado</dt><dd class="break-words">{{ data.hostname || 'No informado' }}</dd></div>
            <div><dt class="font-semibold">Versión AngryBox</dt><dd class="break-words">{{ data.appVersion || 'No informada' }}</dd></div>
            <div><dt class="font-semibold">Sistema operativo</dt><dd class="break-words">{{ data.operatingSystem || 'No informado' }}</dd></div>
            <div><dt class="font-semibold">Método HTTP</dt><dd>{{ data.httpMethod }}</dd></div>
            <div><dt class="font-semibold">Bloqueo forzado entregado</dt><dd>{{ data.forcedLock == null ? 'Sin datos' : data.forcedLock ? 'Sí' : 'No' }}</dd></div>
            <div><dt class="font-semibold">Fecha de bloqueo entregada</dt><dd>{{ timestamp(data.lockDateEpochMs) }}</dd></div>
            <div class="sm:col-span-2"><dt class="font-semibold">User-Agent declarado</dt><dd class="break-words">{{ data.userAgent || 'No informado' }}</dd></div>
          </dl>
        </template>
      </DataTable>
    </div>
    <ListPaginator :page="page" :size="size" :total="total" @change="paginate" />
  </template></Card>
</template>
