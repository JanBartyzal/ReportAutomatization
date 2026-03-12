package com.reportplatform.tmplpptx.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "template_placeholders")
public class TemplatePlaceholderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private TemplateVersionEntity version;

    @Column(name = "placeholder_key", nullable = false)
    private String placeholderKey;

    @Column(name = "placeholder_type", nullable = false, length = 20)
    private String placeholderType;

    @Column(name = "slide_index")
    private Integer slideIndex;

    @Column(name = "shape_name")
    private String shapeName;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    protected TemplatePlaceholderEntity() {}

    public TemplatePlaceholderEntity(TemplateVersionEntity version, String placeholderKey,
                                     String placeholderType, Integer slideIndex, String shapeName) {
        this.version = version;
        this.placeholderKey = placeholderKey;
        this.placeholderType = placeholderType;
        this.slideIndex = slideIndex;
        this.shapeName = shapeName;
    }

    @PrePersist
    void onCreate() {
        this.detectedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public TemplateVersionEntity getVersion() { return version; }
    public String getPlaceholderKey() { return placeholderKey; }
    public String getPlaceholderType() { return placeholderType; }
    public Integer getSlideIndex() { return slideIndex; }
    public String getShapeName() { return shapeName; }
    public Instant getDetectedAt() { return detectedAt; }
}
