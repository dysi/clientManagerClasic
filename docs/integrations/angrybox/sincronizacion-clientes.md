# Recepción de clientes y deuda de Angrybox

## Alcance

`POST /api/clients/sync` reemplaza la fotografía de
`ClientAccountBalance` en ARS: positivo es deuda, negativo es saldo a favor. Actualiza
`synchronizedAt` con la fecha de datos presentada en `portal.review.time-zone`.
No crea clientes, cambia su identidad, reactiva anulados, genera pagos o modifica licencias.
Opcionalmente propaga la baja lógica de Angrybox al Client vinculado.
La baja en Angrybox no borra una deuda: esas filas también se procesan. La ausencia
de un cliente en el envío no elimina ni pone a cero su saldo.

El padrón Client es global, sin separación por License.companyId. La recepción
requiere usuario activo con el authority configurado por Bearer y una única empresa contable de origen configurada.
Los roles ADMIN/USER/SELLER no conceden este permiso por sí solos. ROLE_API habilita este endpoint, sin conceder administración web ni ampliar los otros endpoints.
`companyId` es el identificador empresarial enviado por Angrybox, no Client.id ni
License.id. La cuenta de integración debe estar autorizada para ese padrón global.

## Configuración

```properties
angrybox.sync.enabled=false
angrybox.sync.sync-deactivations=false
angrybox.sync.required-authority=ROLE_API
angrybox.sync.source-company-id=
angrybox.sync.code-target=EXTERNAL_CODE
angrybox.sync.matching-mode=CODE_AND_DOCUMENT
angrybox.sync.max-future-seconds=300
```

Variables equivalentes: `CLIENTMANAGER_ANGRYBOX_SYNC_ENABLED`,
`CLIENTMANAGER_ANGRYBOX_SYNC_COMPANY_ID`, `CLIENTMANAGER_ANGRYBOX_SYNC_REQUIRED_AUTHORITY`, `CLIENTMANAGER_ANGRYBOX_SYNC_CODE_TARGET`,
`CLIENTMANAGER_ANGRYBOX_SYNC_MATCHING_MODE` y
`CLIENTMANAGER_ANGRYBOX_SYNC_MAX_FUTURE_SECONDS`.
Habilitar sin empresa positiva impide el arranque; deshabilitado devuelve 403.

`sync-deactivations` (variable `CLIENTMANAGER_ANGRYBOX_SYNC_DEACTIVATIONS`) está
desactivada por defecto, incluso si falta la propiedad. Con `true`, una fila con
`baja=true` y coincidencia válida actualiza la deuda y da de baja lógicamente al
Client en la misma transacción. Conserva fecha/actor de la primera baja si ya estaba
dado de baja y permite actualizar su deuda en posteriores envíos con `baja=true`.
Los errores de identidad, duplicados o saldo impiden tanto la baja como la actualización
de esa fila. No modifica usuarios, licencias ni pagos. `baja=false`, null o ausente
no reactiva al Client; un Client anulado con esos valores continúa devolviendo error.
Con la opción desactivada, se conserva el comportamiento anterior: no propaga bajas
y rechaza actualizar clientes anulados en ClientManager. Los reenvíos con igual UUID
recuperan el resultado original, incluso si luego cambia la configuración; para
aplicar la opción se debe generar una nueva sincronización con fotografía posterior.

| code-target | Comparación de codigoExterno enviado por Angrybox |
| --- | --- |
| EXTERNAL_CODE | Client.externalCode, quitando espacios de extremos, sensible a mayúsculas |
| ID | Client.id, entero positivo; admite ceros iniciales |

El `id` propio de la fila Angrybox solo relaciona solicitud y resultado, nunca se
interpreta como ID remoto.

| matching-mode | Elementos que deben coincidir |
| --- | --- |
| CODE | Código obligatorio y único; documento no condiciona la escritura. |
| DOCUMENT | Documento obligatorio y único; código no condiciona la escritura. |
| CODE_AND_DOCUMENT | Ambos obligatorios y únicos, apuntando al mismo Client. Valor inicial. |
| CODE_OR_DOCUMENT | Uno puede servir como alternativa si el otro falta o no tiene candidatos. No admite ambigüedad ni que ambos apunten a clientes distintos. |

En modos simples se usa solo el criterio configurado. Para contrastar los dos,
elegir CODE_AND_DOCUMENT. No se elige el primer duplicado ni se ignoran contradicciones
en el modo alternativo. Los clientes anulados también sirven como candidatos para
diagnosticar conflictos. Solo reciben una actualización de saldo si
`sync-deactivations=true` y la fila trae `baja=true`.

## Interpretación de DNI/CUIT

