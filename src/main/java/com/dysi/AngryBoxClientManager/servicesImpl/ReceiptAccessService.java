package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.models.Attachment;
import com.dysi.AngryBoxClientManager.models.PaymentAttachment;
import com.dysi.AngryBoxClientManager.storage.ReceiptStorage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ReceiptAccessService {
    private final EntityManager em;
    private final ReceiptStorage storage;
    private final PortalAccessService access;
    public ReceiptAccessService(EntityManager em, ReceiptStorage storage, PortalAccessService access) { this.em = em; this.storage = storage; this.access = access; }

    @PreAuthorize("hasRole('ADMIN')")
    public Content open(Long paymentId, Long attachmentId) {
        List<PaymentAttachment> links = em.createQuery("select pa from PaymentAttachment pa join fetch pa.attachment a "
                + "where pa.payment.id = :payment and a.id = :attachment and pa.removeDate is null and a.removeDate is null", PaymentAttachment.class)
                .setParameter("payment", paymentId).setParameter("attachment", attachmentId).setMaxResults(1).getResultList();
        if (links.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el comprobante.");
        return content(links.get(0).getAttachment());
    }

    @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public Content openOwn(com.dysi.AngryBoxClientManager.security.PortalPrincipal principal, Long paymentId, Long attachmentId) {
        access.requireSensitiveAccess(principal);
        Long client = access.revalidate(principal).getClientId();
        List<Attachment> files = em.createQuery("select a from PaymentAttachment pa join pa.attachment a "
                + "where pa.payment.id = :payment and pa.payment.client.id = :client and a.id = :attachment "
                + "and pa.payment.removeDate is null and pa.removeDate is null and a.removeDate is null", Attachment.class)
                .setParameter("payment", paymentId).setParameter("client", client).setParameter("attachment", attachmentId).setMaxResults(1).getResultList();
        if (files.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el comprobante.");
        return content(files.get(0));
    }

    private Content content(Attachment attachment) {
        String name = attachment.getOriginalName();
        if (attachment.getMimeType().equals("image/jpeg")) name = name.substring(0, name.lastIndexOf('.') + 1) + "jpg";
        try {
            return new Content(name, attachment.getMimeType(), attachment.getStoredSize(), storage.open(attachment.getStorageKey()));
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo abrir el comprobante. Contactá a un administrador.");
        }
    }

    public static final class Content {
        public final String name, mimeType;
        public final long size;
        public final InputStream stream;
        Content(String name, String mimeType, long size, InputStream stream) {
            this.name = name; this.mimeType = mimeType; this.size = size; this.stream = stream;
        }
    }
}
