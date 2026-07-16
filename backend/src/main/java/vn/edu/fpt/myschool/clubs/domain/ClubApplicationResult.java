package vn.edu.fpt.myschool.clubs.domain;

import java.util.Objects;

public record ClubApplicationResult(ClubDetails club, boolean created) {
    public ClubApplicationResult {
        Objects.requireNonNull(club, "club must not be null");
    }
}
