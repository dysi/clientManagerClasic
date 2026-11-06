# Entrega de pagos aprobados a Angrybox

La integración entrega todos los Payment aprobados del padrón global a la única
empresa `angrybox.sync.source-company-id`; License es opcional y no determina el
destino. Requiere Bearer de usuario activo con `required-authority` (ROLE_API por
defecto), username coincidente, empresa autorizada y `angrybox.sync.enabled=true`.
No amplía permisos del portal ni permite aprobar pagos mediante la API de integración.

## Despliegue y datos

Ejecutar una vez, antes de iniciar la versión nueva y con backup verificado,
`src/main/resources/db/20261102-01-migracion-consolidada.sql` en MySQL representativo.
Crea la tabla auditada `angrybox_payment_delivery` y una entrega por aprobado histórico.
No cambia Payment, licencias ni ClientAccountBalance. No se ejecuta automáticamente.
Coordinar con el SQL y despliegue correspondiente de Angrybox.

Las nuevas aprobaciones insertan su entrega en la misma transacción que estado,
historial y acciones de licencia. Si falla la cola, se revierte la aprobación completa.
Los datos de auditoría de las nuevas entidades proceden del actor persistido autenticado.
La carga inicial no inventa actores históricos.

## Contrato v1

Base `/api/payments/sync`, query `username` y `companyId` obligatorios:

1. `POST /matching`: `{clientes:[{id,nombre,codigoExterno,cuitCuil}]}`. Reutiliza
   `AngryboxClientMatcher`, code-target y matching-mode de deuda. Admite clientes
   dados de baja para aplicar cobros previamente aprobados. Devuelve por Client
   `clientId`, `localId` o null, `identity`, nombre, código, DNI/CUIT y `errores`.
   Detecta varios clientes locales para el mismo remoto. No modifica clientes ni saldos.
2. `GET /pending?after=0&desde=yyyy-MM-dd`: hasta 50 pagos; respuesta
   `{versionContrato:1,until,next,hasMore,pagos:[...]}`. Para continuar, conservar
   until y usar after=next, avanzando también cuando una fila no pueda incorporarse.
   Cada pago contiene id, clientId, nombre, identity, hash, importe decimal, moneda,
   método, issuedAt local ISO y creationDate como instante ISO. Incluye opcionalmente
   `cuentaDestino: {banco,titular,cbu,alias}`, obtenida exclusivamente de los campos
   destinationBank/destinationHolder/destinationCbu/destinationAlias de Payment.
   No consulta los datos editables actuales de BankAccount. Si no hay datos de destino,
   el campo es null y Angrybox conserva el comportamiento anterior.
3. `POST /ack`: `{paymentId,movementId,hash}`. Después del commit local de Angrybox.
   Respuesta `{confirmed:true}`. La misma confirmación es idempotente; otra referencia
   o contenido causa 409. Persiste empresa, movimiento, fecha y auditoría de entrega.

`desde` se compara con **Payment.creationDate**, inclusive desde el inicio del día
en `portal.review.time-zone` (Argentina por defecto). No filtra issuedAt ni fecha
de aprobación. Ausente/vacía omite la comparación; con fecha, una creación desconocida
no cumple el filtro. La configuración del filtro vive en Angrybox y no descarta
pendientes de la cola: ampliar el rango permite recuperarlos.

El índice `(delivered_at,payment_id)` permite consultar pendientes y avanzar por
clave, sin reenviar el histórico entregado, OFFSET ni listas de IDs descargados.
until acota una ejecución. Las aprobaciones concurrentes de IDs anteriores al
cursor se recogen al comenzar otra ejecución desde after=0. No existe una fecha
global de avance que pueda perder los pagos rechazados localmente.

La huella comercial excluye adjuntos, comentarios e identidad editable del cliente:
esos cambios no invalidan la confirmación de un cobro existente. La huella identity
se compara antes de generar un cobro nuevo si el cliente cambió desde matching.

## Recuperación

La entrega no es una transacción distribuida. Angrybox conserva una clave única
empresa/Payment y el movimiento en la misma transacción del saldo; solo luego confirma.
Un ACK fallido deja el pago pendiente; el reintento confirma el movimiento existente.
Un ACK ya recibido deja de ofrecerse, aunque se haya perdido su respuesta HTTP.
No volver a poner una entrega confirmada en pendiente para corregir un cobro: primero
conciliar ambos sistemas. Una anulación comercial local no implica una nueva entrega.

Los pagos sin cliente local permanecen pendientes y Angrybox muestra sus errores.
La cola no crea cobros, clientes, saldos ni habilita QR: conserva las restricciones
actuales de presentación y revisión de Payment. Solo exporta los datos bancarios de
destino guardados en el pago para documentar el movimiento en Angrybox, sin adjuntos,
credenciales ni comentarios privados. La información adicional no cambia la huella
comercial ni invalida confirmaciones anteriores. No requiere cambios de esquema.

Verificación: `clean verify` con JDK 11, suite existente y comprobación manual aislada
H2/HTTP con el ApiClient/orquestador de Angrybox. Incluye rollback de aprobación por
fallo de cola, páginas, fecha mínima por creación, matching, confirmaciones repetidas,
interrupción antes de ACK y permisos. H2 no demuestra compatibilidad del SQL con MySQL;
validar migración y recepción completa en una copia representativa antes de producción.


### Consulta de pendientes de auditoría desde Angrybox

GET `/api/payments/sync/review-pending?username=...&companyId=...` usa el mismo
Bearer, permiso de integración y validación del origen que matching/pending/ack.
Responde `{"versionContrato":1,"pendientesAuditar":3}`. El conteo incluye
Payment con estado PENDIENTE y removeDate nulo, sin filtro de fecha de descarga.
Es una consulta readOnly agregada en base de datos: no genera entregas, aprobaciones,
confirmaciones, movimientos ni auditorías de escritura. No devuelve datos de clientes.

No confundir con `/pending`, que entrega pagos ya APROBADO para incorporar en
Angrybox. La pantalla Angrybox muestra círculo verde/Online cuando esta consulta
funciona, rojo/Offline ante errores y No disponible en lugar de cero ante fallos.
Con cantidad mayor que cero ofrece abrir la URL configurada en una nueva pestaña,
para que un administrador ingrese a revisar. Requiere actualizar ambas aplicaciones;
un ClientManager anterior sin esta ruta no podrá informar el estado en la barra.
