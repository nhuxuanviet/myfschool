package vn.edu.fpt.myschool.home.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AcademicYearJpaRepository extends JpaRepository<AcademicYearJpaEntity, UUID> {
}
