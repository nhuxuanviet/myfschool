package vn.edu.fpt.myschool.shared.error;

import java.net.URI;
import java.time.Clock;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ApiProblemDetailFactory {

    private final Clock clock;

    ApiProblemDetailFactory(Clock clock) {
        this.clock = clock;
    }

    ProblemDetail create(HttpStatusCode status, String detail, String code, URI instance) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        enrich(problemDetail, code, instance);
        return problemDetail;
    }

    void enrich(ProblemDetail problemDetail, String code, URI instance) {
        if (problemDetail.getInstance() == null) {
            problemDetail.setInstance(instance);
        }
        if (problemDetail.getProperties() == null
                || !problemDetail.getProperties().containsKey("code")) {
            problemDetail.setProperty("code", code);
        }
        if (problemDetail.getProperties() == null
                || !problemDetail.getProperties().containsKey("timestamp")) {
            problemDetail.setProperty("timestamp", clock.instant());
        }
    }
}
