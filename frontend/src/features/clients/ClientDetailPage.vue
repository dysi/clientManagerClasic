<script setup lang="ts">
import { money, displayDateTime } from "../../services/format";
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Message from "primevue/message";
import Skeleton from "primevue/skeleton";
import PageHeader from "../../components/PageHeader.vue";
import { clientsApi, type Client } from "./api";
const route = useRoute();
const client = ref<Client>();
const error = ref("");
onMounted(async () => {
  try {
    client.value = await clientsApi.detail(Number(route.params.id));
  } catch (failure) {
    error.value = (failure as Error).message;
  }
});
</script>
<template>
  <Message v-if="error" severity="error">{{ error }}</Message>
  <template v-else-if="client">
    <PageHeader
      :title="`${client.firstname} ${client.lastname}`"
      :description="`Cliente #${client.id}`"
      ><Button
        as="router-link"
        to="/clients"
        label="Volver a clientes"
        text /><Button
        as="router-link"
        :to="`/clients/${client.id}/edit`"
        label="Editar datos"
        icon="pi pi-pencil"
    /></PageHeader>
    <Message v-if="client.portalUserMissing" severity="warn" :closable="false" class="mb-5">
      Este cliente no tiene un usuario vinculado y no puede ingresar al portal. Vinculá una cuenta habilitada desde Usuarios.
    </Message>
    <div class="grid gap-6 lg:grid-cols-3">
      <Card class="lg:col-span-2"
        ><template #title>Contacto e identificación</template
        ><template #content
          ><dl class="grid gap-6 py-2 sm:grid-cols-2">
            <div><dt class="text-sm text-muted-color">Razón social</dt>
              <dd class="mt-2 break-words">{{ client.legalName || "Sin registrar" }}</dd></div>
            <div><dt class="text-sm text-muted-color">Nombre de fantasía</dt>
              <dd class="mt-2 break-words">{{ client.tradeName || "Sin registrar" }}</dd></div>
            <div><dt class="text-sm text-muted-color">Código externo</dt>
              <dd class="mt-2 break-all">{{ client.externalCode || "Sin registrar" }}</dd></div>
            <div><dt class="text-sm text-muted-color">DNI</dt>
              <dd class="mt-2">{{ client.dni || "Sin DNI registrado" }}</dd></div>
            <div>
              <dt class="text-sm text-muted-color">Correo electrónico</dt>
              <dd class="mt-2 break-all">
                {{ client.email || "Sin correo registrado" }}
              </dd>
            </div>
            <div>
              <dt class="text-sm text-muted-color">Teléfono</dt>
              <dd class="mt-2">
                {{ client.phone || "Sin teléfono registrado" }}
              </dd>
            </div>
            <div>
              <dt class="text-sm text-muted-color">CUIL / CUIT</dt>
              <dd class="mt-2">
                {{ client.cuil || "Sin identificación registrada" }}
              </dd>
            </div>
            <div>
              <dt class="text-sm text-muted-color">Usuario vinculado</dt>
              <dd class="mt-2">{{ client.username }}</dd>
            </div>
<div v-if="client.street"><dt class="text-sm text-muted-color">Calle</dt><dd class="mt-2 whitespace-pre-wrap break-words">{{ client.street }}</dd></div>
<div v-if="client.streetNumber"><dt class="text-sm text-muted-color">Número</dt><dd class="mt-2 whitespace-pre-wrap break-words">{{ client.streetNumber }}</dd></div>
<div v-if="client.addressComplement"><dt class="text-sm text-muted-color">Interior / Piso / Departamento / Oficina</dt><dd class="mt-2 whitespace-pre-wrap break-words">{{ client.addressComplement }}</dd></div>
<div v-if="client.neighborhood"><dt class="text-sm text-muted-color">Barrio / Sector</dt><dd class="mt-2 whitespace-pre-wrap break-words">{{ client.neighborhood }}</dd></div>
<div v-if="client.city"><dt class="text-sm text-muted-color">Ciudad / Localidad</dt><dd class="mt-2 whitespace-pre-wrap break-words">{{ client.city }}</dd></div>
<div v-if="client.countryName"><dt class="text-sm text-muted-color">País</dt><dd class="mt-2 whitespace-pre-wrap break-words">{{ client.countryName }}</dd></div>
<div v-if="client.provinceName"><dt class="text-sm text-muted-color">Provincia</dt><dd class="mt-2 whitespace-pre-wrap break-words">{{ client.provinceName }}</dd></div>
<div v-if="client.postalCode"><dt class="text-sm text-muted-color">Código postal</dt><dd class="mt-2 whitespace-pre-wrap break-words">{{ client.postalCode }}</dd></div>
<div v-if="client.observations"><dt class="text-sm text-muted-color">Observaciones</dt><dd class="mt-2 whitespace-pre-wrap break-words">{{ client.observations }}</dd></div>
          </dl></template
        ></Card
      >
      <Card
        ><template #title>Gestión del cliente</template
        ><template #content
          ><div class="flex flex-col gap-4">
            <p class="text-muted-color">
              Cada operación tiene su espacio, con el cliente ya seleccionado.
            </p>
            <Button
              as="router-link"
              :to="{ path: '/licenses', query: { clientId: client.id } }"
              label="Ver licencias"
              icon="pi pi-key"
              outlined
            /><Button
              as="router-link"
              :to="{
                path: '/licenses/new',
                query: {
                  clientId: client.id,
                  clientName: `${client.firstname} ${client.lastname}`,
                },
              }"
              label="Nueva licencia"
              icon="pi pi-plus"
            /><Button
              as="router-link"
              :to="{ path: '/users', query: { clientId: client.id } }"
              label="Ver usuarios vinculados"
              icon="pi pi-users"
              text
            /></div></template
      ></Card>
      <Card class="lg:col-span-3">
        <template #title>Deuda y saldo del cliente</template>
        <template #content>
          <dl v-if="client.accountBalance" class="grid gap-6 py-2 sm:grid-cols-2 lg:grid-cols-3">
            <div><dt class="text-sm text-muted-color">Importe</dt>
              <dd class="mt-2 font-semibold">{{ money(client.accountBalance.amount) }}</dd></div>
            <div><dt class="text-sm text-muted-color">Estado</dt>
              <dd class="mt-2">{{ client.accountBalance.amount > 0 ? "Deuda" : client.accountBalance.amount < 0 ? "Saldo a favor" : "Sin deuda" }}</dd></div>
            <div><dt class="text-sm text-muted-color">Moneda</dt>
              <dd class="mt-2">{{ client.accountBalance.currency }}</dd></div>
            <div><dt class="text-sm text-muted-color">Última sincronización</dt>
              <dd class="mt-2">{{ displayDateTime(client.accountBalance.synchronizedAt) }}</dd></div>
            <div><dt class="text-sm text-muted-color">ID de saldo</dt>
              <dd class="mt-2">{{ client.accountBalance.id }}</dd></div>
            <div><dt class="text-sm text-muted-color">ID de cliente</dt>
              <dd class="mt-2">{{ client.accountBalance.clientId }}</dd></div>
          </dl>
          <p v-else class="text-muted-color">Sin informar: no hay información de saldo para este cliente.</p>
          <p class="mt-4 text-sm text-muted-color">Fotografía externa de solo lectura. Los pagos presentados o aprobados no actualizan este saldo automáticamente.</p>
        </template>
      </Card>
    </div> </template
  ><Skeleton v-else height="20rem" />
</template>
