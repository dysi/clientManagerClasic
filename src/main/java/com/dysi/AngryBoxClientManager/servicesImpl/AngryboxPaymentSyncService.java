package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.config.AngryboxSyncProperties;
import com.dysi.AngryBoxClientManager.dtos.AngryboxClientSync.IncomingClient;
import com.dysi.AngryBoxClientManager.dtos.AngryboxPaymentSync.*;
import com.dysi.AngryBoxClientManager.dtos.ClientSyncIdentity;
import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.ClientRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import javax.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@PreAuthorize("hasAuthority(@angryboxSyncProperties.requiredAuthority.name())")
public class AngryboxPaymentSyncService {
    private final EntityManager em;
    private final AngryboxClientSyncService access;
    private final AngryboxSyncProperties settings;
    private final ClientRepository clients;
    private final ZoneId zone;

    public AngryboxPaymentSyncService(EntityManager em, AngryboxClientSyncService access,
            AngryboxSyncProperties settings, ClientRepository clients,
            @Value("${portal.review.time-zone:America/Argentina/Buenos_Aires}") String zone) {
        this.em = em;
        this.access = access;
        this.settings = settings;
        this.clients = clients;
        this.zone = ZoneId.of(zone);
    }

    /** Reutiliza exactamente el resolvedor de deuda, incluyendo conflictos y documentos. */
    @Transactional(readOnly = true)
    public List<Link> matching(String username, Long companyId, Matching input) {
        access.authorizeIntegration(username, companyId);
        List<ClientSyncIdentity> directory = clients.findSyncIdentities();
        AngryboxClientMatcher matcher = new AngryboxClientMatcher(directory, settings.getCodeTarget(), settings.getMatchingMode());
        Map<Long, Link> links = new LinkedHashMap<>();
        for (ClientSyncIdentity client : directory) {
            Link link = new Link();
            link.clientId = client.id;
            link.identity = identity(client.id, client.externalCode, client.dni, client.cuil);
            link.nombre = client.name();
            link.codigoExterno = client.externalCode;
            link.dni = client.dni;
            link.cuit = client.cuil;
            links.put(client.id, link);
        }
        Set<Long> localIds = new HashSet<>();
        Set<Long> ambiguous = new HashSet<>();
        for (IncomingClient local : input.clientes) {
            if (local == null || local.id == null || local.id <= 0 || !localIds.add(local.id)) {
                throw bad("El padrón local contiene IDs inválidos o repetidos.");
            }
            // Los pagos aprobados también se aplican a clientes dados de baja.
            AngryboxClientMatcher.Resolution match = matcher.resolve(local, true);
            for (Long related : match.relatedIds) {
                links.get(related).errores.addAll(match.errors);
            }
            if (match.target != null && match.errors.isEmpty()) {
                Link link = links.get(match.target.id);
                if (link.localId != null) {
                    ambiguous.add(link.clientId);
                } else {
                    link.localId = local.id;
                }
            }
        }
        for (Link link : links.values()) {
            if (ambiguous.contains(link.clientId)) {
                link.localId = null;
                link.errores.add("Varios clientes Angrybox identifican este Client. Corregí los códigos/documentos duplicados.");
            }
            if (link.localId == null && link.errores.isEmpty()) {
                link.errores.add("No se identificó un cliente local. Revisá código externo, DNI/CUIT y el criterio configurado en ClientManager.");
            }
            if (link.localId != null && !ambiguous.contains(link.clientId)) {
                link.errores.clear();
            }
        }
        return new ArrayList<>(links.values());
    }

