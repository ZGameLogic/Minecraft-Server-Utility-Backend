package com.zgamelogic.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class HealthControllerTest {

    @Value(value="${local.server.port}")
    private int port;

    @Autowired
    private HealthController healthController;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Context loads")
    public void contextLoads() throws Exception {
        assertThat(healthController).isNotNull();
    }

    @Nested
    class Endpoints {
        @Test
        @DisplayName("Health Test")
        public void healthyEndpointTest() throws Exception {
            assertThat(restTemplate.getForObject("http://localhost:" + port + "/health",
                    String.class)).contains("Healthy");
        }
    }
}
