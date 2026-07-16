package vn.edu.fpt.myschool.home.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import vn.edu.fpt.myschool.home.domain.AnnouncementAudience;

@Entity
@Table(name = "announcements")
class AnnouncementJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AnnouncementAudience audience;

    @Column(name = "audience_grade_level")
    private Short audienceGradeLevel;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "visible_from", nullable = false)
    private Instant visibleFrom;

    @Column(name = "visible_until")
    private Instant visibleUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AnnouncementJpaEntity() {
    }

    AnnouncementJpaEntity(
            UUID id,
            String title,
            String body,
            AnnouncementAudience audience,
            Integer audienceGradeLevel,
            Instant publishedAt,
            Instant visibleFrom,
            Instant visibleUntil,
            Instant now) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.audience = audience;
        this.audienceGradeLevel = audienceGradeLevel == null
                ? null
                : audienceGradeLevel.shortValue();
        this.publishedAt = publishedAt;
        this.visibleFrom = visibleFrom;
        this.visibleUntil = visibleUntil;
        this.createdAt = now;
        this.updatedAt = now;
    }

    UUID getId() {
        return id;
    }
}
