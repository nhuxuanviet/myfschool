package vn.edu.fpt.myschool.timetable.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import vn.edu.fpt.myschool.home.domain.AcademicTerm;

public record Timetable(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<AcademicTerm> academicTerms,
        List<TimetableDay> days) {

    private static final Comparator<AcademicTerm> TERM_ORDER = Comparator
            .comparing(AcademicTerm::startsOn)
            .thenComparing(AcademicTerm::endsOn)
            .thenComparing(AcademicTerm::code)
            .thenComparing(AcademicTerm::academicYear);

    public Timetable {
        Objects.requireNonNull(weekStart, "weekStart must not be null");
        Objects.requireNonNull(weekEnd, "weekEnd must not be null");
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("weekStart must be a Monday");
        }
        if (!weekEnd.equals(weekStart.plusDays(6))) {
            throw new IllegalArgumentException("weekEnd must be six days after weekStart");
        }
        academicTerms = List.copyOf(Objects.requireNonNull(
                academicTerms, "academicTerms must not be null"));
        days = List.copyOf(Objects.requireNonNull(days, "days must not be null"));
        for (int index = 0; index < academicTerms.size(); index++) {
            AcademicTerm academicTerm = academicTerms.get(index);
            if (academicTerm.endsOn().isBefore(weekStart)
                    || academicTerm.startsOn().isAfter(weekEnd)) {
                throw new IllegalArgumentException(
                        "Each academic term must intersect the requested week");
            }
            if (index > 0 && TERM_ORDER.compare(academicTerms.get(index - 1), academicTerm) > 0) {
                throw new IllegalArgumentException("academicTerms must be in chronological order");
            }
        }
        if (days.size() != 7) {
            throw new IllegalArgumentException("A timetable must contain exactly seven days");
        }
        for (int index = 0; index < days.size(); index++) {
            TimetableDay day = days.get(index);
            LocalDate expectedDate = weekStart.plusDays(index);
            if (!day.date().equals(expectedDate) || day.dayOfWeek() != expectedDate.getDayOfWeek()) {
                throw new IllegalArgumentException("Timetable days must cover the requested week in order");
            }
        }
        if (academicTerms.isEmpty() && days.stream().anyMatch(day -> !day.lessons().isEmpty())) {
            throw new IllegalArgumentException("A week outside all terms must not contain lessons");
        }
    }
}
