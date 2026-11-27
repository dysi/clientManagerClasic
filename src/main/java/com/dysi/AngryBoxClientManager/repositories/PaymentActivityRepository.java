package com.dysi.AngryBoxClientManager.repositories;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Pagina ambas fuentes juntas antes de hidratar sus datos con JPA. */
@Repository
public class PaymentActivityRepository {
    private static final String SOURCES =
            "select 'COMENTARIO' as kind, c.id, c.occurred_at from payment_comment pc "
            + "join comment c on c.id = pc.comment_id where pc.payment_id = :id "
            + "and pc.removedate is null and c.removedate is null "
            + "union all select 'ESTADO' as kind, h.id, h.occurred_at "
            + "from payment_status_history h where h.payment_id = :id";
    private final NamedParameterJdbcTemplate jdbc;

    public PaymentActivityRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Page<Reference> findByPayment(Long paymentId, Pageable page) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("id", paymentId)
                .addValue("size", page.getPageSize()).addValue("offset", page.getOffset());
        Long total = jdbc.queryForObject("select count(*) from (" + SOURCES + ") activity", parameters, Long.class);
        List<Reference> content = jdbc.query("select kind, id from (" + SOURCES + ") activity "
                + "order by occurred_at desc, id desc, kind desc limit :size offset :offset", parameters,
                (row, index) -> new Reference(row.getString("kind"), row.getLong("id")));
        return new PageImpl<>(content, page, total == null ? 0 : total);
    }

    public static final class Reference {
        public final String kind;
        public final Long id;
        private Reference(String kind, Long id) { this.kind = kind; this.id = id; }
    }
}
