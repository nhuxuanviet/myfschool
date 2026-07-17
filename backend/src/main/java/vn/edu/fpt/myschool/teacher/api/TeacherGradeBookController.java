package vn.edu.fpt.myschool.teacher.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.teacher.application.GradeBookService;
import vn.edu.fpt.myschool.teacher.domain.GradeBookView;

@RestController
@RequestMapping("/api/v1/teacher/gradebooks")
@Tag(name = "Teacher grade book", description = "Entering and publishing marks for an assigned class")
@SecurityRequirement(name = "bearerAuth")
public class TeacherGradeBookController {

    private final GradeBookService service;

    public TeacherGradeBookController(GradeBookService service) {
        this.service = service;
    }

    public record OpenBookRequest(
            @NotNull UUID classId,
            @NotNull UUID subjectId,
            @NotNull UUID academicTermId) {
    }

    public record AddColumnRequest(
            @NotBlank @Size(max = 16) String assessmentKind,
            @NotBlank @Size(max = 16) String assessmentForm,
            @NotBlank @Size(max = 120) String displayLabel,
            Integer durationMinutes) {
    }

    /** Exactly one of score or outcome, matching the subject's assessment mode. */
    public record RecordMarkRequest(
            @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal score,
            @Size(max = 16) String outcome) {
    }

    public record VersionedRequest(@NotNull @PositiveOrZero Long version) {
    }

    public record BookResponse(
            UUID id,
            UUID classId,
            String classCode,
            UUID subjectId,
            String subjectCode,
            String subjectName,
            UUID academicTermId,
            String academicTermCode,
            Instant publishedAt,
            Instant lockedAt,
            boolean published,
            boolean locked,
            long version) {

        static BookResponse from(GradeBookView.Book book) {
            return new BookResponse(
                    book.id(), book.classId(), book.classCode(), book.subjectId(),
                    book.subjectCode(), book.subjectName(), book.academicTermId(),
                    book.academicTermCode(), book.publishedAt(), book.lockedAt(),
                    book.isPublished(), book.isLocked(), book.version());
        }
    }

    public record SheetResponse(
            BookResponse book,
            List<GradeBookView.Column> columns,
            List<TeacherController.ClassStudentResponse> students,
            List<GradeBookView.Mark> marks) {
    }

    public record CreatedResponse(UUID id) {
    }

    @PostMapping
    public BookResponse openBook(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OpenBookRequest request) {
        return BookResponse.from(service.openBook(
                userId(jwt), request.classId(), request.subjectId(), request.academicTermId()));
    }

    @GetMapping("/{bookId}")
    public SheetResponse getSheet(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookId) {
        GradeBookView.Sheet sheet = service.getSheet(userId(jwt), bookId);
        return new SheetResponse(
                BookResponse.from(sheet.book()),
                sheet.columns(),
                sheet.students().stream().map(TeacherController.ClassStudentResponse::from).toList(),
                sheet.marks());
    }

    @PostMapping("/{bookId}/columns")
    public ResponseEntity<CreatedResponse> addColumn(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID bookId,
            @Valid @RequestBody AddColumnRequest request) {
        UUID id = service.addColumn(
                userId(jwt), bookId, request.assessmentKind(), request.assessmentForm(),
                request.displayLabel(), request.durationMinutes());
        return ResponseEntity.status(201).body(new CreatedResponse(id));
    }

    @PutMapping("/marks/{assessmentId}")
    public ResponseEntity<Void> recordMark(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID assessmentId,
            @Valid @RequestBody RecordMarkRequest request) {
        service.recordMark(userId(jwt), assessmentId, request.score(), request.outcome());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/publish")
    public ResponseEntity<Void> publish(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID bookId,
            @Valid @RequestBody VersionedRequest request) {
        service.publish(userId(jwt), bookId, request.version());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/unpublish")
    public ResponseEntity<Void> unpublish(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID bookId,
            @Valid @RequestBody VersionedRequest request) {
        service.unpublish(userId(jwt), bookId, request.version());
        return ResponseEntity.noContent().build();
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
