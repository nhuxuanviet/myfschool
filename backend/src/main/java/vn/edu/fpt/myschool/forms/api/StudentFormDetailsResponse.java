package vn.edu.fpt.myschool.forms.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.forms.domain.StudentFormDetails;
import vn.edu.fpt.myschool.forms.domain.StudentFormStatusEntry;

public record StudentFormDetailsResponse(
        UUID id,
        String type,
        String reason,
        LocalDate startsOn,
        LocalDate endsOn,
        String status,
        Instant submittedAt,
        Instant updatedAt,
        boolean canCancel,
        List<TimelineEntry> timeline) {

    static StudentFormDetailsResponse from(StudentFormDetails details) {
        StudentFormSummaryResponse summary = StudentFormSummaryResponse.from(details.summary());
        return new StudentFormDetailsResponse(
                summary.id(),
                summary.type(),
                details.reason(),
                summary.startsOn(),
                summary.endsOn(),
                summary.status(),
                summary.submittedAt(),
                summary.updatedAt(),
                summary.canCancel(),
                details.timeline().stream().map(TimelineEntry::from).toList());
    }

    public record TimelineEntry(
            UUID id,
            String status,
            Instant occurredAt,
            String note) {

        static TimelineEntry from(StudentFormStatusEntry entry) {
            return new TimelineEntry(
                    entry.id(),
                    entry.status().name(),
                    entry.occurredAt(),
                    entry.note());
        }
    }
}
