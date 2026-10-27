<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Card from "primevue/card";
import InputText from "primevue/inputtext";
import Password from "primevue/password";
import Button from "primevue/button";
import Message from "primevue/message";
import Divider from "primevue/divider";
import SelectButton from "primevue/selectbutton";
import FormField from "../../components/FormField.vue";
import { signIn, signInDocument } from "./session";
import { returnPath } from "./navigation";
const username = ref("");
const password = ref("");
const busy = ref(false);
const error = ref("");
const help = ref(false);
const props = withDefaults(defineProps<{ documentMode?: boolean }>(), { documentMode: false });
const documentType = ref<"DNI" | "CUIT">("DNI");
const document = ref("");
const documentTypes = ["DNI", "CUIT"];
watch(documentType, () => { document.value = ""; error.value = ""; });
const router = useRouter();
const route = useRoute();
async function submit() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    if (props.documentMode) await signInDocument(documentType.value, document.value);
    else await signIn(username.value.trim(), password.value);
    password.value = "";
    await router.replace(returnPath(route.query.redirect, router));
  } catch (failure) {
    error.value =
      failure instanceof Error ? failure.message : "No se pudo iniciar sesión.";
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <main class="grid min-h-screen bg-surface-50 text-color lg:grid-cols-2">
    <section
      class="relative isolate flex flex-col justify-between overflow-hidden bg-linear-to-br from-primary-950 via-primary-900 to-slate-950 px-6 py-8 text-white sm:px-12 lg:px-16 lg:py-12"
      aria-label="Portal de clientes DySI"
    >
      <div
        aria-hidden="true"
        class="pointer-events-none absolute inset-0 -z-10 overflow-hidden"
      >
        <div
          class="absolute -right-48 top-24 size-160 rounded-full border border-white/10"
        />
        <div
          class="absolute -right-24 top-48 size-112 rounded-full border border-white/10"
        />
        <div class="absolute -right-4 top-68 size-72 rounded-full bg-white/5" />
        <div
          class="absolute -bottom-32 -left-32 size-96 rounded-full bg-primary-500/10 blur-3xl"
        />
      </div>
      <div class="flex items-center gap-3">
        <span
          class="flex size-11 items-center justify-center rounded-xl border border-white/20 bg-white/10"
          ><i class="pi pi-building text-xl" aria-hidden="true"
        /></span>
        <div>
          <p class="text-2xl font-semibold tracking-tight">
            DySI<span class="text-primary-300">.</span>
          </p>
          <p class="text-xs tracking-widest text-white/65">
            PORTAL DE CLIENTES
          </p>
        </div>
      </div>
      <p class="mt-5 text-lg text-white/85 lg:hidden">Tu cuenta, más cerca.</p>
      <div class="relative my-10 hidden max-w-lg lg:my-20 lg:block">
        <span
          class="mb-6 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-3 py-1.5 text-xs font-medium text-white/85"
          ><span class="size-1.5 rounded-full bg-primary-300" />Un espacio
          pensado para vos</span
        >
        <h2
          class="text-4xl leading-tight font-semibold tracking-tight sm:text-5xl lg:text-6xl"
        >
          Tu cuenta,<br /><span class="text-primary-200">más cerca.</span>
        </h2>
        <p
          class="mt-6 max-w-sm text-base leading-relaxed text-white/75 sm:text-lg"
        >
          Cada día es más simple cuando tenés un lugar al que volver. Bienvenido
          a tu espacio en DySI.
        </p>
        <div
          class="mt-12 hidden max-w-sm gap-5 border-t border-white/15 pt-8 lg:flex"
        >
          <i
            class="pi pi-heart mt-1 text-xl text-primary-200"
            aria-hidden="true"
          />
          <div>
            <p class="font-medium">Cerca tuyo, en cada paso.</p>
            <p class="mt-2 text-sm leading-relaxed text-white/65">
              Una experiencia clara y personal, desde el primer momento.
            </p>
          </div>
        </div>
      </div>
      <p class="hidden text-xs text-white/50 lg:block">
        DySI · Tecnología que nos conecta
      </p>
    </section>
    <section
      class="flex flex-col items-center justify-center px-5 py-10 sm:px-12 lg:px-16 lg:py-12"
      aria-labelledby="welcome-title"
    >
      <div class="w-full max-w-md">
        <div class="mb-6 flex justify-end">
          <Button as="router-link" :to="documentMode ? '/login' : '/access'"
            :label="documentMode ? 'Ingreso para usuarios' : 'Ingreso con DNI / CUIT'" text
            icon="pi pi-sign-in" />
        </div>
        <div class="mb-8">
          <p class="mb-3 text-xs font-semibold tracking-widest text-primary">
            TU PORTAL DYSI
          </p>
          <h1
            id="welcome-title"
            class="text-3xl font-semibold tracking-tight sm:text-4xl"
          >
            Te damos la bienvenida
          </h1>
          <p class="mt-3 leading-relaxed text-muted-color">
            {{ documentMode ? 'Ingresá tu DNI o CUIT registrado para acceder al portal.' : 'Ingresá con tu usuario y contraseña para continuar.' }}
          </p>
        </div>
        <Card class="border border-surface shadow-sm">
          <template #content>
            <form
              class="flex flex-col gap-6"
              :aria-busy="busy"
              @submit.prevent="submit"
            >
              <Message v-if="error" severity="error" role="alert">{{
                error
              }}</Message>
              <template v-if="documentMode">
                <div class="flex flex-col gap-2">
                  <span id="document-type-label" class="font-medium">Tipo de documento</span>
                  <SelectButton v-model="documentType" :options="documentTypes" :allow-empty="false"
                    :disabled="busy" aria-labelledby="document-type-label" />
                </div>
                <FormField id="document" :label="documentType" required v-slot="field"
                  :hint="documentType === 'DNI' ? 'Hasta ocho dígitos; podés usar puntos.' : 'Once dígitos; podés usar guiones.'">
                  <InputText id="document" v-model="document" inputmode="numeric" autocomplete="off"
                    :placeholder="documentType === 'DNI' ? '12.345.678' : '20-12345678-9'"
                    :pattern="documentType === 'DNI' ? '[0-9 .\\-]{1,14}' : '[0-9 .\\-]{11,18}'"
                    :maxlength="documentType === 'DNI' ? 14 : 18" required :disabled="busy"
                    :aria-describedby="field.describedby" fluid />
                </FormField>
              </template>
              <template v-else>
              <FormField
                id="username"
                label="Usuario"
                required
                hint="El usuario que te asignó DySI."
                v-slot="field"
              >
                <InputText
                  id="username"
                  v-model="username"
                  autocomplete="username"
                  autocapitalize="none"
                  :spellcheck="false"
                  required
                  maxlength="50"
                  :disabled="busy"
                  :aria-describedby="field.describedby"
                  fluid
                />
              </FormField>
              <FormField id="password" label="Contraseña" required>
                <Password
                  input-id="password"
                  v-model="password"
                  autocomplete="current-password"
                  :feedback="false"
                  toggle-mask
                  :input-props="{ required: true, maxlength: 200 }"
                  :disabled="busy"
                  fluid
                />
              </FormField>
              </template>
              <Button
                type="submit"
                :label="documentMode ? 'Ingresar al portal' : 'Iniciar sesión'"
                icon="pi pi-arrow-right"
                icon-pos="right"
                :loading="busy"
                fluid
              />
              <div
                class="flex items-start gap-3 text-xs leading-relaxed text-muted-color"
              >
                <i
                  class="pi pi-shield mt-0.5 text-primary"
                  aria-hidden="true"
                />
                <p>
                  {{ documentMode ? 'El ingreso por documento permite un acceso limitado y no verifica tu identidad.' : 'Tu acceso es personal. No compartas tu contraseña ni tus datos de ingreso.' }}
                </p>
              </div>
            </form>
          </template>
        </Card>
        <div class="mt-5 text-center">
          <Button
            label="¿Necesitás ayuda para ingresar?"
            text
            size="small"
            :aria-expanded="help"
            aria-controls="login-help"
            @click="help = !help"
          />
          <Message
            v-if="help"
            id="login-help"
            severity="info"
            class="mt-3 text-left"
            >{{ documentMode ? 'Si tu documento no está registrado o no podés ingresar, contactá a un administrador para revisar tus datos y el usuario vinculado.' : 'Si olvidaste tu contraseña o todavía no tenés usuario, contactá a tu referente de DySI para recuperar el acceso.' }}</Message
          >
        </div>
        <Divider class="my-6" />
        <p class="text-center text-xs leading-relaxed text-muted-color">
          Gracias por ser parte de DySI.<br /><span
            >Estamos para acompañarte.</span
          >
        </p>
      </div>
    </section>
  </main>
</template>
