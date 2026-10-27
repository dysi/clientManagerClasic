package com.dysi.AngryBoxClientManager.storage;

import com.dysi.AngryBoxClientManager.repositories.AttachmentRepository;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Recupera residuos tras caídas del proceso sin borrar archivos referenciados, incluso anulados. */
@Component
@EnableScheduling
public class ReceiptReconciliation {
    private static final Logger LOG = LoggerFactory.getLogger(ReceiptReconciliation.class);
    private final ReceiptStorage storage;
    private final AttachmentRepository attachments;
    private final PortalFileProperties limits;

    public ReceiptReconciliation(ReceiptStorage storage, AttachmentRepository attachments, PortalFileProperties limits) {
        this.storage = storage; this.attachments = attachments; this.limits = limits;
    }

    @Scheduled(initialDelayString = "${portal.files.reconcile-delay-ms:60000}", fixedDelayString = "${portal.files.reconcile-interval-ms:3600000}")
    public void reconcile() {
        Instant before = Instant.now().minusSeconds(limits.getOrphanGraceSeconds());
        try {
            for (String key : storage.staleKeys(true, before)) storage.discardPending(key);
            for (String key : storage.staleKeys(false, before)) {
                if (!attachments.existsByStorageKey(key)) storage.discard(key);
            }
        } catch (IOException | org.springframework.dao.DataAccessException error) {
            // Si la BD no está disponible, nunca inferir ausencia ni eliminar archivos finales.
            LOG.error("No se pudo completar la reconciliación de comprobantes privados.");
        }
    }
}
