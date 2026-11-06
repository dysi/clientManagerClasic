<script setup lang="ts">
import { onMounted, ref } from "vue";
import Panel from "primevue/panel";
import Card from "primevue/card";
import Message from "primevue/message";
import Skeleton from "primevue/skeleton";
import Tag from "primevue/tag";
import PageHeader from "../../components/PageHeader.vue";
import { request } from "../../services/http";
import { displayDateTime, money } from "../../services/format";
import { overviewLabels, type PortalOverview } from "../../services/portal";
import PortalLicenses from "./PortalLicenses.vue";
import PortalPayments from "./PortalPayments.vue";
const overview = ref<PortalOverview | null>(null);
const licensesCollapsed = ref(true);
const error = ref("");
const loading = ref(true);
onMounted(async () => {
  try { overview.value = await request<PortalOverview>("/web/portal/overview"); }
  catch (failure) { error.value = failure instanceof Error ? failure.message : "No se pudo abrir el portal."; }
  finally { loading.value = false; }
});
</script>
<template>
  <Skeleton v-if="loading" height="12rem" />
  <Message v-else-if="error" severity="error" role="alert">{{ error }}</Message>
  <template v-else-if="overview">
    <PageHeader :title="'Cuenta de ' + ([overview.client.firstname, overview.client.lastname].filter(Boolean).join(' ') || overview.name)" description="Revisá los datos de tu cuenta y consultá tus pagos y saldo informado." />
    <Card v-if="overview.sensitiveAccess" class="mb-6"><template #title>Datos de tu cuenta</template><template #content>
      <dl class="grid min-w-0 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div><dt class="text-sm text-muted-color">DNI</dt><dd class="break-all">{{ overview.client.dni || 'Sin registrar' }}</dd></div>
        <div><dt class="text-sm text-muted-color">CUIL / CUIT</dt><dd class="break-all">{{ overview.client.cuil || 'Sin registrar' }}</dd></div>
        <div><dt class="text-sm text-muted-color">Correo electrónico</dt><dd class="break-all">{{ overview.client.email || 'Sin registrar' }}</dd></div>
        <div><dt class="text-sm text-muted-color">Teléfono</dt><dd class="break-all">{{ overview.client.phone || 'Sin registrar' }}</dd></div>
      </dl>
    </template></Card>
    <div class="mb-6 grid gap-6" :class="{ 'lg:grid-cols-2': overview.sensitiveAccess }">
      <Card v-if="overview.sensitiveAccess && overview.licenses && overview.state"><template #title>Estado general</template><template #content>
        <p class="font-medium">{{ overviewLabels[overview.state] }}</p>
        <dl class="mt-4 grid grid-cols-2 gap-4 text-sm">
          <div><dt class="text-muted-color">Licencias</dt><dd>{{ overview.licenses.total }}</dd></div>
          <div><dt :class="overview.licenses.blocked > 0 ? 'font-bold' : 'text-muted-color'">Bloqueadas</dt><dd>
            <Tag v-if="overview.licenses.blocked > 0" :value="overview.licenses.blocked" severity="danger" icon="pi pi-exclamation-triangle" class="font-bold" :aria-label="overview.licenses.blocked + (overview.licenses.blocked === 1 ? ' licencia bloqueada' : ' licencias bloqueadas')" />
            <span v-else>{{ overview.licenses.blocked }}</span>
          </dd></div>
          <div><dt :class="overview.licenses.expired > 0 ? 'font-bold' : 'text-muted-color'">Vencidas</dt><dd>
            <Tag v-if="overview.licenses.expired > 0" :value="overview.licenses.expired" severity="danger" icon="pi pi-exclamation-circle" class="font-bold" :aria-label="overview.licenses.expired + (overview.licenses.expired === 1 ? ' licencia vencida' : ' licencias vencidas')" />
            <span v-else>{{ overview.licenses.expired }}</span>
          </dd></div>
          <div><dt :class="overview.licenses.expiring > 0 ? 'font-bold' : 'text-muted-color'">Por vencer</dt><dd>
            <Tag v-if="overview.licenses.expiring > 0" :value="overview.licenses.expiring" severity="warn" icon="pi pi-exclamation-triangle" class="font-bold" :aria-label="overview.licenses.expiring + (overview.licenses.expiring === 1 ? ' licencia por vencer' : ' licencias por vencer')" />
            <span v-else>{{ overview.licenses.expiring }}</span>
          </dd></div>
        </dl>
      </template></Card>
      <Card><template #title>Saldo informado</template><template #content>
        <template v-if="overview.balance">
          <p class="text-sm text-muted-color">{{ overview.balance.state === 'DEUDA' ? 'Deuda' : overview.balance.state === 'A_FAVOR' ? 'Saldo a favor' : 'Sin deuda' }}</p>
          <p class="mt-2 text-3xl font-semibold">{{ money(Math.abs(overview.balance.amount)) }}</p>
          <p class="mt-3 text-sm text-muted-color">Informado: {{ displayDateTime(overview.balance.synchronizedAt) }}</p>
        </template>
        <Message v-else severity="info" :closable="false">Saldo aún no disponible</Message>
      </template></Card>
    </div>
    <PortalPayments :sensitive-access="overview.sensitiveAccess" class="mb-6" />
    <div class="flex flex-col gap-6">
      <Panel v-if="overview.sensitiveAccess" v-model:collapsed="licensesCollapsed" header="Mis licencias" toggleable :toggle-button-props="{ 'aria-label': licensesCollapsed ? 'Mostrar mis licencias' : 'Ocultar mis licencias' }">
        <template #toggleicon="{ collapsed }"><i :class="['pi', collapsed ? 'pi-chevron-down' : 'pi-chevron-up']" aria-hidden="true" /></template>
        <PortalLicenses v-if="!licensesCollapsed" />
      </Panel>
    </div>
  </template>
</template>
