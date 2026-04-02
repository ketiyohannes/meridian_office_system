package com.meridian.portal.service;

import com.meridian.portal.dto.CreateExceptionRequest;
import com.meridian.portal.dto.CreateTaskRequest;
import com.meridian.portal.dto.DecisionExceptionRequest;
import com.meridian.portal.dto.ExceptionRequestResponse;
import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.dto.TaskResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationsWorkflowService {

    private final TaskWorkflowService taskWorkflowService;
    private final ExceptionWorkflowService exceptionWorkflowService;

    public OperationsWorkflowService(
        TaskWorkflowService taskWorkflowService,
        ExceptionWorkflowService exceptionWorkflowService
    ) {
        this.taskWorkflowService = taskWorkflowService;
        this.exceptionWorkflowService = exceptionWorkflowService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> myTasks(Authentication auth, String statusRaw, int page, int size) {
        return taskWorkflowService.myTasks(auth, statusRaw, page, size);
    }

    @Transactional
    public TaskResponse assignTask(CreateTaskRequest request, Authentication auth) {
        return taskWorkflowService.assignTask(request, auth);
    }

    @Transactional
    public TaskResponse completeTask(long taskId, Authentication auth) {
        return taskWorkflowService.completeTask(taskId, auth);
    }

    @Transactional
    public ExceptionRequestResponse createExceptionRequest(CreateExceptionRequest request, Authentication auth) {
        return exceptionWorkflowService.createExceptionRequest(request, auth);
    }

    @Transactional(readOnly = true)
    public List<ExceptionRequestResponse> myExceptions(Authentication auth) {
        return exceptionWorkflowService.myExceptions(auth);
    }

    @Transactional(readOnly = true)
    public List<ExceptionRequestResponse> pendingExceptions() {
        return exceptionWorkflowService.pendingExceptions();
    }

    @Transactional
    public ExceptionRequestResponse decideException(long id, DecisionExceptionRequest request, Authentication auth) {
        return exceptionWorkflowService.decideException(id, request, auth);
    }
}
