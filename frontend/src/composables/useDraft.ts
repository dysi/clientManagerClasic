import { computed, onMounted, onUnmounted, ref, type Ref } from "vue";
import { onBeforeRouteLeave } from "vue-router";
import { useToast } from "primevue/usetoast";
import { ApiError } from "../services/http";
import { session } from "../features/auth/session";
import { useConfirmAction } from "./useConfirmAction";
export function useDraft<T>(model: Ref<T>) {
  const snapshot = ref(JSON.stringify(model.value));
  const dirty = computed(() => JSON.stringify(model.value) !== snapshot.value);
  const saving = ref(false);
  const error = ref("");
  const fields = ref<Record<string, string>>({});
  const confirm = useConfirmAction();
  const toast = useToast();
  function reset() {
    snapshot.value = JSON.stringify(model.value);
  }
  async function save<R>(
    operation: () => Promise<R>,
    message = "Los cambios se guardaron correctamente.",
  ): Promise<R | undefined> {
    if (saving.value) return;
    saving.value = true;
    error.value = "";
    fields.value = {};
    try {
      const result = await operation();
      reset();
      toast.add({
        severity: "success",
        summary: "Guardado",
        detail: message,
        life: 4500,
      });
      return result;
    } catch (failure) {
      error.value =
        failure instanceof Error ? failure.message : "No se pudo guardar.";
      if (failure instanceof ApiError) fields.value = failure.fields;
    } finally {
      saving.value = false;
    }
  }
  async function discard() {
    const accepted =
      !dirty.value ||
      (await confirm(
        "¿Descartar los cambios sin guardar?",
        "Cambios pendientes",
      ));
    if (accepted) reset();
    return accepted;
  }
  onBeforeRouteLeave(() => !session.user || (!saving.value && discard()));
  function unload(event: BeforeUnloadEvent) {
    if (dirty.value) {
      event.preventDefault();
      event.returnValue = "";
    }
  }
  onMounted(() => window.addEventListener("beforeunload", unload));
  onUnmounted(() => window.removeEventListener("beforeunload", unload));
  return { dirty, saving, error, fields, reset, save, discard };
}
