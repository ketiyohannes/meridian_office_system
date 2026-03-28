package com.meridian.portal.controller;

import com.meridian.portal.dto.CreateExceptionRequest;
import com.meridian.portal.dto.DecisionExceptionRequest;
import com.meridian.portal.dto.ExceptionRequestResponse;
import com.meridian.portal.service.OperationsWorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exceptions")
@PreAuthorize("isAuthenticated()")
public class ExceptionRequestApiController {

    private final OperationsWorkflowService operationsWorkflowService;

    public ExceptionRequestApiController(OperationsWorkflowService operationsWorkflowService) {
        this.operationsWorkflowService = operationsWorkflowService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('REGULAR_USER','MERCHANDISER','OPS_MANAGER','ADMIN')")
    public ExceptionRequestResponse create(
        @Valid @RequestBody CreateExceptionRequest body,
        Authentication auth
    ) {
        return operationsWorkflowService.createExceptionRequest(body, auth);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('REGULAR_USER','MERCHANDISER','OPS_MANAGER','ADMIN')")
    public List<ExceptionRequestResponse> mine(Authentication auth) {
        return operationsWorkflowService.myExceptions(auth);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('OPS_MANAGER','ADMIN')")
    public List<ExceptionRequestResponse> pending() {
        return operationsWorkflowService.pendingExceptions();
    }

    @PutMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('OPS_MANAGER','ADMIN')")
    public ExceptionRequestResponse decide(
        @PathVariable long id,
        @Valid @RequestBody DecisionExceptionRequest body,
        Authentication auth
    ) {
        return operationsWorkflowService.decideException(id, body, auth);
    }
}