- Se interpreta `cuitCuil` por su contenido, sin equiparar IDs documentales entre sistemas.
- Solo se quitan espacios, puntos y guiones. Letras u otros caracteres invalidan el dato.
- De 1 a 8 dígitos: DNI, comparado con ocho posiciones completadas con ceros.
- 11 dígitos: CUIT/CUIL completo. Documentos completamente en cero no identifican.
- Si Client.cuil histórico contiene hasta ocho dígitos, se interpreta como DNI sin modificarlo.
- Para prefijos personales 20, 23, 24 y 27, un DNI puede contrastarse con el contenido
  del CUIT. No se extrae un supuesto DNI de CUIT de personas jurídicas.
- CUIT personal entrante puede coincidir con DNI remoto cuando no hay CUIT remoto.
  Si ya hay CUIT completo, tiene que coincidir completo: no alcanza compartir DNI.
  Un CUIT remoto mal formado y no vacío no se trata como ausente.
- Si DNI explícito y CUIT personal de la ficha remota contienen DNI distintos,
  se rechaza la identificación por documento y se indica qué corregir.

Se interpreta formato e identidad; no se valida el dígito verificador tributario ni
se consultan organismos externos. La consulta de login documental tiene otro contrato,
limita resultados y no exporta el padrón; no se modifica ni se reutiliza para este proceso.

## Contrato v1

Se conserva `POST /auth` y el token Bearer existente:

```http
POST /api/clients/sync?username=<usuario-del-token>&companyId=<empresa-configurada>
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "versionContrato": 1,
  "sincronizacionId": "bb695687-f8c4-42e1-9c5b-322ed31fc37c",
  "companyId": 9900,
  "fechaDatos": "2026-10-28T15:30:00-03:00",
  "clientes": [{
    "id": 37, "nombre": "Cliente de ejemplo", "codigoExterno": "EJEMPLO",
    "cuitCuil": "12.345.678", "deudaCuentaCorriente": 123.45, "baja": false
  }]
}
```

Se aceptan los demás campos descriptivos enviados por Angrybox, sin editar las
fichas remotas. El UUID, fecha con zona, empresa y lista son obligatorios.
Cada ID local debe ser positivo y único; si no, se rechaza todo con 400.
Lista vacía es válida y no borra saldos. La deuda es obligatoria por fila: hasta
12 dígitos enteros y dos decimales significativos, sin redondeos ni ceros inventados.

HTTP 200 incluye `versionContrato`, `sincronizacionId`, `clientesIn` y `clientesOut`.
Por cada entrada se devuelve id, nombre, código, documento, deuda, `sincronizo`
booleano y lista de `errores`. También `clientManagerId` y `criterioCoincidencia`
cuando se pudo resolver un destino; Angrybox puede ignorar estos campos adicionales.

Los errores identifican el dato entrante, los candidatos y qué revisar. Ejemplo:

> El código externo y el documento de Angrybox apuntan a clientes distintos.
> Ambos deben identificar al mismo Client; no se actualizó ninguna deuda para esta fila.
> Candidatos: ID 100 'Cliente Alfa', código 'EJEMPLO', DNI '87654321'; ID 200
> 'Cliente Beta', código 'OTRO', DNI '12345678'. Corregí el código/documento en
> Angrybox o editá la ficha incorrecta en ClientManager.

Los mensajes reales incluyen DNI, CUIT/CUIL y código/ID esperado según code-target.
Una ficha sin código indica que debe cargarse, sin inventar un valor. Se muestran
hasta cinco candidatos, indicando si hay más. Dos filas que resuelven el mismo
Client fallan juntas: nunca gana la última deuda. Una fotografía de saldo anulada
se rechaza sin restaurarla ni crear un saldo duplicado.

`clientesOut` contiene clientes remotos activos sin referencia en la información
enviada. Se excluyen también candidatos mencionados en conflictos para no
presentarlos como ausencias. Incluye ID remoto, nombre, correo, celular, DNI,
CUIT y código externo. Domicilio/observaciones son null porque Client no tiene
esos campos. No habilita altas automáticas. Ambas listas se devuelven aun vacías.

| HTTP | Resultado del lote |
| --- | --- |
| 200 | Confirmado; revisar errores por fila. |
| 400 | Contrato inválido, IDs duplicados, fecha futura fuera de tolerancia o empresa distinta entre URL y JSON. Sin procesamiento. |
| 401/403 | Autenticación, authority requerido, usuario activo, empresa/usuario o habilitación insuficientes. |
| 409 | UUID reutilizado con otros datos, fotografía no posterior o conflicto concurrente. |
| 503 | No se pudo confirmar; conservar UUID y JSON originales antes de reintentar. |

Los errores de lote usan `{"message":"..."}`. Angrybox muestra el mensaje público
de rechazos 400/401/403/409/413 como texto, sin registrar cuerpos remotos.

## Persistencia y concurrencia

