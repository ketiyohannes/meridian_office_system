package com.meridian.portal.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class WorkflowConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Test
    void parallelTaskCompletionIsDeterministic() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "race_task_user", "REGULAR_USER");
        var regular = login("race_task_user", "StrongPass12345!");

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"race_task_user","title":"Task race","description":"parallel"}
                    """))
            .andExpect(status().isOk());
        Long taskId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM user_tasks WHERE username='race_task_user'", Long.class);

        List<Integer> statuses = runInParallel(2, () ->
            mockMvc.perform(put("/api/tasks/" + taskId + "/complete").with(csrf()).session(regular))
                .andReturn()
                .getResponse()
                .getStatus()
        );

        assertTrue(statuses.contains(200));
        long completedRows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_tasks WHERE id = ? AND status='COMPLETED' AND completed_at IS NOT NULL",
            Long.class,
            taskId
        );
        assertEquals(1L, completedRows);
    }

    @Test
    void parallelExceptionDecisionsProcessExactlyOnce() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "race_req_user", "REGULAR_USER");
        createUser(admin, "race_ops_user", "OPS_MANAGER");
        var requester = login("race_req_user", "StrongPass12345!");
        var ops = login("race_ops_user", "StrongPass12345!");

        mockMvc.perform(post("/api/exceptions")
                .with(csrf())
                .session(requester)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"SHIFT_EXCEPTION","details":"parallel decision test"}
                    """))
            .andExpect(status().isOk());
        Long exId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM exception_requests WHERE requester_username='race_req_user'", Long.class);

        List<Integer> statuses = runInParallel(2, () ->
            mockMvc.perform(put("/api/exceptions/" + exId + "/decision")
                    .with(csrf())
                    .session(ops)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"approved":true,"comment":"parallel"}
                        """))
                .andReturn()
                .getResponse()
                .getStatus()
        );

        assertTrue(statuses.contains(200));
        String finalStatus = jdbcTemplate.queryForObject("SELECT status FROM exception_requests WHERE id = ?", String.class, exId);
        assertEquals("APPROVED", finalStatus);

        Long decisionAuditCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_audit_logs WHERE action='EXCEPTION_REQUEST_DECIDE' AND target_id = ?",
            Long.class,
            exId
        );
        assertEquals(1L, decisionAuditCount);

        Long approvalEvents = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notification_events WHERE event_key = ?",
            Long.class,
            "exception-" + exId
        );
        assertEquals(1L, approvalEvents);
    }

    private List<Integer> runInParallel(int count, Callable<Integer> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        } finally {
            executor.shutdownNow();
        }
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
}
