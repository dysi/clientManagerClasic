# AGENTS.md — DySI Client Manager

## Alcance y fuente de instrucciones

Este archivo contiene las instrucciones de trabajo para agentes en todo este repositorio: backend, frontend, pruebas, configuración, scripts y documentación. Las instrucciones explícitas del usuario y las de mayor prioridad prevalecen sobre estas convenciones.

- No crear archivos de instrucciones paralelos como `agent.md`, `CLAUDE.md` u otros `AGENTS.md` sin pedido explícito.
- Usar el código, la configuración y las pruebas actuales como evidencia del comportamiento. El README y los documentos históricos pueden quedar desactualizados; corregirlos cuando el cambio lo requiera.
- Los documentos de otros proyectos son referencias, no instrucciones activas ni evidencia de que sus tecnologías o reglas existen aquí.
- Antes de editar, revisar `git status --short` y leer los archivos involucrados. Preservar los cambios locales existentes; no revertirlos, sobrescribirlos ni incluirlos en una limpieza ajena al pedido.

## Conducta y alcance del cambio

- Un pedido de análisis o revisión requiere presentar hallazgos; un pedido de implementación, corrección o actualización autoriza los cambios necesarios dentro de su alcance.
- Investigar los consumidores, contratos y pruebas relacionados antes de modificar una funcionalidad. No mezclar actualizaciones de dependencias o refactors sin relación con el requerimiento.
- Resolver decisiones rutinarias y reversibles dentro del alcance autorizado. Consultar cuando falte una decisión funcional imprescindible o una acción pueda destruir datos o romper un contrato que debe mantenerse.
- Priorizar correctitud, seguridad y legibilidad. Usar nombres descriptivos, responsabilidades acotadas y flujo de control claro; evitar expresiones excesivamente compactas y abstracciones innecesarias.
- Documentar intención, reglas de negocio, restricciones y efectos laterales no evidentes mediante Javadoc, TSDoc o comentarios breves. No repetir literalmente el código ni conservar comentarios obsoletos.
- Mantener el formato de los archivos involucrados y UTF-8. Verificar el encoding real antes de corregir supuestos problemas de caracteres. No aplicar formato masivo para un cambio localizado.
- Eliminar imports, variables y dependencias que queden sin uso por el cambio, sin limpiar deuda ajena al alcance.
- No incorporar secretos, contraseñas reales, tokens, datos personales ni credenciales de producción a código, fixtures, capturas, logs o documentación.
- Actualizar la documentación afectada y explicar las limitaciones reales de lo implementado.

## Contexto técnico

Monolito web para administrar clientes, usuarios, licencias y pagos de AngryBox. Se distribuye como un único JAR con frontend compilado y Tomcat embebido.

| Capa | Tecnología y convención |
| --- | --- |
| Backend | Java 11, Spring Boot 2.7.18, Spring MVC, Spring Security, Bean Validation |
| Persistencia | Spring Data JPA / Hibernate, MySQL en ejecución normal, H2 en pruebas |
| Build | Maven Wrapper 3.9.9, `frontend-maven-plugin`, Spring Boot repackage |
| Frontend | Vue 3, Composition API, componentes SFC con TypeScript estricto, Vite |
| Interfaz | PrimeVue 4, tema Aura con tokens semánticos, Tailwind CSS 4, PrimeIcons |
| Navegación | Vue Router con historial hash y carga diferida de páginas |
| Seguridad web | Sesión HttpOnly, CSRF y autorización administrativa en servidor |
| Integraciones | API existente con Bearer/JWT; compatibilidad con AngryBox |
| Pruebas | JUnit 5, MockMvc, servidor HTTP real en pruebas puntuales, Vitest, Vue Test Utils y Playwright |
| Idioma y formatos | Español con voseo, fechas locales y formatos `es-AR`; importes mostrados en ARS |

Las versiones efectivas se consultan en `pom.xml`, `.mvn/wrapper/maven-wrapper.properties`, `frontend/package.json` y `frontend/package-lock.json`. No actualizar versiones mayores como parte incidental de otro cambio. Mantener compatibilidad con Java 11; no introducir APIs de Java 17 ni migrar imports `javax.persistence`, `javax.validation` o `javax.servlet` a Jakarta por uniformidad. La dependencia JAXB explícita para JJWT no implica que el backend use Spring Boot 3.

### Ubicaciones principales

