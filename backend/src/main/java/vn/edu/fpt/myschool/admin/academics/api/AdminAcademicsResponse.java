package vn.edu.fpt.myschool.admin.academics.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.admin.academics.domain.AdminAcademics;

public record AdminAcademicsResponse(
        List<AcademicYearResponse> academicYears,
        List<AcademicTermResponse> terms,
        List<SubjectResponse> subjects,
        List<SchoolClassResponse> classes) {

    static AdminAcademicsResponse from(AdminAcademics.Catalog catalog) {
        return new AdminAcademicsResponse(
                catalog.academicYears().stream().map(AcademicYearResponse::from).toList(),
                catalog.terms().stream().map(AcademicTermResponse::from).toList(),
                catalog.subjects().stream().map(SubjectResponse::from).toList(),
                catalog.classes().stream().map(SchoolClassResponse::from).toList());
    }

    public record AcademicYearResponse(
            UUID id, String code, LocalDate startsOn, LocalDate endsOn, long version) {
        static AcademicYearResponse from(AdminAcademics.AcademicYear value) {
            return new AcademicYearResponse(
                    value.id(), value.code(), value.startsOn(), value.endsOn(), value.version());
        }
    }

    public record AcademicTermResponse(
            UUID id,
            UUID academicYearId,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn,
            long version) {
        static AcademicTermResponse from(AdminAcademics.AcademicTerm value) {
            return new AcademicTermResponse(
                    value.id(), value.academicYearId(), value.code(), value.name(),
                    value.startsOn(), value.endsOn(), value.version());
        }
    }

    public record SubjectResponse(
            UUID id, String code, String name, boolean enabled, long version) {
        static SubjectResponse from(AdminAcademics.Subject value) {
            return new SubjectResponse(
                    value.id(), value.code(), value.name(), value.enabled(), value.version());
        }
    }

    public record SchoolClassResponse(
            UUID id,
            UUID academicYearId,
            String code,
            String name,
            int gradeLevel,
            boolean enabled,
            long version,
            long studentCount) {
        static SchoolClassResponse from(AdminAcademics.SchoolClass value) {
            return new SchoolClassResponse(
                    value.id(), value.academicYearId(), value.code(), value.name(),
                    value.gradeLevel(), value.enabled(), value.version(), value.studentCount());
        }
    }
}
