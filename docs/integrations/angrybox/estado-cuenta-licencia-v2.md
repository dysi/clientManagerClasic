# Estado de cuenta en la consulta de licencia v2

`POST /api/v2/clients/license?username=...&companyId=...` conserva su autenticación Bearer, la validación del propietario y el cuerpo JSON de metadatos opcionales (puede ser `{}`). La v1 no cambia. La respuesta v2 conserva la licencia y sus estados efectivos y agrega:

```json
{
  "estadoCuenta": {
    "saldoDisponible": true,
    "saldo": 12500.50,
    "deuda": 12500.50,
    "tieneDeuda": true,
    "moneda": "ARS",
    "saldoActualizadoEn": "2026-11-16T09:30:00",
    "pagosPendientes": 1,
    "bloqueoForzado": false,
    "diasHastaBloqueo": 10,
    "color": "AZUL",
    "etiqueta": "En proceso de revisión",
    "mensaje": "Tenés deuda informada y un pago pendiente de aprobación. La presentación del pago no cancela el saldo ni habilita automáticamente la licencia."
  },
  "mensajes": []
}
```

`mensajes` es un array reservado para avisos futuros. Cada elemento tendrá `mensaje` (texto plano) y `nivel` (`info`, `warning` o `error`). Actualmente se devuelve siempre vacío: no se generan anuncios ni se infieren avisos de pagos rechazados.

## Fuentes y prioridades

La deuda proviene de la fotografía vigente de `ClientAccountBalance`, igual que en el portal: saldo positivo significa deuda y saldo negativo significa saldo a favor. No se crean saldos cero ni se restan pagos pendientes. Si no existe saldo, `saldo`, `deuda`, `tieneDeuda`, `moneda` y `saldoActualizadoEn` son nulos y `saldoDisponible` es falso.

Los pagos pendientes se cuentan para el mismo cliente, con estado `PENDIENTE` y sin baja lógica. No se cuentan pagos de otro cliente ni aprobados, rechazados o anulados. La consulta es agregada en base de datos y no carga el listado de pagos.

Se utiliza el primer estado y la fecha efectiva que ya resuelve `LicenseExtensionService`, incluidas sus extensiones temporales. La comparación por día usa `portal.review.time-zone`, cuya configuración ya existe; no se agrega una propiedad.

| Condición | Color |
| --- | --- |
| Deuda y pagos pendientes | AZUL, con prioridad |
| Bloqueo forzado, sin el caso anterior | ROJO |
| Saldo o fecha ausentes | AMARILLO, sin información completa |
| Deuda y fecha a 15 días o menos, incluidas vencidas | ROJO |
| Deuda y fecha a más de 15 días | AMARILLO |
| Sin deuda y fecha a 15 días o menos, incluidas vencidas | AMARILLO |
| Sin deuda y fecha a más de 15 días | VERDE |

La situación informativa no modifica bloqueos, extensiones, importes, aprobaciones ni saldo. El azul no garantiza la aprobación de un pago ni habilita la licencia; un bloqueo forzado se informa en el mensaje aun cuando el indicador sea azul.

## Despliegue y comprobación

Actualizar ClientManager antes que Angrybox. Angrybox actualizado utiliza exclusivamente la consulta v2; no reintenta con v1 ante un error. Versiones anteriores de Angrybox pueden continuar usando v1.

Comprobar cada fila de la matriz, especialmente 15 días exactos, fecha vencida, bloqueo forzado, saldo ausente y saldo a favor. Agregar pagos propios y ajenos en los distintos estados y verificar el conteo. Comparar v1 y v2, y rechazar un `username` distinto del principal. Las verificaciones con H2 aislada no sustituyen la validación sobre MySQL; este cambio no modifica el esquema.