- Backend: `src/main/java/com/dysi/AngryBoxClientManager/`.
- Configuración: `src/main/resources/application*.properties` y `application.yml`.
- Pruebas Java: `src/test/java/`; configuración aislada: `src/test/resources/application-test.properties`.
- Frontend: `frontend/src/`; páginas por dominio en `features/{auth,clients,licenses,payments,users}/`.
- Componentes compartidos: `frontend/src/components/`; comportamiento reutilizable: `frontend/src/composables/`.
- HTTP, contratos y formatos: `frontend/src/services/`.
- Pruebas unitarias Vue/TypeScript: archivos `*.test.ts` junto al código; navegador: `frontend/tests/e2e/`.
- `src/main/webapp` no contiene la aplicación Vue. No recrear allí el frontend ni reintroducir las pantallas Thymeleaf retiradas.
- Los archivos bajo `target/` y `frontend/node_modules/` son generados. No editarlos como fuentes ni incorporarlos al control de versiones.

## Reglas de dominio sensibles

Antes de cambiar estas reglas, leer servicios, entidades, repositorios, DTO y pruebas relacionados. Si el usuario solicita modificarlas, actualizar implementación, consumidores y verificaciones de forma coherente.

### Clientes y usuarios

- La edición de clientes conserva nombre, apellido, correo, teléfono y CUIL/CUIT. No guardar una proyección incompleta sobre la entidad ni perder campos de contacto omitidos por un consumidor anterior.
- La vinculación usuario–cliente debe actualizar ambos lados de la relación. Un cliente no puede quedar vinculado a dos cuentas desde el flujo web.
- Editar la identidad o los permisos de un usuario conserva su hash de contraseña. El cambio de contraseña es una operación y pantalla independientes.
- El usuario activo no puede deshabilitar su propia cuenta ni quitarse su rol ADMIN desde el flujo administrativo.
- Los roles son `ROLE_ADMIN`, `ROLE_USER` y `ROLE_SELLER`; sus IDs dependen de la base. No fijar IDs ni crear administradores o accesos de demostración en producción.
- Las contraseñas nuevas del flujo web requieren al menos 12 caracteres y como máximo 72 bytes UTF-8. Verificar bytes, no solo caracteres, por el límite de BCrypt.

### Licencias

- `License.companyId` identifica a la empresa y es distinto del ID de cliente y del ID interno de licencia. No importar el concepto `Site.uniqueSiteId` de DysiCRM: este proyecto no tiene esa entidad.
- Una licencia existente no cambia de cliente desde el formulario actual. Comprobar duplicados de `companyId` al crear y editar; la comprobación de aplicación no sustituye una futura restricción de base de datos.
- `LicenseStatus` contiene vigencia y bloqueo forzado. Actualmente el flujo web toma el estado no eliminado de menor ID y crea uno al guardar una licencia sin estado. No cambiar la selección histórica ni asumir que siempre existe `get(0)`.
- Con bloqueo forzado, la licencia está bloqueada. Sin él, vence al comenzar la fecha de bloqueo; el estado por vencer comprende los próximos 15 días. El filtro de activas incluye las vigentes que están por vencer.
- En la tabla, mostrar fecha de bloqueo y días restantes calculados por el servidor. El bloqueo forzado prioriza fondo rojo; vencida usa fondo/estado amarillo; por vencer (1 a 15 días) se indica en rojo. Conservar los colores semánticos durante hover.
- Bloquear/desbloquear expresa el estado deseado. Reintentar la misma petición no debe alternarlo.

### Pagos y fechas

- Un pago pertenece a una licencia y no cambia de licencia desde su edición. Un ID de edición inexistente debe producir 404, no un alta implícita ni una respuesta de éxito vacía.
- El descuento no supera el importe; validar precisión y límites en servidor. El contrato web utiliza `BigDecimal`, pero el modelo persistente conserva `Double`: no afirmar que existe precisión decimal garantizada en la base ni cambiar columnas sin plan de migración.
- Anular marca la fecha de baja y conserva el historial. Repetir la anulación preserva esa fecha; reactivar la limpia. Un pago anulado debe reactivarse antes de editarse.
- Registrar o cobrar un pago no extiende automáticamente la vigencia de la licencia.
- El contrato web transmite fechas de calendario como `YYYY-MM-DD`, sin conversión UTC que cambie el día. Reutilizar `services/format.ts` y `DateInput.vue`; en backend respetar la conversión entre `LocalDate` y el modelo existente con `Calendar`.
- La zona horaria del servidor afecta las reglas actuales. Verificar límites de día, vencimiento y nulabilidad al cambiar fechas.
- El portal de estado de cuenta es una evolución pendiente. No asumir que existen facturas, ledger, saldo a favor o conciliación; tampoco mostrar datos ficticios como información real del cliente.

