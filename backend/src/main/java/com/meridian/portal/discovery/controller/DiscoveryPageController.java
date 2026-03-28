package com.meridian.portal.discovery.controller;

import com.meridian.portal.discovery.service.DiscoveryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DiscoveryPageController {

    private final DiscoveryService discoveryService;

    public DiscoveryPageController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/discovery")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANDISER','OPS_MANAGER')")
    public String discovery(Model model) {
        model.addAttribute("categories", discoveryService.categories());
        model.addAttribute("conditions", discoveryService.conditions());
        return "discovery/index";
    }
}