Dos entidades nuevas extienden AuditableEntity y usan el actor persistido del JWT:

- AngryboxSyncState: cursor de fecha por empresa, con bloqueo pesimista para serializar lotes.
- AngryboxSyncBatch: empresa/UUID únicos, SHA-256 de datos deserializados, fecha,
  modo, code-target y respuesta JSON original. La respuesta tiene datos personales:
  es auditoría privada y debe incluirse en las políticas de backup/retención.

Saldos, cursor y respuesta se confirman juntos en READ_COMMITTED. Un fallo de
persistencia revierte todo. Los errores funcionales permiten resultados parciales;
los de infraestructura no se convierten en éxitos o rechazos ficticios por cliente.
Los destinos se bloquean por ID ascendente y se revalidan identificadores antes
de actualizar. No se mantienen llamadas HTTP salientes dentro de la transacción.

Repetir UUID y contenido devuelve la respuesta original aunque después cambien
fichas o configuración. Reutilizarlo con otro contenido devuelve 409. El orden de
filas forma parte de la huella; importes con ceros decimales equivalentes y fechas
equivalentes en otra zona representan el mismo contenido. Tras corregir una ficha,
crear una fotografía nueva: no se reprocesan rechazos del mismo UUID.

Una fecha menor o igual al último lote devuelve 409, salvo repetición idempotente.
El cursor avanza también si todas las filas tuvieron errores funcionales. Sincronizar
relojes y mantener zonas consistentes entre servidores. No cambiar el origen ni
borrar cursores para saltar protecciones: una migración de origen requiere revisar
fechas y saldos con Soporte.

## Despliegue y verificación

1. Aplicar primero en una copia MySQL compatible
   `src/main/resources/db/20261102-01-migracion-consolidada.sql`.
   Crea dos tablas y conserva saldos/clientes históricos; seguir sus precondiciones.
2. Desplegar con empresa y reglas configuradas. Conviene iniciar con CODE_AND_DOCUMENT.
3. Configurar en Angrybox una cuenta activa de integración con ROLE_API y habilitar
   ambas propiedades; su permiso 274 sigue siendo necesario. No repetir el SQL de
   Angrybox si ya se ejecutó.
   Angrybox reutiliza la conexión de licencias: esa cuenta también debe conservar
   sus vínculos cliente/licencia necesarios para las consultas existentes. No
   reemplazarla por un administrador sin esos vínculos, porque afectaría la licencia.
4. Ejecutar desde Clientes > Sincronizar ClientManager, corregir los conflictos en
   el sistema indicado y enviar una nueva fotografía.

El padrón se obtiene mediante proyección, sin relaciones de usuarios/licencias, y
se indexa para resolver todas las filas. Las actualizaciones y bloqueos son por
cliente resuelto. Medir con el volumen real: el envío sigue siendo síncrono y Angrybox
tiene 30 segundos de lectura. No se implementa cola ni envío paginado.

Se verifican manualmente HTTP real, JWT real y H2 aislada: modos, DNI/CUIT,
duplicados, mensajes correctivos, deuda/saldo a favor, bajas, clientesOut, permisos,
empresa/usuario, auditoría, idempotencia, fechas antiguas y reenvíos simultáneos.
Un CHECK temporal de la base de prueba provoca una falla para comprobar rollback
de saldos, respuesta y cursor. Se ejecutan las pruebas existentes y un build limpio;
no se agregan suites JUnit por defecto. H2 no demuestra compatibilidad SQL o
bloqueos MySQL: validar migración y concurrencia en una copia representativa.
También se verifica el transporte real `ClientManagerApiClient` de Angrybox contra
este receptor HTTP real, incluyendo autenticación, deserialización y su validador
del contrato, en una base H2 aislada.

## Asignación de acceso API

En Usuarios, seleccionar **Acceso API** y guardar desde una sesión administradora.
Si falta ROLE_API en la tabla authority, se crea al asignarlo usando el generador
de IDs existente y un bloqueo para serializar asignaciones concurrentes. No se
asigna a usuarios existentes automáticamente ni se necesitan nuevas tablas o columnas.
El permiso configurado se exige tanto en el controlador como en el servicio y
se revalida contra los authorities persistidos del usuario activo.

El valor predeterminado de required-authority es ROLE_API. Acepta nombres del enum
AuthorityName; una configuración desconocida impide el arranque. Elegir otro authority
es una decisión explícita que cambia quién puede usar este endpoint.

Para conservar las consultas de licencia existentes desde la misma conexión,
la cuenta puede combinar ROLE_USER y ROLE_API, con sus vínculos cliente/licencia.
Después de asignar o revocar el permiso, una nueva llamada Bearer consulta los
authorities actuales. No hace falta conceder ROLE_ADMIN para sincronizar.
