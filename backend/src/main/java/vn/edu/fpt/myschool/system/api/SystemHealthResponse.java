package vn.edu.fpt.myschool.system.api;

import java.time.Instant;

public record SystemHealthResponse(String status, Instant timestamp) {
}
