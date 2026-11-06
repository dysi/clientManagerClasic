import { createApp } from "vue";
import PrimeVue from "primevue/config";
import ConfirmationService from "primevue/confirmationservice";
import ToastService from "primevue/toastservice";
import Aura from "@primeuix/themes/aura";
import { definePreset } from "@primeuix/themes";
import App from "./App.vue";
import { router } from "./router";
import { loadSession } from "./features/auth/session";
import "./styles.css";

const DysiTheme = definePreset(Aura, {
  semantic: {
    primary: {
      50: "#eef8f9",
      100: "#d4eeef",
      200: "#a9dadd",
      300: "#73bdc3",
      400: "#469da6",
      500: "#267d89",
      600: "#1c6573",
      700: "#184f5d",
      800: "#143e4b",
      900: "#12333e",
      950: "#0c222a",
    },
    colorScheme: {
      light: {
        primary: {
          color: "{primary.600}",
          hoverColor: "{primary.700}",
          activeColor: "{primary.800}",
          contrastColor: "#ffffff",
        },
      },
    },
  },
});

async function start() {
  localStorage.removeItem("token");
  await loadSession();
  createApp(App)
    .use(router)
    .use(ConfirmationService)
    .use(ToastService)
    .use(PrimeVue, {
      theme: {
        preset: DysiTheme,
        options: {
          darkModeSelector: false,
          cssLayer: {
            name: "primevue",
            order: "theme, base, primevue, utilities",
          },
        },
      },
      locale: {
        emptyMessage: "Sin resultados",
        startsWith: "Empieza con",
        contains: "Contiene",
        emptySearchMessage: "Sin resultados",
        selectionMessage: "{0} elementos seleccionados",
        emptySelectionMessage: "Sin selección",
        searchMessage: "{0} resultados disponibles",
        today: "Hoy",
        clear: "Limpiar",
        firstDayOfWeek: 1,
        dateFormat: "dd/mm/yy",
        dayNames: [
          "domingo",
          "lunes",
          "martes",
          "miércoles",
          "jueves",
          "viernes",
          "sábado",
        ],
        dayNamesShort: ["dom", "lun", "mar", "mié", "jue", "vie", "sáb"],
        dayNamesMin: ["do", "lu", "ma", "mi", "ju", "vi", "sá"],
        monthNames: [
          "enero",
          "febrero",
          "marzo",
          "abril",
          "mayo",
          "junio",
          "julio",
          "agosto",
          "septiembre",
          "octubre",
          "noviembre",
          "diciembre",
        ],
        monthNamesShort: [
          "ene",
          "feb",
          "mar",
          "abr",
          "may",
          "jun",
          "jul",
          "ago",
          "sep",
          "oct",
          "nov",
          "dic",
        ],
        accept: "Aceptar",
        reject: "Cancelar",
        chooseDate: "Elegir fecha",
        chooseYear: "Elegir año",
        chooseMonth: "Elegir mes",
        prevMonth: "Mes anterior",
        nextMonth: "Mes siguiente",
        prevYear: "Año anterior",
        nextYear: "Año siguiente",
        prevDecade: "Década anterior",
        nextDecade: "Década siguiente",
        aria: {
          close: "Cerrar",
          navigation: "Navegación",
          previous: "Anterior",
          next: "Siguiente",
          pageLabel: "Página {page}",
          firstPageLabel: "Primera página",
          lastPageLabel: "Última página",
          nextPageLabel: "Página siguiente",
          prevPageLabel: "Página anterior",
          rowsPerPageLabel: "Registros por página",
          selectAll: "Seleccionar todos",
          unselectAll: "Quitar selección",
          listLabel: "Opciones",
        },
      },
    })
    .mount("#app");
}
start().catch(() => {
  const root = document.getElementById("app")!;
  root.textContent =
    "No se pudo conectar con el servidor. Comprobá tu conexión y volvé a cargar la página.";
  const retry = document.createElement("button");
  retry.textContent = "Volver a intentar";
  retry.addEventListener("click", () => window.location.reload());
  root.append(retry);
});
