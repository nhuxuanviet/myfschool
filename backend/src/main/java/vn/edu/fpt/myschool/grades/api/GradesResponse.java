package vn.edu.fpt.myschool.grades.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.grades.domain.GradeAssessment;
import vn.edu.fpt.myschool.grades.domain.GradeTerm;
import vn.edu.fpt.myschool.grades.domain.SemesterGradeSubject;
import vn.edu.fpt.myschool.grades.domain.SemesterGrades;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

public record GradesResponse(
        String timeZone,
        Term selectedTerm,
        List<Term> availableTerms,
        List<Subject> subjects) {

    /** Public so the guardian view reports the same shape rather than a parallel one. */
    public static GradesResponse from(SemesterGrades semesterGrades) {
        return new GradesResponse(
                SchoolTimeZone.ZONE.getId(),
                Term.from(semesterGrades.selectedTerm()),
                semesterGrades.availableTerms().stream().map(Term::from).toList(),
                semesterGrades.subjects().stream().map(Subject::from).toList());
    }

    public record Term(
            UUID id,
            String academicYear,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn) {

        private static Term from(GradeTerm term) {
            return new Term(
                    term.id(),
                    term.academicYear(),
                    term.code(),
                    term.name(),
                    term.startsOn(),
                    term.endsOn());
        }
    }

    public record Subject(
            String code,
            String name,
            String assessmentMode,
            Integer annualLessonCount,
            int requiredRegularAssessments,
            BigDecimal termAverage,
            String termResult,
            List<Assessment> assessments) {

        private static Subject from(SemesterGradeSubject subject) {
            return new Subject(
                    subject.code(),
                    subject.name(),
                    subject.assessmentMode().name(),
                    subject.annualLessonCount(),
                    subject.requiredRegularAssessments(),
                    subject.termAverage(),
                    subject.termResult() == null ? null : subject.termResult().name(),
                    subject.assessments().stream().map(Assessment::from).toList());
        }
    }

    public record Assessment(
            String kind,
            String form,
            String displayLabel,
            Integer durationMinutes,
            String status,
            BigDecimal score,
            String outcome,
            LocalDate assessedOn) {

        private static Assessment from(GradeAssessment assessment) {
            return new Assessment(
                    assessment.kind().name(),
                    assessment.form().name(),
                    assessment.displayLabel(),
                    assessment.durationMinutes(),
                    assessment.status().name(),
                    assessment.score(),
                    assessment.outcome() == null ? null : assessment.outcome().name(),
                    assessment.assessedOn());
        }
    }
}
