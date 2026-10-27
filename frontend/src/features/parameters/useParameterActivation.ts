import { ref, type Ref } from "vue";
import { useToast } from "primevue/usetoast";
import { useConfirmAction } from "../../composables/useConfirmAction";
import { request } from "../../services/http";

/** Envía el estado deseado y mantiene una única acción pendiente por listado. */
export function useParameterActivation(
  resource: "banks" | "bank-accounts",
  error: Ref<string>,
  reload: () => Promise<void>,
) {
  const acting = ref<number>();
  const confirm = useConfirmAction();
  const toast = useToast();
  async function change(id: number, active: boolean, label: string) {
    if (acting.value != null) return;
    acting.value = id;
    try {
      const warning = resource === "banks" && !active ? " Sus cuentas dejarán de estar disponibles para nuevos pagos." : "";
      if (!(await confirm("¿" + (active ? "Activar " : "Desactivar ") + label + "?" + warning))) return;
      error.value = "";
      await request("/web/api/" + resource + "/" + id + "/active", {
        method: "PUT", body: JSON.stringify({ active }),
      });
      toast.add({ severity: "success", summary: active ? "Activado" : "Desactivado", life: 3500 });
      await reload();
    } catch (failure) {
      error.value = failure instanceof Error ? failure.message : "No se pudo cambiar el estado.";
    } finally { acting.value = undefined; }
  }
  return { acting, change };
}
