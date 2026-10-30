package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LicenseExtensionService {
    private final EntityManager em;
    private final ObjectMapper mapper;
    private final long hours;

    public LicenseExtensionService(EntityManager em, ObjectMapper mapper,
            @Value("${portal.license-extension.hours:72}") long hours) {
        if (hours < 1 || hours > 87600) throw new IllegalArgumentException("portal.license-extension.hours debe estar entre 1 y 87600.");
        this.em = em; this.mapper = mapper; this.hours = hours;
    }

    /** El llamador mantiene el bloqueo del cliente durante toda la presentación. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void issue(Payment payment) {
        Instant start = payment.getCreationDate().toInstant();
        LicenseExtensionCertificate previous = latest(payment.getClient().getId());
        if (previous != null) {
            if (previous.getRemoveDate() == null && previous.getExpiresAt().isAfter(start)) return;
            if (previous.getRemoveDate() != null && previous.getRecoveredByPayment() == null) return;
        }
        LicenseExtensionCertificate certificate = new LicenseExtensionCertificate();
        certificate.setClient(payment.getClient()); certificate.setPayment(payment);
        certificate.setStartsAt(start); certificate.setExpiresAt(start.plus(hours, ChronoUnit.HOURS));
        em.persist(certificate);
    }

    /** Rechazar solo anula el certificado originado por ese pago. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reject(Payment payment) {
        em.createQuery("select c from LicenseExtensionCertificate c where c.payment.id = :id", LicenseExtensionCertificate.class)
                .setParameter("id", payment.getId()).getResultList().forEach(LicenseExtensionCertificate::markRemoved);
    }

    /** Recupera futuras presentaciones sin reactivar ni extender el certificado anulado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void approve(Payment payment) {
        LicenseExtensionCertificate previous = latest(payment.getClient().getId());
        if (previous != null && previous.getRemoveDate() != null && previous.getRecoveredByPayment() == null
                && payment.getId() > previous.getPayment().getId()) previous.setRecoveredByPayment(payment);
    }

    private LicenseExtensionCertificate latest(Long clientId) {
        List<LicenseExtensionCertificate> certificates = em.createQuery("select c from LicenseExtensionCertificate c "
                + "where c.client.id = :id order by c.id desc", LicenseExtensionCertificate.class)
                .setParameter("id", clientId).setMaxResults(1).getResultList();
        return certificates.isEmpty() ? null : certificates.get(0);
    }

    /** Proyecta el vencimiento efectivo exclusivamente en el JSON de compatibilidad. */
    public ObjectNode licenseResponse(Client client, License license) {
        ObjectNode response = mapper.valueToTree(license);
        // El consumidor legado espera Long; la configuración web de Jackson usa fechas ISO.
        if (license.getLicenseStatuses() != null) {
            for (int i = 0; i < license.getLicenseStatuses().size(); i++) {
                Calendar date = license.getLicenseStatuses().get(i).getLockDate();
                if (date != null) ((ObjectNode) response.path("licenseStatuses").get(i)).put("lockDate", date.getTimeInMillis());
            }
        }
        if (client.getRemoveDate() != null) {
            response.path("licenseStatuses").forEach(state -> ((ObjectNode) state).put("forcedLock", true));
            return response;
        }
        if (license.getRemoveDate() != null || license.getLicenseStatuses() == null) return response;
        LicenseStatus current = license.getLicenseStatuses().stream().filter(s -> s.getRemoveDate() == null)
                .min(Comparator.comparing(LicenseStatus::getId)).orElse(null);
        if (current == null || current.isForcedLock() || current.getLockDate() == null) return response;
        LocalDate expiry = current.getLockDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (expiry.isAfter(LocalDate.now())) return response;
        LicenseExtensionCertificate certificate = latest(client.getId());
        Instant now = Instant.now();
        if (certificate == null || certificate.getRemoveDate() != null || now.isBefore(certificate.getStartsAt())
                || !now.isBefore(certificate.getExpiresAt())) return response;
        com.fasterxml.jackson.databind.node.ArrayNode states = mapper.createArrayNode();
        response.path("licenseStatuses").forEach(state -> {
            if (state.path("id").asLong() == current.getId()) {
                ((ObjectNode) state).put("lockDate", certificate.getExpiresAt().toEpochMilli());
                states.insert(0, state);
            } else {
                states.add(state);
            }
        });
        // AngryBox consume el primer estado; el vigente es el no eliminado de menor ID.
        response.set("licenseStatuses", states);
        return response;
    }
}
