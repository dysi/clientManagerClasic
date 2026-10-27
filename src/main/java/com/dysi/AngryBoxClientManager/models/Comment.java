package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import javax.persistence.*;
import javax.validation.constraints.*;

/** Comentario reutilizable con autor y origen explícitos; su contenido es evidencia inmutable. */
@Entity
@Table(name = "COMMENT")
public class Comment extends AuditableEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank @Column(name = "TEXT", length = 4000, nullable = false) @Size(max = 4000)
    private String text;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "VISIBILITY", length = 32, nullable = false)
    private CommentVisibility visibility;

    @NotNull @Column(name = "OCCURRED_AT", nullable = false)
    private LocalDateTime occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AUTHOR_ID", nullable = false) @JsonIgnore @NotNull
    private User author;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "ORIGIN", length = 32, nullable = false)
    private OperationOrigin origin;

    public Long getId() { return id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public CommentVisibility getVisibility() { return visibility; }
    public void setVisibility(CommentVisibility visibility) { this.visibility = visibility; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public OperationOrigin getOrigin() { return origin; }
    public void setOrigin(OperationOrigin origin) { this.origin = origin; }

    @PrePersist
    private void validateVisibility() {
        if (origin == OperationOrigin.PORTAL_DOCUMENTO && visibility != CommentVisibility.PUBLICO)
            throw new IllegalStateException("El portal solo admite comentarios públicos.");
    }
    /** La evidencia se agrega; no se reemplaza ni borra de forma silenciosa. */
    @PreUpdate @PreRemove
    private void preserveEvidence() { throw new IllegalStateException("La evidencia es inmutable."); }
}
