# DySI Client Manager

La recepción de clientes y deudas desde Angrybox está implementada en
`POST /api/clients/sync`. Requiere habilitación explícita, empresa de origen y
Bearer con el authority configurado (ROLE_API por defecto). Ver [contrato, reglas de vinculación y despliegue](docs/integrations/angrybox/sincronizacion-clientes.md).

Monolito web para administrar clientes, cuentas, licencias de empresas y pagos asociados a AngryBox. Backend en **Java 11 / Spring Boot 2.7.18** y frontend en **Vue 3 / TypeScript / PrimeVue 4 / Tailwind CSS 4**, distribuido en un único **JAR ejecutable** con Tomcat embebido.

## Compilar

Seleccionar un JDK 11 y ejecutar desde la raíz:

```powershell
$env:JAVA_HOME = '<ruta-al-jdk-11>'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean verify
```

En Linux/macOS: `./mvnw clean verify`. El wrapper utiliza Maven 3.9.9. Maven instala Node 24.22.1 y npm 10.9.2 en `target/frontend-runtime`, instala el lockfile con `npm ci`, comprueba TypeScript, compila Vue, ejecuta Vitest y las pruebas Java, y genera:

```text
target/clientmanager-0.0.1-SNAPSHOT.jar
```

La primera compilación necesita conexión con Maven Central, nodejs.org y npm. El servidor de destino solo necesita Java 11 y acceso a MySQL: no requiere Node, npm, Maven ni una instalación externa de Tomcat. Desplegar el `.jar`, no el `.jar.original`.

## Ejecutar

Configurar el entorno antes de iniciar:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/dysi_clientmanager'
$env:SPRING_DATASOURCE_USERNAME = '<usuario>'
$env:SPRING_DATASOURCE_PASSWORD = '<contraseña>'
$env:JWT_SECRET = '<secreto-del-entorno>'
$env:SERVER_PORT = '3000'
java -jar target/clientmanager-0.0.1-SNAPSHOT.jar
```

Entrada desde el puerto: **http://localhost:3000/**. Redirige automáticamente al contexto configurado y a la interfaz, también accesible en **http://localhost:3000/clientmanager/ui/**. La raíz redirige a `/ui/`; el router utiliza fragmentos, por ejemplo `/ui/#/clients`. En producción, las llamadas HTTP deducen el contexto desde la URL. Para cambiarlo, configurar `server.servlet.context-path`; para cambiar el puerto, `--server.port=8080`.

Puede usarse configuración externa con `--spring.config.additional-location=file:./config/`. En un servidor, ejecutar el proceso mediante el gestor de servicios del sistema operativo y configurar HTTPS y las cabeceras del proxy. La cookie de sesión es HttpOnly y SameSite=Lax; exigir Secure en un despliegue HTTPS, especialmente si TLS termina en un proxy.

