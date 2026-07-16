package vn.edu.fpt.myschool.forms.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.forms.domain.StudentFormStatus;
import vn.edu.fpt.myschool.forms.domain.StudentFormType;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

@Component
@Profile("(dev | e2e) & !prod")
@Order(600)
class StudentFormsDataSeeder implements ApplicationRunner {

    static final UUID PENDING_LEAVE_FORM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001701");
    static final UUID APPROVED_CONFIRMATION_FORM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001702");
    static final UUID REJECTED_TRANSCRIPT_FORM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001703");
    static final UUID PEER_PENDING_CARD_FORM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001704");

    private static final UUID SEEDED_STUDENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PEER_STUDENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000211");

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    StudentFormsDataSeeder(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, SchoolTimeZone.ZONE);
        seedPendingLeave(now, today);
        seedApprovedConfirmation(now);
        seedRejectedTranscript(now);
        seedPeerPendingCard(now);
    }

    private void seedPendingLeave(Instant now, LocalDate today) {
        Instant submittedAt = now.minus(Duration.ofHours(2));
        insertForm(
                PENDING_LEAVE_FORM_ID,
                SEEDED_STUDENT_ID,
                StudentFormType.LEAVE_OF_ABSENCE,
                "Xin nghỉ học để khám sức khỏe theo lịch hẹn của bệnh viện.",
                today.plusDays(2),
                today.plusDays(2),
                StudentFormStatus.SUBMITTED,
                submittedAt,
                submittedAt);
        insertHistory(
                "00000000-0000-0000-0000-000000001801",
                PENDING_LEAVE_FORM_ID,
                1,
                StudentFormStatus.SUBMITTED,
                submittedAt,
                "Đơn đã được gửi");
    }

    private void seedApprovedConfirmation(Instant now) {
        Instant submittedAt = now.minus(Duration.ofDays(10));
        Instant reviewedAt = now.minus(Duration.ofDays(9));
        Instant approvedAt = now.minus(Duration.ofDays(8));
        insertForm(
                APPROVED_CONFIRMATION_FORM_ID,
                SEEDED_STUDENT_ID,
                StudentFormType.STUDENT_CONFIRMATION,
                "Xin giấy xác nhận đang là học sinh của trường để bổ sung hồ sơ.",
                null,
                null,
                StudentFormStatus.APPROVED,
                submittedAt,
                approvedAt);
        insertHistory("00000000-0000-0000-0000-000000001802", APPROVED_CONFIRMATION_FORM_ID, 1,
                StudentFormStatus.SUBMITTED, submittedAt, "Đơn đã được gửi");
        insertHistory("00000000-0000-0000-0000-000000001803", APPROVED_CONFIRMATION_FORM_ID, 2,
                StudentFormStatus.IN_REVIEW, reviewedAt, "Văn phòng trường đang xử lý");
        insertHistory("00000000-0000-0000-0000-000000001804", APPROVED_CONFIRMATION_FORM_ID, 3,
                StudentFormStatus.APPROVED, approvedAt, "Đơn đã được phê duyệt");
    }

    private void seedRejectedTranscript(Instant now) {
        Instant submittedAt = now.minus(Duration.ofDays(6));
        Instant rejectedAt = now.minus(Duration.ofDays(5));
        insertForm(
                REJECTED_TRANSCRIPT_FORM_ID,
                SEEDED_STUDENT_ID,
                StudentFormType.TRANSCRIPT_REQUEST,
                "Xin cấp bản sao bảng điểm học kỳ để hoàn thiện hồ sơ cá nhân.",
                null,
                null,
                StudentFormStatus.REJECTED,
                submittedAt,
                rejectedAt);
        insertHistory("00000000-0000-0000-0000-000000001805", REJECTED_TRANSCRIPT_FORM_ID, 1,
                StudentFormStatus.SUBMITTED, submittedAt, "Đơn đã được gửi");
        insertHistory("00000000-0000-0000-0000-000000001806", REJECTED_TRANSCRIPT_FORM_ID, 2,
                StudentFormStatus.REJECTED, rejectedAt, "Vui lòng liên hệ giáo vụ để xác minh thông tin");
    }

    private void seedPeerPendingCard(Instant now) {
        Instant submittedAt = now.minus(Duration.ofHours(1));
        insertForm(
                PEER_PENDING_CARD_FORM_ID,
                PEER_STUDENT_ID,
                StudentFormType.STUDENT_CARD_REISSUE,
                "Xin cấp lại thẻ học sinh do thẻ cũ bị thất lạc.",
                null,
                null,
                StudentFormStatus.SUBMITTED,
                submittedAt,
                submittedAt);
        insertHistory("00000000-0000-0000-0000-000000001807", PEER_PENDING_CARD_FORM_ID, 1,
                StudentFormStatus.SUBMITTED, submittedAt, "Đơn đã được gửi");
    }

    private void insertForm(
            UUID id,
            UUID studentId,
            StudentFormType type,
            String reason,
            LocalDate startsOn,
            LocalDate endsOn,
            StudentFormStatus status,
            Instant submittedAt,
            Instant updatedAt) {
        jdbcTemplate.update("""
                INSERT INTO student_forms (
                    id, student_id, form_type, reason, starts_on, ends_on, status,
                    submitted_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                id,
                studentId,
                type.name(),
                reason,
                startsOn,
                endsOn,
                status.name(),
                Timestamp.from(submittedAt),
                Timestamp.from(updatedAt));
    }

    private void insertHistory(
            String id,
            UUID formId,
            int sequenceNumber,
            StudentFormStatus status,
            Instant occurredAt,
            String note) {
        jdbcTemplate.update("""
                INSERT INTO student_form_status_history (
                    id, form_id, sequence_number, status, occurred_at, note
                ) VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                UUID.fromString(id),
                formId,
                sequenceNumber,
                status.name(),
                Timestamp.from(occurredAt),
                note);
    }
}
