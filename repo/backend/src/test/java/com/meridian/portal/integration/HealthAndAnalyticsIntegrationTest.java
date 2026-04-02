package com.meridian.portal.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class HealthAndAnalyticsIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminCanReadAndUpdateHealthThresholds() throws Exception {
        var adminSession = loginAsAdmin();

        mockMvc.perform(get("/api/admin/health/summary").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metricCode").value("HTTP_5XX_RATE"));

        mockMvc.perform(put("/api/admin/health/thresholds/HTTP_5XX_RATE")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "windowMinutes": 20,
                      "thresholdPercent": 2.5,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.windowMinutes").value(20))
            .andExpect(jsonPath("$.thresholdPercent").value(2.5));

        mockMvc.perform(get("/api/admin/health/alerts")
                .session(adminSession)
                .param("unknown", "1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void analystCanAccessAnalyticsButNotAdminHealth() throws Exception {
        var adminSession = loginAsAdmin();
        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "analyst_01",
                      "password": "AnalystPass12345!",
                      "roles": ["ANALYST"],
                      "enabled": true
                    }
                    """))
            .andExpect(status().isCreated());

        var analystSession = login("analyst_01", "AnalystPass12345!");

        mockMvc.perform(get("/api/analytics/kpis")
                .session(analystSession)
                .param("product", "SKU-1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderVolume").value(greaterThan(0)))
            .andExpect(jsonPath("$.gmv").value(greaterThan(0.0)));

        mockMvc.perform(get("/api/admin/health/summary").session(analystSession))
            .andExpect(status().isForbidden());
    }

    @Test
    void analyticsExportsAndReportPackagesEndpointWork() throws Exception {
        var adminSession = loginAsAdmin();

        Path reportDir = Path.of("/tmp/meridian-report-packages");
        Files.createDirectories(reportDir);
        try (var existing = Files.list(reportDir)) {
            existing.filter(Files::isRegularFile).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        }

        mockMvc.perform(get("/api/analytics/export.csv").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("metric,value")));

        mockMvc.perform(get("/api/analytics/export.xlsx").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        mockMvc.perform(get("/api/analytics/report-packages").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(0)));

        String stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String validCsv = "kpi-package-" + stamp + ".csv";
        String validXlsx = "kpi-package-" + stamp + ".xlsx";
        Files.writeString(reportDir.resolve(validCsv), "metric,value\nx,1\n", StandardCharsets.UTF_8);
        Files.write(reportDir.resolve(validXlsx), new byte[] {1, 2, 3});
        Files.writeString(reportDir.resolve("rogue-export.csv"), "rogue,data\n1,2\n", StandardCharsets.UTF_8);

        mockMvc.perform(get("/api/analytics/report-packages").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@ == '%s')]".formatted(validCsv)).isNotEmpty())
            .andExpect(jsonPath("$[?(@ == '%s')]".formatted(validXlsx)).isNotEmpty())
            .andExpect(jsonPath("$[?(@ == 'rogue-export.csv')]").isEmpty());

        mockMvc.perform(get("/api/analytics/report-packages/download")
                .session(adminSession)
                .param("file", validCsv))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("metric,value")));

        mockMvc.perform(get("/api/analytics/report-packages/download")
                .session(adminSession)
                .param("file", "rogue-export.csv"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/analytics/report-packages/download")
                .session(adminSession)
                .param("file", "../kpi-package-" + stamp + ".csv"))
            .andExpect(status().isBadRequest());
    }
}
