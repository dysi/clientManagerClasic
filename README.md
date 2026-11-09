# DySI Client Manager

La recepción de clientes y deudas desde Angrybox está implementada en
`POST /api/clients/sync`. Requiere habilitación explícita, empresa de origen y
Bearer con el authority configurado (ROLE_API por defecto). Ver [contrato, reglas de vinculación y despliegue](docs/integrations/angrybox/sincronizacion-clientes.md).
La opción `angrybox.sync.sync-deactivations` permite propagar las bajas lógicas de
Angrybox junto con la deuda; por defecto está desactivada y nunca reactiva clientes.
La entrega de pagos aprobados utiliza una cola transaccional y confirmación individual
por `/api/payments/sync`. Ver [contrato y despliegue](docs/integrations/angrybox/sincronizacion-pagos.md).
En `/ui/#/payments`, los pagos con recepción confirmada por Angrybox muestran un badge
verde «Sincronizado» con un rayo. Su título informa la fecha y hora de confirmación en
`portal.review.time-zone` (por defecto `America/Argentina/Buenos_Aires`).

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

La configuración heredada contiene valores de conexión y secreto que deben sustituirse mediante configuración externa y rotarse si se utilizaron. Este documento no reproduce sus valores. Hibernate usa `ddl-auto=validate` en ejecución normal y dev; Liquibase administra el esquema. Revisar que ninguna configuración externa vuelva a habilitar `update`.

## Migraciones automáticas con Liquibase

Liquibase se integra mediante `liquibase-core`, con la versión administrada por Spring Boot 2.7.18. Ejecuta los cambios pendientes antes de inicializar JPA; si falla una migración, falla el arranque. El maestro es `src/main/resources/db/changelog/db.changelog-master.xml`.

Se parte de una **base existente y actualizada**. Los SQL históricos directamente bajo `src/main/resources/db/` se asumen ya ejecutados y quedan como referencia: no se incluyen, no se repiten ni se registran mediante `changelogSync`. El maestro incluye la migración de dirección de clientes y los catálogos de país y provincia, sin baseline histórico. Liquibase puede crear `DATABASECHANGELOG` y `DATABASECHANGELOGLOCK` para su control. Este punto de partida no instala una base desde cero. Hibernate valida el mapeo JPA, pero no certifica todos los triggers, índices ni datos históricos.

Para agregar migraciones futuras:

1. Crear un archivo SQL formateado Liquibase en **`src/main/resources/db/changelog/changes/`** (crear la subcarpeta con la primera migración), con nombre `yyyyMMdd-NN-descripcion-breve.sql` y el siguiente número del día. Primera línea: `--liquibase formatted sql`. Usar changesets pequeños, con autor estable, ID único y `dbms:mysql`.
2. Registrar el archivo en el maestro mediante un include explícito, por ejemplo `<include file="changes/AAAAMMdd-01-descripcion.sql" relativeToChangelogFile="true"/>`, en orden de dependencias. Copiar el archivo sin agregar el include no lo incorpora al despliegue.
3. Documentar alcance, precondiciones, conservación de datos y verificaciones. Detener ante incompatibilidades. Agregar rollback cuando sea viable o documentar respaldo y recuperación. No editar ni mover changesets aplicados: corregir con uno nuevo. Para triggers y procedimientos configurar delimitadores JDBC; no usar la orden de cliente `DELIMITER`.
4. Probar primero sobre una copia compatible de MySQL y revisar los cambios pendientes antes de desplegar. MySQL confirma DDL implícitamente: una falla puede dejar cambios parciales que se deben revisar antes de reintentar. No hay rollback automático al volver al JAR anterior.

Al desplegar el JAR, Liquibase usa el DataSource configurado, registra changesets y checksums, y en los siguientes arranques ejecuta solo los pendientes. El usuario de base necesita permisos para las tablas de control y las operaciones de cada migración. El bloqueo de Liquibase coordina migraciones, pero no garantiza compatibilidad entre versiones de aplicación: diseñar cambios compatibles si conviven instancias. Respaldar antes del despliegue.

El perfil `test` deshabilita Liquibase y conserva H2 con `create-drop` para las pruebas existentes; esas pruebas no verifican las migraciones MySQL. Esta integración no aplica cambios a bases compartidas.