    @Transactional(readOnly = true)
    public Page pending(String username, Long companyId, long after, Long until, String desde) {
        access.authorizeIntegration(username, companyId);
        if (after < 0 || until != null && until < after) {
            throw bad("El cursor de pagos es inválido.");
        }
        Long maximum = em.createQuery("select max(d.paymentId) from AngryboxPaymentDelivery d", Long.class).getSingleResult();
        long bound = until == null ? (maximum == null ? 0 : maximum) : until;
        Calendar minimum = null;
        if (desde != null && !desde.isBlank()) {
            try {
                minimum = GregorianCalendar.from(LocalDate.parse(desde).atStartOfDay(zone));
            } catch (RuntimeException error) {
                throw bad("La fecha mínima debe tener formato yyyy-MM-dd.");
            }
        }
        String filter = minimum == null ? "" : " and p.creationDate >= :minimum";
        TypedQuery<AngryboxPaymentDelivery> query = em.createQuery(
                "select d from AngryboxPaymentDelivery d join fetch d.payment p join fetch p.client "
                + "where d.deliveredAt is null and d.removeDate is null and d.paymentId > :after "
                + "and d.paymentId <= :until and p.status = :status and p.removeDate is null"
                + filter + " order by d.paymentId", AngryboxPaymentDelivery.class);
        query.setParameter("after", after).setParameter("until", bound).setParameter("status", PaymentStatus.APROBADO);
        if (minimum != null) {
            query.setParameter("minimum", minimum);
        }
        List<AngryboxPaymentDelivery> found = query.setMaxResults(51).getResultList();
        Page page = new Page();
        page.until = bound;
        page.next = after;
        page.hasMore = found.size() > 50;
        for (AngryboxPaymentDelivery delivery : found.subList(0, Math.min(50, found.size()))) {
            Payment payment = delivery.getPayment();
            Client client = payment.getClient();
            Row row = new Row();
            row.id = payment.getId();
            row.clientId = client.getId();
            row.nombre = ((client.getFirstname() == null ? "" : client.getFirstname()) + " "
                    + (client.getLastname() == null ? "" : client.getLastname())).trim();
            row.identity = identity(client.getId(), client.getExternalCode(), client.getDni(), client.getCuil());
            row.hash = paymentHash(payment);
            row.importe = payment.getAmount();
            row.moneda = payment.getCurrency().name();
            row.metodo = payment.getMethod().name();
            row.issuedAt = payment.getIssuedAt().toString();
            row.creationDate = payment.getCreationDate() == null ? null : payment.getCreationDate().toInstant().toString();
            row.cuentaDestino = cuentaDestino(payment);
            page.pagos.add(row);
            page.next = row.id;
        }
        return page;
    }

    /** Consulta liviana: PENDIENTE de auditoría no equivale a APROBADO pendiente de entrega. */
    @Transactional(readOnly = true)
    public PendingReview pendingReview(String username, Long companyId) {
        access.authorizeIntegration(username, companyId);
        PendingReview result = new PendingReview();
        result.pendientesAuditar = em.createQuery(
                "select count(p) from Payment p where p.status = :status and p.removeDate is null", Long.class)
                .setParameter("status", PaymentStatus.PENDIENTE).getSingleResult();
        return result;
    }

    public void acknowledge(String username, Long companyId, Ack input) {
        access.authorizeIntegration(username, companyId);
        AngryboxPaymentDelivery delivery = em.find(AngryboxPaymentDelivery.class, input.paymentId, LockModeType.PESSIMISTIC_WRITE);
        if (delivery == null) {
            throw bad("El pago no está disponible para entrega.");
        }
        if (delivery.getDeliveredAt() != null) {
            if (!Objects.equals(delivery.getCompanyId(), companyId)
                    || !Objects.equals(delivery.getMovementId(), input.movementId)
                    || !Objects.equals(delivery.getPayloadHash(), input.hash)) {
                throw conflict("El pago ya fue confirmado con otra referencia local. Revisá la conciliación.");
            }
            return;
        }
        Payment payment = delivery.getPayment();
        if (payment.getStatus() != PaymentStatus.APROBADO || payment.getRemoveDate() != null
                || !paymentHash(payment).equals(input.hash)) {
            throw conflict("El pago cambió respecto de la fotografía incorporada. Revisá la conciliación antes de confirmar.");
        }
        delivery.confirm(companyId, input.movementId, input.hash);
        em.flush();
    }

    private String identity(Long id, String code, String dni, String cuit) {
        return hashValues(id, code, dni, cuit);
    }

    /** Conservar la fotografia original del pago: no consultar la cuenta bancaria editable actual. */
    private CuentaDestino cuentaDestino(Payment payment) {
        if (AngryboxClientMatcher.text(payment.getDestinationBank()).isEmpty()
                && AngryboxClientMatcher.text(payment.getDestinationHolder()).isEmpty()
                && AngryboxClientMatcher.text(payment.getDestinationCbu()).isEmpty()
                && AngryboxClientMatcher.text(payment.getDestinationAlias()).isEmpty()) {
            return null;
        }
        CuentaDestino cuenta = new CuentaDestino();
        cuenta.banco = payment.getDestinationBank();
        cuenta.titular = payment.getDestinationHolder();
        cuenta.cbu = payment.getDestinationCbu();
        cuenta.alias = payment.getDestinationAlias();
        return cuenta;
    }

    private String paymentHash(Payment payment) {
        return hashValues(payment.getId(), payment.getClient().getId(),
                payment.getAmount().stripTrailingZeros().toPlainString(), payment.getCurrency(),
                payment.getMethod(), payment.getIssuedAt());
    }

    private String hashValues(Object... values) {
        try {
            StringBuilder canonical = new StringBuilder();
            for (Object item : values) {
                String value = item == null ? null : item.toString();
                canonical.append(value == null ? "-1:" : value.length() + ":" + value);
            }
            String value = canonical.toString();
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : bytes) {
                result.append(String.format("%02x", item & 255));
            }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
