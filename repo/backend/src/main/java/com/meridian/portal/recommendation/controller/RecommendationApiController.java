package com.meridian.portal.recommendation.controller;

import com.meridian.portal.recommendation.dto.RecommendationEventRequest;
import com.meridian.portal.recommendation.dto.RecommendationExplainResponse;
import com.meridian.portal.recommendation.dto.RecommendationResponse;
import com.meridian.portal.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@PreAuthorize("hasAnyRole('MERCHANDISER','OPS_MANAGER','ADMIN')")
public class RecommendationApiController {

    private final RecommendationService recommendationService;

    public RecommendationApiController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> recommendations(
        @RequestParam(name = "surface", required = false) String surface,
        @RequestParam(name = "limit", required = false) Integer limit,
        @RequestParam(name = "region", required = false) String region,
        Authentication auth
    ) {
        var result = recommendationService.recommendWithMeta(auth, surface, limit, region);
        List<RecommendationResponse> recommendations = result.items();
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        if (result.servedLimit() < result.requestedLimit()) {
            responseBuilder.header(
                "X-Recommendation-Limit-Adjusted",
                "requested=%d,served=%d".formatted(result.requestedLimit(), result.servedLimit())
            );
        }
        String reasonCode = result.reasonCode();
        if (reasonCode != null && !reasonCode.isBlank()) {
            return responseBuilder
                .header("X-Recommendation-Reason-Code", reasonCode)
                .body(recommendations);
        }
        return responseBuilder.body(recommendations);
    }

    @GetMapping("/explain")
    public List<RecommendationExplainResponse> explain(
        @RequestParam(name = "surface", required = false) String surface,
        @RequestParam(name = "limit", required = false) Integer limit,
        @RequestParam(name = "region", required = false) String region,
        Authentication auth
    ) {
        return recommendationService.explain(auth, surface, limit, region);
    }

    @PostMapping("/events")
    public void trackEvent(@Valid @RequestBody RecommendationEventRequest body, Authentication auth) {
        recommendationService.trackEvent(auth, body);
    }
}
