package com.dysi.AngryBoxClientManager.mail;

/** Variables de plantilla permitidas; no representan etiquetas HTML. */
public enum PaymentMailVariable {
    CLIENT_NAME("cliente-nombre", "Nombre y apellido del cliente"),
    CLIENT_NAME_ALIAS("nombre-cliente", "Alias de cliente-nombre"),
    CLIENT_ID("cliente-id", "Identificador interno del cliente"),
    PAYMENT_ID("pago-id", "Identificador del pago presentado"),
    AMOUNT("pago-monto", "Importe es-AR con dos decimales, sin símbolo monetario"),
    CURRENCY("pago-moneda", "Moneda del pago, ARS"),
    ISSUED_AT("pago-emision", "Fecha y hora local de emisión informada por el cliente"),
    RECEIPTS("pago-comprobantes", "Cantidad de comprobantes de la presentación inicial"),
    PENDING_URL("pagos-pendientes-url", "Listado administrativo de pendientes; requiere autenticación");

    private final String key;
    private final String description;
    PaymentMailVariable(String key, String description) { this.key = key; this.description = description; }
    public String getKey() { return key; }
    public String getDescription() { return description; }
}
