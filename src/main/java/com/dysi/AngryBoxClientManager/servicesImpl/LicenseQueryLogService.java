package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.LicenseQueryViews.*;
import com.dysi.AngryBoxClientManager.models.LicenseQueryLog;
import com.dysi.AngryBoxClientManager.repositories.LicenseQueryLogRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.persistence.*;
import javax.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class LicenseQueryLogService {
    private final LicenseQueryLogRepository logs;
    private final EntityManager em;
    private final int retentionDays;
    private final ZoneId zone;

    public LicenseQueryLogService(LicenseQueryLogRepository logs, EntityManager em,
            @Value("${license.query-log.retention-days:90}") int retentionDays,
            @Value("${license.query-log.time-zone:America/Argentina/Buenos_Aires}") String timeZone) {
        if (retentionDays < 1 || retentionDays > 36500) throw new IllegalArgumentException("license.query-log.retention-days debe estar entre 1 y 36500.");
        this.logs = logs; this.em = em; this.retentionDays = retentionDays; this.zone = ZoneId.of(timeZone);
    }

    /** Transacción independiente: una falla de telemetría no revierte ni altera la consulta original. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(LicenseQueryLog entry) { logs.saveAndFlush(entry); }

    public Page<Entry> entries(Filter filter, int page, int size) {
        if (page < 0 || !Arrays.asList(25, 50, 100).contains(size) || (long) page * size > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La paginación no es válida.");
        return logs.findAll(specification(filter), PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("receivedAt"), Sort.Order.desc("id")))).map(Entry::new);
    }

    public Summary summary(Filter filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<LicenseQueryLog> root = query.from(LicenseQueryLog.class);
        query.multiselect(cb.count(root), errors(cb, root), cb.countDistinct(root.get("remoteIp")),
                cb.countDistinct(root.get("installationId")), cb.least(root.<Instant>get("receivedAt")),
                cb.greatest(root.<Instant>get("receivedAt")));
        query.where(specification(filter).toPredicate(root, query, cb));
        Tuple result = em.createQuery(query).getSingleResult();
        Summary summary = new Summary();
        summary.total = result.get(0, Long.class); summary.errors = number(result, 1);
        summary.distinctIps = result.get(2, Long.class); summary.distinctInstallations = result.get(3, Long.class);
        summary.firstSeen = result.get(4, Instant.class); summary.lastSeen = result.get(5, Instant.class);
        summary.retentionDays = retentionDays; summary.timeZone = zone.getId();
        return summary;
    }

    /** Top 50 por actividad reciente; agrega y limita en base de datos, sin cargar la bitácora en memoria. */
    public List<Origin> origins(Filter filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<LicenseQueryLog> root = query.from(LicenseQueryLog.class);
        List<Expression<?>> keys = Arrays.asList(root.get("companyId"), root.get("remoteIp"),
                root.get("authenticatedUsername"), root.get("installationId"), root.get("hostname"));
        Expression<Instant> last = cb.greatest(root.<Instant>get("receivedAt"));
        query.multiselect(root.get("companyId"), root.get("remoteIp"), root.get("authenticatedUsername"),
                root.get("installationId"), root.get("hostname"), cb.count(root), errors(cb, root),
                cb.least(root.<Instant>get("receivedAt")), last);
        query.where(specification(filter).toPredicate(root, query, cb));
        query.groupBy(keys).orderBy(cb.desc(last), cb.asc(root.get("companyId")), cb.asc(root.get("remoteIp")));
        List<Origin> results = new ArrayList<>();
        for (Tuple row : em.createQuery(query).setMaxResults(50).getResultList()) {
            Origin origin = new Origin();
            origin.companyId = row.get(0, Long.class); origin.remoteIp = row.get(1, String.class);
            origin.authenticatedUsername = row.get(2, String.class); origin.installationId = row.get(3, String.class);
            origin.hostname = row.get(4, String.class); origin.queries = row.get(5, Long.class);
            origin.errors = number(row, 6); origin.firstSeen = row.get(7, Instant.class); origin.lastSeen = row.get(8, Instant.class);
            results.add(origin);
        }
        return results;
    }

    private static long number(Tuple tuple, int position) {
        Number value = tuple.get(position, Number.class);
        return value == null ? 0 : value.longValue();
    }

    private static Expression<Long> errors(CriteriaBuilder cb, Root<LicenseQueryLog> root) {
        return cb.sum(cb.<Long>selectCase().when(cb.ge(root.get("httpStatus"), 400), 1L).otherwise(0L));
    }

    private Specification<LicenseQueryLog> specification(Filter filter) {
        if (filter.from != null && filter.to != null && filter.from.isAfter(filter.to))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha desde debe ser anterior o igual a la fecha hasta.");
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.companyId != null) predicates.add(cb.equal(root.get("companyId"), filter.companyId));
            if (filter.licenseId != null) predicates.add(cb.equal(root.get("licenseId"), filter.licenseId));
            if (filter.ip != null && !filter.ip.trim().isEmpty()) predicates.add(cb.equal(root.get("remoteIp"), filter.ip.trim()));
            if (filter.from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), filter.from.atStartOfDay(zone).toInstant()));
            if (filter.to != null) predicates.add(cb.lessThan(root.get("receivedAt"), filter.to.plusDays(1).atStartOfDay(zone).toInstant()));
            if ("SUCCESS".equals(filter.result)) predicates.add(cb.between(root.get("httpStatus"), 200, 299));
            if ("ERROR".equals(filter.result)) predicates.add(cb.ge(root.get("httpStatus"), 400));
            if (!"ALL".equals(filter.version)) predicates.add(cb.equal(root.get("apiVersion"), filter.version));
            if (filter.keyword != null && !filter.keyword.trim().isEmpty()) {
                String term = filter.keyword.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                List<Predicate> matches = new ArrayList<>();
                for (String field : Arrays.asList("companyName", "authenticatedUsername", "requestedUsername", "remoteIp", "installationId", "hostname"))
                    matches.add(cb.like(cb.lower(root.get(field)), "%" + term + "%", '\\'));
                predicates.add(cb.or(matches.toArray(new Predicate[0])));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Borrado físico por retención, no anulación de negocio. Cada lote confirma por separado. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeBatch(Instant cutoff) {
        List<Long> ids = logs.expired(cutoff, PageRequest.of(0, 1000));
        if (!ids.isEmpty()) logs.deleteAllByIdInBatch(ids);
        return ids.size();
    }

    public Instant retentionCutoff() { return Instant.now().minus(retentionDays, ChronoUnit.DAYS); }
}
