package vn.edu.fpt.myschool.system.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.fpt.myschool.shared.config.CoreConfiguration;
import vn.edu.fpt.myschool.shared.error.ApiProblemDetailFactory;

@WebMvcTest(SystemHealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CoreConfiguration.class, ApiProblemDetailFactory.class})
class SystemHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthEndpoint healthEndpoint;

    @Test
    void returnsOkWhenAggregateHealthIsUp() throws Exception {
        HealthDescriptor health = mock(IndicatedHealthDescriptor.class);
        given(health.getStatus()).willReturn(Status.UP);
        given(healthEndpoint.health()).willReturn(health);

        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void returnsServiceUnavailableWhenAggregateHealthIsDown() throws Exception {
        HealthDescriptor health = mock(IndicatedHealthDescriptor.class);
        given(health.getStatus()).willReturn(Status.DOWN);
        given(healthEndpoint.health()).willReturn(health);

        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }
}
