package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import javax.persistence.*;
import javax.validation.constraints.*;

/** Metadatos inmutables del archivo privado validado y, si es imagen, optimizado. */
@Entity
@Table(name = "ATTACHMENT", uniqueConstraints = @UniqueConstraint(name = "uk_attachment_storage", columnNames = "STORAGE_KEY"))
public class Attachment extends AuditableEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank @Column(name = "STORAGE_KEY", length = 255, nullable = false) @Size(max = 255)
    private String storageKey;

    @NotBlank @Column(name = "ORIGINAL_NAME", length = 255, nullable = false) @Size(max = 255)
    private String originalName;

    @NotBlank @Column(name = "MIME_TYPE", length = 100, nullable = false) @Size(max = 100)
    private String mimeType;

    @NotNull @Positive @Column(name = "STORED_SIZE", nullable = false)
    private Long storedSize;

    @Pattern(regexp = "[a-f0-9]{64}") @NotBlank @Column(name = "SHA256", length = 64, nullable = false) @Size(max = 64)
    private String sha256;

    @NotNull @Column(name = "UPLOADED_AT", nullable = false)
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UPLOADED_BY_ID", nullable = false) @JsonIgnore @NotNull
    private User uploadedBy;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "ORIGIN", length = 32, nullable = false)
    private OperationOrigin origin;

    @Positive @Column(name = "WIDTH")
    private Integer width;

    @Positive @Column(name = "HEIGHT")
    private Integer height;

    @Size(max = 1000) @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getId() { return id; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getStoredSize() { return storedSize; }
    public void setStoredSize(Long storedSize) { this.storedSize = storedSize; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public OperationOrigin getOrigin() { return origin; }
    public void setOrigin(OperationOrigin origin) { this.origin = origin; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    /** La evidencia se agrega; no se reemplaza ni borra de forma silenciosa. */
    @PreUpdate @PreRemove
    private void preserveEvidence() { throw new IllegalStateException("La evidencia es inmutable."); }
}
