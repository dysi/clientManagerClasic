<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import InputText from "primevue/inputtext";
import Password from "primevue/password";
import MultiSelect from "primevue/multiselect";
import ToggleSwitch from "primevue/toggleswitch";
import Message from "primevue/message";
import Skeleton from "primevue/skeleton";
import PageHeader from "../../components/PageHeader.vue";
import FormField from "../../components/FormField.vue";
import EntitySelect from "../../components/EntitySelect.vue";
import { useDraft } from "../../composables/useDraft";
import { session } from "../auth/session";
import {
  detail,
  saveRecord,
  roleOptions,
  type User,
  type UserInput,
} from "../../services/admin";
const route = useRoute();
const router = useRouter();
const id = route.params.id ? Number(route.params.id) : undefined;
const form = ref<UserInput>({
  username: "",
  firstname: "",
  lastname: "",
  email: "",
  enabled: true,
  roles: ["ROLE_USER"],
  clientId: route.query.clientId ? Number(route.query.clientId) : null,
  ...(id ? {} : { password: "" }),
});
const clientName = ref("");
const loading = ref(true);
const loadError = ref("");
const { saving, error, fields, save, reset } = useDraft(form);
onMounted(async () => {
  try {
    if (id) {
      const user = await detail<User>("users", id);
      form.value = {
        username: user.username,
        firstname: user.firstname,
        lastname: user.lastname,
        email: user.email,
        enabled: user.enabled,
        roles: [...user.roles],
        clientId: user.clientId,
      };
      clientName.value = user.clientName;
    } else if (form.value.clientId) {
      const client = await detail<{ firstname: string; lastname: string }>(
        "clients",
        form.value.clientId,
      );
      clientName.value = `${client.firstname} ${client.lastname}`;
    }
    reset();
  } catch (failure) {
    loadError.value = (failure as Error).message;
  } finally {
    loading.value = false;
  }
});
async function submit() {
  const result = await save(() => saveRecord<User>("users", form.value, id));
  if (result) await router.push("/users");
}
</script>
<template>
  <PageHeader
    :title="id ? 'Editar usuario' : 'Nuevo usuario'"
    description="Los datos personales, el acceso y la contraseña se administran por separado."
    ><Button
      as="router-link"
      to="/users"
      label="Volver a usuarios"
      text
      icon="pi pi-arrow-left" /><Button
      v-if="id"
      as="router-link"
      :to="`/users/${id}/password`"
      label="Cambiar contraseña"
      icon="pi pi-lock"
      outlined
  /></PageHeader>
  <Skeleton v-if="loading" height="24rem" /><Message
    v-else-if="loadError"
    severity="error"
    >{{ loadError }}</Message
  >
  <form
    v-else
    @submit.prevent="submit"
    :aria-busy="saving"
    class="flex flex-col gap-6"
  >
    <Message v-if="error" severity="error">{{ error }}</Message>
    <Card
      ><template #title>Identidad</template
      ><template #content
        ><div class="grid gap-5 md:grid-cols-2">
          <FormField
            id="username"
            label="Nombre de usuario"
            required
            :error="fields.username"
            :hint="
              id
                ? 'El nombre de usuario se conserva para las integraciones existentes.'
                : 'Entre 4 y 50 caracteres.'
            "
            v-slot="field"
            ><InputText
              id="username"
              v-model="form.username"
              required
              minlength="4"
              maxlength="50"
              autocomplete="off"
              :disabled="saving || !!id"
              :invalid="field.invalid"
              fluid
          /></FormField>
          <FormField
            id="email"
            label="Correo electrónico"
            required
            :error="fields.email"
            v-slot="field"
            ><InputText
              id="email"
              v-model="form.email"
              type="email"
              required
              maxlength="50"
              :disabled="saving"
              :invalid="field.invalid"
              fluid
          /></FormField>
          <FormField
            id="firstname"
            label="Nombre"
            required
            :error="fields.firstname"
            hint="Al menos 4 caracteres."
            v-slot="field"
            ><InputText
              id="firstname"
              v-model="form.firstname"
              required
              minlength="4"
              maxlength="50"
              :disabled="saving"
              :invalid="field.invalid"
              fluid
          /></FormField>
          <FormField
            id="lastname"
            label="Apellido"
            required
            :error="fields.lastname"
            v-slot="field"
            ><InputText
              id="lastname"
              v-model="form.lastname"
              required
              minlength="4"
              maxlength="50"
              :disabled="saving"
              :invalid="field.invalid"
              fluid
          /></FormField></div></template
    ></Card>
    <Card
      ><template #title>Permisos y vinculación</template
      ><template #content
        ><div class="grid gap-5 md:grid-cols-2">
          <FormField
            id="roles"
            label="Roles de acceso"
            required
            :error="fields.roles"
            v-slot="field"
            ><MultiSelect
              input-id="roles"
              v-model="form.roles"
              :options="roleOptions"
              option-label="label"
              option-value="value"
              display="chip"
              :disabled="saving"
              :invalid="field.invalid"
              fluid
          /></FormField>
          <FormField
            id="clientId"
            label="Cliente vinculado"
            :error="fields.clientId"
            hint="Opcional. Un cliente puede vincularse con una sola cuenta."
            v-slot="field"
            ><EntitySelect
              v-model="form.clientId"
              :label="clientName"
              kind="clients"
              input-id="clientId"
              :disabled="saving"
              :invalid="field.invalid"
              :describedby="field.describedby" /><Button
              v-if="form.clientId"
              label="Quitar vinculación"
              text
              size="small"
              severity="secondary"
              :disabled="saving"
              @click="form.clientId = null"
          /></FormField>
          <FormField
            id="enabled"
            label="Cuenta habilitada"
            :error="fields.enabled"
            :hint="
              id && form.username === session.user?.username
                ? 'No podés deshabilitar tu propia cuenta.'
                : undefined
            "
            ><ToggleSwitch
              input-id="enabled"
              v-model="form.enabled"
              :disabled="
                saving || (!!id && form.username === session.user?.username)
              "
          /></FormField></div></template
    ></Card>
    <Card v-if="!id"
      ><template #title>Contraseña inicial</template
      ><template #content
        ><FormField
          id="initialPassword"
          label="Contraseña inicial"
          required
          :error="fields.password"
          hint="Al menos 12 caracteres y hasta 72 bytes UTF-8."
          ><Password
            input-id="initialPassword"
            v-model="form.password"
            autocomplete="new-password"
            toggle-mask
            :feedback="false"
            :input-props="{ required: true, minlength: 12, maxlength: 72 }"
            :disabled="saving"
            :invalid="!!fields.password"
            fluid /></FormField></template
    ></Card>
    <div class="flex flex-wrap justify-end gap-3">
      <Button
        as="router-link"
        to="/users"
        label="Cancelar"
        severity="secondary"
        outlined
        :disabled="saving"
      /><Button
        type="submit"
        label="Guardar usuario"
        icon="pi pi-check"
        :loading="saving"
      />
    </div>
  </form>
</template>
