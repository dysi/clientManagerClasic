# Dysi Client Manager / AngryBox Client Manager

Aplicación web monolítica para administrar clientes, usuarios, licencias de empresas y pagos asociados al producto AngryBox.

## Estado actual

Backend en **Java 11 y Spring Boot 2.7.18**, empaquetado como **JAR ejecutable con Tomcat embebido**. Para desplegarlo se necesitan Java 11, acceso a MySQL y la configuración del entorno. No hace falta instalar Maven ni un servidor Apache Tomcat en el servidor de destino.

### Compilar y verificar

Desde la raíz del proyecto, con un JDK 11 seleccionado:

```powershell
# Ejemplo del JDK disponible en este equipo; adaptar la ruta en otros entornos.
$env:JAVA_HOME = 'C:\Users\rusza\.jdks\ms-11.0.31'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

En Linux/macOS, usar `./mvnw clean verify` con `JAVA_HOME` apuntando al JDK 11. El wrapper utiliza Maven 3.9.9 y necesita acceso a las dependencias la primera vez.

Artefacto desplegable: **`target/clientmanager-0.0.1-SNAPSHOT.jar`**. Incluye clases, dependencias, vistas, recursos estáticos y servidor HTTP. El archivo `.jar.original`, si Maven lo deja en `target`, es el archivo anterior al reempaquetado; desplegar el `.jar` indicado.

### Ejecutar y desplegar

Configurar las variables del entorno de destino antes de iniciar el proceso. Ejemplo en PowerShell, reemplazando los marcadores:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/dysi_clientmanager'
$env:SPRING_DATASOURCE_USERNAME = '<usuario-del-entorno>'
$env:SPRING_DATASOURCE_PASSWORD = '<clave-del-entorno>'
$env:JWT_SECRET = '<secreto-del-entorno>'
$env:SERVER_PORT = '3000'
java -jar target/clientmanager-0.0.1-SNAPSHOT.jar
```

Al copiar el JAR al servidor, ejecutar `java -jar clientmanager-0.0.1-SNAPSHOT.jar` desde la carpeta donde se lo haya ubicado. El proceso permanece en primer plano; para mantenerlo en ejecución en un servidor, administrarlo con el gestor de servicios de ese sistema. Para detener una ejecución interactiva, usar `Ctrl+C`.

URL del login con los valores predeterminados: **http://localhost:3000/clientmanager/users/login**. Para cambiar el puerto: `java -jar clientmanager-0.0.1-SNAPSHOT.jar --server.port=8080`. El contexto se configura con `server.servlet.context-path` o `SERVER_SERVLET_CONTEXTPATH`; el frontend contiene rutas fijas con `/clientmanager`, por lo que cambiarlo requiere adaptar esas rutas.

También se puede pasar un archivo externo sin incorporar credenciales al JAR:

```powershell
java -jar clientmanager-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:./config/
```

En ese ejemplo, crear `config/application.properties` con los valores del despliegue. La aplicación no incluye bootstrap de roles/administrador ni migraciones de esquema. La configuración actual usa `ddl-auto=update`: al arrancar contra MySQL puede modificar el esquema. Validar primero sobre una copia y definir la política de esquema del despliegue, por ejemplo `SPRING_JPA_HIBERNATE_DDLAUTO=validate` cuando el esquema ya esté preparado.

### Cambios realizados

- Java 11 con `maven.compiler.release=11` y Maven Wrapper 3.9.9.
- Spring Boot 2.7.18; consultas con `findById(...).orElse(null)` y paginación con `PageRequest.of(...)`.
- Inicialización de seguridad sin dependencia circular y bean `AuthenticationManager` explícito.
- Spring Mobile como dependencia directa, con interceptor y resolución de argumentos en `WebMvcConfig`.
- Validación y API JAXB explícitas; MySQL Connector/J y Gson administrados por Spring Boot; WebJars Locator Core.
- Thymeleaf en modo `HTML`, sin NekoHTML, y contexto consolidado en las propiedades actuales.
- Nombres físicos con `CamelCaseToUnderscoresNamingStrategy` y generadores de IDs heredados con `spring.jpa.hibernate.use-new-id-generator-mappings=false`.
- JUnit 5 y perfil `test` con H2 aislado de MySQL.
- Empaquetado `jar`, Tomcat incluido por `spring-boot-starter-web` y entrada mediante `main`; retirada la configuración específica de despliegue WAR.

