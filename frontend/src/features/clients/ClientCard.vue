<script setup lang="ts">
import Button from "primevue/button";
import Card from "primevue/card";
import Message from "primevue/message";
import Tag from "primevue/tag";
import { money, displayDateTime } from "../../services/format";
import type { Client } from "./api";

defineProps<{ client: Client; acting: boolean; actionsDisabled: boolean }>();
defineEmits<{ changeStatus: [] }>();

function hasAddress(client: Client): boolean {
  return Boolean(client.street || client.streetNumber || client.addressComplement || client.neighborhood
    || client.city || client.provinceName || client.countryName || client.postalCode);
}

function licenseSummary(client: Client): string {
  const counts = client.licenseCounts;
  return [
    counts.active ? `${counts.active} ${counts.active === 1 ? 'activa' : 'activas'}` : '',
    counts.expired ? `${counts.expired} ${counts.expired === 1 ? 'vencida' : 'vencidas'}` : '',
    counts.blocked ? `${counts.blocked} ${counts.blocked === 1 ? 'bloqueada' : 'bloqueadas'}` : '',
    counts.unknown ? `${counts.unknown} sin vigencia informada` : '',
  ].filter(Boolean).join(' / ');
}
</script>
<template>
  <article :aria-labelledby="`client-heading-${client.id}`" class="min-w-0">
    <Card :class="client.removed ? 'border border-red-200 bg-red-50! dark:border-red-900' : ''">
      <template #content>
        <div class="flex min-w-0 flex-col gap-5 border-b border-surface pb-5 lg:flex-row lg:justify-between">
          <div class="min-w-0 flex-1">
            <div class="mb-2 flex flex-wrap items-center gap-2 text-sm">
              <span class="text-muted-color">Cliente #{{ client.id }}</span>
              <Tag :value="client.removed ? 'Dado de baja' : 'Activo'" :severity="client.removed ? 'danger' : 'success'" />
            </div>
            <h2 :id="`client-heading-${client.id}`" class="m-0 text-xl font-semibold [overflow-wrap:anywhere]">
              <RouterLink :to="`/clients/${client.id}`" class="text-primary hover:underline">
                {{ client.tradeName || client.legalName || `${client.lastname}, ${client.firstname}` }}
              </RouterLink>
            </h2>
            <p v-if="client.tradeName || client.legalName" class="mt-2 text-muted-color [overflow-wrap:anywhere]">
              {{ client.lastname }}, {{ client.firstname }}
            </p>
            <p class="mt-2 text-sm [overflow-wrap:anywhere]">
              <span class="font-medium">{{ client.licenseCounts.total }} {{ client.licenseCounts.total === 1 ? 'licencia' : 'licencias' }}</span><template v-if="client.licenseCounts.total">: {{ licenseSummary(client) }}</template>
            </p>
          </div>
          <div class="flex shrink-0 flex-wrap items-start gap-2 lg:max-w-96" role="group" :aria-label="`Acciones del cliente ${client.id}`">
            <Button as="router-link" :to="`/clients/${client.id}/edit`" label="Editar" icon="pi pi-pencil"
              size="small" :aria-label="`Editar cliente ${client.id}`" />
            <Button as="router-link" :to="{ path: '/licenses', query: { clientId: client.id } }"
              label="Licencias" icon="pi pi-key" outlined size="small" :aria-label="`Ver licencias del cliente ${client.id}`" />
            <Button as="router-link" :to="{ path: '/users', query: { clientId: client.id } }"
              label="Usuario" icon="pi pi-users" outlined size="small" :aria-label="`Ver usuarios del cliente ${client.id}`" />
            <Button as="router-link" :to="`/clients/${client.id}`" label="Ver detalle" text size="small"
              :aria-label="`Ver detalle de ${client.firstname} ${client.lastname}`" />
            <Button type="button" :label="client.removed ? 'Dar de alta' : 'Dar de baja'"
              :severity="client.removed ? 'secondary' : 'danger'" text size="small"
              :loading="acting" :disabled="actionsDisabled" @click="$emit('changeStatus')" />
          </div>
        </div>
        <Message v-if="client.portalUserMissing" severity="warn" :closable="false" class="mt-4">
          Sin cuenta de referencia para el acceso por DNI/CUIT: vinculá un usuario desde Usuarios.
        </Message>
        <div class="grid min-w-0 gap-6 pt-5 sm:grid-cols-2 xl:grid-cols-4">
          <section class="min-w-0" :aria-labelledby="`identity-${client.id}`">
            <h3 :id="`identity-${client.id}`" class="mb-3 text-sm font-semibold"><i class="pi pi-id-card mr-2" aria-hidden="true" />Identificación</h3>
            <dl class="space-y-3 text-sm [overflow-wrap:anywhere]">
              <div v-if="client.legalName"><dt class="text-muted-color">Razón social</dt><dd class="mt-1">{{ client.legalName }}</dd></div>
              <div v-if="client.tradeName"><dt class="text-muted-color">Nombre de fantasía</dt><dd class="mt-1">{{ client.tradeName }}</dd></div>
              <div><dt class="text-muted-color">CUIT/DNI</dt><dd class="mt-1 space-y-1">
                <p v-if="client.cuil">CUIL / CUIT: {{ client.cuil }}</p>
                <p v-if="client.dni">DNI: {{ client.dni }}</p>
                <p v-if="!client.cuil && !client.dni">Sin registrar</p>
              </dd></div>
              <div v-if="client.externalCode"><dt class="text-muted-color">Código externo</dt><dd class="mt-1">{{ client.externalCode }}</dd></div>
            </dl>
          </section>
          <section class="min-w-0" :aria-labelledby="`contact-${client.id}`">
            <h3 :id="`contact-${client.id}`" class="mb-3 text-sm font-semibold"><i class="pi pi-address-book mr-2" aria-hidden="true" />Contacto</h3>
            <dl class="space-y-3 text-sm [overflow-wrap:anywhere]">
              <div><dt class="text-muted-color">Correo electrónico</dt><dd class="mt-1">{{ client.email || 'Sin registrar' }}</dd></div>
              <div><dt class="text-muted-color">Teléfono</dt><dd class="mt-1">{{ client.phone || 'Sin registrar' }}</dd></div>
              <div><dt class="text-muted-color">Usuarios vinculados</dt><dd class="mt-1"><RouterLink :to="{ path: '/users', query: { clientId: client.id } }" class="text-primary hover:underline">{{ client.userCount }} {{ client.userCount === 1 ? 'cuenta' : 'cuentas' }} · Ver usuarios</RouterLink></dd></div>
            </dl>
          </section>
          <section class="min-w-0" :aria-labelledby="`address-${client.id}`">
            <h3 :id="`address-${client.id}`" class="mb-3 text-sm font-semibold"><i class="pi pi-map-marker mr-2" aria-hidden="true" />Dirección</h3>
            <dl v-if="hasAddress(client)" class="space-y-3 text-sm [overflow-wrap:anywhere]">
              <div v-if="client.street || client.streetNumber"><dt class="text-muted-color">Calle y número</dt><dd class="mt-1">{{ [client.street, client.streetNumber].filter(Boolean).join(' ') }}</dd></div>
              <div v-if="client.addressComplement"><dt class="text-muted-color">Interior / Piso / Departamento / Oficina</dt><dd class="mt-1">{{ client.addressComplement }}</dd></div>
              <div v-if="client.neighborhood"><dt class="text-muted-color">Barrio / Sector</dt><dd class="mt-1">{{ client.neighborhood }}</dd></div>
              <div v-if="client.city"><dt class="text-muted-color">Ciudad / Localidad</dt><dd class="mt-1">{{ client.city }}</dd></div>
              <div v-if="client.provinceName || client.countryName"><dt class="text-muted-color">Provincia / País</dt><dd class="mt-1">{{ [client.provinceName, client.countryName].filter(Boolean).join(', ') }}</dd></div>
              <div v-if="client.postalCode"><dt class="text-muted-color">Código postal</dt><dd class="mt-1">{{ client.postalCode }}</dd></div>
            </dl>
            <p v-else class="text-sm text-muted-color">Sin dirección registrada</p>
          </section>
          <section class="min-w-0 rounded-xl bg-emphasis p-4" :aria-labelledby="`balance-${client.id}`">
            <h3 :id="`balance-${client.id}`" class="mb-3 text-sm font-semibold"><i class="pi pi-wallet mr-2" aria-hidden="true" />Deuda / saldo</h3>
            <template v-if="client.accountBalance">
              <p class="text-sm text-muted-color">{{ client.accountBalance.amount > 0 ? 'Deuda' : client.accountBalance.amount < 0 ? 'Saldo a favor' : 'Sin deuda' }}</p>
              <p class="mt-1 text-2xl font-semibold [overflow-wrap:anywhere]">{{ money(client.accountBalance.amount) }}</p>
              <p class="mt-1 text-sm text-muted-color">{{ client.accountBalance.currency }} · Saldo externo</p>
              <dl class="mt-4 space-y-3 text-sm [overflow-wrap:anywhere]">
                <div><dt class="text-muted-color">Última sincronización</dt><dd class="mt-1">{{ displayDateTime(client.accountBalance.synchronizedAt) }}</dd></div>
                <div><dt class="text-muted-color">ID de saldo</dt><dd class="mt-1">{{ client.accountBalance.id }}</dd></div>
              </dl>
            </template>
            <p v-else class="text-sm text-muted-color">Sin informar: no hay información de saldo para este cliente.</p>
          </section>
        </div>
        <section v-if="client.observations" class="mt-5 border-t border-surface pt-4" :aria-labelledby="`notes-${client.id}`">
          <h3 :id="`notes-${client.id}`" class="mb-2 text-sm font-semibold"><i class="pi pi-comment mr-2" aria-hidden="true" />Observaciones</h3>
          <p class="whitespace-pre-wrap text-sm leading-relaxed [overflow-wrap:anywhere]">{{ client.observations }}</p>
        </section>
      </template>
    </Card>
  </article>
</template>
