package vn.edu.fpt.myschool.grades.application.port;

import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.grades.domain.GradeSubjectEnrollment;
import vn.edu.fpt.myschool.grades.domain.GradeTerm;

/** Student-scoped read model for the semester-grade screen. */
public interface GradesStore {

    List<GradeTerm> findAvailableTerms(UUID studentId);

    List<GradeSubjectEnrollment> findSubjectEnrollments(UUID studentId, UUID academicTermId);
}
