package com.dysi.AngryBoxClientManager.mail;

import com.dysi.AngryBoxClientManager.models.Payment;
import com.dysi.AngryBoxClientManager.models.PaymentStatus;
import com.dysi.AngryBoxClientManager.repositories.PaymentRepository;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

/** Recordatorio por instancia: fotografía de pendientes, sin modificar pagos ni su auditoría. */
@Service
public class PendingPaymentReminder {
    private static final Logger LOG = LoggerFactory.getLogger(PendingPaymentReminder.class);
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([^{}]+)\\}\\}");
    private final PaymentRepository payments;
    private final Environment config;
    private final MailService mail;

    public PendingPaymentReminder(PaymentRepository payments, Environment config, MailService mail) {
        this.payments = payments; this.config = config; this.mail = mail;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${mail.builder.pending-payment-cron:0 0 14 * * *}",
            zone = "${mail.builder.pending-payment-zone:America/Argentina/Buenos_Aires}")
    @Transactional(readOnly = true)
    public synchronized void remind() {
        if (!mail.enabled() || !config.getProperty("mail.builder.pending-payment-active", Boolean.class, true)) return;
        try {
            // Limitar el detalle en la base: el total incluye todos los pendientes, sin cargar todo en memoria.
            Page<Payment> pending = payments.findPendingReminder(PaymentStatus.PENDIENTE, PageRequest.of(0, 50));
            if (pending.isEmpty()) return;
            NumberFormat amount = NumberFormat.getNumberInstance(new Locale("es", "AR"));
            amount.setMinimumFractionDigits(2); amount.setMaximumFractionDigits(2);
            StringBuilder rows = new StringBuilder();
            for (Payment payment : pending) {
                rows.append("<tr>");
                cell(rows, "#" + payment.getId());
                cell(rows, (Objects.toString(payment.getClient().getFirstname(), "") + " "
                        + Objects.toString(payment.getClient().getLastname(), "")).trim()
                        + " (ID " + payment.getClient().getId() + ")");
                cell(rows, payment.getCurrency() + " $ " + amount.format(payment.getAmount()));
                cell(rows, payment.getIssuedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                rows.append("</tr>");
            }
            Map<String, String> values = new LinkedHashMap<>();
            values.put("pagos-cantidad", Long.toString(pending.getTotalElements()));
            values.put("pagos-mostrados", Integer.toString(pending.getNumberOfElements()));
            values.put("pagos-pendientes-url", PaymentMailAlert.pendingUrl(config));
            values.put("pagos-resumen", rows.toString());
            String subject = render(config.getRequiredProperty("mail.builder.pending-payment-asunto"), values, false);
            String body = render(config.getRequiredProperty("mail.builder.pending-payment-body"), values, true);
            mail.sendHtml(config.getRequiredProperty("mail.builder.alert-payment-to"), subject, body);
        } catch (Exception error) {
            LOG.error("No se pudo preparar el recordatorio de pagos pendientes ({}); sin reintento.",
                    error.getClass().getSimpleName());
        }
    }

    private static void cell(StringBuilder rows, String value) {
        rows.append("<td style=\"padding:12px;border-bottom:1px solid #e2e8f0\">")
                .append(HtmlUtils.htmlEscape(value, "UTF-8")).append("</td>");
    }

    private static String render(String template, Map<String, String> values, boolean html) {
        Matcher matcher = VARIABLE.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values.get(key);
            if (value == null || (!html && "pagos-resumen".equals(key)))
                throw new IllegalArgumentException("Variable de recordatorio inválida");
            // Única excepción al escape: filas generadas aquí con celdas ya escapadas, nunca HTML del cliente.
            if (html && !"pagos-resumen".equals(key)) value = HtmlUtils.htmlEscape(value, "UTF-8");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
