package vn.edu.fpt.myschool.home.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "academic_terms")
class AcademicTermJpaEntity {

    @Id
    private UUID id;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(nullable = false, length = 16)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AcademicTermJpaEntity() {
    }

    AcademicTermJpaEntity(
            UUID id,
            UUID academicYearId,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn,
            Instant now) {
        this.id = id;
        this.academicYearId = academicYearId;
        this.code = code;
        this.name = name;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
