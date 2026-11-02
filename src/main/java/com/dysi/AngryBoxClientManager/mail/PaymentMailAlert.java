package com.dysi.AngryBoxClientManager.mail;

import com.dysi.AngryBoxClientManager.models.Payment;
import java.net.URI;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

/** Fotografía de datos dentro de la transacción; envío solo después de confirmar el alta. */
@Service
public class PaymentMailAlert {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentMailAlert.class);
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([^{}]+)\\}\\}");
    private final Environment config;
    private final MailService mail;
    public PaymentMailAlert(Environment config, MailService mail) { this.config = config; this.mail = mail; }

    public void afterCreation(Payment payment, int receiptCount) {
        try {
            if (!mail.enabled() || !config.getProperty("mail.builder.alert-payment-active", Boolean.class, false)) return;
            NumberFormat amount = NumberFormat.getNumberInstance(new Locale("es", "AR"));
            amount.setMinimumFractionDigits(2); amount.setMaximumFractionDigits(2);
            Map<PaymentMailVariable, String> values = new EnumMap<>(PaymentMailVariable.class);
            String name = (Objects.toString(payment.getClient().getFirstname(), "") + " "
                    + Objects.toString(payment.getClient().getLastname(), "")).trim();
            values.put(PaymentMailVariable.CLIENT_NAME, name);
            values.put(PaymentMailVariable.CLIENT_NAME_ALIAS, name);
            values.put(PaymentMailVariable.CLIENT_ID, payment.getClient().getId().toString());
            values.put(PaymentMailVariable.PAYMENT_ID, payment.getId().toString());
            values.put(PaymentMailVariable.AMOUNT, amount.format(payment.getAmount()));
            values.put(PaymentMailVariable.CURRENCY, payment.getCurrency().toString());
            values.put(PaymentMailVariable.ISSUED_AT, payment.getIssuedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            values.put(PaymentMailVariable.RECEIPTS, Integer.toString(receiptCount));
            values.put(PaymentMailVariable.PENDING_URL, pendingUrl(config));
            String subject = render(config.getRequiredProperty("mail.builder.alert-payment-asunto"), values, false);
            String body = render(config.getRequiredProperty("mail.builder.alert-payment-body"), values, true);
            String recipients = config.getRequiredProperty("mail.builder.alert-payment-to");
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { mail.sendHtml(recipients, subject, body); }
            });
        } catch (Exception error) {
            LOG.error("No se pudo preparar la alerta del pago {} ({}); sin reintento.", payment.getId(), error.getClass().getSimpleName());
        }
    }

    static String pendingUrl(Environment config) {
        String base = config.getRequiredProperty("clientmanager.public-url").trim();
        URI uri = URI.create(base);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL pública inválida");
        }
        return base.replaceAll("/+$", "") + "/ui/#/payments?status=PENDIENTE";
    }

    private String render(String template, Map<PaymentMailVariable, String> values, boolean html) {
        Matcher matcher = VARIABLE.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            PaymentMailVariable variable = Arrays.stream(PaymentMailVariable.values())
                    .filter(item -> item.getKey().equals(matcher.group(1))).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Variable de correo desconocida"));
            String value = values.get(variable);
            matcher.appendReplacement(result, Matcher.quoteReplacement(html ? HtmlUtils.htmlEscape(value, "UTF-8") : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