## Backend

### Arquitectura y contratos

- Mantener el flujo controlador REST -> servicio transaccional -> repositorio -> entidad. Los controladores coordinan HTTP y validación; la lógica de negocio reutilizable pertenece a servicios.
- El flujo web usa `WebClientController`, `WebAdminController`, `WebAdminService`, `ClientInput`, `AdminInputs` y `AdminViews`. Revisar estas piezas antes de extenderlo.
- Conviven servicios existentes con interfaces y servicios concretos. Seguir la estructura vecina sin imponer una reorganización global.
- Preferir inyección por constructor en código nuevo.
- Usar DTO explícitos para la API web, Bean Validation y respuestas JSON coherentes mediante `WebErrorHandler`. No exponer entidades completas, hashes, trazas o detalles internos.
- Mantener tipos y nulabilidad alineados entre DTO, servicio y formulario. Validar relaciones, IDs, permisos y registros inexistentes antes de persistir.
- Preservar estados HTTP apropiados: 400 para entrada inválida, 401 sin sesión válida, 403 sin permisos, 404 para ausencias y 409 para conflictos de negocio.
- Los listados web usan 50 registros por defecto y ofrecen tamaños 25, 50 y 100 en el frontend.
- Paginar y filtrar en base de datos; reutilizar `JpaSpecificationExecutor` y las consultas existentes. Mantener los límites actuales del contrato web o cambiar sus consumidores y pruebas explícitamente.
- Parametrizar JPQL/SQL y escapar comodines cuando la búsqueda deba ser literal. No concatenar entradas externas ni cargar todo el listado para paginarlo en memoria.

### Transacciones y esquema

- Definir transacciones en servicios y usar `@Transactional(readOnly = true)` para lecturas. Las operaciones compuestas de asociaciones deben ser atómicas.
- Revisar lado propietario, cascadas y `orphanRemoval` antes de cambiar relaciones. No resolver N+1 convirtiendo todas las asociaciones a EAGER.
- Preservar nombres físicos y comportamiento de generadores de IDs necesarios para el esquema existente.
- No hay Liquibase ni Flyway configurado. No inventar changelogs, comandos de migración o garantías de esquema inexistentes.
- `ddl-auto=update` está presente en la configuración normal y puede modificar MySQL. Antes de ejecutar la aplicación, verificar el destino y las propiedades efectivas; no usar una base compartida para pruebas.
- Un cambio de esquema requiere definir migración reproducible, compatibilidad con datos existentes y validación; H2 no demuestra por sí solo compatibilidad con MySQL.
- No recrear bases con datos, ejecutar restauraciones ni aplicar correcciones destructivas sin autorización explícita y estrategia de recuperación.

## Seguridad y compatibilidad

- Mantener separadas las dos cadenas: `WebSessionSecurityConfig` para el navegador y `WebSecurityConfig` para Bearer/JWT. Revisar sus matchers y orden al agregar rutas.
- El frontend consume `/web/session` y `/web/api/...` mediante `services/http.ts`. Preservar cookies same-origin, CSRF en escrituras, rotación de sesión al ingresar y logout real en servidor.
- No almacenar tokens de acceso en localStorage, deshabilitar CSRF para facilitar formularios ni registrar globalmente el filtro JWT sobre el tráfico de sesión.
- La API administrativa exige ADMIN en servidor; los guards, botones ocultos y filtros de cliente del frontend no constituyen autorización.
- Preservar la revalidación de cuenta, roles y cambio de contraseña en `WebSessionUserFilter`. No permitir que una sesión deshabilitada siga operando.
- No debilitar CSP para incorporar scripts inline, `unsafe-eval`, CDNs o HTML arbitrario. PrimeVue requiere actualmente estilos dinámicos; no confundir esa excepción con permiso para ejecutar scripts inline.
- Preservar los contratos externos existentes de `/auth`, `/refresh`, `/user` y `/api/...`, incluidos nombres JSON y parámetros utilizados por AngryBox. Leer controlador y consumidores antes de cambiarlos; no trasladar contratos del otro proyecto.
- Las operaciones y problemas pendientes de los servicios antiguos siguen existiendo aunque el frontend nuevo no los use. No presentar una corrección web como una auditoría completa de JWT o de toda la API anterior.
- El futuro portal de clientes requiere autorización por propietario en cada consulta. No habilitar las rutas administrativas para USER/SELLER como sustituto de ese módulo.

## Frontend

### Componentes y estado