Las referencias posteriores a la ejecución manual del consolidado describen el esquema histórico previo a Liquibase. **No son pasos de este despliegue**: se asumen completados. No repetir el consolidado, que contiene operaciones destructivas. Una base atrasada requiere evaluación independiente antes de adoptar este punto de partida. Referencias: [Spring Boot 2.7 y Liquibase](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/howto.html#howto.data-initialization.migration-tool.liquibase) y [SQL formateado Liquibase](https://www.liquibase.com/blog/liquibase-formatted-sql).

Los iconos de Client Manager se sirven localmente desde `frontend/public/icons/`: logo de cabecera y bienvenida, favicon ICO/PNG y acceso directo Apple. Sus rutas respetan la base relativa de Vite y el contexto de despliegue.

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

La baja de un cliente completa `removeDate` y `removedBy` de auditoría; el alta limpia ambos campos. No cambia las fechas ni los bloqueos propios de sus licencias. La consulta de AngryBox (`/api/clients/license`) devuelve `forcedLock=true` mientras el cliente esté dado de baja; AngryBox utiliza ese valor al construir su String de versión. El ingreso al portal mediante DNI/CUIT o cuenta vinculada conserva los controles del usuario y permite consultar el saldo aun con el cliente dado de baja.

Clientes filtra por estado (todos, activos, dados de baja), con activos por defecto, y muestra estado, CUIT y DNI. El estado se presenta con un badge verde para activos y rojo para dados de baja; estos últimos tienen toda la fila roja, también durante el hover. Licencias informa la baja del cliente y filtra por estado del cliente (activos por defecto) y visibilidad (visibles por defecto). Solo una licencia con bloqueo forzado propio puede ocultarse; ni el vencimiento ni la baja del cliente habilitan esa acción. Ocultar no anula la licencia ni modifica su respuesta a AngryBox. Las ocultas no aparecen en listados ni contadores del portal; administración puede recuperarlas con Ocultos/Todos y hacerlas visibles incluso si se desbloquearon después. Reactivar un cliente o desbloquear una licencia no cambia su visibilidad. Los comprobantes y pagos históricos conservan sus referencias.

El cambio de esquema requiere aplicar manualmente `src/main/resources/db/20261102-01-migracion-consolidada.sql`, previa validación en una copia MySQL compatible. No fue aplicado a una base compartida; H2 no valida la compatibilidad MySQL.

Los pagos nuevos tienen estado inicial PENDIENTE. La aprobación o rechazo serán decisiones terminales del futuro flujo administrativo; los pagos no admiten edición genérica, anulación ni reactivación. Registrar un pago **no extiende automáticamente la licencia ni modifica el saldo sincronizado**. El bloqueo de licencias conserva su petición idempotente con el estado deseado.

## API web y seguridad

Todas las rutas siguientes son relativas al contexto del backend:

| Método / ruta | Contrato |
| --- | --- |
| `GET /web/session/support` | Contacto público de WhatsApp configurado; disponible sin sesión |
| `GET /web/session/turnstile` | Configuración pública de Turnstile (habilitado y site key), sin sesión |
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

La política CSP de `/ui` restringe los recursos al mismo origen y permite scripts, frames y conexiones de `https://challenges.cloudflare.com` para Turnstile; bloquea objetos y que otros sitios embeban el portal. Permite estilos inline porque PrimeVue genera estilos dinámicos; no permite scripts inline ni `unsafe-eval`. La interfaz no utiliza `v-html` para mostrar datos del servidor.

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

Como referencia histórica anterior a Liquibase, el esquema se preparaba aplicando manualmente **`src/main/resources/db/20261102-01-migracion-consolidada.sql`**, primero sobre una copia del esquema. El script agrega los campos faltantes y las claves foráneas a usuario; conserva `payment.creationdate/removedate` y `licensestatus.creationdate/removedate` y no rellena fechas ni actores ficticios. Leer sus precondiciones de motor InnoDB, nombres físicos y tipo de ID. MySQL hace commit implícito de DDL; la serie no tiene rollback transaccional global. `ddl-auto=update` no sustituye este procedimiento y puede adelantarse al script: aplicar el SQL con la aplicación detenida y utilizar `validate` al arrancar contra el esquema preparado.

El consolidado [20261102-01-migracion-consolidada.sql](src/main/resources/db/20261102-01-migracion-consolidada.sql) reúne las once migraciones del 26 al 31 de octubre, adaptadas a **MySQL 5.7.24**. Conserva su fecha y secuencia para ejecutarse antes de los scripts posteriores. Sustituye la revisión anterior para MySQL 8 y se ejecuta una sola vez sobre una copia original sin migrar. **La sección 02 borra todos los pagos del modelo anterior.** La variante 5.7 comprueba previamente señales de migración y admite una FK histórica simple o su ausencia, verificando las siete columnas antiguas de payment; rechaza FKs incompatibles y la compuesta nueva. Sustituye los diez CHECK por triggers de INSERT y UPDATE, porque MySQL 5.7 ignora CHECK. Mantener la aplicación detenida hasta completar las validaciones finales; requiere InnoDB, IDs BIGINT con signo, permisos DDL/rutinas/TRIGGER y un cliente que se detenga al primer error, compatible con `DELIMITER`, sin `--force`. DDL hace commit implícito: no hay rollback global.

El único archivo a ejecutar sobre una **base original sin migrar** es el consolidado 01: incluye las once secciones históricas, los diez triggers de validación del portal y la tabla `license_query_log` con sus índices y referencias de auditoría. Los archivos separados 02 y 04 fueron retirados para evitar ejecuciones duplicadas. **No repetir el consolidado sobre una base parcial o totalmente migrada.** En ese caso, revisar el esquema y el registro de ejecución para determinar los cambios pendientes. Al finalizar, la consulta de triggers debe mostrar diez filas y la bitácora debe estar vacía. Probar inserciones y actualizaciones inválidas en una copia aislada y arrancar con `ddl-auto=validate`. Los triggers conservan las reglas de los CHECK originales. Los índices utf8mb4 requieren prefijos grandes y formato de fila compatible en InnoDB. La unificación no se ejecutó sobre MySQL real; H2 no acredita compatibilidad MySQL ni validación en producción.

Los cambios futuros de BD se agregan como migraciones Liquibase en `src/main/resources/db/changelog/changes/` y se incluyen explícitamente en el maestro, según la sección «Migraciones automáticas con Liquibase». Toda entidad nueva debe heredar la auditoría; `AGENTS.md` establece esta obligación.

Para los datos comerciales de clientes, aplicar [20261102-01-migracion-consolidada.sql](src/main/resources/db/20261102-01-migracion-consolidada.sql) con la aplicación detenida y las columnas nuevas aún ausentes. Agrega `legal_name`, `trade_name` y `external_code` anulables, conserva filas y auditoría y deja los valores históricos en NULL. Incluye comprobaciones de tipos, nulabilidad y cantidad de filas. Si `ddl-auto=update` ya creó las columnas, revisar el esquema antes de aplicar; no repetir el ALTER. Validar primero sobre una copia compatible de MySQL.

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
4. Consultar el historial de Liquibase y ampliar las migraciones con restricciones de unicidad/asociación, una regla explícita para estados históricos de licencia.
5. Validar esquema MySQL real, copias de respaldo, configuración de proxy/HTTPS y compatibilidad con AngryBox antes del despliegue.
6. Planificar actualización posterior de Java/Spring Boot y dependencias del backend; Java 11 y Boot 2.7 son la base de compatibilidad solicitada, no una actualización a las versiones mayores más recientes.
7. Medir consultas JPA, mejorar logs y ampliar pruebas de concurrencia, accesibilidad y carga según el uso real.

El análisis no aporta una necesidad demostrada de microservicios. La separación por módulos y contratos dentro del monolito permite mantener un despliegue único y evolucionar gradualmente el backend.

## Evolución del portal — etapa 1

Se incorporan los modelos persistentes de bancos, cuentas, saldo sincronizado, pagos, comentarios, adjuntos e historial. Todas las entidades nuevas heredan auditoría. Payment requiere cliente, admite licencia opcional y conserva destino bancario y teléfono propios. Los importes son BigDecimal / DECIMAL(14,2), en ARS. El saldo admite signo y no se crea automáticamente: representa una fotografía externa; no hay carga manual. La recepción desde Angrybox se documenta en docs/integrations/angrybox/sincronizacion-clientes.md.

El DNI tiene un campo de texto separado, inicialmente NULL. Se conserva el CUIL/CUIT histórico sin reinterpretar datos potencialmente inconsistentes. La resolución y normalización de documentos se implementan en la etapa 3, sin reinterpretar ni reescribir el CUIL histórico.

Las relaciones con pagos, comentarios, adjuntos e historial no tienen cascadas de eliminación. La licencia pierde cascade ALL/orphanRemoval sobre pagos. El esquema MySQL agrega una FK compuesta para asegurar la pertenencia licencia–cliente. Las evidencias rechazan actualización/borrado ordinarios en JPA. El pago incluye versión optimista y clave de presentación única por cliente para los servicios transaccionales. La etapa 4 implementa idempotencia de presentación; la aprobación administrativa registra una transición única y las acciones de licencia confirmadas.

El flujo anterior no puede construir un pago válido con evidencia, cuenta y datos de emisión. Sus escrituras web y Bearer se rechazan con 409; los IDs web inexistentes conservan 404. La interfaz muestra un aviso y permite consultar el nuevo DTO mínimo con licencia nullable. Ya no ofrece edición, cobro, descuento ni anulación. Los contratos de consulta de licencias permanecen sin cambios. Los consumidores externos de pagos deben adaptarse al nuevo contrato antes de habilitar los flujos siguientes.

Las primeras tres secciones del consolidado siguen este orden, con aplicación detenida y copia recuperable:

1. Sección 01: auditoría de entidades.
2. [Vaciado autorizado de pagos anteriores](src/main/resources/db/20261102-01-migracion-consolidada.sql). Solo borra payment; aborta si detecta dependencias entrantes desconocidas. No deshabilita FKs ni elimina clientes, usuarios, licencias o sus estados.
3. [Estructura del portal](src/main/resources/db/20261102-01-migracion-consolidada.sql). Exige tabla payment vacía, crea la FK compuesta RESTRICT de licencia, retirando antes la FK histórica simple si existe, retira columnas antiguas y crea tablas/constraints nuevas.


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

Los ABM utilizan las tablas de [20261102-01-migracion-consolidada.sql](src/main/resources/db/20261102-01-migracion-consolidada.sql), que define AUTO_INCREMENT para las nueve entidades nuevas. Es una migración de ejecución única sobre el esquema previo; no repetirla sobre una base ya migrada. El acceso y la seguridad del portal se describen en la etapa 3; el estado de cuenta y la presentación se describen en la etapa 4.

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

La ampliación de adjuntos requiere aplicar manualmente 20261102-01-migracion-consolidada.sql, primero en una copia compatible de MySQL. Agrega description nullable a attachment y upload_key nullable e índice a payment_attachment; conserva los datos existentes. La generación del script no lo ejecuta. La etapa 5 incorpora revisión administrativa, comentarios privados, rechazo, aprobación y acciones sobre licencias; no se agregaron en esa etapa extracción de aprobados, conciliación ni servicios externos de pago. La sincronización de deuda se incorpora por separado.

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

Rechazar exige un motivo público. Aprobar abre un diálogo de dos pasos: ajustar licencias y revisar antes de confirmar. Carga automáticamente todas las licencias no anuladas del cliente, excluyendo las que están simultáneamente ocultas y con bloqueo forzado. No hay una selección adicional de licencias. El límite actual del contrato sigue siendo 100 licencias por revisión: si el cliente lo supera, se informa y se impide aprobar desde ese diálogo, sin omitir licencias silenciosamente.

La opción «Actualizar vencimiento a todas las licencias» inicia seleccionada y propone el día 20 del próximo mes. Solo aplica a las visibles sin bloqueo forzado, incluidas las vencidas, y conserva fechas posteriores a la propuesta. La fecha se acompaña de mes en letras y días calendario desde la fecha de negocio. Al desmarcarla se habilitan ajustes individuales: quitar bloqueo forzado y/o actualizar vencimiento. «Mantener licencia sin cambios» conserva la tarjeta en modo lectura y excluye sus ajustes; «Ajustar licencia» restaura su edición. Se puede aprobar el pago sin modificar ninguna licencia.

«Siguiente: revisar cambios» obtiene la vista previa del servidor y muestra todas las licencias incluidas, también las que quedan sin cambios, con estado y vencimiento anterior → nuevo. Desde allí se puede volver a ajustar o confirmar la aprobación; los acortamientos individuales requieren confirmación expresa. Una licencia vencida continúa vencida si solo se quita el bloqueo forzado; una fecha ausente continúa sin fecha. No se tocan límites de transacciones ni observaciones existentes. Cuando hace falta un estado nuevo por actualizar fecha, conserva los valores iniciales que ya utiliza el flujo web.

La fecha de negocio usa portal.review.time-zone, inicialmente America/Argentina/Buenos_Aires, configurable mediante CLIENTMANAGER_PAYMENT_TIME_ZONE. Se sugiere día 20 del próximo mes; un vencimiento posterior se conserva como sugerencia. No se admiten fechas anteriores a hoy; reducir una fecha previa requiere confirmación expresa por licencia. La vista previa se invalida al cambiar acciones. El servidor vuelve a calcular y comprobar versión del pago, revisión de cada licencia (incluida su visibilidad), listado completo del cliente, fecha de negocio y huella del plan al confirmar. Cambiar de día requiere revisar de nuevo.

La conversión del calendario persistente conserva el contrato previo: fecha al comienzo del día en la zona del servidor. El bloqueo efectivo y el estado resultante respetan esa regla y la selección del estado no anulado de menor ID. Configurar también la zona del proceso/servidor según el negocio; cambiar solo la zona de sugerencias no redefine el comportamiento histórico de la API de licencias.

Las decisiones toman bloqueos en orden cliente → pago → licencias ordenadas por ID y usan READ_COMMITTED para no conservar lecturas anteriores a una espera de bloqueo en MySQL. Solo PENDIENTE puede resolverse y la versión debe coincidir; reintentar una decisión ya confirmada devuelve 409 y no crea eventos nuevos. La decisión, su historial y las acciones efectivas de licencia se confirman o revierten juntos. Los cambios sin efecto no generan acciones ficticias. La auditoría de modificación del pago y de LicenseStatus usa el actor persistido; el historial identifica responsable, fecha y valores anteriores/nuevos. Aprobar o rechazar no modifica ClientAccountBalance ni reabre pagos.

Rutas administrativas nuevas bajo /web/api/payments/{id}: GET detail, comments, attachments, history, license-actions, licenses y approval-licenses; POST comments, approve-preview, approve y reject. Las escrituras exigen CSRF. La descarga de bytes mantiene su endpoint protegido anterior. No hay rutas de edición/borrado de comentarios o evidencias, extracción de aprobados ni integración de pagos externos. La sincronización de deuda usa el endpoint de integración Angrybox. Se reutilizan las tablas del modelo existente: esta etapa no requiere SQL adicional.

Verificación de la etapa 5: `clean verify` finalizó correctamente y los recorridos Playwright existentes pasaron contra H2 aislada con cuentas sintéticas. Las comprobaciones manuales de API y navegador cubrieron permisos y CSRF, comentarios públicos/privados, comprobantes, filtros, actualización del detalle, aprobación/rechazo únicos, decisiones simultáneas, revisión obsoleta, confirmación de acortamiento y auditoría. Una falla de persistencia provocada en esa base confirmó la reversión conjunta de pago, historial y acciones; el saldo se conservó. También se revisaron escritorio/móvil y cálculos de fechas en límites de mes, año y año bisiesto. No se agregaron suites de cobertura. Falta verificar estos bloqueos y operaciones contra una copia compatible de MySQL; H2 no demuestra su comportamiento en producción.

### Saldos en la administración de clientes

El listado muestra saldo externo en ARS y última sincronización. Permite filtrar deuda positiva, saldo a favor negativo, saldo cero y ausencia de información vigente. El filtro de antigüedad selecciona saldos informados con sincronización estrictamente anterior a la fecha/hora actual en `portal.review.time-zone` (por defecto `America/Argentina/Buenos_Aires`) menos 15, 30, 45 o 60 días; se combina con los demás filtros y pagina en base de datos. Los saldos anulados se consideran sin informar. La ficha muestra identificación, importe, moneda y sincronización del saldo, sin edición manual.


### Privacidad del portal según el ingreso

El acceso por DNI/CUIT oculta Datos de tu cuenta, Estado general, Mis licencias y la columna Detalle de pagos. El saldo ocupa todo el ancho disponible cuando no se muestra Estado general. El servidor también omite el estado general, los contadores de licencias, documentos y contacto en el resumen, no precarga el teléfono en el formulario y rechaza con 403 la consulta de licencias, detalles, comentarios y comprobantes por URL directa. El formulario permite informar pagos y adjuntar comprobantes nuevos sin consultar las licencias existentes. El ingreso con usuario y contraseña conserva las consultas completas del propietario.

Mis pagos informados muestra emisiones desde la fecha/hora comercial actual menos 45 días (inclusive), sin fecha hasta, con filtro y paginación en base de datos. Usa `portal.review.time-zone` (por defecto `America/Argentina/Buenos_Aires`), sin depender de la zona predeterminada de la JVM. Las emisiones posteriores al reloj actual también se incluyen. La columna Archivos Adjuntos cuenta los vínculos y archivos no anulados, incluidos agregados posteriores, sin exponer sus metadatos. El saldo muestra «Informado:» junto a su fecha y se retira la descripción anterior. Estos cambios no alteran el esquema ni requieren migración SQL.

Verificación de privacidad: `clean verify` correcto con Java 11, pruebas Java sobre H2 y pruebas frontend existentes. Se revisaron ambos modos en escritorio (1440 px) y móvil (390 px) con respuestas sintéticas, comprobando visibilidad y ausencia de desborde de página. No se ejecutó la prueba opcional de migración MySQL ni se modificaron datos compartidos.


## Correos y alertas de pagos

El servicio general `MailService` envía HTML UTF-8 por SMTP, independiente de los pagos. Configurar `src/main/resources/mail.properties`, leído explícitamente como UTF-8 para conservar acentos y Ñ. Para configuración externa usar `--clientmanager.mail.config=file:C:/config/clientmanager-mail.properties` (archivo completo UTF-8); reiniciar después de cambiarlo. Las propiedades de Spring de mayor prioridad y variables referenciadas permiten sobrescribir la configuración. No colocar plantillas con acentos crudos en otros archivos properties cuya lectura no garantice UTF-8.

El envío está deshabilitado inicialmente. Completar servidor, usuario, contraseña, remitente, `mail.builder.alert-payment-to` (direcciones separadas por `;`) y `clientmanager.public-url` (URL absoluta de producción con contexto, sin `/ui`, query ni `#`). Habilitar `mail.enable` y mantener `mail.builder.alert-payment-active=true`. Las credenciales usan variables de entorno; no versionarlas. STARTTLS está habilitado y requerido para el puerto 587; ajustar las opciones según el servidor. `mail.debug=false` evita la salida de diagnóstico SMTP. `mail.to-test`, cuando no está vacío, reemplaza todos los destinatarios, incluidos los de futuros usos del servicio. Los destinatarios se envían por BCC.

La alerta se prepara exclusivamente al crear un pago desde el portal y se encola después del commit. Un rollback, reenvío idempotente o agregado posterior de comprobantes no genera una alerta nueva. Incluye cliente, ID, importe ARS, emisión local, cantidad inicial de comprobantes y enlace a `/ui/#/payments?status=PENDIENTE`; la autenticación y autorización administrativas siguen siendo obligatorias. No adjunta archivos privados ni confirma acreditación, aprobación o extensión de licencias.

Asunto y HTML son editables mediante `mail.builder.alert-payment-asunto` y `mail.builder.alert-payment-body`. `PaymentMailVariable` enumera las variables `{{...}}` admitidas con una descripción, incluido el alias `{{nombre-cliente}}`. Las variables desconocidas cancelan esa alerta con ERROR; las sustituciones HTML se escapan y no se evalúan como expresiones. El HTML es configuración de confianza del operador; usar variables en texto o atributos con comillas dobles. Las variables de importe no incluyen símbolo: el template agrega moneda y `$`.

El recordatorio de pendientes se ejecuta al finalizar el arranque y diariamente a las 14:00 de Buenos Aires. Se configura con `mail.builder.pending-payment-active`, `mail.builder.pending-payment-cron` (cron Spring de seis campos, `0 0 14 * * *`) y `mail.builder.pending-payment-zone` (`America/Argentina/Buenos_Aires`). Requiere `mail.enable=true` y reutiliza `mail.builder.alert-payment-to`, `mail.to-test` y la cola SMTP; es independiente de la activación de la alerta de alta. Sin pendientes no envía correo ni modifica datos.

Asunto y HTML: `mail.builder.pending-payment-asunto` y `mail.builder.pending-payment-body`, incluidos en ambos archivos de correo. Variables: `{{pagos-cantidad}}` (total), `{{pagos-mostrados}}` (hasta 50), `{{pagos-pendientes-url}}` y `{{pagos-resumen}}`. Esta última contiene filas HTML y debe colocarse exclusivamente dentro de `tbody`, nunca en atributos ni en el asunto. Sus celdas se escapan; las variables desconocidas cancelan el aviso. La consulta limita el detalle a las 50 presentaciones más antiguas e incluye ID de pago, cliente, importe y emisión informada. El total y el detalle pueden cambiar por revisiones concurrentes. No requiere cambios de esquema.

Cada instancia ejecuta su propio recordatorio, sin coordinación entre réplicas ni deduplicación entre arranque y horario diario. Reiniciar puede generar otro aviso si siguen existiendo pendientes. Comparte las limitaciones de entrega de la cola descritas a continuación.

La cola FIFO es volátil y admite 1000 mensajes pendientes. `mail.tiempo-min-entre-mensajes-seg` establece segundos mínimos entre intentos (90 por defecto; -1/0 sin espera; máximo 86400), sin demorar la presentación del pago. Cada correo tiene un único intento, con tiempos de espera SMTP limitados. Los fallos registran ERROR sin cuerpos, direcciones ni secretos; no hay reintentos, persistencia ni garantía de entrega. Una cola llena descarta el mensaje con ERROR. Al detener el servicio se descartan pendientes con ERROR; una caída abrupta puede perderlos sin registro. Un fallo no revierte el pago. No requiere cambios de base de datos.

Verificación de correos: `clean verify` correcto con Java 11; después del ajuste final del intervalo se repitieron compilación, pruebas Java existentes y empaquetado. Comprobación manual con SMTP local sin envío externo: configuración UTF-8, asunto/HTML con acentos y Ñ, sustitución y escape de variables, importe es-AR, enlace de pendientes, BCC, reemplazo por destinatario de prueba, espera entre mensajes y rechazo SMTP con un único ERROR sin reintento, continuando la cola. No se validaron credenciales, TLS ni entrega en el servidor SMTP real.

El perfil `dev` selecciona `classpath:mail-dev.properties`, con los valores SMTP del ejemplo: envío habilitado, servidor `mail.dysi.com.ar:587`, usuario/contraseña de ejemplo `test` / `you-password`, debug activo, sin SSL ni STARTTLS e intervalo de 90 segundos. Definir `CLIENTMANAGER_MAIL_USERNAME` y `CLIENTMANAGER_MAIL_PASSWORD` para usar credenciales reales sin versionarlas. `mail.to-test=info@dysi.com.ar` reemplaza los destinatarios de ejemplo de la alerta; los enlaces apuntan a `http://localhost:3000/clientmanager`. Conserva la plantilla completa y las variables documentadas. Iniciar con `--spring.profiles.active=dev`; se puede reemplazar el archivo mediante `--clientmanager.mail.config=file:C:/config/clientmanager-mail-dev.properties`. El perfil dev requiere revisar las opciones SMTP del proveedor y reiniciar después de cambios.

Los archivos de correo incluidos usan comentarios ASCII y escapes Unicode estándar de `.properties` en los valores (por ejemplo `\u00f3` para ó). Esto evita mojibake en editores que interpretan `.properties` como ISO-8859-1. La lectura explícita UTF-8 se conserva y los escapes se resuelven al cargar: los correos mantienen sus acentos y Ñ. Los archivos externos también pueden usar UTF-8 directamente.

### Certificados de extensión de licencias

Cada presentación nueva del portal puede generar un certificado por cliente, en la misma transacción del pago. `portal.license-extension.hours` (`CLIENTMANAGER_LICENSE_EXTENSION_HOURS`, por defecto 72, rango 1–87600) determina la duración exacta desde la creación del pago. Los cambios de configuración no alteran certificados existentes. No se genera retrospectivamente para pagos históricos ni para reenvíos idempotentes.

Un certificado activo cubre todas las licencias existentes, no eliminadas, sin bloqueo forzado y con vencimiento conocido ya alcanzado. Solo cambia el vencimiento enviado en `/api/clients/license`; no modifica licencias, saldos ni la estructura JSON. En este endpoint legado, `licenseStatuses[].lockDate` se devuelve como milisegundos numéricos, según el contrato consumido por AngryBox, independientemente del formato ISO de Jackson para otras respuestas. Una licencia vigente conserva su respuesta habitual. El certificado deja de ser válido exactamente al alcanzar su fecha/hora final.

No se acumulan certificados activos. Uno vencido normalmente permite otro con una nueva presentación. Rechazar su pago de origen lo anula, incluso si ya venció. Si el último certificado está anulado, las nuevas presentaciones no generan otro hasta que se aprueba un pago presentado posteriormente al que lo originó. Esa aprobación registra la recuperación para futuras presentaciones, sin reactivar el certificado ni generar otro automáticamente. Aprobar el pago de origen conserva el certificado; el auditor sigue ajustando explícitamente los vencimientos que correspondan.

Las presentaciones y decisiones se serializan mediante el bloqueo del cliente. Los certificados conservan auditoría y el pago que recuperó el beneficio. El cambio histórico está incluido en `src/main/resources/db/20261102-01-migracion-consolidada.sql`, asumido ya ejecutado. El SQL histórico queda fuera del changelog de Liquibase.

Limitación de compatibilidad: AngryBox convierte el vencimiento recibido a una fecha sin hora y lo conserva localmente. ClientManager verifica las horas exactas en cada consulta, pero no puede revocar una respuesta ya guardada ni garantizar el corte horario local sin cambios en AngryBox. La anulación se refleja en la siguiente consulta. No se incorporó una pantalla ni un PDF del certificado.
### Corrección administrativa de pagos pendientes

En el detalle de un pago PENDIENTE, el acceso administrativo ADMIN dispone de «Editar pago» para corregir importe en ARS, fecha/hora local de emisión, teléfono, licencia opcional del mismo cliente y destino bancario. Para cambiar el destino se elige una cuenta activa configurada, con banco activo; se muestran banco, titular, CBU y alias y se comprueba su revisión al guardar. Sin nueva cuenta se conserva la fotografía registrada, incluso si su cuenta fue desactivada posteriormente. Se exige un motivo privado y se registra un comentario inmutable con los valores anteriores y nuevos, actor y fecha/hora. El cliente y los comprobantes se conservan. Guardar la corrección mantiene el estado PENDIENTE; la aprobación es una operación posterior.

El servidor bloquea cliente y pago, comprueba la versión y rechaza correcciones sobre pagos aprobados o rechazados. Una corrección invalida las vistas previas anteriores de aprobación. La huella de los campos originales conserva la idempotencia de reenvíos del portal, aunque el auditor haya corregido esos datos. No existe un rol AUDITOR separado: se usa el permiso ADMIN de la revisión actual.

Requiere aplicar manualmente `src/main/resources/db/20261102-01-migracion-consolidada.sql`, primero sobre una copia compatible de MySQL. Agrega una huella nullable a payment; no modifica los datos históricos ni se ejecuta automáticamente. La verificación con H2 no sustituye esa comprobación en MySQL.
La corrección bancaria requiere además `src/main/resources/db/20261102-01-migracion-consolidada.sql`: conserva una huella de la cuenta y fotografía originales para validar los reenvíos del portal contra la presentación inicial, aunque el destino haya sido corregido varias veces. Los datos bancarios previos quedan en los comentarios privados de corrección y no se exponen como auditoría en el portal.

### Bitácora de seguimiento

La administración dispone de «Bitácora de seguimiento», ubicada en el menú inmediatamente antes de «Parámetros», y de un acceso desde cada licencia. Registra las consultas que llegan a `/api/clients/license` y `/api/v2/clients/license`, incluidos rechazos de seguridad de la aplicación: recepción, método, versión, resultado HTTP, duración, IP remota, User-Agent, usuario solicitado, cuenta autenticada y empresa solicitada. Solo una respuesta exitosa agrega el ID interno de licencia, nombre de empresa y fotografía del primer estado entregado (`forcedLock` y `lockDate` efectivo, incluidas extensiones). Un intento fallido puede declarar una empresa sin haber validado su licencia. No guarda JWT, contraseñas, cuerpos completos ni cabeceras arbitrarias. La entidad conserva la auditoría común; la escritura tiene una transacción independiente y una falla de la bitácora se informa en el log sin cambiar la respuesta de licencia. No se garantiza captura ante caída del proceso/base, rechazos de infraestructura anteriores a la aplicación o errores posteriores al envío de la respuesta.

El endpoint histórico y AngryBox2 actual conservan su contrato y comportamiento. No se corrige incidentalmente la autorización histórica basada en el parámetro `username`; la bitácora distingue ese parámetro de la cuenta autenticada. La nueva versión sí exige coincidencia exacta entre `username` y el principal autenticado, cuenta habilitada/no anulada y licencia del cliente asociado. No hay excepción para consultar otras cuentas como ADMIN.

Contrato nuevo: `POST /api/v2/clients/license?username=<cuenta>&companyId=<empresa>` con el mismo Bearer de `/auth`, roles ADMIN/USER/SELLER y `Content-Type: application/json`. El cuerpo puede ser `{}` o incluir los campos opcionales `installationId` (hasta 100 caracteres), `processId` (hasta 100, identificador nuevo por arranque), `hostname` (hasta 255), `appVersion` (hasta 100) y `operatingSystem` (hasta 100). Devuelve el mismo JSON de licencia que v1. Datos inválidos producen 400, falta de autenticación 401, identidad/cuenta no autorizada 403 y licencia ausente 404. Estos campos son declarativos: ClientManager no genera ni valida una identidad de máquina. AngryBox2 todavía no los envía y no se modificó ese proyecto en esta etapa.

Los listados administrativos bajo `/web/api/license-query-log` exigen ADMIN. Admiten `keyword` literal (empresa, usuarios, IP, instalación y hostname), `companyId`, `licenseId` interno validado, `ip` exacta, `from`/`to` como fechas de calendario inclusivas, `version=ALL|v1|v2`, `result=ALL|SUCCESS|ERROR`, `page` y `size=25|50|100` (50 por defecto). `/summary` agrega todos los registros filtrados; `/origins` devuelve hasta 50 grupos de actividad más reciente por empresa solicitada, IP, cuenta autenticada, instalación y hostname. El análisis muestra cantidades, IP e instalaciones distintas y primera/última actividad; no confirma clonaciones ni prueba simultaneidad. El ID de empresa agrupa también intentos fallidos; filtrar por ID interno de licencia incluye solo respuestas con licencia validada. Los instantes se transportan en ISO con zona y se muestran en `license.query-log.time-zone`; no se cambia el contrato local de fechas de pagos.

Configuración en `application.properties` (admite las variables de entorno indicadas allí):

```properties
license.query-log.retention-days=90
license.query-log.cleanup-cron=0 0 3 * * *
license.query-log.time-zone=America/Argentina/Buenos_Aires
```

El cron se ejecuta diariamente a las 03:00 en esa zona. Elimina físicamente, en transacciones de hasta 1000 filas, las consultas cuya recepción sea estrictamente anterior al instante actual menos los días configurados de 24 horas (valor permitido: 1 a 36500). El límite exacto se conserva hasta la siguiente ejecución; registros vencidos pueden permanecer hasta el próximo cron. `cleanup-cron=-` desactiva la tarea. La purga por retención es intencionalmente un borrado físico de esta tabla, no una anulación de negocio ni una modificación de su auditoría. No afecta licencias ni usuarios.

La IP se toma de `HttpServletRequest.getRemoteAddr()`: no se aceptan directamente `X-Forwarded-For` ni `Forwarded`. Si hay proxy, revisar y configurar su confianza en la infraestructura/Tomcat antes de interpretar el dato como IP del servidor AngryBox. NAT, VPN e IP dinámicas impiden usarla como identidad única; MAC y datos locales no son observables remotamente por este endpoint. Una copia puede conservar identificadores y credenciales. No hay bloqueo automático, activación por máquina ni latido periódico; el estado local de AngryBox sigue funcionando según sus reglas actuales.

Como referencia histórica, `src/main/resources/db/20261102-01-migracion-consolidada.sql` preparaba una base original sin migrar; la bitácora está incluida al final del único consolidado, primero en una copia MySQL compatible. Crea una tabla vacía con índices y referencias de auditoría sin cascadas; no reconstruye consultas anteriores. No se ejecuta automáticamente. La verificación aislada con H2 no acredita compatibilidad MySQL ni validación en producción.

### Protección antibots del acceso público

Los dos ingresos web (documento y contraseña) requieren Cloudflare Turnstile. El backend valida el token de un solo uso con Siteverify antes de autenticar y comprueba la acción (document_login/password_login) y el hostname contra una lista configurada. No envía DNI/CUIT ni contraseñas al proveedor. La API Bearer/JWT mantiene su contrato.

Configuración inicial:

1. Creá una cuenta en [Cloudflare](https://dash.cloudflare.com/) y abrí Turnstile → Add widget.
2. Agregá los hostnames del portal (sin esquema, puerto ni ruta), elegí **Managed** y dejá pre-clearance deshabilitado. No necesitás cambiar DNS ni hosting.
3. Configurá estas variables en el proceso que ejecuta Java:

| Variable | Valor |
| --- | --- |
| CLIENTMANAGER_TURNSTILE_SITE_KEY | Site key pública del widget |
| CLIENTMANAGER_TURNSTILE_SECRET_KEY | Secret key privada; guardarla fuera del repositorio |
| CLIENTMANAGER_TURNSTILE_HOSTNAMES | Hostnames admitidos separados por comas, iguales a los configurados en Cloudflare |
| CLIENTMANAGER_TURNSTILE_ENABLED | true por defecto; false por defecto con el perfil dev |

Reiniciá el backend y recargá la página. La clave pública se obtiene de GET /web/session/turnstile, sin caché; no requiere recompilar Vue. La secreta nunca se devuelve al navegador. El servidor requiere salida HTTPS a challenges.cloudflare.com; el navegador también debe poder cargar sus scripts y frames. Revisá cualquier CSP adicional del proxy y la información de privacidad del portal para reflejar este proveedor.

Sin claves o hostnames, con token inválido, vencido/reutilizado o ante falla del proveedor, no se concede acceso. El formulario conserva los datos y permite reintentar la verificación. Siteverify tiene timeout de conexión de 3 segundos y lectura de 5 segundos. El modo Managed se elige en Cloudflare, no desde el frontend.

Para desactivar temporalmente Turnstile por un problema operativo, configurá `CLIENTMANAGER_TURNSTILE_ENABLED=false` o cambiá la propiedad general a `portal.turnstile.enabled=false` en una configuración externa. Reiniciá el backend y recargá el navegador. Esto permite ambos ingresos sin CAPTCHA ni claves y conserva CSRF, permisos y el límite de intentos por documento. Para reactivarlo, usá `true`, verificá las claves y hostnames, reiniciá y recargá. Si usás la variable de entorno, recordá que prevalece sobre el valor por defecto del placeholder.

Para uso local, el perfil `dev` deshabilita Turnstile por defecto; la variable `CLIENTMANAGER_TURNSTILE_ENABLED=true` permite reactivarlo. Las cuatro propiedades de Turnstile están en `application-dev.properties`. En IntelliJ, agregá `--spring.profiles.active=dev` en **Program arguments** (o seleccioná `dev` en **Active profiles** si la configuración ofrece ese campo). También podés ejecutar `java -jar target/clientmanager-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev`. Este perfil conserva su configuración existente de SMTP, sincronización y base de datos; revisá la base de destino antes de iniciar.

El perfil test deshabilita expresamente Turnstile para no depender de Internet. Para desarrollo local también podés probar el widget del frontend con las [claves oficiales de prueba](https://developers.cloudflare.com/turnstile/troubleshooting/testing/). La respuesta de ejemplo de Siteverify de prueba usa la acción `test`, que el backend rechaza deliberadamente: para verificar el flujo completo con las acciones reales usá un widget separado de desarrollo o una respuesta simulada de Siteverify en un harness aislado. Nunca usar claves de prueba en producción. Playwright también debe apuntar a un backend aislado con esta configuración explícita.

Se conserva el límite del acceso por documento (por defecto 10 solicitudes por dirección remota cada 15 minutos), en memoria por instancia: se reinicia con el proceso y no se comparte entre servidores. Detrás de un proxy hay que verificar qué dirección recibe Java; no se confía directamente en cabeceras arbitrarias. Turnstile no acredita la titularidad del documento ni sustituye los permisos restringidos del portal. La validación real con el dominio y las claves definitivas debe completarse en el entorno de despliegue.

### Dirección y observaciones de clientes

La administración permite registrar calle, número (texto para conservar valores como «s/n»), interior/piso/departamento/oficina, barrio/sector, ciudad/localidad libre, país, provincia, código postal y observaciones generales de hasta 4000 caracteres. Son opcionales: no se asigna una dirección ni un país a clientes históricos. El formulario separa datos principales de contacto y dirección; la ficha y el listado muestran los nuevos datos. CUIT y DNI se presentan juntos en una columna, conservando ambos valores.

La migración `20261109-01-direccion-clientes.sql` crea catálogos auditados y carga únicamente Argentina, sus 23 provincias y CABA. Al crear un cliente, el formulario selecciona Argentina y Mendoza por defecto usando los IDs del catálogo; al editar conserva la selección existente. No hay una pantalla de mantenimiento geográfico. El servidor comprueba la relación provincia–país. En el contrato web, omitir/NULL en los campos nuevos conserva el valor; texto vacío lo limpia y el ID cero limpia país/provincia (se deben limpiar ambos cuando corresponde). Los contratos de sincronización AngryBox no incorporan estos datos.

Antes del despliegue, validar precondiciones, actualización y rollback sobre una copia compatible de MySQL. El rollback elimina columnas y catálogos: requiere respaldo previo de direcciones/observaciones para recuperarlos; MySQL confirma DDL implícitamente y no ofrece reversión transaccional global. Restaurar desde el respaldo y volver a la versión anterior de la aplicación si se revierte el esquema. Las pruebas H2 no validan esta migración.
