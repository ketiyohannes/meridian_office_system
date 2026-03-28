package com.meridian.portal.analytics.controller;

import com.meridian.portal.analytics.dto.KpiDashboardResponse;
import com.meridian.portal.analytics.service.AnalyticsService;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('ADMIN','ANALYST','OPS_MANAGER')")
public class AnalyticsApiController {

    private final AnalyticsService analyticsService;

    public AnalyticsApiController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/kpis")
    public KpiDashboardResponse kpis(
        @RequestParam(name = "from", required = false) String from,
        @RequestParam(name = "to", required = false) String to,
        @RequestParam(name = "product", required = false) String product,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "channel", required = false) String channel,
        @RequestParam(name = "role", required = false) String role,
        @RequestParam(name = "region", required = false) String region
    ) {
        return analyticsService.dashboard(from, to, product, category, channel, role, region);
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(
        @RequestParam(name = "from", required = false) String from,
        @RequestParam(name = "to", required = false) String to,
        @RequestParam(name = "product", required = false) String product,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "channel", required = false) String channel,
        @RequestParam(name = "role", required = false) String role,
        @RequestParam(name = "region", required = false) String region
    ) {
        KpiDashboardResponse response = analyticsService.dashboard(from, to, product, category, channel, role, region);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("kpi-export.csv").build().toString())
            .contentType(MediaType.TEXT_PLAIN)
            .body(analyticsService.exportCsv(response));
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportXlsx(
        @RequestParam(name = "from", required = false) String from,
        @RequestParam(name = "to", required = false) String to,
        @RequestParam(name = "product", required = false) String product,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "channel", required = false) String channel,
        @RequestParam(name = "role", required = false) String role,
        @RequestParam(name = "region", required = false) String region
    ) {
        KpiDashboardResponse response = analyticsService.dashboard(from, to, product, category, channel, role, region);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("kpi-export.xlsx").build().toString())
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(analyticsService.exportXlsx(response));
    }

    @GetMapping("/report-packages")
    public List<String> reportPackages() {
        return analyticsService.reportPackages();
    }

    @GetMapping("/report-packages/download")
    public ResponseEntity<byte[]> downloadReportPackage(@RequestParam(name = "file") String file) {
        MediaType contentType = file.endsWith(".xlsx")
            ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            : MediaType.TEXT_PLAIN;
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file).build().toString())
            .contentType(contentType)
            .body(analyticsService.loadReportPackage(file));
    }
}
