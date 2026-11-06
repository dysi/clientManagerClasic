<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Password from "primevue/password";
import Message from "primevue/message";
import FormField from "../../components/FormField.vue";
import PageHeader from "../../components/PageHeader.vue";
import { useDraft } from "../../composables/useDraft";
import { detail, type User } from "../../services/admin";
import { request } from "../../services/http";
import { session } from "../auth/session";
const route = useRoute();
const router = useRouter();
const id = Number(route.params.id);
const user = ref<User>();
const loadError = ref("");
const form = ref({ password: "", confirmation: "" });
const { saving, error, fields, save, reset } = useDraft(form);
onMounted(async () => {
  try {
    user.value = await detail<User>("users", id);
  } catch (failure) {
    loadError.value = (failure as Error).message;
  }
});
async function submit() {
  if (form.value.password !== form.value.confirmation) {
    fields.value = { confirmation: "Las contraseñas no coinciden." };
    return;
  }
  const result = await save(async () => {
    await request(`/web/api/users/${id}/password`, {
      method: "PUT",
      body: JSON.stringify({ password: form.value.password }),
    });
    return true;
  }, "La contraseña se actualizó y las sesiones anteriores dejarán de ser válidas.");
  if (result) {
    form.value.password = "";
    form.value.confirmation = "";
    reset();
    if (user.value?.username === session.user?.username) {
      session.user = null;
      await router.replace("/login");
    } else await router.push("/users");
  }
}
</script>
<template>
  <PageHeader
    title="Cambiar contraseña"
    :description="user ? `Cuenta: ${user.username}` : 'Cargando cuenta…'"
    ><Button
      as="router-link"
      to="/users"
      label="Volver a usuarios"
      text
      icon="pi pi-arrow-left"
  /></PageHeader>
  <Message v-if="loadError" severity="error">{{ loadError }}</Message>
  <form v-else-if="user" class="max-w-2xl" @submit.prevent="submit">
    <Card
      ><template #title>Nueva contraseña</template
      ><template #content
        ><div class="flex flex-col gap-5">
          <Message v-if="error" severity="error">{{ error }}</Message>
          <Message severity="info"
            >Este cambio invalida las sesiones anteriores de la cuenta. Si es tu
            propia cuenta, deberás volver a iniciar sesión.</Message
          >
          <FormField
            id="newPassword"
            label="Nueva contraseña"
            required
            :error="fields.password"
            hint="Al menos 12 caracteres y hasta 72 bytes UTF-8."
            ><Password
              input-id="newPassword"
              v-model="form.password"
              autocomplete="new-password"
              toggle-mask
              :feedback="false"
              :input-props="{ required: true, minlength: 12, maxlength: 72 }"
              :disabled="saving"
              fluid
          /></FormField>
          <FormField
            id="confirmation"
            label="Repetir nueva contraseña"
            required
            :error="fields.confirmation"
            ><Password
              input-id="confirmation"
              v-model="form.confirmation"
              autocomplete="new-password"
              toggle-mask
              :feedback="false"
              :input-props="{ required: true, minlength: 12, maxlength: 72 }"
              :disabled="saving"
              :invalid="!!fields.confirmation"
              fluid
          /></FormField>
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
              label="Actualizar contraseña"
              icon="pi pi-lock"
              :loading="saving"
            />
          </div></div></template
    ></Card>
  </form>
</template>
