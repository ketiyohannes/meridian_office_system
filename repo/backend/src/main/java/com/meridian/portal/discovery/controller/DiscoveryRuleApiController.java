package com.meridian.portal.discovery.controller;

import com.meridian.portal.discovery.dto.DiscoveryRuleResponse;
import com.meridian.portal.discovery.dto.UpsertDiscoveryRuleRequest;
import com.meridian.portal.discovery.service.DiscoveryRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discovery/rules")
@PreAuthorize("hasAnyRole('ADMIN','MERCHANDISER')")
public class DiscoveryRuleApiController {

    private final DiscoveryRuleService discoveryRuleService;

    public DiscoveryRuleApiController(DiscoveryRuleService discoveryRuleService) {
        this.discoveryRuleService = discoveryRuleService;
    }

    @GetMapping
    public List<DiscoveryRuleResponse> list() {
        return discoveryRuleService.listRules();
    }

    @PostMapping
    public DiscoveryRuleResponse create(
        @Valid @RequestBody UpsertDiscoveryRuleRequest body,
        Authentication auth
    ) {
        return discoveryRuleService.createRule(body, auth);
    }

    @PutMapping("/{id}")
    public DiscoveryRuleResponse update(
        @PathVariable long id,
        @Valid @RequestBody UpsertDiscoveryRuleRequest body
    ) {
        return discoveryRuleService.updateRule(id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        discoveryRuleService.deleteRule(id);
    }
}
