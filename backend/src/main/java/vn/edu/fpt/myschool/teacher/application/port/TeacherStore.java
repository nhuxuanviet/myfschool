package vn.edu.fpt.myschool.teacher.application.port;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.teacher.domain.TeacherWorkload;

/**
 * Reads a teacher's own workload.
 *
 * <p>Every method takes the teacher id resolved from the authenticated principal and narrows by
 * it in SQL. A caller-supplied class or subject id is only ever an extra filter on top of that,
 * never the thing that decides what is returned.
 */
public interface TeacherStore {

    Optional<UUID> findTeacherIdByUserId(UUID userId);

    List<TeacherWorkload.AssignedClass> findAssignedClasses(UUID teacherId, UUID academicTermId);

    List<TeacherWorkload.HomeroomClass> findHomeroomClasses(UUID teacherId);

    TeacherWorkload.TeachingWeek findTeachingWeek(UUID teacherId, LocalDate weekStart);

    /** True when this teacher holds an assignment covering the class in force for the term. */
    boolean isAssignedToClass(UUID teacherId, UUID classId, UUID academicTermId);

    boolean isHomeroomTeacherOf(UUID teacherId, UUID classId);

    List<TeacherWorkload.ClassStudent> findClassStudents(UUID classId);
}
