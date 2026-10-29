package com.dysi.AngryBoxClientManager.mail;

import java.util.Properties;
import java.util.concurrent.*;
import javax.annotation.PreDestroy;
import javax.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Servicio reutilizable: cola FIFO en memoria, un intento por mensaje, sin persistencia. */
@Service
public class MailService {
    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);
    private final Environment config;
    private final ExecutorService queue = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1000), task -> {
                Thread thread = new Thread(task, "clientmanager-mail"); thread.setDaemon(true); return thread;
            });
    private long lastAttempt;
    public MailService(Environment config) { this.config = config; }
    public boolean enabled() { return config.getProperty("mail.enable", Boolean.class, false); }

    /** Encola sin esperar al SMTP; BCC preserva la privacidad de los destinatarios. */
    public void sendHtml(String recipients, String subject, String html) {
        if (!enabled()) return;
        try { queue.execute(() -> deliver(recipients, subject, html)); }
        catch (RejectedExecutionException error) { LOG.error("Correo descartado: cola llena o detenida; sin reintento."); }
    }

    private void deliver(String recipients, String subject, String html) {
        try {
            long interval = config.getProperty("mail.tiempo-min-entre-mensajes-seg", Long.class, 90L);
            if (interval < -1 || interval > 86400) throw new IllegalArgumentException("Intervalo inválido");
            if (lastAttempt != 0 && interval > 0) {
                long remaining = TimeUnit.SECONDS.toNanos(interval) - (System.nanoTime() - lastAttempt);
                if (remaining > 0) TimeUnit.NANOSECONDS.sleep(remaining);
            }
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(config.getRequiredProperty("mail.host"));
            sender.setPort(config.getProperty("mail.port", Integer.class, 587));
            sender.setUsername(config.getProperty("mail.username", ""));
            sender.setPassword(config.getProperty("mail.password", ""));
            Properties properties = sender.getJavaMailProperties();
            for (String name : new String[] { "transport.protocol", "smtp.auth", "smtp.ssl.enable",
                    "smtp.ssl.protocols", "smtp.ssl.trust", "smtp.starttls.enable", "smtp.starttls.required", "debug" }) {
                String value = config.getProperty("mail." + name);
                if (value != null && !value.isEmpty()) properties.setProperty("mail." + name, value);
            }
            properties.setProperty("mail.smtp.connectiontimeout", "10000");
            properties.setProperty("mail.smtp.timeout", "15000");
            properties.setProperty("mail.smtp.writetimeout", "15000");
            String test = config.getProperty("mail.to-test", "").trim();
            String target = test.isEmpty() ? recipients : test;
            InternetAddress[] addresses = InternetAddress.parse(target.replace(';', ','), true);
            if (addresses.length == 0) throw new IllegalArgumentException("Faltan destinatarios");
            for (InternetAddress address : addresses) address.validate();
            MimeMessageHelper message = new MimeMessageHelper(sender.createMimeMessage(), "UTF-8");
            message.setValidateAddresses(true);
            message.setFrom(config.getRequiredProperty("mail.from"));
            message.setBcc(addresses);
            message.setSubject(subject.replace('\r', ' ').replace('\n', ' '));
            message.setText(html, true);
            sender.send(message.getMimeMessage());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            LOG.error("Correo descartado por detención del servicio; sin reintento.");
        } catch (Exception error) {
            // Las excepciones SMTP pueden contener direcciones y datos sensibles: registrar solo el tipo.
            LOG.error("Falló el envío de correo ({}); sin reintento.", error.getClass().getSimpleName());
        } finally {
            // Esperar desde el fin del intento, también si SMTP falla o tarda en responder.
            lastAttempt = System.nanoTime();
        }
    }

    @PreDestroy
    public void close() {
        int discarded = queue.shutdownNow().size();
        if (discarded > 0) LOG.error("Se descartaron {} correos pendientes al detener el servicio; sin reintento.", discarded);
    }
}
