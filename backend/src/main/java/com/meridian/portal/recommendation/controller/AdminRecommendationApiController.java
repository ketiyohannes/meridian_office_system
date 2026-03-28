package com.meridian.portal.recommendation.controller;

import com.meridian.portal.recommendation.dto.RecommendationConfigResponse;
import com.meridian.portal.recommendation.dto.RecommendationExposureStatResponse;
import com.meridian.portal.recommendation.dto.UpdateRecommendationConfigRequest;
import com.meridian.portal.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/recommendations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRecommendationApiController {

    private final RecommendationService recommendationService;

    public AdminRecommendationApiController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/config")
    public List<RecommendationConfigResponse> config() {
        return recommendationService.configs();
    }

    @PutMapping("/config/{key}")
    public RecommendationConfigResponse updateConfig(
        @PathVariable String key,
        @Valid @RequestBody UpdateRecommendationConfigRequest body
    ) {
        return recommendationService.updateConfig(key, body.value());
    }

    @GetMapping("/stats")
    public List<RecommendationExposureStatResponse> stats(
        @RequestParam(name = "days", required = false) Integer days,
        @RequestParam(name = "region", required = false) String region,
        @RequestParam(name = "surface", required = false) String surface
    ) {
        return recommendationService.exposureStats(days, region, surface);
    }
}
