package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.dtos.PortalViews;
import com.dysi.AngryBoxClientManager.security.PortalPrincipal;
import com.dysi.AngryBoxClientManager.repositories.ClientRepository;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class LicenseExtensionService {
    private final EntityManager em;
    private final ObjectMapper mapper;
    private final long hours;
    private final boolean manualRequestEnabled;
    private final PortalAccessService access;
    private final ClientRepository clients;
    private final ZoneId portalZone;

    public LicenseExtensionService(EntityManager em, ObjectMapper mapper,
            @Value("${portal.license-extension.hours:72}") long hours,
            @Value("${portal.license-extension.manual-request-enabled:true}") boolean manualRequestEnabled,
            PortalAccessService access, ClientRepository clients,
            @Value("${portal.review.time-zone:America/Argentina/Buenos_Aires}") String portalZone) {
        if (hours < 1 || hours > 87600) throw new IllegalArgumentException("portal.license-extension.hours debe estar entre 1 y 87600.");
        this.em = em; this.mapper = mapper; this.hours = hours;
        this.manualRequestEnabled = manualRequestEnabled;
        this.access = access; this.clients = clients; this.portalZone = ZoneId.of(portalZone);
    }

    /** El llamador mantiene el bloqueo del cliente durante toda la presentación. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void issue(Payment payment) {
        Instant start = Instant.now();
        // Evaluar el historial anterior a esta presentación: el pago nuevo aún está pendiente.
        if (!evaluate(payment.getClient().getId(), start, payment.getId()).eligible) return;
        persist(payment.getClient(), payment, start);
    }

    private void persist(Client client, Payment payment, Instant start) {
        LicenseExtensionCertificate certificate = new LicenseExtensionCertificate();
        certificate.setClient(client); certificate.setPayment(payment);
        certificate.setOrigin(payment == null ? LicenseExtensionCertificate.Origin.CLIENT_REQUEST : LicenseExtensionCertificate.Origin.PAYMENT);
        certificate.setStartsAt(start); certificate.setExpiresAt(start.plus(hours, ChronoUnit.HOURS));
        em.persist(certificate);
    }

    public PortalViews.Extension overview(Long clientId) {
        PortalViews.Extension view = evaluate(clientId, Instant.now(), null);
        // La configuración solo afecta solicitudes manuales; las emisiones por pago comparten las reglas de negocio.
        if (!manualRequestEnabled && !view.active && view.expiredLicenses) {
            view.eligible = false;
            return deny(view, "MANUAL_REQUEST_DISABLED", "La solicitud de extensiones temporales no está disponible. Regularizá tu deuda e informá el pago desde Informar un nuevo pago.");
        }
        return view;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PortalViews.Extension request(PortalPrincipal principal) {
        Long clientId = access.revalidate(principal).getClientId();
        if (!manualRequestEnabled) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "La solicitud de extensiones temporales no está disponible.");
        Client client = clients.lockPortalClient(clientId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el cliente."));
        em.refresh(client);
        em.refresh(em.find(com.dysi.AngryBoxClientManager.models.User.class, principal.getId()));
        access.revalidate(principal);
        Instant now = Instant.now();
        PortalViews.Extension result = evaluate(clientId, now, null);
        if (result.active) return result;
        if (!result.eligible) throw new ResponseStatusException(HttpStatus.CONFLICT, result.message);
        persist(client, null, now);
        em.flush();
        return evaluate(clientId, now, null);
    }

    /** El certificado es por cliente; solo los estados sin bloqueo forzado pueden recibirlo. */
    private PortalViews.Extension evaluate(Long clientId, Instant now, Long excludedPaymentId) {
        PortalViews.Extension view = new PortalViews.Extension();
        view.durationDays = java.math.BigDecimal.valueOf(hours).divide(java.math.BigDecimal.valueOf(24), 4,
                java.math.RoundingMode.HALF_UP).stripTrailingZeros();
        LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();
        List<LicenseStatus> states = em.createQuery("select s from LicenseStatus s where s.license.client.id = :client "
                + "and s.license.removeDate is null and s.removeDate is null and s.id = "
                + "(select min(t.id) from LicenseStatus t where t.license = s.license and t.removeDate is null)", LicenseStatus.class)
                .setParameter("client", clientId).getResultList();
        boolean allBlocked = !states.isEmpty() && states.stream().allMatch(LicenseStatus::isForcedLock);
        LocalDate oldestExpiry = null;
        for (LicenseStatus state : states) {
            if (state.isForcedLock() || state.getLockDate() == null) continue;
            LocalDate expiry = state.getLockDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (!expiry.isAfter(today) && (oldestExpiry == null || expiry.isBefore(oldestExpiry))) oldestExpiry = expiry;
        }
        view.expiredLicenses = oldestExpiry != null;
        LicenseExtensionCertificate latest = latest(clientId);
        if (active(latest, now)) {
            view.active = true; view.origin = latest.getOrigin().name(); view.expiresAt = latest.getExpiresAt();
            view.expiresOn = latest.getExpiresAt().atZone(portalZone).toLocalDate();
            return view;
        }
        if (allBlocked) return deny(view, "FORCED_LOCK", "Tus licencias tienen un bloqueo administrativo. Una extensión temporal no puede habilitarlas. Contactanos para revisar tu situación.");
        if (oldestExpiry == null) return deny(view, "NO_EXPIRED_LICENSE", "No tenés licencias vencidas que puedan recibir una extensión temporal.");
        List<LicenseExtensionCertificate> recent = em.createQuery("select c from LicenseExtensionCertificate c "
                + "where c.client.id = :client and c.startsAt >= :since order by c.startsAt desc, c.id desc", LicenseExtensionCertificate.class)
                .setParameter("client", clientId).setParameter("since", now.minus(30, ChronoUnit.DAYS)).setMaxResults(1).getResultList();
        if (recent.isEmpty()) {
            // Cubre todas las vencidas elegibles; no habilitar una antigua por tener otra reciente.
            if (oldestExpiry.isBefore(today.minusDays(15))) return deny(view, "EXPIRY_TOO_OLD",
                    "Hay licencias vencidas hace más de 15 días. No podemos otorgar una extensión temporal. Regularizá tu deuda y contactanos para revisar la habilitación.");
        } else {
            LicenseExtensionCertificate previous = recent.get(0);
            String exclusion = excludedPaymentId == null ? "" : " and p.id <> :excluded";
            javax.persistence.TypedQuery<Payment> query = em.createQuery("select p from Payment p where p.client.id = :client "
                    + "and p.removeDate is null and p.creationDate > :since" + exclusion
                    + " order by p.creationDate desc, p.id desc", Payment.class)
                    .setParameter("client", clientId)
                    .setParameter("since", GregorianCalendar.from(previous.getStartsAt().atZone(ZoneOffset.UTC))).setMaxResults(1);
            if (excludedPaymentId != null) query.setParameter("excluded", excludedPaymentId);
            List<Payment> payments = query.getResultList();
            if (payments.isEmpty()) return deny(view, "NO_LATER_PAYMENT",
                    "La extensión anterior terminó y no hay un pago posterior informado. Regularizá tu deuda e informá el pago. Para solicitar otra extensión, ese pago debe estar aprobado.");
            PaymentStatus status = payments.get(0).getStatus();
            if (status == PaymentStatus.RECHAZADO) return deny(view, "PAYMENT_REJECTED",
                    "El último pago informado después de la extensión fue rechazado. Revisá el motivo en Mis pagos y regularizá tu deuda. Necesitás un pago posterior aprobado para solicitar otra extensión.");
            if (status != PaymentStatus.APROBADO) return deny(view, "PAYMENT_PENDING",
                    "El último pago informado después de la extensión todavía está pendiente de revisión. Podrás solicitar otra extensión cuando esté aprobado.");
        }
        view.eligible = true;
        return view;
    }

    private PortalViews.Extension deny(PortalViews.Extension view, String reason, String message) {
        view.reason = reason; view.message = message; return view;
    }

    private boolean active(LicenseExtensionCertificate certificate, Instant now) {
        return certificate != null && certificate.getRemoveDate() == null && !now.isBefore(certificate.getStartsAt())
                && now.isBefore(certificate.getExpiresAt());
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
        if (previous != null && previous.getRecoveredByPayment() == null
                && payment.getCreationDate().toInstant().isAfter(previous.getStartsAt())) previous.setRecoveredByPayment(payment);
    }

    private LicenseExtensionCertificate latest(Long clientId) {
        List<LicenseExtensionCertificate> certificates = em.createQuery("select c from LicenseExtensionCertificate c "
                + "where c.client.id = :id order by c.startsAt desc, c.id desc", LicenseExtensionCertificate.class)
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
        if (current == null) return response;
        // AngryBox consume el primer estado, también cuando tiene bloqueo forzado.
        com.fasterxml.jackson.databind.node.ArrayNode states = mapper.createArrayNode();
        response.path("licenseStatuses").forEach(state -> {
            if (state.path("id").asLong() == current.getId()) states.insert(0, state);
            else states.add(state);
        });
        response.set("licenseStatuses", states);
        if (current.isForcedLock() || current.getLockDate() == null) return response;
        LocalDate expiry = current.getLockDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (expiry.isAfter(LocalDate.now())) return response;
        LicenseExtensionCertificate certificate = latest(client.getId());
        Instant now = Instant.now();
        if (!active(certificate, now)) return response;
        ((ObjectNode) states.get(0)).put("lockDate", certificate.getExpiresAt().toEpochMilli());
        return response;
    }
}