- Usar Composition API con `<script setup lang="ts">`, tipos explícitos y la organización por funcionalidades existente.
- Separar listados, fichas y formularios según la operación. El formulario de contraseña permanece separado del perfil. No reconstruir una pantalla monolítica con múltiples formularios mezclados.
- Reutilizar `PageHeader`, `FormField`, `EntitySelect`, `DateInput`, `ListToolbar`, `ListPaginator`, `useDraft`, `usePagedList` y `useConfirmAction` antes de duplicar comportamiento.
- Usar tipos concretos y `unknown` validado. No introducir `any`, aserciones no nulas o casts para esconder incompatibilidades modelables.
- Mantener el HTTP común y los contratos en servicios. Construir parámetros mediante `URLSearchParams` y respetar `ApiError` y sus errores por campo.
- Representar carga, error, vacío y éxito. Restablecer loading en `finally`, impedir doble envío y proteger respuestas de búsquedas que lleguen fuera de orden.
- Conservar borradores ante errores y confirmar descarte cuando haya cambios pendientes. Al guardar o cancelar, actualizar el estado de borrador para evitar confirmaciones repetidas.
- Usar claves estables de dominio en listas; no mutar objetos compartidos o props al editar una ficha.

### Navegación y acceso

- Mantener historial hash, páginas lazy y resolución del contexto sin hostname fijo en producción.
- La entrada por el puerto se resuelve en `PortEntryConfig`; la entrada SPA y los alias anteriores, en `FrontendController`. Una prueba MockMvc no verifica una ruta fuera del contexto servlet: usar HTTP real para ese caso.
- La pantalla principal actual es `/licenses`. Sin sesión, redirigir al login; con sesión, abrir la principal o el destino interno solicitado.
- Reutilizar `features/auth/navigation.ts` para conservar destino y filtros tras autenticación o vencimiento. Rechazar retornos externos, desconocidos o circulares al login.
- Un login fallido conserva el destino pendiente. El logout voluntario no debe recuperar automáticamente una pantalla protegida.
- La configuración de desarrollo utiliza `/clientmanager` y proxy Vite al backend en 3000. Si cambia ese contrato, revisar conjuntamente `vite.config.ts`, `services/http.ts`, documentación y pruebas.

### Estilos, contenido y accesibilidad

- Usar componentes PrimeVue, tema Aura y tokens semánticos centralizados en `main.ts`; componer layout y espaciado con Tailwind y `tailwindcss-primeui`.
- No reintroducir Bootstrap, jQuery, Knockout, Alertify, FontAwesome, WebJars ni estilos de las pantallas retiradas.
- No crear una nueva capa de CSS artesanal ni sobreescrituras de selectores internos `.p-*`. Agotar primero componentes, tokens, utilidades y APIs públicas del framework.
- Mantener recursos locales; no agregar fuentes o scripts de terceros sin una necesidad concreta y revisión de compatibilidad con CSP.
- Escribir textos claros en español y con voseo consistente. No asumir una infraestructura i18n multilingüe que no existe ni introducirla fuera del alcance.
- Conservar la identidad de portal del login, la bienvenida y la versión móvil que prioriza el formulario. No agregar recuperación de contraseña, saldos o garantías de seguridad meramente decorativas.
- Usar HTML semántico, labels asociados, nombres accesibles en controles de icono, foco visible y navegación por teclado. Asociar ayuda/errores con el campo; no transmitir estados solo con color.
- Los botones que no envían formularios deben usar `type="button"`; la navegación usa enlaces. Evitar que enlaces a anclas rompan el historial hash.
- Revisar visualmente los cambios de presentación en escritorio y móvil. El scroll de tablas debe quedar en su contenedor y no desbordar toda la página.

## Pruebas y verificación

- Elegir verificaciones proporcionales al cambio. Una corrección de lógica, permisos, persistencia o navegación necesita regresión significativa cuando sea viable; un cambio documental o visual simple no requiere pruebas unitarias que repliquen la implementación.
- Usar JUnit 5 y MockMvc para contratos, validación, seguridad y persistencia observable; servidor real para redirecciones fuera del contexto. Mantener `@ActiveProfiles("test")` y H2 aislada.
- Probar consultas y relaciones no triviales contra la base de pruebas; mocks no verifican Criteria, JPQL ni cascadas.
- Usar Vitest y Vue Test Utils para estado y comportamiento; Playwright para recorridos completos, sesión, formularios y adaptación visual.
- Las pruebas Playwright crean y modifican datos: ejecutarlas solo contra una base aislada con cuentas de prueba. Los tres roles deben estar configurados si el recorrido crea usuarios.
- No incorporar cuentas de bootstrap o H2 al artefacto de producción para facilitar las pruebas. No depender de scripts temporales de otro equipo que no estén versionados.
- No desactivar pruebas ni debilitar assertions para obtener una suite verde. Informar qué no se verificó y por qué.
- Tras cambios que afecten recursos empaquetados, verificar un build limpio: una copia incremental a `target/classes` puede conservar assets antiguos.