Las siguientes secciones describen el código actual. Los hallazgos de seguridad y negocio indicados como pendientes no se resolvieron mediante los cambios de plataforma y empaquetado.

Fuentes: [despliegue de aplicaciones Spring Boot](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/deployment.html) y [compatibilidad de Spring Boot 2.7](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/getting-started.html). Spring Boot 2.7.18 admite Java 11, pero su soporte comunitario finalizó; ver el [anuncio de la versión](https://spring.io/blog/2023/11/23/spring-boot-2-7-18-available-now/).

## Alcance del análisis

Análisis realizado el **24 de octubre de 2026** sobre el código fuente y la configuración presentes en este repositorio. Incluye arquitectura, dominio, interfaz, API, seguridad, persistencia, ejecución, pruebas y deuda técnica.

Los comportamientos descriptos se deducen de la implementación. No se verificó el sistema contra una base MySQL ni una instalación de AngryBox. Las fallas señaladas son hallazgos de revisión estática; no constituyen una prueba de explotación. No se reproducen los secretos encontrados en los archivos de configuración.

## 1. Propósito y funcionalidad

El proyecto se identifica en Maven como `com.dysi:clientmanager:0.0.1-SNAPSHOT` y se describe como «Gestor de cliente y licencias de AngryBox». El paquete Java principal es `com.dysi.AngryBoxClientManager`.

Funciones implementadas:

- Inicio de sesión con usuario y contraseña; emisión de JWT.
- Alta y edición de usuarios, habilitación, asignación de roles y vinculación con clientes.
- Alta y edición de clientes, con nombre, apellido, correo, teléfono y CUIL.
- Alta y edición de licencias por empresa, fecha de inicio y fecha de bloqueo.
- Bloqueo y desbloqueo manual de licencias.
- Consulta de clientes y licencias por texto y estado de vencimiento.
- Registro y edición de pagos, importes, descuentos, fechas y observaciones.
- Baja lógica y reactivación de pagos.
- Consulta de licencia por usuario y empresa para la integración con AngryBox.

No se encontraron integraciones con pasarelas de pago, facturación electrónica, correo, colas, servicios externos invocados desde el backend ni procesos programados de cobro. El registro de pagos es administrativo. La integración visible con AngryBox consiste en un endpoint de consulta; el consumidor externo no está incluido.

## 2. Arquitectura

Es un **monolito por capas**: las vistas, los recursos estáticos, la API, la autenticación y la lógica de negocio se empaquetan en un único JAR ejecutable y comparten una base de datos. La organización se realiza por tipo técnico, no por módulo de negocio.

```text
Navegador
  ├── HTML renderizado con Thymeleaf
  └── JavaScript / Knockout / jQuery / Axios
                   │ HTTP + JSON + JWT
                   ▼
Spring Security → Controllers MVC / RestControllers
                                    │
                                    ▼
                         Services / ServicesImpl
                                    │
                                    ▼
                       Repositories Spring Data JPA
                                    │
                                    ▼
                            Hibernate → MySQL
```

La clase `AngryBoxClientManagerApplication` inicia Spring Boot desde su método `main`. El plugin Maven genera un JAR ejecutable y `spring-boot-starter-web` incorpora Tomcat embebido. No hay dependencia de un contenedor Servlet instalado por separado.

Fortalezas de la estructura:

- Separación reconocible entre acceso HTTP, servicios y repositorios.
- Interfaces para los servicios principales.
- Consultas JPQL parametrizadas y uso de DTO en varias lecturas.
- Contraseñas codificadas con BCrypt.
- Restricciones de roles en los métodos administrativos de la API.

Limitaciones: algunos controladores coordinan varios servicios, se mezclan entidades y DTO en las respuestas, y no hay límites transaccionales explícitos para varias operaciones compuestas.

## 3. Tecnologías y dependencias

Versiones declaradas o identificadas en los archivos del proyecto; no representan una comprobación de versiones instaladas en producción.

| Componente | Evidencia / versión |
| --- | --- |
| Java | `11`, compilación con `--release 11` |
| Spring Boot | `2.7.18` |
| Backend web | Spring MVC y starter Web |
| Seguridad | Spring Security, BCrypt, autenticación stateless |
| Persistencia | Spring Data JPA, Hibernate y driver MySQL |
| Spring Data REST | Starter incluido; revisar exposición automática de repositorios |
| Vistas | Thymeleaf 3, modo `HTML` |
| JWT | JJWT `0.7.0`; firma HS512 |
| JSON | Jackson y Gson administrados por Spring Boot |
| Dispositivos | Spring Mobile Device `1.1.5.RELEASE` y configuración MVC explícita |
| Frontend | Knockout `3.4.2`, jQuery local `3.0.0`, Axios, Moment, Lodash, Alertify, jQuery Validation |
| Estilos | Bootstrap y Font Awesome locales |
| WebJars | jQuery `2.1.1`, Bootstrap `4.0.0-beta`, Font Awesome `4.5.0` |
| Build | Maven Wrapper `3.9.9` |
| Pruebas | Spring Boot Test, Spring Security Test, JUnit 5 y H2 |
| Empaquetado | JAR ejecutable `clientmanager-0.0.1-SNAPSHOT.jar`, Tomcat 9 embebido |

El frontend carga principalmente archivos locales desde `static`; los WebJars agregan otra fuente de dependencias y versiones. No hay `package.json` ni pipeline de compilación frontend. La antigüedad de las versiones amerita una revisión específica de dependencias; este análisis no incluyó un escaneo de vulnerabilidades ni una consulta a catálogos de CVE.

## 4. Estructura del repositorio

```text
pom.xml                         Dependencias y empaquetado
mvnw / mvnw.cmd                 Maven Wrapper
.mvn/wrapper/                   Configuración del wrapper
src/main/java/com/dysi/AngryBoxClientManager/
  AngryBoxClientManagerApplication.java
  config/                       Spring Security y resolución MVC de dispositivos
  controllers/                  Vistas MVC: inicio y usuarios
  restControllers/              Autenticación, clientes, pagos y usuarios
  services/                     Contratos de servicios
  servicesImpl/                 Implementación de negocio y UserDetailsService
  repositories/                 Seis repositorios JPA
  models/                       Seis entidades y enum de roles
  dtos/                         Objetos de entrada y proyecciones
  security/                     JWT, filtro y usuario autenticado
  utils/                        Fechas y proveedor de hora
src/main/resources/
  application.properties        Configuración general y conexión
  application-dev.properties    Perfil de desarrollo
  application.yml               Configuración JWT y Jackson
  templates/                    home, users y fragmentos shared
  static/                       JavaScript, CSS, fuentes e imágenes
src/test/java/                  Pruebas de contexto, login y JWT
src/test/resources/             Perfil test con H2 en memoria
target/                         Salidas de compilación; ignoradas por Git
```

Se identificaron 47 archivos Java en `src/main/java`. No se encontraron migraciones Flyway/Liquibase, scripts de datos iniciales, Dockerfile, configuración de CI ni licencia de distribución en los archivos revisados.

## 5. Modelo de datos

| Entidad / tabla declarada | Datos y responsabilidad |
| --- | --- |
| `User` / `USER` | Usuario único, hash de contraseña, nombre, apellido, correo, habilitación y última fecha de cambio de contraseña |
| `Authority` / `AUTHORITY` | Rol representado por `AuthorityName` |
| `Client` / `CLIENT` | Datos personales y fiscales de contacto |
| `License` / `LICENSE` | Empresa, identificador externo, descripción, inicio de actividad y pago anticipado |
| `LicenseStatus` / `LICENSESTATUS` | Fecha de bloqueo, bloqueo forzado, límite de transacciones, observaciones y fechas de creación/baja |
| `Payment` / `PAYMENT` | Importe, descuento, fechas de pago, pago real, vencimiento, creación y baja lógica |

Relaciones:

```text
User    N ── N Authority       tabla intermedia USER_AUTHORITY
User    1 ── 1 Client         ambos declaran @OneToOne propio
Client  1 ── N License
License 1 ── N LicenseStatus
License 1 ── N Payment
```

`License` propaga operaciones a pagos y estados con `cascade=ALL` y `orphanRemoval=true`. El servicio usa el primer elemento de `licenseStatuses` como estado operativo, aunque el modelo permite varios y no define un orden explícito.

La relación usuario–cliente merece revisión: ambos lados declaran `@OneToOne` sin `mappedBy`, por lo que no representan una única asociación bidireccional con un lado inverso. El servicio intenta mantener ambas referencias manualmente.

Los importes y descuentos usan `Double`, lo que introduce riesgo de precisión decimal. Las fechas usan `Calendar` y `Date`, sin política de zona horaria explícita. Los nombres físicos finales de tablas y columnas dependen también de la estrategia de nombres y del motor.

## 6. Reglas y flujos de negocio

### Autenticación

1. El navegador envía credenciales a `POST /auth`.
2. Spring Security carga al usuario desde MySQL y verifica la contraseña con BCrypt.
3. Se genera un JWT con usuario, audiencia según dispositivo, emisión y expiración.
4. El frontend guarda el token en `localStorage`.
5. Las llamadas AJAX protegidas envían `Authorization: Bearer <token>`.
6. El filtro valida el token y vuelve a cargar al usuario de la base en cada petición autenticada.

La configuración establece una duración de `604800` segundos, equivalente a siete días. No se encontró una lista de revocación ni un endpoint de logout; entrar a la página de login borra el token local.

### Clientes y licencias

- Los clientes se crean o editan usando `ClientDTO`; `idOld` determina si se carga un registro existente.
- Una licencia se vincula con un cliente y un `companyId` externo.
- Al crear una licencia se comprueba si el `companyId` ya está en uso. Esa comprobación no se repite en edición y no hay una restricción `unique` declarada para ese campo.
- Una licencia nueva recibe un estado con `forcedLock=false`, límite `9999999` y bloqueo a 90 días si no se informa fecha.
- Editar una licencia sin fecha de bloqueo también asigna 90 días desde el momento actual.
- El bloqueo manual alterna el booleano `forcedLock` del primer estado.
- La consulta de integración busca al usuario indicado, obtiene su cliente y selecciona la licencia de la empresa solicitada.

### Filtros de licencias

| `statusId` | Interpretación en `ClientRepository` |
| --- | --- |
| `-1` o ausente | Todas |
| `1` | Sin bloqueo forzado y con fecha de bloqueo futura |
| `2` | Sin bloqueo forzado, futura y dentro de `daysToLock` |
| `88` | Sin bloqueo forzado y con fecha de bloqueo pasada |
| `99` | Bloqueo forzado |

`daysToLock` toma 15 por defecto. La búsqueda usa nombre de empresa o nombre y apellido del cliente. Los estados activos incluyen también los próximos a vencer. El DTO usa `<=` para marcar vencimiento, mientras la consulta usa `<`: hay una diferencia de frontera temporal.

### Pagos

- Se crean y editan dentro de una licencia y se guardan mediante la cascada de esta.
- Importe y descuento ausentes se convierten a cero.
- La baja lógica alterna `removeDate` entre la fecha actual y `null`; repetir la operación reactiva el pago.
- Las consultas ordenan por `paymentDate` descendente y no excluyen pagos dados de baja.
- La paginación recibe páginas desde 1, con tamaño predeterminado 30.
- Registrar un pago no actualiza automáticamente la fecha de bloqueo de la licencia en los servicios revisados.

## 7. Interfaz web

Vistas principales:

| Ruta | Vista / comportamiento |
| --- | --- |
| `/users/login` | Inicio de sesión |
| `/index/home` | Administración de clientes, licencias y pagos |
| `/users/create` | Formulario y listas de usuarios, clientes y roles |
| `/users/create/{id}` | Carga del usuario para edición |
| `/users/edit` con POST | Selección de usuario a editar mediante MVC |

Las páginas se renderizan en el servidor y Knockout mantiene el estado de formularios y listas. jQuery y Axios realizan las llamadas HTTP. Moment convierte fechas de formularios a milisegundos antes del envío.

Aspectos relevantes:

- Varias consultas jQuery usan `async:false`, bloqueando la interacción del navegador mientras esperan.
- Existen rutas absolutas con `/clientmanager`, por lo que cambiar el contexto exige revisar HTML y JavaScript.
- `vm.header-autentication.js` comprueba la presencia del token y redirige; esto es una ayuda de navegación, no una autorización del servidor.
- `vm.home.js` concentra múltiples responsabilidades de clientes, licencias y pagos.
- `vm.user-form.js` usa una URL relativa `../api/users/edit`; desde `/users/create/{id}` puede resolverse a una ruta incorrecta bajo `/users/api/`.
- El lookup utilizado para seleccionar clientes no completa correo, teléfono ni CUIL en su constructor JPQL. La edición toma ese mismo objeto y el servicio convierte campos ausentes en cadenas vacías: existe riesgo de borrar esos datos al guardar desde la pantalla principal.

## 8. API HTTP

Las rutas siguientes son relativas al contexto configurado, normalmente `/clientmanager`. **Libre** significa que `@RequestMapping` no restringe el método HTTP; no equivale a acceso anónimo. Las lecturas de este tipo son llamadas con GET por el frontend cuando corresponde.

| Método declarado | Ruta | Entrada principal | Acceso / efecto |
| --- | --- | --- | --- |
| POST | `/auth` | JSON `username`, `password` | Público; devuelve `{token}` |
| GET | `/refresh` | Cabecera `Authorization` | Autenticado; renovación con inconsistencias |
| GET | `/user` | Bearer JWT | Autenticado; datos de `JwtUser`, sin contraseña |
| GET | `/protected` | — | ADMIN; endpoint de ejemplo |
| Libre | `/api/clients/license` | `username`, `companyId` | ADMIN, USER o SELLER; devuelve licencia |
| Libre | `/api/clients/getClients` | — | ADMIN; lista `ClientDTO` |
| Libre | `/api/clients/getClientsLookups` | `keyword`, `statusId`, `daysToLock` opcionales | ADMIN; listado combinado |
| POST | `/api/clients/edit` | `ClientDTO` | ADMIN; alta/edición |
| Libre | `/api/users/edit` | JSON `UserCreateDTO` | ADMIN; alta/edición |
| Libre | `/api/payments/paginated` | `username`, `companyId`, `pageSize`, `pageNumber` | ADMIN; pagos paginados |
| Libre | `/api/payments/licenses` | `clientID` | ADMIN; licencias del cliente |
| Libre | `/api/payments/licenciaBloquear` | `licenciaID` | ADMIN; alterna bloqueo |
| Libre | `/api/payments/paymentsByLicense` | `licenseId` | ADMIN; pagos de licencia |
| POST | `/api/payments/paymentEdit` | `LicenseDTO` | ADMIN; **guarda una licencia** |
| POST | `/api/payments/licenseEdit` | `PaymentDTO` | ADMIN; **guarda un pago** |
| Libre | `/api/payments/bajaPago` | `pagoID` | ADMIN; alterna baja lógica |

Los nombres `paymentEdit` y `licenseEdit` están invertidos respecto de su operación real; el frontend los consume así. Varias mutaciones devuelven texto con formato `código|mensaje`: `1` indica éxito, y otros códigos como `98` o `99` representan errores de negocio. Puede haber un HTTP 200 con un error de negocio en el cuerpo.

Los errores técnicos se traducen de forma desigual a 400, 403 o 500. No se encontró un manejador global de excepciones ni un contrato OpenAPI.

## 9. Configuración y ejecución local

Para construir, ejecutar y desplegar, usar los comandos de la sección «Estado actual» al comienzo del documento. Se requiere JDK 11 para compilar y Java 11 para ejecutar el JAR. MySQL debe estar disponible y contar con el esquema y los datos de acceso necesarios.

| Archivo | Contenido |
| --- | --- |
| `application.properties` | Puerto 3000, contexto `/clientmanager`, MySQL, esquema automático, nombres físicos y Thymeleaf |
| `application-dev.properties` | Configuración del perfil `dev` |
| `application.yml` | JWT y salida JSON indentada |
| `src/test/resources/application-test.properties` | H2 en memoria, dialecto H2 y esquema efímero para las pruebas |

La declaración de contexto YAML vacía fue eliminada. El contexto actual se declara mediante `server.servlet.context-path`. El perfil `dev` no se activa automáticamente; para desarrollo se puede usar:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

No hay mecanismo automático para crear al primer administrador. Los roles admitidos son `ROLE_USER`, `ROLE_ADMIN` y `ROLE_SELLER`; sus IDs dependen de cada base. El administrador inicial requiere contraseña BCrypt y habilitación. La página de usuarios no sustituye ese bootstrap porque la API de escritura exige ADMIN.

Los archivos del repositorio todavía contienen credenciales y un secreto JWT de configuración. Sustituir los valores usados en cada despliegue mediante configuración externa; este documento no reproduce esos secretos. La actualización de dependencias no verifica por sí sola la compatibilidad del servidor MySQL ni los nombres físicos e IDs de un esquema existente.

## 10. Seguridad: hallazgos

| Prioridad | Hallazgo y evidencia | Impacto / acción sugerida |
| --- | --- | --- |
| Alta | Credenciales de base, incluidas alternativas comentadas, en `application*.properties`; secreto JWT fijo en YAML | Externalizar y reemplazar los valores; revisar también el historial Git y rotar los que hayan sido utilizados |
| Alta | `WebSecurityConfig` permite `/users/create/**` y `/users/edit/**`; `UserController` carga datos sin restricción de rol | Posible exposición de datos de usuarios y clientes en HTML antes de cualquier redirección JavaScript; proteger los controladores MVC |
| Alta | `/api/clients/license` acepta un `username` arbitrario para USER y SELLER | No vincula el usuario solicitado con el principal autenticado; definir y probar autorización por propietario |
| Alta | `JwtTokenUtil.validateToken` no verifica `isEnabled()` y el filtro establece la autenticación manualmente | Un usuario deshabilitado podría conservar acceso con un token previo; comprobar habilitación durante validación |
| Alta | Cambiar contraseña en `UserServiceImpl` no actualiza `lastPasswordResetDate` | La invalidación de tokens anteriores al cambio queda sin efecto para ese flujo |
| Media | `/refresh` pasa la cabecera completa al parser, mientras el filtro exige el prefijo Bearer | Con el formato esperado por el filtro, la renovación intenta parsear `Bearer ...` como JWT |
| Media | `refreshToken` cambia `iat` pero conserva `exp` | No extiende la expiración; además la intención de renovar tokens móviles vencidos entra en conflicto con el parser y el filtro |
| Media | El filtro solo captura `IllegalArgumentException` al interpretar el token | Excepciones por expiración, firma o formato pueden escapar sin una respuesta 401 uniforme |
| Media | JWT en `localStorage` | Un XSS podría extraerlo; revisar defensa XSS, política de contenido y estrategia de almacenamiento |
| Media | Bloqueo y baja de pagos se ejecutan desde GET y alternan estado | Reintentos pueden revertir una operación; usar métodos explícitos y operaciones idempotentes |
| A verificar | Starter Data REST y repositorios públicos sin restricciones de exportación visibles | Comprobar rutas automáticas y autorización: no asumir que todas quedan cubiertas por los `@PreAuthorize` de los controladores |

CSRF está desactivado y la aplicación declara sesiones stateless. Esa decisión debe revisarse junto con el mecanismo final de transporte de credenciales. No se verificaron TLS, proxy, cabeceras efectivas ni controles de infraestructura.

## 11. Integridad y mantenibilidad: hallazgos

| Área | Evidencia / problema | Consecuencia |
| --- | --- | --- |
| Pérdida de datos de cliente | Constructor JPQL de `ClientLookup` omite contacto; UI reutiliza el lookup; `editClient` vacía los nulos | Guardar una edición puede borrar correo, teléfono y CUIL |
| Asociación usuario–cliente | En `UserServiceImpl`, se llama a `setClient(null)` antes de consultar el cliente anterior | La rama de limpieza nunca obtiene la asociación previa; pueden quedar referencias inconsistentes |
| Contraseñas | Toda edición de usuario vuelve a codificar `user.getPassword()` | No hay una operación separada ni conservación explícita de contraseña cuando no se cambia |
| Pagos inexistentes | `editPayment` crea un objeto nuevo si no encuentra el ID solicitado dentro de la licencia | Puede responder éxito sin actualizar el pago esperado |
| Licencia inexistente | `getPaymentsPaginated` llama al servicio antes de comprobar si la licencia es `null` | El servicio desreferencia `license.getId()` y termina en error |
| Estados de licencia | Varias operaciones usan `get(0)` sin verificar tamaño ni orden | Fallos ante listas vacías y ambigüedad si hay varios estados |
| Unicidad de empresa | Comprobación solo al crear, sin `unique` en el modelo | Duplicados en edición o por carreras concurrentes |
| Transacciones | No hay `@Transactional` en servicios con varias escrituras | Fallas intermedias pueden dejar una parte de la operación persistida |
| Validación | DTO de entrada sin uso de `@Valid` en los endpoints revisados | La validación de navegador se puede omitir; faltan controles de IDs, fechas, importes y paginación |
| Precisión monetaria | `Double` para dinero | Adoptar representación decimal y reglas explícitas de redondeo |
| Consultas | Listados completos, asociaciones y búsqueda con comodines | Posibles costos de carga, consultas adicionales y crecimiento sin límites; medir antes de optimizar |
| Fechas | `Calendar.getInstance()` distribuido y truncamiento de horas en `FechasHelper.getDiferenciaDias` | Resultados sensibles a la zona horaria y al redondeo; centralizar reloj y reglas |
| Contratos HTTP | Métodos libres, nombres cruzados y respuestas `código|mensaje` | Dificulta pruebas, clientes externos y evolución de la API |
| Observabilidad | `System.out`, logs de autenticación por petición y excepciones genéricas | Falta contexto operativo y diferenciación clara de errores |
| Esquema | `ddl-auto=update`, sin migraciones versionadas | Cambios difíciles de auditar y reproducir entre entornos |

## 12. Pruebas y validación

Las pruebas usan JUnit 5, `@ActiveProfiles("test")` y una base H2 en memoria. La URL, usuario, contraseña, driver y dialecto de las pruebas sustituyen los de MySQL; el esquema se crea y destruye durante su ejecución.

Tres casos automatizados:

1. Carga completa del contexto de Spring y los repositorios JPA.
2. Renderizado HTTP del formulario de login con MockMvc.
3. Creación de un usuario de prueba, autenticación BCrypt, emisión de JWT, comprobación de audiencia y acceso con Bearer a `/user`.

Ejecutar `mvnw.cmd test` para las pruebas o `mvnw.cmd clean verify` para compilar, probar y generar el JAR. H2 tiene alcance `test` y no se incluye en el artefacto desplegable. La comprobación de arranque del JAR se realiza por separado con H2 en un classpath de verificación y configuración temporal; no agrega un driver de pruebas al JAR de producción.

No hay todavía cobertura específica de las reglas de licencias y pagos, ni una verificación contra MySQL o el cliente AngryBox. Tampoco se midió rendimiento ni se hizo una revisión visual completa en navegador.

Pruebas que aportarían mayor valor al continuar el mantenimiento:

1. Acceso anónimo a formularios de usuario y matriz de roles de la API, incluyendo repositorios Data REST.
2. Consulta de licencias entre usuarios diferentes y uso de token de un usuario deshabilitado.
3. Token inválido/vencido, renovación e invalidación después de cambio de contraseña.
4. Editar un cliente desde home conservando todos sus datos de contacto.
5. Crear/editar licencia sin estados, con empresa duplicada y con fechas de frontera.
6. Crear/editar pago con ID inexistente, paginación inválida y baja/reactivación.
7. Consistencia de asociaciones usuario–cliente y rollback de operaciones compuestas.

## 13. Mejoras priorizadas

### Etapa 1: seguridad e integridad

- Rotar y externalizar secretos; cerrar acceso anónimo a datos administrativos.
- Definir autorización por propietario y controlar usuarios deshabilitados.
- Corregir renovación, excepciones e invalidación de JWT.
- Evitar pérdida de contacto en edición de clientes.
- Corregir asociaciones, casos nulos y edición de pagos inexistentes.
- Agregar validación de entrada y transacciones en operaciones compuestas.

### Etapa 2: reproducibilidad y contratos

- Preparar datos de bootstrap y un entorno de pruebas aislado.
- Introducir migraciones del esquema, respaldos y restricciones de integridad.
- Ampliar las pruebas existentes a los flujos críticos de negocio y agregar una ejecución de CI.
- Homogeneizar respuestas JSON, estados HTTP y métodos por operación.
- Documentar el contrato con AngryBox antes de modificar rutas o nombres existentes.

### Etapa 3: evolución técnica

- Planificar una actualización gradual de Java, Spring Boot y dependencias con pruebas de regresión y revisión de compatibilidad.
- Unificar dependencias frontend, quitar llamadas síncronas y generar URLs desde el contexto.
- Separar responsabilidades dentro de `vm.home.js` y organizar backend por dominios cuando ayude al mantenimiento.
- Reemplazar dinero flotante por decimales y definir fechas y zona horaria explícitamente.
- Medir consultas, agregar paginación donde corresponda y mejorar logs operativos.

El código permite mejorar la separación de dominios dentro del monolito. No se encontró evidencia de carga, equipos o necesidades de despliegue que justifique por sí sola una separación en microservicios.

## 14. Archivos clave para continuar

- [Dependencias y build](pom.xml)
- [Configuración de seguridad](src/main/java/com/dysi/AngryBoxClientManager/config/WebSecurityConfig.java)
- [Filtro JWT](src/main/java/com/dysi/AngryBoxClientManager/security/JwtAuthenticationTokenFilter.java)
- [Generación y validación JWT](src/main/java/com/dysi/AngryBoxClientManager/security/JwtTokenUtil.java)
- [Autenticación HTTP](src/main/java/com/dysi/AngryBoxClientManager/restControllers/AuthenticationRestController.java)
- [Servicio de usuarios](src/main/java/com/dysi/AngryBoxClientManager/servicesImpl/UserServiceImpl.java)
- [Servicio de clientes](src/main/java/com/dysi/AngryBoxClientManager/servicesImpl/ClientServiceImpl.java)
- [Servicio de licencias](src/main/java/com/dysi/AngryBoxClientManager/servicesImpl/LicenseServiceImpl.java)
- [Servicio de pagos](src/main/java/com/dysi/AngryBoxClientManager/servicesImpl/PaymentServiceImpl.java)
- [Consultas y filtros](src/main/java/com/dysi/AngryBoxClientManager/repositories/ClientRepository.java)
- [Interfaz principal](src/main/resources/static/js/vm/vm.home.js)
- [Pruebas de contexto y login](src/test/java/com/dysi/AngryBoxClientManager/AngryBoxClientManagerApplicationTests.java)
- [Prueba de autenticación JWT](src/test/java/com/dysi/AngryBoxClientManager/security/Java11AuthenticationTests.java)

## 15. Validación del empaquetado

Verificado el 24 de octubre de 2026 con Microsoft OpenJDK 11.0.31 y Maven Wrapper 3.9.9:

- `mvnw.cmd -B clean verify`: **BUILD SUCCESS**, 47 fuentes principales compiladas y **3 pruebas aprobadas**, sin fallos ni errores.
- Generado `target/clientmanager-0.0.1-SNAPSHOT.jar`, con `Main-Class: org.springframework.boot.loader.JarLauncher` y la clase principal de la aplicación como `Start-Class`.
- Comprobada la inclusión de Tomcat embebido, la plantilla de login y sus recursos JavaScript; H2 queda fuera del JAR.
- Arranque real del artefacto mediante `JarLauncher`, con H2 añadido únicamente al classpath de verificación y variables temporales que sustituyen la configuración MySQL. Tomcat inició en una dirección local y un puerto libre; el login y su JavaScript respondieron **HTTP 200**. El proceso de comprobación se detuvo al terminar.

Para desplegar contra MySQL se usa directamente `java -jar clientmanager-0.0.1-SNAPSHOT.jar`, sin el classpath H2 empleado en la comprobación. No hace falta una instalación de Tomcat externa.

La compilación y las pruebas no acreditan el funcionamiento contra el MySQL existente. Los hallazgos pendientes de seguridad e integridad de este documento requieren su propio trabajo de corrección.
