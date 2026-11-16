package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.models.ClientAccountBalance;
import com.dysi.AngryBoxClientManager.models.PaymentStatus;
import com.dysi.AngryBoxClientManager.repositories.ClientAccountBalanceRepository;
import com.dysi.AngryBoxClientManager.repositories.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Situación informativa de la cuenta: no modifica saldo, pagos ni habilitación de licencia. */
@Service
@Transactional(readOnly = true)
public class LicenseAccountService {
    private final ClientAccountBalanceRepository balances;
    private final PaymentRepository payments;
    private final ZoneId zone;

    public LicenseAccountService(ClientAccountBalanceRepository balances, PaymentRepository payments,
            @Value("${portal.review.time-zone:America/Argentina/Buenos_Aires}") String zone) {
        this.balances = balances;
        this.payments = payments;
        this.zone = ZoneId.of(zone);
    }

    public void append(ObjectNode response, Long clientId) {
        ObjectNode account = response.putObject("estadoCuenta");
        ClientAccountBalance balance = balances.findByClientIdAndRemoveDateIsNull(clientId).orElse(null);
        long pending = payments.countByClientIdAndStatusAndRemoveDateIsNull(clientId, PaymentStatus.PENDIENTE);
        account.put("saldoDisponible", balance != null);
        account.put("pagosPendientes", pending);
        if (balance == null) {
            account.putNull("saldo");
            account.putNull("deuda");
            account.putNull("tieneDeuda");
            account.putNull("moneda");
            account.putNull("saldoActualizadoEn");
        } else {
            account.put("saldo", balance.getAmount());
            account.put("deuda", balance.getAmount().max(BigDecimal.ZERO));
            account.put("tieneDeuda", balance.getAmount().signum() > 0);
            account.put("moneda", balance.getCurrency().name());
            account.put("saldoActualizadoEn", balance.getSynchronizedAt().toString());
        }
        // La respuesta ya contiene la fecha efectiva y el primer estado resueltos por licenciamiento.
        JsonNode state = response.path("licenseStatuses").path(0);
        boolean forced = state.path("forcedLock").asBoolean(false);
        Long days = null;
        if (state.path("lockDate").isIntegralNumber()) {
            LocalDate expiry = Instant.ofEpochMilli(state.path("lockDate").asLong()).atZone(zone).toLocalDate();
            days = ChronoUnit.DAYS.between(LocalDate.now(zone), expiry);
        }
        account.put("bloqueoForzado", forced);
        if (days == null) {
            account.putNull("diasHastaBloqueo");
        } else {
            account.put("diasHastaBloqueo", days);
        }
        boolean debt = balance != null && balance.getAmount().signum() > 0;
        if (debt && pending > 0) {
            signal(account, "AZUL", "En proceso de revisión",
                    "Tenés deuda informada y un pago pendiente de aprobación. La presentación del pago no cancela el saldo ni habilita automáticamente la licencia."
                    + (forced ? " La licencia mantiene un bloqueo administrativo." : ""));
        } else if (forced) {
            signal(account, "ROJO", "Requiere revisión", "La licencia tiene un bloqueo administrativo. Contactanos para revisar tu situación.");
        } else if (balance == null || days == null) {
            signal(account, "AMARILLO", "Sin información completa",
                    "No disponemos de todos los datos de saldo y vigencia. Consultá el portal o contactanos para revisar tu cuenta.");
        } else if (debt) {
            signal(account, days <= 15 ? "ROJO" : "AMARILLO", "Requiere revisión",
                    days <= 15 ? "Tenés deuda informada y la licencia está vencida o vence dentro de los próximos 15 días. Revisá tu cuenta e informá el pago."
                            : "Tenés deuda informada. Revisá tu cuenta e informá el pago para regularizarla.");
        } else if (days <= 15) {
            signal(account, "AMARILLO", "Requiere revisión", "No tenés deuda informada, pero la licencia está vencida o vence dentro de los próximos 15 días. Revisá su vigencia.");
        } else {
            signal(account, "VERDE", "Al día", "No tenés deuda informada y tu licencia vence en más de 15 días.");
        }
        // Contrato reservado para avisos futuros; actualmente no se generan mensajes.
        response.putArray("mensajes");
    }

    private void signal(ObjectNode account, String color, String label, String message) {
        account.put("color", color);
        account.put("etiqueta", label);
        account.put("mensaje", message);
    }
}
