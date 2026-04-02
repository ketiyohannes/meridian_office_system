package com.meridian.portal.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebClientBuilder;

class UiBrowserBehaviorIntegrationTest extends BaseIntegrationTest {

    @Test
    void tasksPageShowsSkeletonThenLazyLoadsAndEscapesClientRenderedValues() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "browser_tasks_user_01", "REGULAR_USER");
        assignTask(admin, "browser_tasks_user_01", "<img src=x onerror=alert(1)>");

        try (WebClient browser = buildBrowser()) {
            loginViaBrowser(browser, "browser_tasks_user_01", "StrongPass12345!");
            HtmlPage tasksPage = browser.getPage("http://localhost/tasks");

            HtmlElement taskRows = tasksPage.getHtmlElementById("taskRows");
            assertTrue(taskRows.asNormalizedText().isBlank(), "Expected empty table body before lazy data load");

            String html = tasksPage.asXml();
            assertTrue(html.contains(".skeleton-row td"), "Expected skeleton row style contract to exist");
            assertTrue(html.contains("function showTaskSkeleton()"), "Expected task skeleton function");
            assertTrue(html.contains("showTaskSkeleton();"), "Expected loader to trigger skeleton state");
            assertTrue(html.contains("fetch(\"/api/tasks/my?page=0&size=20\""), "Expected lazy task fetch endpoint");
            assertTrue(html.contains("title.textContent = task.title;"), "Expected textContent-based safe rendering");
            assertFalse(html.contains("title.innerHTML"), "Task title must never be rendered via innerHTML");
        }
    }

    @Test
    void analyticsPageSupportsSkeletonAndLazyLoadingInBrowserExecution() throws Exception {
        try (WebClient browser = buildBrowser()) {
            loginViaBrowser(browser, "admin", "AdminPass12345!");
            HtmlPage analyticsPage = browser.getPage("http://localhost/analytics");

            List<?> kpiCards = analyticsPage.getByXPath("//*[contains(@class,'kpi')]");
            assertFalse(kpiCards.isEmpty(), "Expected KPI cards to render on first paint");

            String html = analyticsPage.asXml();
            assertTrue(html.contains("function showKpiSkeleton()"), "Expected KPI skeleton function");
            assertTrue(html.contains("showKpiSkeleton();"), "Expected dashboard loader to apply skeleton state");
            assertTrue(html.contains("fetch(\"/api/analytics/kpis?\" + params.toString())"), "Expected lazy KPI fetch endpoint");
            assertTrue(html.contains("loadDashboard();"), "Expected dashboard lazy-load on page init");
        }
    }

    private WebClient buildBrowser() {
        WebClient browser = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
        browser.getOptions().setThrowExceptionOnScriptError(false);
        return browser;
    }

    private void loginViaBrowser(WebClient browser, String username, String password) throws Exception {
        HtmlPage loginPage = browser.getPage("http://localhost/login");
        HtmlForm form = loginPage.getForms().get(0);
        form.getInputByName("username").setValueAttribute(username);
        form.getInputByName("password").setValueAttribute(password);
        ((HtmlElement) form.getFirstByXPath(".//button[@type='submit']")).click();
    }

    private void createUser(org.springframework.mock.web.MockHttpSession adminSession, String username, String role) throws Exception {
        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"StrongPass12345!","roles":["%s"],"enabled":true}
                    """.formatted(username, role)))
            .andExpect(status().isCreated());
    }

    private void assignTask(org.springframework.mock.web.MockHttpSession adminSession, String username, String title) throws Exception {
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","title":"%s","description":"xss test task"}
                    """.formatted(username, title)))
            .andExpect(status().isOk());
    }
}
