package vn.edu.fpt.myschool.home.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "academic_years")
class AcademicYearJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 16, unique = true)
    private String code;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AcademicYearJpaEntity() {
    }

    AcademicYearJpaEntity(
            UUID id,
            String code,
            LocalDate startsOn,
            LocalDate endsOn,
            Instant now) {
        this.id = id;
        this.code = code;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
