package vn.edu.fpt.myschool.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneOffset.UTC);
        ApiProblemDetailFactory factory = new ApiProblemDetailFactory(clock);
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler(factory))
                .build();
    }

    @Test
    void serializesExpectedApplicationErrorsAsProblemDetails() throws Exception {
        mockMvc.perform(get("/expected"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The request conflicts with current state"))
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"))
                .andExpect(jsonPath("$.instance").value("/expected"))
                .andExpect(jsonPath("$.timestamp").value("2026-07-10T12:00:00Z"));
    }

    @Test
    void doesNotLeakUnexpectedExceptionDetails() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database-password"))))
                .andExpect(jsonPath("$.code").value(ApiErrorCode.INTERNAL_ERROR.name()));
    }

    @RestController
    static class FailingController {

        @GetMapping("/expected")
        void expected() {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "STATE_CONFLICT",
                    "The request conflicts with current state");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("database-password");
        }
    }
}
