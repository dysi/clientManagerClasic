# DySI Client Manager

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

No existe una cuenta de demostración ni bootstrap de administrador. La base debe contener una cuenta habilitada con contraseña BCrypt, rol `ROLE_ADMIN` y las autoridades que se vayan a asignar (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_SELLER`). Los IDs de los roles dependen de la base y no se fijan desde la interfaz.

La configuración heredada contiene valores de conexión y secreto que deben sustituirse mediante configuración externa y rotarse si se utilizaron. Este documento no reproduce sus valores. `ddl-auto=update` puede modificar el esquema al arrancar; validar primero una copia del entorno y usar `SPRING_JPA_HIBERNATE_DDLAUTO=validate` cuando corresponda. Los cambios de base se versionan como scripts MySQL manuales en `src/main/resources/db/`; no se ejecutan automáticamente.

## Navegación y bienvenida

- Abrir `http://localhost:3000/` redirige a la aplicación, conservando el contexto `/clientmanager` y las URLs de las integraciones existentes. Si el despliegue usa otro contexto, la entrada se adapta a esa configuración.
- Sin sesión, la entrada lleva a `/ui/#/login`. Con sesión, lleva a `/ui/#/licenses`, la pantalla principal actual.
- Una URL protegida conserva su ruta y filtros en `redirect`; al autenticarse se regresa a esa pantalla. Solo se aceptan destinos internos conocidos; se rechazan URLs externas, rutas inexistentes y retornos al login.
- Una sesión vencida redirige al login con el destino pendiente. Un intento de acceso fallido conserva ese destino. Cerrar sesión voluntariamente muestra el login sin restaurar la pantalla anterior automáticamente.
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
      payments/       Listado y formulario de pagos
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
| Acceso | `/login` | Inicio de sesión, errores de acceso y cierre de sesión confirmado |
| Clientes | `/clients`, `/clients/new`, `/clients/:id`, `/clients/:id/edit` | Búsqueda, paginación, ficha de contacto, alta/edición y accesos a licencias/usuarios del cliente |
| Licencias | `/licenses`, `/licenses/new`, `/licenses/:id/edit` | Búsqueda y filtros por cliente/estado, empresa, vigencia, modalidad y bloqueo explícito |
| Pagos | `/payments`, `/payments/new`, `/payments/:id/edit` | Filtros por licencia/estado, importes, descuento, período, vencimiento, cobro y anulación/reactivación |
| Usuarios | `/users`, `/users/new`, `/users/:id/edit`, `/users/:id/password` | Identidad, roles, habilitación, vínculo opcional con cliente y formulario de contraseña separado |

Las operaciones iniciadas desde la ficha de un cliente o una licencia llevan la relación preseleccionada. No se cambia el cliente de una licencia existente ni la licencia de un pago existente. El formulario de usuario existente conserva el hash de contraseña; solo el formulario específico de contraseña lo modifica. La cuenta activa no puede deshabilitarse ni quitarse su propio rol ADMIN.

Los listados muestran cargas, errores y estados vacíos. La paginación ofrece 25, 50 y 100 registros, con 50 por defecto. Las filas se resaltan al pasar el mouse mediante la opción nativa de PrimeVue. Licencias permite combinar búsqueda de texto, estado y un selector de clientes con búsqueda remota; limpiar el cliente conserva los demás filtros. Las confirmaciones usan el diálogo del framework y los resultados se notifican con Toast. La navegación y los formularios se adaptan al ancho disponible; las tablas permiten desplazamiento horizontal cuando sus columnas lo requieren.

Las URLs anteriores `/users/login`, `/index/home`, `/users/create`, `/users/create/{id}` y `/users/edit/{id}` redirigen a las rutas nuevas. Ya no renderizan pantallas anteriores.

## Modelo y reglas

- **Client**: identificación, contacto y vinculación con una cuenta. Se conserva toda la ficha durante las ediciones.
- **User / Authority**: identidad, contraseña BCrypt, habilitación y roles. El vínculo usuario–cliente se actualiza en ambos sentidos y se rechaza un cliente ya vinculado a otra cuenta.
- **License**: cliente propietario, identificador/nombre de empresa, descripción, inicio de actividad y modalidad de pago.
- **LicenseStatus**: fecha de bloqueo, bloqueo forzado y datos históricos. Para compatibilidad se utiliza el estado activo de menor ID; al guardar una licencia sin estado se crea uno. Conviene normalizar esta regla mediante una migración futura.
- **Payment**: licencia, importe, descuento, período, vencimiento, fecha de cobro, observación y fecha de baja.

El identificador de empresa se verifica al crear y editar. La garantía bajo concurrencia requiere una restricción de base de datos, pendiente de una migración de esquema. Los importes aceptan hasta dos decimales y el descuento no puede superar el importe. El transporte utiliza `BigDecimal`; las columnas existentes siguen siendo `Double`, por lo que queda pendiente migrarlas a DECIMAL.

La columna Fecha de bloqueo incluye el contador de días calculado en la zona horaria del servidor (`daysUntilLock`). Las filas bloqueadas usan fondo rojo; las vencidas, fondo y etiqueta amarillos; las que vencen dentro de 1 a 15 días muestran Por vencer en rojo. Los estados de peligro/advertencia conservan su color al pasar el mouse. Una licencia está bloqueada si tiene bloqueo forzado, vencida si su fecha de bloqueo es hoy o anterior, y por vencer si vence dentro de 15 días. El filtro de activas incluye las vigentes que están por vencer. Una licencia sin fecha aparece con estado desconocido y debe completarse. Las fechas viajan como `YYYY-MM-DD`; su interpretación usa el día local del servidor. Definir una zona horaria coherente entre despliegues.

Los pagos se consideran pendientes sin fecha de cobro, pagados con ella y anulados si tienen fecha de baja. Anular no borra el registro; debe reactivarse antes de editarlo. Registrar o cobrar un pago **no extiende automáticamente la licencia**. Bloqueo y anulación reciben un estado explícito: repetir la misma petición no alterna el resultado ni reemplaza la fecha de baja original.

## API web y seguridad

Todas las rutas siguientes son relativas al contexto del backend:

| Método / ruta | Contrato |
| --- | --- |
| `GET /web/session/csrf` | Prepara sesión y devuelve nombre de cabecera/token CSRF |
| `POST /web/session/login` | Autentica, rota identificador de sesión y CSRF; no devuelve JWT |
| `GET /web/session` | Identidad y roles de la sesión |
| `POST /web/session/logout` | Requiere CSRF e invalida la sesión |
| `GET /web/api/{clients,licenses,payments,users}` | Listados paginados, `keyword`, `page`, `size`; filtros propios por módulo |
| `GET /web/api/{recurso}/{id}` | Ficha completa |
| `POST /web/api/{recurso}` | Alta validada, respuesta 201 |
| `PUT /web/api/{recurso}/{id}` | Edición validada |
| `PUT /web/api/licenses/{id}/lock` | `{ "locked": true/false }` |
| `PUT /web/api/payments/{id}/status` | `{ "removed": true/false }` |
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

Los usuarios son relaciones LAZY a `User`, sin cascadas. `AuditEntityListener` resuelve el actor a partir del ID del principal de sesión/JWT y registra las fechas en los callbacks JPA, también en persistencias por cascada. No se aceptan metadatos de auditoría desde JSON ni se serializan sus relaciones con usuarios. Los DTO web conservan sus contratos; las fechas preexistentes de pagos y estados mantienen sus propiedades y columnas.

`markRemoved()` anula y `restore()` reactiva. Repetir una anulación conserva la fecha y el actor originales; actualizar otros campos de un registro anulado tampoco los reemplaza. Reactivar limpia fecha/actor y una anulación posterior registra un nuevo evento. Esto no crea endpoints adicionales de anulación para clientes/licencias/usuarios ni cambia automáticamente sus reglas de habilitación o los filtros de negocio.

Los registros históricos mantienen sus fechas conocidas y los datos desconocidos en NULL. Una escritura sin sesión registra fechas con actor NULL, sin inventar una cuenta de sistema. Los cambios exclusivos de roles marcan también al usuario como modificado. Las operaciones masivas JPQL/SQL y las modificaciones externas a JPA eluden los callbacks y requieren auditoría explícita. Se conserva el último evento de cada tipo; no es una tabla histórica de todas las revisiones.

Antes de iniciar esta versión sobre una base existente, aplicar manualmente **`src/main/resources/db/20261026-01-auditoria-entidades.sql`**, primero sobre una copia del esquema. El script agrega los campos faltantes y las claves foráneas a usuario; conserva `payment.creationdate/removedate` y `licensestatus.creationdate/removedate` y no rellena fechas ni actores ficticios. Leer sus precondiciones de motor InnoDB, nombres físicos y tipo de ID. MySQL hace commit implícito de DDL; la serie no tiene rollback transaccional global. `ddl-auto=update` no sustituye este procedimiento y puede adelantarse al script: aplicar el SQL con la aplicación detenida y utilizar `validate` al arrancar contra el esquema preparado.

Los cambios futuros de BD deben tener un archivo nuevo en `src/main/resources/db/`, con formato **`yyyyMMdd-NN-descripcion-breve.sql`**. La secuencia comienza en `01` cada día; antes de crear un archivo se revisan los números existentes y se toma el siguiente al máximo, sin repetir ni renumerar. Ejecutar por fecha y número secuencial. No hay Liquibase/Flyway ni ejecución automática de estos archivos al arrancar. Toda entidad nueva debe heredar la auditoría; `AGENTS.md` establece esta obligación.

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

- Java: auditoría de las cinco entidades, usuarios de sesión/JWT, cascadas, anulación/reactivación, datos históricos y metadatos JSON; contexto, compatibilidad JWT, roles, CSRF, rotación/logout de sesión, cuentas deshabilitadas, conservación de contacto, validación/paginación, redirecciones antiguas, unicidad y propietario de licencias, bloqueo idempotente, fechas/importes de pagos, anulación idempotente, contraseñas y vínculos usuario–cliente.
- Vitest: borradores de clientes, cancelación, errores por campo, contrato HTTP/CSRF y conservación de fechas locales.
- Playwright: acceso, cookie de sesión, alta/edición de los cuatro módulos, descarte confirmado, persistencia tras recarga, bloqueo/desbloqueo, anulación/reactivación, cambio de contraseña, permisos y adaptación móvil.

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
4. Mantener el registro de ejecución de los scripts MySQL y ampliar las migraciones con restricciones de unicidad/asociación, dinero DECIMAL y una regla explícita para estados históricos de licencia.
5. Validar esquema MySQL real, copias de respaldo, configuración de proxy/HTTPS y compatibilidad con AngryBox antes del despliegue.
6. Planificar actualización posterior de Java/Spring Boot y dependencias del backend; Java 11 y Boot 2.7 son la base de compatibilidad solicitada, no una actualización a las versiones mayores más recientes.
7. Medir consultas JPA, mejorar logs y ampliar pruebas de concurrencia, accesibilidad y carga según el uso real.

El análisis no aporta una necesidad demostrada de microservicios. La separación por módulos y contratos dentro del monolito permite mantener un despliegue único y evolucionar gradualmente el backend.

## Resultado de la entrega (26/10/2026)

Verificado con JDK 11: **`mvnw.cmd clean verify` — BUILD SUCCESS**, **22 pruebas Java**, **25 pruebas Vitest** y **3 recorridos Playwright completos** aprobados. La auditoría npm informó **0 vulnerabilidades**. Se revisaron capturas de escritorio y móvil y se comprobó que el JAR no contiene las plantillas, assets ni dependencias visuales retiradas. El recorrido se ejecutó sobre el JAR generado, con H2 y datos temporales externos, y cerró el proceso al finalizar.

No se conectó a MySQL ni se verificaron el esquema de producción, sus datos existentes o el consumidor AngryBox. Esas comprobaciones siguen siendo necesarias antes del despliegue en ese entorno.
