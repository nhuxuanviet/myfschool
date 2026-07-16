package vn.edu.fpt.myschool.forms.domain;

import java.util.List;
import java.util.Objects;

public record StudentFormDetails(
        StudentFormSummary summary,
        String reason,
        List<StudentFormStatusEntry> timeline) {

    public StudentFormDetails {
        Objects.requireNonNull(summary, "summary must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        timeline = List.copyOf(Objects.requireNonNull(timeline, "timeline must not be null"));
        if (timeline.isEmpty()) {
            throw new IllegalArgumentException("timeline must not be empty");
        }
        for (int index = 1; index < timeline.size(); index++) {
            if (timeline.get(index).occurredAt().isBefore(timeline.get(index - 1).occurredAt())) {
                throw new IllegalArgumentException("timeline must be chronological");
            }
        }
        StudentFormStatusEntry latest = timeline.getLast();
        if (latest.status() != summary.status()
                || latest.occurredAt().isAfter(summary.updatedAt())) {
            throw new IllegalArgumentException("timeline must end at the current form status");
        }
    }
}