## Comandos disponibles

Desde la raíz, con JDK 11 seleccionado:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd clean verify
# Alternativa que además instala el artefacto en el repositorio Maven local:
.\mvnw.cmd clean install
java -jar target/clientmanager-0.0.1-SNAPSHOT.jar
```

En Linux/macOS usar `./mvnw`. `clean verify` instala Node/npm locales según el POM, ejecuta `npm ci`, comprueba TypeScript, compila Vue, ejecuta las pruebas Java y Vitest y empaqueta el JAR. Playwright se ejecuta aparte. El servidor no necesita Node ni Tomcat externo; MySQL y los secretos del entorno sí deben estar configurados antes del arranque.

Para una verificación Java focalizada, con dependencias disponibles, sin volver a ejecutar el ciclo npm:

```powershell
.\mvnw.cmd resources:resources compiler:compile resources:testResources compiler:testCompile surefire:test "-Dtest=WebFrontendTests"
```

Este comando no genera un JAR desplegable ni sustituye la verificación integrada cuando esta corresponde. Para propiedades Maven con punto en PowerShell, entrecomillar el argumento completo, por ejemplo `"-Dspring.profiles.active=test"`.

Desde `frontend/`, con Node/npm compatibles disponibles:

```powershell
npm ci
npm run dev
npm test
npm run build
# Contra un backend de pruebas ya iniciado:
$env:E2E_BASE_URL = 'http://127.0.0.1:3000/clientmanager/ui/'
$env:E2E_USER = '<administrador-de-pruebas>'
$env:E2E_PASSWORD = '<contraseña-de-pruebas>'
$env:E2E_CHANNEL = 'msedge'
npm run test:e2e
```

- Maven instala su runtime en `target/frontend-runtime/node/`; si npm no está en PATH, usar ese runtime o una instalación compatible, no inventar un `npmw` inexistente.
- `npm run build` ejecuta `vue-tsc --noEmit` y Vite. No hay scripts `lint` ni `prettier:check` configurados actualmente; comprobar `package.json` antes de indicar comandos.
- No ejecutar `npm ci` simultáneamente con builds o pruebas que utilicen el mismo `node_modules`, especialmente en Windows.
- La salida Vite va a `target/generated-resources/frontend/static/ui` y Maven la incluye en el JAR. No copiar fuentes Vue manualmente a recursos.
- La entrada normal es `http://localhost:3000/`; el contexto predeterminado es `/clientmanager`.

## Documentación y artefactos auxiliares

- Mantener `README.md` como guía de arquitectura, compilación, ejecución, navegación, reglas y limitaciones. No mantener aquí contadores de pruebas o listas de versiones que deban actualizarse en cada entrega.
- Reservar la raíz para archivos estándar de entrada, build y configuración. Si se necesitan documentos adicionales, organizarlos bajo `docs/architecture/`, `docs/deployment/`, `docs/operations/` o `docs/integrations/<sistema>/`, sin crear carpetas vacías por anticipado.
- Si se incorporan scripts, ubicarlos bajo `scripts/development/`, `scripts/deployment/` o `scripts/database/` según su finalidad; resolver rutas desde su propia ubicación.
- Los SQL de diagnóstico, fixtures o corrección no son migraciones ni deben ejecutarse automáticamente. Documentar destino, precondiciones y validaciones; usar fixtures únicamente en bases de prueba.
- Guardar capturas y reportes regenerables en `target/` o los directorios de resultados ignorados. No versionar datos sensibles ni documentos históricos como instrucciones actuales.

## Entrega

- Resumir qué cambió, por qué y cómo se verificó. Distinguir pruebas ejecutadas de recomendaciones o verificaciones pendientes.
- Señalar riesgos materiales, compatibilidad pendiente con MySQL/AngryBox y limitaciones del módulo afectado; no afirmar validación de producción a partir de H2.
- Mantener documentación, mensajes de entrega y descripciones de commits/PR en español salvo indicación contraria.
- No crear commits, publicar, desplegar ni modificar datos compartidos solo por haber terminado un cambio local; respetar el alcance autorizado por el usuario.
