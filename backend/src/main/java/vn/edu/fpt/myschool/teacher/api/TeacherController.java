package vn.edu.fpt.myschool.teacher.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.teacher.application.TeacherService;
import vn.edu.fpt.myschool.teacher.domain.TeacherWorkload;

@RestController
@RequestMapping("/api/v1/teacher")
@Tag(name = "Teacher", description = "A teacher's own classes and teaching schedule")
@SecurityRequirement(name = "bearerAuth")
public class TeacherController {

    private final TeacherService service;

    public TeacherController(TeacherService service) {
        this.service = service;
    }

    public record AssignedClassResponse(
            UUID assignmentId,
            UUID classId,
            String classCode,
            int gradeLevel,
            UUID subjectId,
            String subjectCode,
            String subjectName,
            UUID academicTermId,
            String academicTermCode,
            int studentCount,
            boolean homeroom) {

        static AssignedClassResponse from(TeacherWorkload.AssignedClass item) {
            return new AssignedClassResponse(
                    item.assignmentId(), item.classId(), item.classCode(), item.gradeLevel(),
                    item.subjectId(), item.subjectCode(), item.subjectName(),
                    item.academicTermId(), item.academicTermCode(), item.studentCount(),
                    item.homeroom());
        }
    }

    public record HomeroomClassResponse(
            UUID classId, String classCode, int gradeLevel, UUID academicYearId, int studentCount) {

        static HomeroomClassResponse from(TeacherWorkload.HomeroomClass item) {
            return new HomeroomClassResponse(
                    item.classId(), item.classCode(), item.gradeLevel(),
                    item.academicYearId(), item.studentCount());
        }
    }

    public record ScheduledLessonResponse(
            LocalDate lessonDate,
            String session,
            int periodNumber,
            LocalTime startTime,
            LocalTime endTime,
            String classCode,
            String subjectCode,
            String subjectName,
            String room,
            String status) {

        static ScheduledLessonResponse from(TeacherWorkload.ScheduledLesson item) {
            return new ScheduledLessonResponse(
                    item.lessonDate(), item.session(), item.periodNumber(), item.startTime(),
                    item.endTime(), item.classCode(), item.subjectCode(), item.subjectName(),
                    item.room(), item.status());
        }
    }

    public record TeachingWeekResponse(LocalDate weekStart, List<ScheduledLessonResponse> lessons) {
    }

    public record ClassStudentResponse(
            UUID studentId, String studentCode, String fullName, int gradeLevel) {

        static ClassStudentResponse from(TeacherWorkload.ClassStudent item) {
            return new ClassStudentResponse(
                    item.studentId(), item.studentCode(), item.fullName(), item.gradeLevel());
        }
    }

    @GetMapping("/classes")
    public List<AssignedClassResponse> getClasses(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID academicTermId) {
        return service.getAssignedClasses(userId(jwt), academicTermId).stream()
                .map(AssignedClassResponse::from)
                .toList();
    }

    /** Empty for a teacher who is not a homeroom teacher; the client hides the section then. */
    @GetMapping("/homerooms")
    public List<HomeroomClassResponse> getHomerooms(@AuthenticationPrincipal Jwt jwt) {
        return service.getHomeroomClasses(userId(jwt)).stream()
                .map(HomeroomClassResponse::from)
                .toList();
    }

    @GetMapping("/schedule")
    public TeachingWeekResponse getSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        TeacherWorkload.TeachingWeek week = service.getTeachingWeek(userId(jwt), weekStart);
        return new TeachingWeekResponse(
                week.weekStart(),
                week.lessons().stream().map(ScheduledLessonResponse::from).toList());
    }

    @GetMapping("/classes/{classId}/students")
    public List<ClassStudentResponse> getClassStudents(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID classId,
            @RequestParam(required = false) UUID academicTermId) {
        return service.getClassStudents(userId(jwt), classId, academicTermId).stream()
                .map(ClassStudentResponse::from)
                .toList();
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
