package vn.edu.fpt.myschool.clubs.api;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.clubs.application.ClubsService;
import vn.edu.fpt.myschool.clubs.domain.ClubApplicationResult;
import vn.edu.fpt.myschool.clubs.domain.ClubCategory;

@RestController
@RequestMapping("/api/v1/clubs")
@Tag(name = "Clubs", description = "Student-visible clubs and membership applications")
@SecurityRequirement(name = "bearerAuth")
public class ClubsController {
    private final ClubsService clubsService;

    public ClubsController(ClubsService clubsService) {
        this.clubsService = clubsService;
    }

    @GetMapping
    @Operation(summary = "List clubs visible to the authenticated student")
    public ClubsResponse getClubs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ClubCategory category) {
        return ClubsResponse.from(clubsService.getClubs(jwt.getSubject(), category));
    }

    @GetMapping("/{clubId}")
    public ClubResponse getClub(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID clubId) {
        return ClubResponse.from(clubsService.getClub(jwt.getSubject(), clubId));
    }

    @PostMapping("/{clubId}/applications")
    public ResponseEntity<ClubResponse> apply(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID clubId) {
        ClubApplicationResult result = clubsService.apply(jwt.getSubject(), clubId);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ClubResponse.from(result.club()));
    }

    @DeleteMapping("/{clubId}/applications")
    public ClubResponse withdraw(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID clubId) {
        return ClubResponse.from(clubsService.withdraw(jwt.getSubject(), clubId));
    }
}
