package vn.edu.fpt.myschool.clubs.api;

import java.util.List;

import vn.edu.fpt.myschool.clubs.domain.ClubDetails;

public record ClubsResponse(List<ClubResponse> clubs) {
    static ClubsResponse from(List<ClubDetails> clubs) {
        return new ClubsResponse(clubs.stream().map(ClubResponse::from).toList());
    }
}
