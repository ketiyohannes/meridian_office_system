package com.meridian.portal.discovery.controller;

import com.meridian.portal.discovery.dto.HistoryClearResponse;
import com.meridian.portal.discovery.dto.ProductSearchRequest;
import com.meridian.portal.discovery.dto.ProductSummaryResponse;
import com.meridian.portal.discovery.dto.SuggestionResponse;
import com.meridian.portal.discovery.service.DiscoveryService;
import com.meridian.portal.dto.CursorPageResponse;
import com.meridian.portal.dto.PagedResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discovery")
@PreAuthorize("hasAnyRole('ADMIN','MERCHANDISER','OPS_MANAGER')")
public class DiscoveryApiController {

    private final DiscoveryService discoveryService;

    public DiscoveryApiController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/search")
    public PagedResponse<ProductSummaryResponse> search(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "minPrice", required = false) String minPrice,
        @RequestParam(name = "maxPrice", required = false) String maxPrice,
        @RequestParam(name = "condition", required = false) String condition,
        @RequestParam(name = "postedFrom", required = false) String postedFrom,
        @RequestParam(name = "postedTo", required = false) String postedTo,
        @RequestParam(name = "zipCode", required = false) String zipCode,
        @RequestParam(name = "distanceMiles", required = false) String distanceMiles,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        Authentication auth
    ) {
        return discoveryService.search(new ProductSearchRequest(
            keyword,
            category,
            minPrice,
            maxPrice,
            condition,
            postedFrom,
            postedTo,
            zipCode,
            distanceMiles,
            page,
            size
        ), auth);
    }

    @GetMapping("/search/lazy")
    public CursorPageResponse<ProductSummaryResponse> searchLazy(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "minPrice", required = false) String minPrice,
        @RequestParam(name = "maxPrice", required = false) String maxPrice,
        @RequestParam(name = "condition", required = false) String condition,
        @RequestParam(name = "postedFrom", required = false) String postedFrom,
        @RequestParam(name = "postedTo", required = false) String postedTo,
        @RequestParam(name = "zipCode", required = false) String zipCode,
        @RequestParam(name = "distanceMiles", required = false) String distanceMiles,
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "size", defaultValue = "20") int size,
        Authentication auth
    ) {
        return discoveryService.searchLazy(new ProductSearchRequest(
            keyword,
            category,
            minPrice,
            maxPrice,
            condition,
            postedFrom,
            postedTo,
            zipCode,
            distanceMiles,
            0,
            size
        ), cursor, auth);
    }

    @GetMapping("/suggestions")
    public List<SuggestionResponse> suggestions(
        @RequestParam(name = "q", required = false) String q,
        Authentication auth
    ) {
        return discoveryService.suggestions(q, auth);
    }

    @GetMapping("/trending")
    public List<String> trending(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        return discoveryService.trendingSearches(limit);
    }

    @GetMapping("/history")
    public List<String> history(
        @RequestParam(name = "limit", defaultValue = "10") int limit,
        Authentication auth
    ) {
        return discoveryService.userSearchHistory(auth, limit);
    }

    @DeleteMapping("/history")
    public HistoryClearResponse clearHistory(Authentication auth) {
        return new HistoryClearResponse(discoveryService.clearUserSearchHistory(auth));
    }
}
