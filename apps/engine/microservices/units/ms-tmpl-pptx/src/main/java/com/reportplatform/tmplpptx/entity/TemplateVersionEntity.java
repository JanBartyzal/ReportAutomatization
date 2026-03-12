package com.reportplatform.tmplpptx.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "template_versions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"template_id", "version"}))
public class TemplateVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private PptxTemplateEntity template;

    @Column(nullable = false)
    private int version;

    @Column(name = "blob_url", nullable = false, length = 2048)
    private String blobUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TemplatePlaceholderEntity> placeholders = new ArrayList<>();

    protected TemplateVersionEntity() {}

    public TemplateVersionEntity(PptxTemplateEntity template, int version, String blobUrl,
                                  Long fileSizeBytes, String uploadedBy) {
        this.template = template;
        this.version = version;
        this.blobUrl = blobUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void onCreate() {
        this.uploadedAt = Instant.now();
    }

    // Getters and setters

    public UUID getId() { return id; }

    public PptxTemplateEntity getTemplate() { return template; }

    public int getVersion() { return version; }

    public String getBlobUrl() { return blobUrl; }

    public Long getFileSizeBytes() { return fileSizeBytes; }

    public String getUploadedBy() { return uploadedBy; }

    public Instant getUploadedAt() { return uploadedAt; }

    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }

    public List<TemplatePlaceholderEntity> getPlaceholders() { return placeholders; }
}
