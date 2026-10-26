import { useConfirm } from "primevue/useconfirm";
export function useConfirmAction() {
  const confirm = useConfirm();
  return (message: string, header = "Confirmar acción"): Promise<boolean> =>
    new Promise((resolve) => {
      confirm.require({
        header,
        message,
        icon: "pi pi-exclamation-circle",
        acceptLabel: "Confirmar",
        rejectLabel: "Cancelar",
        rejectProps: { severity: "secondary", outlined: true },
        accept: () => resolve(true),
        reject: () => resolve(false),
        onHide: () => resolve(false),
      });
    });
}
