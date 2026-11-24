# Acceso integrado al portal desde Angrybox

Angrybox puede abrir la interfaz del portal sin entregar al navegador las credenciales configuradas para la API. El frontend y las reglas del portal se reutilizan sin duplicar pantallas ni cálculos de licencia o pagos.

## Contrato

`POST /api/portal-access/tickets`, autenticado mediante `Authorization: Bearer …`, recibe `{"companyId":123}` y devuelve `{"ticket":"…"}`. No admite autenticación por sesión web. La identidad se obtiene del principal Bearer, nunca de un nombre de usuario enviado por el consumidor.

La cuenta debe estar activa y vinculada a un cliente activo, según `PortalAccessService`. El ID de empresa debe identificar una única licencia no anulada y visible de ese cliente. No se exige vigencia para poder consultar una cuenta con licencia vencida o bloqueada.

El servidor Angrybox redirige el navegador a `<contexto>/ui/#/portal-access?ticket=…`. La entrada elimina el fragmento sensible del historial y realiza `POST /web/session/portal-access` con JSON `{"ticket":"…"}` y el CSRF habitual. Este canje no requiere Turnstile porque exige el ticket obtenido tras la autenticación de API. No se deshabilita CSRF ni se modifica el login ordinario.

El ticket contiene 256 bits aleatorios, vence a los 60 segundos y se retira atómicamente antes de validar el canje. La cuenta, el cambio de contraseña y la pertenencia de la empresa se revalidan. La sesión resultante usa `PortalPrincipal` con origen `ACCESO_AUTENTICADO`, sin heredar roles de API, y conserva la rotación de identificador y CSRF del login habitual.

Una sesión administrativa o de otra identidad no se reemplaza: el canje responde 409 y consume el ticket. El usuario debe cerrar esa sesión y volver a abrir el acceso desde Angrybox. La sesión del portal se comparte entre pestañas y no depende del logout de Angrybox.

## Operación

- Publicar ClientManager antes que la integración de Angrybox y conservar la ruta del contrato.
- Usar HTTPS y una URL accesible por el servidor consumidor y por el navegador. No se exponen credenciales ni el JWT en HTML, redirecciones o almacenamiento del navegador. El ticket temporal sí es inspeccionable y debe tratarse como una autorización efímera.
- No registrar cuerpos de estas solicitudes/respuestas ni el ticket. Las respuestas usan `Cache-Control: no-store`; el fragmento no llega al servidor en la navegación HTTP.
- Los tickets se mantienen en memoria, con un máximo global de 1000 pendientes y limpieza de vencidos al emitir. Un reinicio invalida los pendientes. Varias instancias requieren afinidad de tráfico entre emisión y canje o un futuro almacén compartido con consumo atómico.
- No se agregan tablas, migraciones, parámetros ni permisos administrativos. Los cambios visuales posteriores del portal se publican únicamente en ClientManager.

## Comprobaciones

Sobre una base aislada, comprobar emisión con Bearer y empresa propia; rechazo sin Bearer, con sesión web, empresa ajena o licencia oculta; canje con CSRF y rotación de sesión; rechazo sin CSRF, con ticket inválido, vencido o reutilizado; cuenta deshabilitada o contraseña modificada entre emisión y canje; conflicto con otra sesión; y prohibición de rutas administrativas después del ingreso, incluso si el Bearer original pertenece a ADMIN.

La prueba completa entre productos debe verificar también `SISTEMA_FACTURACION`, el nonce de Angrybox, las redirecciones, la eliminación del ticket del historial y la apertura en una nueva pestaña desde escritorio y móvil.