No existe una cuenta de demostración ni bootstrap de administrador. La base debe contener una cuenta habilitada con contraseña BCrypt, rol `ROLE_ADMIN` y las autoridades base (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_SELLER`). El permiso **Acceso API** (`ROLE_API`) se crea al asignarlo explícitamente desde Usuarios si todavía no existe. Los IDs de los roles dependen de la base y no se fijan desde la interfaz.

La configuración heredada contiene valores de conexión y secreto que deben sustituirse mediante configuración externa y rotarse si se utilizaron. Este documento no reproduce sus valores. `ddl-auto=update` puede modificar el esquema al arrancar; validar primero una copia del entorno y usar `SPRING_JPA_HIBERNATE_DDLAUTO=validate` cuando corresponda. Los cambios de base se versionan como scripts MySQL manuales en `src/main/resources/db/`; no se ejecutan automáticamente.

## Navegación y bienvenida

- Abrir `http://localhost:3000/` redirige a la aplicación, conservando el contexto `/clientmanager` y las URLs de las integraciones existentes. Si el despliegue usa otro contexto, la entrada se adapta a esa configuración.
- Sin sesión, la entrada principal lleva a `/ui/#/access` (DNI/CUIT), con un enlace al login de usuarios en `/ui/#/login`. Con sesión, ADMIN autenticado con contraseña abre `/licenses`; los accesos restringidos abren `/portal`.
- Una URL protegida conserva su ruta y filtros en `redirect`; al autenticarse se regresa a esa pantalla. Solo se aceptan destinos internos conocidos y permitidos para el tipo de sesión; se rechazan URLs externas, rutas inexistentes y retornos a las pantallas de ingreso.
- Una sesión vencida conserva el destino pendiente: el portal vuelve al ingreso por documento y la administración al login de usuarios. Un intento fallido conserva ese destino. El cierre voluntario no restaura automáticamente la pantalla anterior.
- Abrir el login con sesión vigente lleva a la pantalla principal, evitando un segundo ingreso innecesario.

El login presenta una bienvenida de portal de clientes, identidad visual DySI, formulario con autocompletado y opción para ver la contraseña, ayuda de acceso y un diseño móvil compacto. Usa componentes PrimeVue y utilidades Tailwind, sin nuevas hojas de estilos manuales. La ayuda explica cómo contactar al referente de DySI; no existe todavía recuperación automática por correo.

El estado de cuenta para clientes aún requiere su módulo, reglas y autorización por propietario. La bienvenida prepara esa identidad de producto; las pantallas administrativas continúan exigiendo ADMIN y no se muestran saldos ficticios ni se concede acceso a datos administrativos a clientes.

## Arquitectura

```text
frontend/
  src/
    components/       Campos, fechas, selectores remotos, cabeceras y paginación
    composables/      Borradores, confirmaciones y listados paginados
    features/
      auth/           Inicio de sesión y estado de autenticación
      clients/        Listado, ficha y formulario de clientes
      licenses/       Listado y formulario de licencias
      payments/       Consulta de pagos y aviso de transición
      users/          Listado, formulario y cambio de contraseña
    services/         HTTP, contratos y formatos
    router.ts         Rutas y control de navegación
    main.ts           Tema, idioma y registro de servicios
    styles.css        Capas e importaciones del framework
  tests/e2e/          Recorrido de navegador con Playwright
src/main/java/com/dysi/AngryBoxClientManager/
  controllers/        Entrada SPA y redirecciones de rutas anteriores
  restControllers/    API web, sesión y API externa compatible
  dtos/               Contratos de entrada/salida y validación
  servicesImpl/       Operaciones transaccionales y servicios existentes
  repositories/       Consultas JPA y filtros paginados
  models/             Entidades persistentes
  security/           JWT y comprobación de sesiones
  config/             Cadenas de seguridad, MVC y configuración
src/main/resources/   Configuración del backend
src/test/             Pruebas con base H2 aislada
```

Maven incorpora el resultado de Vite desde `target/generated-resources/frontend/static/ui` a `BOOT-INF/classes/static/ui` dentro del JAR. No se copian `node_modules` ni las fuentes Vue. `src/main/webapp` no participa en la interfaz y puede permanecer vacío. Las carpetas antiguas `templates` y los recursos manuales de `static` ya no contienen las pantallas del producto.

Se mantiene un solo proceso y una sola base de datos. No se requiere un servidor de frontend independiente para producción.

## Refactor del frontend

Se reemplazaron las pantallas Thymeleaf y los scripts Knockout/jQuery por módulos Vue. Se eliminaron Bootstrap, Alertify, FontAwesome, WebJars y los estilos manuales anteriores, además de las dependencias de Thymeleaf y Data REST. La API externa existente se conserva por compatibilidad; el frontend nuevo usa exclusivamente `/web/...`.

Los estilos se basan en **PrimeVue con tema Aura**, tokens semánticos centralizados y utilidades **Tailwind**, integradas mediante `tailwindcss-primeui`. `styles.css` declara capas, importa el framework y establece la tipografía base; no contiene una hoja de estilos heredada ni sobreescrituras de selectores internos `.p-*`. Los iconos usan PrimeIcons y se empaquetan localmente, sin CDN.

Criterios de mantenimiento:

- Usar componentes PrimeVue para controles, tablas, diálogos, mensajes y navegación.
- Componer distribución y espaciado mediante utilidades; centralizar cambios de identidad visual en el tema.
- Separar listados y formularios; agrupar campos por su propósito en tarjetas.
- Mantener contratos TypeScript y DTO Java explícitos, sin enviar entidades persistentes completas.
- Validar en servidor, conservar borradores ante errores y pedir confirmación al descartar cambios.
- Usar etiquetas asociadas a campos, mensajes por campo y estados de carga; revisar teclado y lectores de pantalla al incorporar nuevas funciones.

### Pantallas y operaciones

| Módulo | Rutas dentro de `/ui/#` | Funciones |
| --- | --- | --- |
| Acceso | `/access`, `/login` | Documento o usuario/contraseña, errores de acceso y cierre de sesión confirmado |
| Revisión de pagos | `/payments`, `/payments/:id` | Filtros, comprobantes, comentarios públicos/privados, historial y decisión con acciones opcionales de licencia |
| Portal | `/portal`, `/portal/payments/new`, `/portal/payments/:id/comments` | Estado general, licencias, saldo sincronizado, presentación de transferencias y comentarios públicos |
| Clientes | `/clients`, `/clients/new`, `/clients/:id`, `/clients/:id/edit` | Búsqueda, paginación, ficha de contacto, alta/edición y accesos a licencias/usuarios del cliente |
| Licencias | `/licenses`, `/licenses/new`, `/licenses/:id/edit` | Búsqueda y filtros por cliente/estado, empresa, vigencia, modalidad y bloqueo explícito |
| Pagos administrativos | `/payments`, `/payments/new`, `/payments/:id/edit` | Consulta por cliente/licencia y estado; escritura antigua cerrada. La revisión y aprobación corresponden a la etapa 5 |
| Usuarios | `/users`, `/users/new`, `/users/:id/edit`, `/users/:id/password` | Identidad, roles, habilitación, vínculo opcional con cliente y formulario de contraseña separado |

Las operaciones iniciadas desde la ficha de un cliente o una licencia llevan la relación preseleccionada. No se cambia el cliente de una licencia existente ni la licencia de un pago existente. El formulario de usuario existente conserva el hash de contraseña; solo el formulario específico de contraseña lo modifica. La cuenta activa no puede deshabilitarse ni quitarse su propio rol ADMIN.

Los listados muestran cargas, errores y estados vacíos. La paginación ofrece 25, 50 y 100 registros, con 50 por defecto. Las filas se resaltan al pasar el mouse mediante la opción nativa de PrimeVue. Licencias permite combinar búsqueda de texto, estado y un selector de clientes con búsqueda remota; limpiar el cliente conserva los demás filtros. Usuarios permite combinar búsqueda de texto, estado habilitado/deshabilitado, cliente con búsqueda remota y permiso asignado (incluido Acceso API); cada cambio vuelve a la primera página y los filtros se aplican en base de datos. Las confirmaciones usan el diálogo del framework y los resultados se notifican con Toast. La navegación y los formularios se adaptan al ancho disponible; las tablas permiten desplazamiento horizontal cuando sus columnas lo requieren.

En el acceso administrativo por usuario y contraseña, la cabecera muestra el nombre del sistema y la cuenta/cierre de sesión. Clientes, Licencias, Pagos, Usuarios y Parámetros se encuentran en el panel vertical izquierdo. Desde 1024 píxeles se puede reducir a iconos; esa preferencia visual se conserva en el navegador. En anchos menores, el botón Menú abre un Drawer de PrimeVue con bloqueo de desplazamiento y navegación por teclado; se cierra con Escape, el botón de cierre, un toque fuera o una navegación confirmada. Cambiar a escritorio también cierra el panel móvil. La sección actual queda marcada y el foco vuelve al control de apertura o al contenido según corresponda. Este panel depende del acceso ADMIN y no aparece en el portal de clientes; la autorización del servidor se mantiene.

Verificación del panel administrativo: `clean verify` y los recorridos Playwright existentes pasaron, estos últimos con H2 aislada y datos sintéticos. Se revisaron escritorio, panel reducido, móviles de 390/320 píxeles, navegación por teclado, sección activa, foco, cierres del Drawer, cambios de ancho y conservación/cancelación de borradores al navegar. Las comprobaciones visuales con respuestas sintéticas también confirmaron la ausencia del panel administrativo en el portal. No requiere SQL ni incorpora pruebas de cobertura nuevas.

Las URLs anteriores `/users/login`, `/index/home`, `/users/create`, `/users/create/{id}` y `/users/edit/{id}` redirigen a las rutas nuevas. Ya no renderizan pantallas anteriores.

## Modelo y reglas

- **Client**: identificación, contacto y vinculación con una cuenta. Incluye razón social (`legalName`, hasta 255 caracteres), nombre de fantasía (`tradeName`, hasta 255) y código externo (`externalCode`, hasta 100), todos opcionales. El ABM permite cargarlos, consultarlos y limpiarlos. Se recortan espacios al comienzo/final y un valor vacío se guarda como NULL. Omitirlos o enviarlos NULL en una edición conserva el dato; enviar una cadena vacía lo limpia. No se impone unicidad al código externo. Se conserva toda la ficha durante las ediciones.
- **User / Authority**: identidad, contraseña BCrypt, habilitación y roles. El vínculo usuario–cliente se actualiza en ambos sentidos y se rechaza un cliente ya vinculado a otra cuenta.
- **License**: cliente propietario, identificador/nombre de empresa, descripción, inicio de actividad y modalidad de pago.
- **LicenseStatus**: fecha de bloqueo, bloqueo forzado y datos históricos. Para compatibilidad se utiliza el estado activo de menor ID; al guardar una licencia sin estado se crea uno. Conviene normalizar esta regla mediante una migración futura.
- **Payment**: cliente obligatorio, licencia opcional, importe decimal ARS, emisión, teléfono, transferencia, cuenta y fotografía bancaria, origen, estado e indicador/motivo de ausencia de comprobante.

El identificador de empresa se verifica al crear y editar. La garantía bajo concurrencia requiere una restricción de base de datos, pendiente de una migración de esquema. Los importes de pagos y saldos utilizan `BigDecimal` y columnas `DECIMAL(14,2)`. Los campos antiguos de descuento y fechas de período/cobro/vencimiento se retiran mediante migración con vaciado previo autorizado.

La columna Fecha de bloqueo incluye el contador de días calculado en la zona horaria del servidor (`daysUntilLock`). Las filas bloqueadas usan fondo rojo; las vencidas, fondo y etiqueta amarillos; las que vencen dentro de 1 a 15 días muestran Por vencer en rojo. Los estados de peligro/advertencia conservan su color al pasar el mouse. Una licencia está bloqueada si tiene bloqueo forzado, vencida si su fecha de bloqueo es hoy o anterior, y por vencer si vence dentro de 15 días. El filtro de activas incluye las vigentes que están por vencer. Una licencia sin fecha aparece con estado desconocido y debe completarse. Las fechas viajan como `YYYY-MM-DD`; su interpretación usa el día local del servidor. Definir una zona horaria coherente entre despliegues.

Los pagos nuevos tienen estado inicial PENDIENTE. La aprobación o rechazo serán decisiones terminales del futuro flujo administrativo; los pagos no admiten edición genérica, anulación ni reactivación. Registrar un pago **no extiende automáticamente la licencia ni modifica el saldo sincronizado**. El bloqueo de licencias conserva su petición idempotente con el estado deseado.

## API web y seguridad

Todas las rutas siguientes son relativas al contexto del backend:

| Método / ruta | Contrato |
| --- | --- |
| `GET /web/session/support` | Contacto público de WhatsApp configurado; disponible sin sesión |
| `GET /web/session/csrf` | Prepara sesión y devuelve nombre de cabecera/token CSRF |
| `POST /web/session/login` | Autentica, rota identificador de sesión y CSRF; no devuelve JWT |
| `POST /web/session/document` | `{ "type": "DNI" o "CUIT", "document": "..." }`; sesión restringida, CSRF y límite de intentos |
| `GET /web/session` | Tipo de acceso, capacidades y origen; oculta el usuario asociado en sesiones del portal |
| `GET /web/portal/context` | Identificación mínima del cliente de la sesión; no acepta un cliente elegido por el navegador |
| `POST /web/session/logout` | Requiere CSRF e invalida la sesión |
| `GET /web/api/{clients,licenses,payments,users}` | Listados paginados, `keyword`, `page`, `size`; filtros propios por módulo |
| `GET /web/api/{recurso}/{id}` | Ficha completa |
| `POST /web/api/{recurso}` | Alta validada, respuesta 201; pagos cerrado con 409 durante la transición |
| `PUT /web/api/{recurso}/{id}` | Edición validada; pagos rechaza edición genérica |
| `PUT /web/api/licenses/{id}/lock` | `{ "locked": true/false }` |
| `PUT /web/api/payments/{id}/status` | Retirado funcionalmente: 409 (404 si el pago no existe) |
| `PUT /web/api/users/{id}/password` | Contraseña independiente, respuesta 204 |
| `GET /web/api/options/{clients,licenses}` | Autocompletado remoto, máximo 20 resultados |

La API administrativa exige `ROLE_ADMIN` en servidor. Las búsquedas limitan `size` a 100 y texto a 150 caracteres; los errores usan estados HTTP y JSON con `message` y, cuando corresponde, `fields`. Los DTO de usuario no exponen contraseñas ni hashes.

El navegador usa sesión HttpOnly, CSRF en escrituras y revalidación de la cuenta en cada petición. Deshabilitar una cuenta, cambiar roles o actualizar la contraseña desde la nueva API afecta a las sesiones existentes. La contraseña nueva debe tener al menos 12 caracteres y hasta 72 bytes UTF-8, por el límite de BCrypt. No se guarda un token de acceso en localStorage.

La política CSP de `/ui` restringe scripts y conexiones al mismo origen, bloquea objetos y frames externos. Permite estilos inline porque PrimeVue genera estilos dinámicos; no permite scripts inline ni `unsafe-eval`. La interfaz no utiliza `v-html` para mostrar datos del servidor.

Una segunda cadena de seguridad mantiene los consumidores Bearer/JWT existentes (`/auth`, `/refresh`, `/user`, `/api/...`). El refactor no supone una auditoría completa de esos contratos. Los servicios y endpoints anteriores conservados deben revisarse antes de ampliar su uso; la interfaz migrada no depende de ellos.

## Auditoría de entidades y cambios de base de datos

`Payment`, `LicenseStatus`, `Client`, `License` y `User` heredan de `AuditableEntity` (`@MappedSuperclass`). Cada tabla dispone de:

| Propiedad | Columna física | Comportamiento |
| --- | --- | --- |
| `creationDate` / `createdBy` | `creationdate` / `created_by_id` | Fecha y usuario de creación, inmutables en actualizaciones |
| `modificationDate` / `modifiedBy` | `modificationdate` / `modified_by_id` | Última escritura de la entidad, incluida su creación |
| `removeDate` / `removedBy` | `removedate` / `removed_by_id` | Anulación lógica; NULL mientras el registro esté activo |

Los usuarios son relaciones LAZY a `User`, sin cascadas. `AuditEntityListener` resuelve el actor a partir del ID del principal de sesión/JWT y registra las fechas en los callbacks JPA, también en persistencias por cascada. No se aceptan metadatos de auditoría desde JSON ni se serializan sus relaciones con usuarios. Los estados de licencia conservan sus propiedades y columnas. El contrato de pagos se reemplaza según la evolución documentada más abajo.

`markRemoved()` anula y `restore()` reactiva. Repetir una anulación conserva la fecha y el actor originales; actualizar otros campos de un registro anulado tampoco los reemplaza. Reactivar limpia fecha/actor y una anulación posterior registra un nuevo evento. Esto no crea endpoints adicionales de anulación para clientes/licencias/usuarios ni cambia automáticamente sus reglas de habilitación o los filtros de negocio.

Los registros históricos mantienen sus fechas conocidas y los datos desconocidos en NULL. Una escritura sin sesión registra fechas con actor NULL, sin inventar una cuenta de sistema. Los cambios exclusivos de roles marcan también al usuario como modificado. Las operaciones masivas JPQL/SQL y las modificaciones externas a JPA eluden los callbacks y requieren auditoría explícita. Se conserva el último evento de cada tipo; no es una tabla histórica de todas las revisiones.

Antes de iniciar esta versión sobre una base existente, aplicar manualmente **`src/main/resources/db/20261026-01-auditoria-entidades.sql`**, primero sobre una copia del esquema. El script agrega los campos faltantes y las claves foráneas a usuario; conserva `payment.creationdate/removedate` y `licensestatus.creationdate/removedate` y no rellena fechas ni actores ficticios. Leer sus precondiciones de motor InnoDB, nombres físicos y tipo de ID. MySQL hace commit implícito de DDL; la serie no tiene rollback transaccional global. `ddl-auto=update` no sustituye este procedimiento y puede adelantarse al script: aplicar el SQL con la aplicación detenida y utilizar `validate` al arrancar contra el esquema preparado.

Los cambios futuros de BD deben tener un archivo nuevo en `src/main/resources/db/`, con formato **`yyyyMMdd-NN-descripcion-breve.sql`**. La secuencia comienza en `01` cada día; antes de crear un archivo se revisan los números existentes y se toma el siguiente al máximo, sin repetir ni renumerar. Ejecutar por fecha y número secuencial. No hay Liquibase/Flyway ni ejecución automática de estos archivos al arrancar. Toda entidad nueva debe heredar la auditoría; `AGENTS.md` establece esta obligación.

Para los datos comerciales de clientes, aplicar [20261027-02-datos-comerciales-cliente.sql](src/main/resources/db/20261027-02-datos-comerciales-cliente.sql) con la aplicación detenida y las columnas nuevas aún ausentes. Agrega `legal_name`, `trade_name` y `external_code` anulables, conserva filas y auditoría y deja los valores históricos en NULL. Incluye comprobaciones de tipos, nulabilidad y cantidad de filas. Si `ddl-auto=update` ya creó las columnas, revisar el esquema antes de aplicar; no repetir el ALTER. Validar primero sobre una copia compatible de MySQL.

La búsqueda del directorio y de todos los selectores administrativos de cliente incluye razón social y nombre de fantasía. Los selectores muestran esos nombres junto al nombre personal. Las búsquedas generales de licencias y pagos, incluidos sus contadores, también los incluyen; se mantienen paginación y filtros en base de datos. La consulta histórica de clientes/licencias por Bearer amplía la misma búsqueda. Los DTO de cliente conservan sus atributos anteriores y agregan las tres propiedades opcionales en inglés.

Verificación de los datos comerciales: `clean verify` y los recorridos Playwright existentes pasaron. Sobre H2 aislada se comprobaron alta, lectura, edición, limpieza y conservación ante atributos omitidos/NULL, límites de longitud, consultas y proyecciones JPA, compatibilidad de la edición Bearer, comodines literales y búsquedas por razón social/fantasía en clientes, licencias, pagos, contadores y selectores. Se revisaron formulario y ficha en escritorio y móvil de 320 píxeles. Se actualizaron las expectativas de pruebas existentes; no se añadieron suites de cobertura. El SQL no se ejecutó contra una base compartida y sigue pendiente su validación en una copia de MySQL.

## Desarrollo del frontend

Con el backend iniciado y un Node compatible instalado, desde `frontend`:

```text
npm ci
npm run dev
npm test
npm run build
```

Vite sirve desarrollo en localhost y proxifica `/clientmanager/web` al backend en el puerto 3000. Si se cambia el contexto o puerto para desarrollo, ajustar `vite.config.ts` y la ruta de desarrollo de `services/http.ts`. El build de producción usa rutas relativas para sus assets y deduce el contexto para las llamadas al servidor. Las páginas se cargan de forma diferida por módulo.

## Verificación

`mvnw.cmd clean verify` comprueba Java 11, TypeScript, build Vite, Vitest, pruebas Java y empaquetado. H2 es una dependencia de pruebas y no forma parte del JAR de producción.

- Java: auditoría de las cinco entidades, usuarios de sesión/JWT, cascadas, anulación/reactivación, datos históricos y metadatos JSON; contexto, compatibilidad JWT, roles, CSRF, rotación/logout de sesión, cuentas deshabilitadas, conservación de contacto, validación/paginación, redirecciones antiguas, unicidad y propietario de licencias, bloqueo idempotente, modelo decimal de pagos, relaciones, cierre de escrituras antiguas, contraseñas y vínculos usuario–cliente.
- Vitest: borradores de clientes, cancelación, errores por campo, contrato HTTP/CSRF y conservación de fechas locales.
- Playwright: acceso, cookie de sesión, alta/edición de clientes, licencias y usuarios y consulta de pagos, descarte confirmado, persistencia tras recarga, bloqueo/desbloqueo, cierre de operaciones antiguas de pagos, cambio de contraseña, permisos y adaptación móvil.

Para Playwright, preparar una **base de pruebas aislada**, con los tres roles y un administrador habilitado, iniciar el backend y ejecutar desde `frontend`:

```powershell
$env:E2E_BASE_URL = 'http://127.0.0.1:3000/clientmanager/ui/'
$env:E2E_USER = '<administrador-de-pruebas>'
$env:E2E_PASSWORD = '<contraseña>'
$env:E2E_CHANNEL = 'msedge'
npm run test:e2e
```

El recorrido crea y modifica datos, por lo que no debe ejecutarse contra producción. Los datos temporales de la comprobación local se mantienen fuera del repositorio y del JAR. Las capturas se generan en `target/frontend-*.png`.

## Pendientes del backend y despliegue

1. Revisar autorización por propietario en `/api/clients/license`, que acepta un nombre de usuario solicitado; comprobar también cuentas deshabilitadas en la validación JWT externa.
2. Revisar renovación JWT, tratamiento de tokens inválidos/vencidos e invalidación de contraseñas en los servicios antiguos. Las correcciones del flujo web no sustituyen esa revisión.
3. Sustituir servicios antiguos que editan usuarios/pagos con contratos ambiguos, documentando primero a sus consumidores externos.
4. Mantener el registro de ejecución de los scripts MySQL y ampliar las migraciones con restricciones de unicidad/asociación, una regla explícita para estados históricos de licencia.
5. Validar esquema MySQL real, copias de respaldo, configuración de proxy/HTTPS y compatibilidad con AngryBox antes del despliegue.
6. Planificar actualización posterior de Java/Spring Boot y dependencias del backend; Java 11 y Boot 2.7 son la base de compatibilidad solicitada, no una actualización a las versiones mayores más recientes.
7. Medir consultas JPA, mejorar logs y ampliar pruebas de concurrencia, accesibilidad y carga según el uso real.

El análisis no aporta una necesidad demostrada de microservicios. La separación por módulos y contratos dentro del monolito permite mantener un despliegue único y evolucionar gradualmente el backend.

## Evolución del portal — etapa 1

Se incorporan los modelos persistentes de bancos, cuentas, saldo sincronizado, pagos, comentarios, adjuntos e historial. Todas las entidades nuevas heredan auditoría. Payment requiere cliente, admite licencia opcional y conserva destino bancario y teléfono propios. Los importes son BigDecimal / DECIMAL(14,2), en ARS. El saldo admite signo y no se crea automáticamente: representa una fotografía externa; no hay carga manual. La recepción desde Angrybox se documenta en docs/integrations/angrybox/sincronizacion-clientes.md.

El DNI tiene un campo de texto separado, inicialmente NULL. Se conserva el CUIL/CUIT histórico sin reinterpretar datos potencialmente inconsistentes. La resolución y normalización de documentos se implementan en la etapa 3, sin reinterpretar ni reescribir el CUIL histórico.

Las relaciones con pagos, comentarios, adjuntos e historial no tienen cascadas de eliminación. La licencia pierde cascade ALL/orphanRemoval sobre pagos. El esquema MySQL agrega una FK compuesta para asegurar la pertenencia licencia–cliente. Las evidencias rechazan actualización/borrado ordinarios en JPA. El pago incluye versión optimista y clave de presentación única por cliente para los servicios transaccionales. La etapa 4 implementa idempotencia de presentación; la aprobación administrativa registra una transición única y las acciones de licencia confirmadas.

El flujo anterior no puede construir un pago válido con evidencia, cuenta y datos de emisión. Sus escrituras web y Bearer se rechazan con 409; los IDs web inexistentes conservan 404. La interfaz muestra un aviso y permite consultar el nuevo DTO mínimo con licencia nullable. Ya no ofrece edición, cobro, descuento ni anulación. Los contratos de consulta de licencias permanecen sin cambios. Los consumidores externos de pagos deben adaptarse al nuevo contrato antes de habilitar los flujos siguientes.

Aplicar manualmente, con la aplicación detenida y una copia recuperable:

1. `20261026-01-auditoria-entidades.sql`, si todavía no fue aplicado.
2. [Vaciado autorizado de pagos anteriores](src/main/resources/db/20261026-02-vaciar-pagos-antiguos.sql). Solo borra payment; aborta si detecta dependencias entrantes desconocidas. No deshabilita FKs ni elimina clientes, usuarios, licencias o sus estados.
3. [Estructura del portal](src/main/resources/db/20261026-03-modelo-portal-pagos.sql). Exige tabla payment vacía, reemplaza la FK histórica de licencia por una FK compuesta RESTRICT, retira columnas antiguas y crea tablas/constraints nuevas.


Requieren MySQL 8.0.16+ con InnoDB para CHECK efectivos. No son scripts idempotentes ni se ejecutan al arrancar. Usar un cliente que se detenga ante el primer error, nunca --force. MySQL hace commits implícitos de DDL; no existe rollback global. No arrancar con ddl-auto=update para intentar suplir estas migraciones: validar primero en una copia y luego usar validate. No se aplicó SQL destructivo a ninguna base existente o compartida.

La prueba optativa MySqlPortalMigrationTests crea exclusivamente el esquema nuevo clientmanager_stage1_disposable en una instancia local desechable por el puerto 33316; falla si el esquema ya existe. Verifica guardas, conservación de datos, restricciones y correspondencia con Hibernate. No usa la conexión de producción. Para ejecutarla con una instancia MySQL vacía ya preparada:

```powershell
$env:CM_STAGE1_MYSQL = 'true'
# CM_STAGE1_MYSQL_PASSWORD: contraseña de root de esa instancia, si requiere una.
.\mvnw.cmd resources:resources compiler:compile resources:testResources compiler:testCompile surefire:test "-Dtest=MySqlPortalMigrationTests"
Remove-Item Env:CM_STAGE1_MYSQL
```

El operador confirmó el 26/10/2026 que aplicó los scripts de la etapa 1 y que la compilación y el arranque funcionaron en su entorno. Esa confirmación habilita continuar con la etapa 2. La prueba automatizada de migración en MySQL desechable no se ejecutó aquí: Windows rechazó el inicio del binario. La suite H2 no sustituye esa comprobación ni valida el consumidor AngryBox.

Están implementados el ingreso y aislamiento (etapa 3), el estado de cuenta y presentación con archivos privados (etapa 4) y la revisión administrativa con aprobación atómica y acciones de licencias (etapa 5). La siguiente etapa es la verificación integral (etapa 6), incluida la compatibilidad con MySQL real. No están implementados QR, servicios externos de pago ni extracción de pagos aprobados. La sincronización de deuda desde Angrybox se incorpora por separado.

## Parámetros — etapa 2

Los administradores acceden desde **Parámetros** a **Bancos** y **Cuentas bancarias**. Cada módulo tiene listado con búsqueda y filtros activo/inactivo, paginación de 25/50/100, alta y edición separadas, borradores y confirmación de descarte. El cambio de estado pide confirmación y envía el valor deseado, de modo que un reintento no lo alterna.

Las bajas se resuelven mediante desactivación. No hay borrado físico desde estos ABM, aun cuando una cuenta todavía no tenga pagos. Desactivar un banco conserva sus cuentas y su estado propio, pero las deja no disponibles; al reactivar el banco, solo vuelven a estar disponibles las cuentas activas. El portal utiliza estas cuentas disponibles para presentar pagos en la etapa 4.

El servidor normaliza espacios en nombres de bancos, elimina espacios/guiones del CBU y conserva sus ceros iniciales, recorta el titular y convierte alias vacíos en NULL y alias informados en minúsculas. Se exigen exactamente 22 dígitos de CBU; no se valida un dígito verificador bancario. Los nombres normalizados, CBU y alias son únicos, también para registros inactivos. Las restricciones existentes de base resuelven además las carreras entre solicitudes, con respuesta 409 sin detalles internos.

Las modificaciones de banco o cuenta no actualizan la fotografía guardada en pagos anteriores. La auditoría existente registra creación y modificación; activar/desactivar actualiza la modificación del parámetro, sin borrar fechas ni actores de creación.

Contratos administrativos (sesión, ADMIN y CSRF en escrituras):

| Ruta | Operaciones |
| --- | --- |
| /web/api/banks | GET paginado y POST de alta |
| /web/api/banks/{id} | GET y PUT de edición |
| /web/api/banks/{id}/active | PUT con active: true/false |
| /web/api/bank-accounts | GET paginado (bankId opcional) y POST de alta |
| /web/api/bank-accounts/{id} | GET y PUT de edición |
| /web/api/bank-accounts/{id}/active | PUT con active: true/false |
| /web/api/options/banks | GET de búsqueda para selectores administrativos (hasta 20) |

Los listados aceptan keyword, status (ALL/ACTIVE/INACTIVE), page y size, con 50 registros por defecto y máximo 100. Las búsquedas tratan los comodines como texto literal. Los DTO no exponen entidades, relaciones de auditoría ni grafos de pagos. USER y SELLER no pueden consultar ni modificar parámetros.

Los ABM utilizan las tablas de [20261026-03-modelo-portal-pagos.sql](src/main/resources/db/20261026-03-modelo-portal-pagos.sql), que define AUTO_INCREMENT para las nueve entidades nuevas. Es una migración de ejecución única sobre el esquema previo; no repetirla sobre una base ya migrada. El acceso y la seguridad del portal se describen en la etapa 3; el estado de cuenta y la presentación se describen en la etapa 4.

### Corrección de IDs al crear bancos

Las entidades nuevas mantienen la misma definición de ID que Client: @Id, @Column(name = "ID") y @GeneratedValue(strategy = GenerationType.AUTO). La configuración heredada use-new-id-generator-mappings=false se conserva; en MySQL se corresponde con columnas AUTO_INCREMENT. No se agregan secuencias, tablas auxiliares ni generadores específicos de Hibernate.

Las nueve tablas nuevas se crean con sus IDs AUTO_INCREMENT desde el script 03. No se mantienen scripts correctivos específicos para bases de desarrollo. Una base que haya aplicado una versión anterior requiere revisar su esquema antes de corregirla; no repetir el vaciado ni la migración estructural.

La prueba optativa MySQL comprueba la migración estructural y las altas sin ID explícito con registros existentes; su ejecución requiere una instancia local desechable. No se ejecutó esta verificación en MySQL ni se aplicaron scripts a bases compartidas desde esta tarea.

## Acceso y seguridad del portal — etapa 3

La pantalla principal comparte el diseño del login y permite elegir DNI o CUIT. El DNI se puede cargar y editar en Clientes; si un consumidor anterior omite ese campo, la edición conserva su valor. La ficha y el listado advierten cuando falta el usuario vinculado. No se crean cuentas al ingresar.

La búsqueda admite puntos, guiones y espacios comunes, sin transformar el dato histórico almacenado. CUIT requiere once dígitos y coincidencia normalizada exacta. DNI admite de uno a ocho dígitos y se compara completando ocho posiciones con ceros a la izquierda, tanto con el DNI registrado como con los ocho dígitos centrales de un CUIT numérico de once dígitos. No se calcula un dígito verificador. Documentos históricos con otros caracteres requieren revisión administrativa. Dos clientes coincidentes impiden el ingreso; no se elige uno arbitrariamente. La normalización y la comprobación de caracteres numéricos usan REPLACE, sin depender de REGEXP_REPLACE. Las consultas filtran y limitan en base de datos, sin cargar el padrón completo en memoria; las expresiones de normalización pueden requerir recorrer filas.

El cliente debe estar vigente, tener una única asociación coherente en ambos sentidos con un usuario habilitado y no anulado. Las sesiones del portal usan un principal propio con el ID del cliente, el ID del usuario asociado y origen PORTAL_DOCUMENTO o ACCESO_AUTENTICADO. Solo reciben capacidades del portal (consulta, presentación y comentario público), aunque el usuario asociado sea ADMIN. Las rutas del portal se enumeran explícitamente en la cadena de seguridad; las demás se deniegan por defecto. La etapa 4 agrega capacidades específicas para presentar pagos y comentarios públicos. Administración, parámetros y rutas antiguas del navegador requieren ADMIN. El ingreso con contraseña de USER y SELLER también queda restringido al portal; una cuenta ADMIN con otros roles conserva administración únicamente mediante contraseña.

Cada petición revalida la cuenta, el cambio de contraseña y la asociación cliente–usuario; una sesión del portal nunca se promueve a ADMIN al recargar roles. El cliente autorizado se toma del principal, no de parámetros del navegador. El acceso por documento no verifica identidad. La consulta usa el nombre de pila y las proyecciones mínimas de sus licencias, saldo y pagos; no expone documentos, correo, usuario asociado, auditoría ni datos privados. El teléfono se precarga únicamente en el formulario de presentación. Se conservan CSRF, cookie HttpOnly, renovación del identificador y del token CSRF al ingresar, logout y expiración de sesión (30 minutos de inactividad en la configuración normal).

El límite inicial es de diez ingresos por documento por dirección remota cada quince minutos, incluidos los exitosos. Se configura con portal.document.max-attempts y portal.document.window-seconds. Es un límite en memoria por instancia, reiniciado al reiniciar el proceso; no es distribuido. No se confía en X-Forwarded-For: detrás de un proxy, los clientes que compartan dirección remota comparten el límite. No se almacenan documentos en el limitador ni se agregan a los logs.

No hay migración adicional: se utiliza la columna DNI del script 03 ya definido. La consulta de licencias, saldo y pagos y la presentación de transferencias se incorporan en la etapa 4.

Verificación local de esta etapa: build limpio con clean verify, pruebas existentes adaptadas al nuevo acceso y recorridos Playwright sobre H2 aislada. Se comprobaron DNI/CUIT, sesiones restringidas incluso con usuario asociado ADMIN, login de SELLER, bloqueo de rutas administrativas, ambigüedad, CSRF, cierre/expiración, límite de intentos y presentación en escritorio/móvil. Queda pendiente validar las consultas de documentos y los datos históricos reales en una copia compatible de MySQL; H2 no demuestra esa compatibilidad.

## Estado de cuenta y presentación de pagos — etapa 4

El portal identifica la cuenta propia con nombre y apellido, DNI, CUIL/CUIT, correo y teléfono registrados; los campos ausentes se indican como tales. Muestra un resumen de licencias, saldo sincronizado y pagos propios. Estado general y Saldo informado aparecen antes de Mis pagos informados; este último conserva el enlace destacado Informar un nuevo pago sobre la tabla y aparece antes de Mis licencias. Los contadores positivos de bloqueadas y vencidas se resaltan con severidad danger, negrita e iconos de alerta; por vencer usa warn, negrita y advertencia. Los contadores en cero permanecen neutros. Mis licencias inicia colapsado, usa una flecha hacia abajo para abrir y hacia arriba para cerrar, y carga su listado al expandirse; muestra empresa y estado, sin fecha de bloqueo ni días de vigencia. El estado de licencia reutiliza la regla administrativa: estado no anulado de menor ID, bloqueo forzado prioritario y vencimiento al comenzar la fecha de bloqueo, en la zona horaria del servidor. Los listados de pagos y licencias paginan en base de datos (50 por defecto; 25, 50 y 100 en pantalla). El formulario muestra completo el pequeño catálogo de cuentas disponibles, sin paginador. Nunca se elige el cliente por parámetros del navegador.

ClientAccountBalance es una fotografía: positivo significa deuda, negativo saldo a favor y cero ausencia de deuda. Si no existe, se muestra “Saldo aún no disponible”; no se inserta un saldo cero. Se informa la fecha/hora de sincronización. No hay edición manual; el saldo se recibe por el endpoint de integración Angrybox. Presentar un pago no modifica el saldo ni las licencias.

La presentación tiene tres pasos: selección expresa de una cuenta mediante tarjetas, datos y comprobantes, revisión y confirmación. Las cuentas y sus bancos deben estar activos y no anulados. Si cambia la información bancaria durante el formulario, se exige actualizar y revisar la selección. El servidor conserva su fotografía; cambios posteriores no alteran los pagos existentes. La licencia es opcional y debe pertenecer al cliente. El importe es positivo, ARS y de hasta doce enteros y dos decimales. La emisión se envía como fecha/hora local ISO, sin convertir a UTC, y es distinta de la creación auditada. Se precarga el teléfono, que puede corregirse para este pago. Las cuentas se presentan como tarjetas en una única columna, con todos sus datos y selección expresa. El importe se precarga con la deuda positiva informada al abrir el formulario; saldo ausente, cero o a favor dejan el importe vacío. Puede modificarse o limpiarse (por teclado o con Limpiar importe). Volver de paso o actualizar las cuentas no vuelve a imponer el importe sugerido.

Se permite exactamente una alternativa: uno o más comprobantes, o declaración de ausencia con motivo obligatorio. La segunda muestra “Sin un comprobante asociado, la verificación del pago puede extenderse hasta 48 horas.” El comentario público opcional es independiente de ese motivo. QR se rechaza en servidor. Toda presentación crea PENDIENTE, un evento inicial y evidencia auditada con usuario asociado y origen de acceso; no admite edición genérica, borrado ni reapertura.

La clave de presentación pertenece al cliente y se conserva al reintentar. Un bloqueo transaccional del cliente y lecturas READ_COMMITTED serializan presentaciones concurrentes sin conservar una instantánea anterior al primer envío; la restricción única también se mantiene. Un reenvío idéntico devuelve la referencia original sin duplicar historial, comentarios o comprobantes. La misma clave con otros datos o archivos produce 409. El navegador conserva el borrador y los archivos ante errores de validación, conexión o guardado mientras la sesión sigue vigente, y bloquea el doble envío. El cierre o la expiración de sesión requiere un nuevo ingreso y seleccionar nuevamente los archivos. Los comentarios posteriores siempre son públicos y se agregan sin editar ni borrar los anteriores. El portal filtra privados en la consulta de base de datos y no devuelve actores ni auditoría. Los motivos de rechazo se leen del evento público de rechazo.

### Comprobantes y almacenamiento

Se agregó una abstracción ReceiptStorage con implementación local en disco. Configurar CLIENTMANAGER_RECEIPTS_DIRECTORY con una ruta privada, persistente y escribible por la cuenta del servicio; por defecto usa .clientmanager/receipts dentro del directorio personal de esa cuenta. Debe ser una carpeta vacía al inicializarse, exclusiva de esta base y fuera de directorios públicos. Se guarda una marca con la huella del destino de base de datos, sin URL ni credenciales, y se rechaza reutilizarla desde otro destino para evitar que la reconciliación borre archivos ajenos. No eliminar ni modificar esa marca sin verificar una recuperación o cambio de destino; parámetros de conexión y credenciales no forman parte de la huella. Respaldar esta carpeta junto con la base de datos. El JAR no contiene los comprobantes. Esta implementación corresponde a una instancia con disco persistente; un despliegue con varias instancias necesita almacenamiento privado compartido y coordinación equivalente o una implementación posterior de la abstracción.

Valores iniciales configurables en portal.files: max-receipts=5 por envío (tanto en el alta como en agregados posteriores), max-file-bytes=10485760 (10 MiB), max-dimension=12000 por lado, max-pixels=40000000, max-image-side=2000 e image-quality=0.8. Los límites multipart son spring.servlet.multipart.max-file-size=10MB y max-request-size=52MB; mantenerlos coherentes al cambiar los límites del portal. El procesamiento de comprobantes se limita a una presentación por instancia para acotar memoria; solicitudes simultáneas con archivos pueden recibir 429 y reintentarse. La validación del servidor no confía en Content-Type ni en el nombre como ruta.

Se admiten PDF, JPG/JPEG y PNG con extensión y contenido concordantes. PDF se analiza con [Apache PDFBox](https://pdfbox.apache.org/download.cgi), debe poder abrirse sin contraseña y tener páginas; se acepta protección de edición si no requiere contraseña de apertura; se conserva byte por byte, sin rasterizar ni recomprimir. La orientación EXIF se obtiene con [metadata-extractor](https://github.com/drewnoakes/metadata-extractor/releases). JPG y PNG se reorientan, mantienen proporción, no se amplían y reducen su lado mayor al máximo configurado. Se guardan como JPEG con calidad configurable; transparencias se componen sobre blanco. Se eliminan los metadatos del original y se conserva únicamente la imagen procesada. No se promete un peso final fijo.

La base guarda clave interna aleatoria, nombre original saneado, MIME comprobado, tamaño almacenado, SHA-256 de esos bytes, fecha/usuario/origen y dimensiones de la imagen final. Los bytes procesados se preparan en pending; se mueven atómicamente a stored antes del commit. Un rollback limpia ambos destinos. Un resultado de commit desconocido conserva los archivos hasta reconciliar referencias. Tras una caída del proceso, la reconciliación periódica elimina temporales antiguos y archivos finales sin referencia en Attachment, sin borrar evidencia referenciada, aunque esté anulada. El margen inicial es de 24 horas (orphan-grace-seconds), superior al timeout de escritura de 120 segundos. La reconciliación no reemplaza copias de respaldo ni puede reconstruir archivos perdidos por una falla del disco.

El portal lista, abre y descarga comprobantes de sus propios pagos, comprobando en servidor cliente de sesión y vínculo pago–adjunto en cada consulta. ADMIN conserva su acceso separado; no hay URLs públicas. El endpoint admite vista inline o descarga con cabeceras no-store, nosniff y sandbox. La interfaz administrativa permite revisar pagos, comentarios, comprobantes e historial, y resolverlos mediante aprobación o rechazo.

### Rutas de esta etapa

- GET /web/portal/overview: estado general y saldo del cliente de sesión.
- GET /web/portal/licenses y /accounts: listados propios o cuentas disponibles paginadas.
- GET /web/portal/payment-form: teléfono, límites de carga, catálogo completo de cuentas activas y deuda positiva sugerida como importe. La cuenta y el saldo se resuelven desde el cliente autenticado.
- GET /web/portal/payments: pagos propios y cantidad de comentarios públicos (los privados no se cuentan).
- GET /web/portal/payments/{id}: detalle del pago propio, estado y fotografía de la cuenta destinataria.
- GET /web/portal/payments/{id}/attachments: metadatos paginados de sus comprobantes, con descripción opcional de hasta 1000 caracteres.
- GET /web/portal/payments/{id}/attachments/{attachmentId}: abrir comprobante propio; download=true permite descargar.
- POST /web/portal/payments/{id}/attachments: agregar comprobantes mediante multipart; exige CSRF y capacidad de presentación.
- POST /web/portal/payments: multipart con data JSON y partes receipts; exige CSRF y capacidad de presentación. No acepta cliente, actor, moneda ni estado desde el navegador. accountRevision verifica la información bancaria revisada.
- GET y POST /web/portal/payments/{id}/comments: consulta y agregado de comentarios públicos del pago propio.
- GET /web/api/payments/{paymentId}/attachments/{attachmentId}?download=true: bytes de evidencia solo para ADMIN, con verificación de la relación.

La ampliación de adjuntos requiere aplicar manualmente 20261027-01-descripcion-y-agregado-adjuntos.sql, primero en una copia compatible de MySQL. Agrega description nullable a attachment y upload_key nullable e índice a payment_attachment; conserva los datos existentes. La generación del script no lo ejecuta. La etapa 5 incorpora revisión administrativa, comentarios privados, rechazo, aprobación y acciones sobre licencias; no se agregaron en esa etapa extracción de aprobados, conciliación ni servicios externos de pago. La sincronización de deuda se incorpora por separado.

Verificación de etapa 4: se ejecutaron comprobaciones HTTP y de persistencia sobre H2 desechable, con pagos y cuentas sintéticos. Se verificaron aislamiento, comentarios privados, validaciones, concurrencia e idempotencia, metadatos y bytes guardados, orientación EXIF, imágenes pequeñas y transparentes, PDF idéntico, rollback ante fallos de BD y almacenamiento, acceso administrativo a archivos (antes de ampliar la consulta al propietario) y reconciliación. El recorrido de navegador comprobó los tres pasos, reintento sin perder archivos, alternativa sin comprobante, confirmación, comentarios escapados, descarte de borradores y presentación en escritorio/móvil. Las comprobaciones manuales no se incorporan como una nueva suite de cobertura. La compatibilidad de este flujo con el esquema y los datos reales de MySQL/AngryBox queda pendiente; no se ejecutó SQL sobre bases compartidas.

La verificación integrada terminó con clean verify y los recorridos administrativos existentes de Playwright correctos. Después del ajuste de compatibilidad de PDF se recompiló Java, se repitieron sus verificaciones focalizadas y se reempaquetó el JAR. El flujo con archivos se volvió a verificar con las dependencias finales, incluidos PDF protegidos solo para edición, autenticación por documento y autenticación con contraseña, sin modificar saldo. No se aplicaron migraciones ni cambios a la base compartida.

### Detalle y agregados posteriores

En Mis pagos informados se muestra la cantidad de comentarios públicos y un enlace al detalle. Allí se consultan importe, emisión, teléfono, licencia opcional, estado, rechazo y destino bancario original. Los comentarios públicos pueden consultarse y agregarse; los adjuntos pueden abrirse, descargarse y ampliarse. No se permite borrar los ya enviados ni modificar sus descripciones. Si un archivo era incorrecto, se agrega el correcto y se aclara mediante un comentario. Agregar evidencia no reabre el pago, no cambia su estado ni modifica saldo o licencia; también se permite en pagos ya rechazados/aprobados. La declaración original de ausencia y su motivo se conservan aunque después se agreguen comprobantes.

El selector muestra los motivos de rechazo por tamaño, formato, cantidad y archivo vacío. Cada archivo admite una descripción opcional que se guarda asociada a sus bytes procesados. Las descripciones se envían en el mismo orden que las partes receipts; un desajuste se rechaza. Los agregados usan una clave por envío, serializan por cliente y coordinan archivos con la transacción igual que la presentación. Reintentar el mismo envío no duplica evidencia; cambiar archivos o descripciones con esa clave produce 409. La repetición de la presentación original compara únicamente sus comprobantes originales, aunque haya agregados posteriores. Los listados de adjuntos y comentarios paginan en base de datos.

Verificación de esta ampliación: clean verify correcto; tras ajustar el accessor de validación de descripciones se recompiló Java, se repitieron las pruebas existentes focalizadas y se reempaquetó el JAR. Comprobaciones manuales HTTP/H2 verificaron propiedad de pagos y archivos, conteo sin comentarios privados, persistencia de descripciones, validaciones, paginación, envíos concurrentes, repetición de la presentación original después de agregar archivos, rollback ante fallo de disco y conservación de saldo/estado. Se comprobó el rechazo HTTP 413 con el límite explícito. El navegador verificó archivos de 26 MB y formato inválido con mensajes visibles, asociación de descripciones al quitar un archivo, alta, detalle, apertura de imagen, agregado de comprobantes y comentarios, sin desborde de página en escritorio/móvil. Los recorridos Playwright existentes también pasaron sobre la misma base aislada. No se aplicó el SQL ni se validó esta migración contra MySQL real.

La reorganización del portal y la precarga de deuda no cambian el esquema ni requieren SQL adicional. La identidad del cliente se devuelve mediante DTO del propietario autenticado; no se expone el usuario asociado ni su auditoría. Se conserva el endpoint paginado de cuentas para compatibilidad, aunque el formulario usa el catálogo completo incluido en payment-form.

Verificación de la reorganización del portal: clean verify correcto, con las pruebas existentes de Java y frontend. Se verificaron mediante HTTP y navegador sobre H2 aislada la identidad del propietario, deuda positiva, saldo a favor, cero y saldo ausente; seis cuentas activas completas en una única columna y sin paginador; selección obligatoria; apertura/cierre de licencias sin las columnas retiradas; y conservación del importe escrito o vacío al volver de paso y actualizar cuentas. Se revisaron capturas en escritorio y móvil sin desborde de página. No se modificó la base MySQL ni se ejecutaron migraciones.

### Ayuda por WhatsApp

El login por documento, el login con contraseña y todas las pantallas del portal muestran un enlace flotante Necesito ayuda con el icono local de WhatsApp. Abre una conversación en una pestaña nueva; no envía mensajes ni datos del formulario automáticamente. Se mantiene compacto, sin animaciones ni ventanas automáticas, respeta el área segura del móvil y reserva espacio al final de la página para no tapar sus últimas acciones. No se muestra en las pantallas administrativas.

Configurar portal.support.whatsapp-phone en application.properties (valor inicial +54 9 2634 22-3523), o mediante la variable CLIENTMANAGER_WHATSAPP_PHONE. Usar el número internacional con código de país; se admiten +, espacios, paréntesis y guiones. El enlace resultante es https://wa.me/5492634223523 para el valor inicial. El servidor valida el formato al iniciar y expone solamente este enlace público mediante GET /web/session/support, sin autenticación y con no-store. Cambiar el número requiere reiniciar el backend y recargar la página; no requiere recompilar el frontend ni modificar la base.

Verificación de ayuda: clean verify correcto y comprobaciones manuales sobre H2 aislada. Se revisaron ambos logins y todas las pantallas del portal, enlace internacional normalizado, icono local, posición fija al desplazarse, escritorio y móviles de 390/320 píxeles, espacio para Continuar y reintento al fallar la carga del contacto. Se comprobó que un número alternativo configurado se normaliza y que una configuración inválida se rechaza al iniciar. No se abrió WhatsApp ni se enviaron mensajes durante la verificación. El JAR actualizado no requiere SQL adicional.

Verificación del ajuste de alertas: clean verify correcto. Se revisó el frontend empaquetado en escritorio y móvil con respuestas sintéticas interceptadas en navegador, sin consultar ni modificar una base real: orden estado/saldo antes de pagos, flechas abajo/arriba, apertura/cierre por teclado, contadores positivos con severity danger/warn, negrita e iconos, y contadores en cero neutros. Las capturas no mostraron desbordes de página. Este ajuste visual no requiere SQL.

## Revisión administrativa de pagos — etapa 5

El listado de pagos conserva el orden por emisión y sus filtros por licencia; agrega cliente, estados combinables, texto y rango inclusivo de fechas de emisión. Los botones Pendientes, Aprobados y Rechazados permiten seleccionar varios estados; sin selección se muestran todos y Todos limpia la selección. Cada botón muestra su cantidad respetando cliente, licencia, texto y fechas, independientemente de los estados seleccionados y la página. Los filtros y la paginación viajan en la URL para volver desde el detalle o recuperar el destino tras autenticarse. El parámetro `status` conserva valores individuales y `ALL`, y admite varios estados separados por coma; `/web/api/payments/counts` devuelve los contadores con los mismos filtros de contexto. No se priorizan visualmente los pendientes. La presentación existente sigue creando PENDIENTE; los antiguos formularios y escrituras genéricas permanecen cerrados.

La botonera se verificó con `clean verify`, el recorrido administrativo Playwright existente y comprobaciones puntuales de API/navegador sobre H2 aislada: cantidades por contexto, estados combinados, selección vacía, limpieza con Todos, paginación, permisos y conservación al recargar o volver del detalle. Se revisaron escritorio y móvil de 320 píxeles. No requiere cambios de esquema.

El detalle muestra los datos financieros originales, cuenta bancaria histórica, teléfono, usuario atribuido y origen, comprobantes con sus descripciones, comentarios públicos/privados, historial de estados y acciones sobre licencias. Los listados de evidencias paginan en base de datos. Los textos se muestran escapados. Solo ADMIN del acceso autenticado puede usar estas rutas; el usuario asociado a una sesión por documento no obtiene esta capacidad. Los contratos del portal continúan filtrando comentarios privados y actores, incluso si el JSON intenta elegir visibilidad privada.

Rechazar exige un motivo público. Aprobar abre un diálogo que propone, sin acciones preseleccionadas, la licencia asociada y permite elegir otras del mismo cliente. Se puede aprobar sin modificar ninguna licencia. Para cada licencia se elige quitar el bloqueo forzado y/o actualizar vencimiento. La vista previa del servidor muestra fecha actual, fecha anterior/propuesta, días desde hoy, diferencia y estado resultante. Una licencia vencida continúa vencida si solo se quita el bloqueo forzado; una fecha ausente continúa sin fecha. No se tocan límites de transacciones ni observaciones existentes. Cuando hace falta un estado nuevo por actualizar fecha, conserva los valores iniciales que ya utiliza el flujo web.

La fecha de negocio usa portal.review.time-zone, inicialmente America/Argentina/Buenos_Aires, configurable mediante CLIENTMANAGER_PAYMENT_TIME_ZONE. Se sugiere día 10 del próximo mes; un vencimiento posterior se conserva como sugerencia. No se admiten fechas anteriores a hoy; reducir una fecha previa requiere confirmación expresa por licencia. La vista previa se invalida al cambiar acciones. El servidor vuelve a calcular y comprobar versión del pago, revisión de cada licencia, fecha de negocio y huella del plan al confirmar. Cambiar de día requiere revisar de nuevo.

La conversión del calendario persistente conserva el contrato previo: fecha al comienzo del día en la zona del servidor. El bloqueo efectivo y el estado resultante respetan esa regla y la selección del estado no anulado de menor ID. Configurar también la zona del proceso/servidor según el negocio; cambiar solo la zona de sugerencias no redefine el comportamiento histórico de la API de licencias.

Las decisiones toman bloqueos en orden cliente → pago → licencias ordenadas por ID y usan READ_COMMITTED para no conservar lecturas anteriores a una espera de bloqueo en MySQL. Solo PENDIENTE puede resolverse y la versión debe coincidir; reintentar una decisión ya confirmada devuelve 409 y no crea eventos nuevos. La decisión, su historial y las acciones efectivas de licencia se confirman o revierten juntos. Los cambios sin efecto no generan acciones ficticias. La auditoría de modificación del pago y de LicenseStatus usa el actor persistido; el historial identifica responsable, fecha y valores anteriores/nuevos. Aprobar o rechazar no modifica ClientAccountBalance ni reabre pagos.

Rutas administrativas nuevas bajo /web/api/payments/{id}: GET detail, comments, attachments, history, license-actions y licenses; POST comments, approve-preview, approve y reject. Las escrituras exigen CSRF. La descarga de bytes mantiene su endpoint protegido anterior. No hay rutas de edición/borrado de comentarios o evidencias, extracción de aprobados ni integración de pagos externos. La sincronización de deuda usa el endpoint de integración Angrybox. Se reutilizan las tablas del modelo existente: esta etapa no requiere SQL adicional.

Verificación de la etapa 5: `clean verify` finalizó correctamente y los recorridos Playwright existentes pasaron contra H2 aislada con cuentas sintéticas. Las comprobaciones manuales de API y navegador cubrieron permisos y CSRF, comentarios públicos/privados, comprobantes, filtros, actualización del detalle, aprobación/rechazo únicos, decisiones simultáneas, revisión obsoleta, confirmación de acortamiento y auditoría. Una falla de persistencia provocada en esa base confirmó la reversión conjunta de pago, historial y acciones; el saldo se conservó. También se revisaron escritorio/móvil y cálculos de fechas en límites de mes, año y año bisiesto. No se agregaron suites de cobertura. Falta verificar estos bloqueos y operaciones contra una copia compatible de MySQL; H2 no demuestra su comportamiento en producción.

### Saldos en la administración de clientes

El listado muestra saldo externo en ARS y última sincronización. Permite filtrar deuda positiva, saldo a favor negativo, saldo cero y ausencia de información vigente. El filtro de antigüedad selecciona saldos informados con sincronización estrictamente anterior a la fecha/hora actual en `portal.review.time-zone` (por defecto `America/Argentina/Buenos_Aires`) menos 15, 30, 45 o 60 días; se combina con los demás filtros y pagina en base de datos. Los saldos anulados se consideran sin informar. La ficha muestra identificación, importe, moneda y sincronización del saldo, sin edición manual.


### Privacidad del portal según el ingreso

El acceso por DNI/CUIT oculta Datos de tu cuenta, Estado general, Mis licencias y la columna Detalle de pagos. El saldo ocupa todo el ancho disponible cuando no se muestra Estado general. El servidor también omite el estado general, los contadores de licencias, documentos y contacto en el resumen, no precarga el teléfono en el formulario y rechaza con 403 la consulta de licencias, detalles, comentarios y comprobantes por URL directa. El formulario permite informar pagos y adjuntar comprobantes nuevos sin consultar las licencias existentes. El ingreso con usuario y contraseña conserva las consultas completas del propietario.

Mis pagos informados muestra únicamente emisiones entre la fecha/hora local actual del servidor menos 45 días y el momento actual, con filtro y paginación en base de datos. La columna Archivos Adjuntos cuenta los vínculos y archivos no anulados, incluidos agregados posteriores, sin exponer sus metadatos. El saldo muestra «Informado:» junto a su fecha y se retira la descripción anterior. Estos cambios no alteran el esquema ni requieren migración SQL.

Verificación de privacidad: `clean verify` correcto con Java 11, pruebas Java sobre H2 y pruebas frontend existentes. Se revisaron ambos modos en escritorio (1440 px) y móvil (390 px) con respuestas sintéticas, comprobando visibilidad y ausencia de desborde de página. No se ejecutó la prueba opcional de migración MySQL ni se modificaron datos compartidos.
