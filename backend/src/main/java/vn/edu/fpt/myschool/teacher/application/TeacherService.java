package vn.edu.fpt.myschool.teacher.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;
import vn.edu.fpt.myschool.teacher.application.port.TeacherStore;
import vn.edu.fpt.myschool.teacher.domain.TeacherWorkload;

/**
 * A teacher's view of their own work.
 *
 * <p>The teacher is always resolved from the authenticated user id. A class id from the client is
 * never trusted: it is checked against the assignment records before anything is read with it.
 */
@Service
public class TeacherService {

    private final TeacherStore store;
    private final Clock clock;

    public TeacherService(TeacherStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UUID requireTeacherId(UUID userId) {
        return store.findTeacherIdByUserId(userId)
                .orElseThrow(TeacherException::noTeacherProfile);
    }

    @Transactional(readOnly = true)
    public List<TeacherWorkload.AssignedClass> getAssignedClasses(UUID userId, UUID academicTermId) {
        return store.findAssignedClasses(requireTeacherId(userId), academicTermId);
    }

    @Transactional(readOnly = true)
    public List<TeacherWorkload.HomeroomClass> getHomeroomClasses(UUID userId) {
        return store.findHomeroomClasses(requireTeacherId(userId));
    }

    @Transactional(readOnly = true)
    public TeacherWorkload.TeachingWeek getTeachingWeek(UUID userId, LocalDate requestedWeekStart) {
        LocalDate weekStart = normalizeToMonday(requestedWeekStart);
        return store.findTeachingWeek(requireTeacherId(userId), weekStart);
    }

    /**
     * Lists the students of a class, but only for a teacher who is responsible for it.
     *
     * <p>Being assigned to any subject in the class is enough to see who is in it; so is being
     * its homeroom teacher. Anything else is refused, whatever the class id says.
     */
    @Transactional(readOnly = true)
    public List<TeacherWorkload.ClassStudent> getClassStudents(
            UUID userId, UUID classId, UUID academicTermId) {
        UUID teacherId = requireTeacherId(userId);
        boolean allowed = store.isAssignedToClass(teacherId, classId, academicTermId)
                || store.isHomeroomTeacherOf(teacherId, classId);
        if (!allowed) {
            throw TeacherException.notAssigned();
        }
        return store.findClassStudents(classId);
    }

    private LocalDate normalizeToMonday(LocalDate requested) {
        LocalDate date = requested != null
                ? requested
                : LocalDate.ofInstant(clock.instant(), SchoolTimeZone.ZONE);
        return date.with(DayOfWeek.MONDAY);
    }
}
