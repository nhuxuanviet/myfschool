package vn.edu.fpt.myschool.timetable.domain;

import java.util.Objects;
import java.util.UUID;

import vn.edu.fpt.myschool.home.domain.AcademicTerm;

public record TimetableTerm(UUID id, AcademicTerm academicTerm) {

    public TimetableTerm {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(academicTerm, "academicTerm must not be null");
    }
}
