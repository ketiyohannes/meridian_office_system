package com.meridian.portal.controller;

import com.meridian.portal.dto.CreateTaskRequest;
import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.dto.TaskResponse;
import com.meridian.portal.service.OperationsWorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@PreAuthorize("hasAnyRole('REGULAR_USER','MERCHANDISER','OPS_MANAGER','ADMIN')")
public class TaskApiController {

    private final OperationsWorkflowService operationsWorkflowService;

    public TaskApiController(OperationsWorkflowService operationsWorkflowService) {
        this.operationsWorkflowService = operationsWorkflowService;
    }

    @GetMapping("/my")
    public PagedResponse<TaskResponse> myTasks(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        Authentication auth
    ) {
        return operationsWorkflowService.myTasks(auth, status, page, size);
    }

    @PutMapping("/{taskId}/complete")
    public TaskResponse completeTask(@PathVariable long taskId, Authentication auth) {
        return operationsWorkflowService.completeTask(taskId, auth);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    public TaskResponse assignTask(
        @Valid @RequestBody CreateTaskRequest body,
        Authentication auth
    ) {
        return operationsWorkflowService.assignTask(body, auth);
    }
}
