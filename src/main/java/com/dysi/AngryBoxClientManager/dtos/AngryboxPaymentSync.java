package com.dysi.AngryBoxClientManager.dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;

/** Contrato de entrega v1; solo incluye el destino bancario guardado en el pago, sin adjuntos ni comentarios privados. */
public final class AngryboxPaymentSync {
    /** Solo informa pendientes de revisión administrativa, sin entregar ni confirmar pagos. */
    public static class PendingReview {
        public int versionContrato = 1;
        public long pendientesAuditar;
    }

    public static class Matching {
        @NotNull
        public List<AngryboxClientSync.IncomingClient> clientes;
    }

    public static class Link {
        public Long clientId;
        public Long localId;
        public String identity;
        public String nombre;
        public String codigoExterno;
        public String dni;
        public String cuit;
        public List<String> errores = new ArrayList<>();
    }

    public static class Page {
        public int versionContrato = 1;
        public long until;
        public long next;
        public boolean hasMore;
        public List<Row> pagos = new ArrayList<>();
    }

    public static class Row {
        public Long id;
        public Long clientId;
        public String nombre;
        public String identity;
        public String hash;
        public BigDecimal importe;
        public String moneda;
        public String metodo;
        public String issuedAt;
        public String creationDate;
        public CuentaDestino cuentaDestino;
    }

    public static class CuentaDestino {
        public String banco;
        public String titular;
        public String cbu;
        public String alias;
    }

    public static class Ack {
        @NotNull @Positive
        public Long paymentId;
        @NotNull @Positive
        public Long movementId;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}")
        public String hash;
    }
}
