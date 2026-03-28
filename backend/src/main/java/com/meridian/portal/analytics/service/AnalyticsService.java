package com.meridian.portal.analytics.service;

import com.meridian.portal.analytics.dto.CancellationReasonStat;
import com.meridian.portal.analytics.dto.KpiDashboardResponse;
import com.meridian.portal.analytics.dto.KpiFilters;
import com.meridian.portal.config.ReportingProperties;
import com.meridian.portal.exception.ValidationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final Pattern REPORT_PACKAGE_NAME_PATTERN =
        Pattern.compile("^kpi-package-\\d{8}\\.(csv|xlsx)$");

    private final JdbcTemplate jdbcTemplate;
    private final ReportingProperties reportingProperties;

    public AnalyticsService(JdbcTemplate jdbcTemplate, ReportingProperties reportingProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.reportingProperties = reportingProperties;
    }

    @Transactional(readOnly = true)
    public KpiDashboardResponse dashboard(
        String fromRaw,
        String toRaw,
        String product,
        String category,
        String channel,
        String role,
        String region
    ) {
        KpiFilters filters = AnalyticsFilterParser.parse(fromRaw, toRaw, product, category, channel, role, region);
        Aggregate aggregate = aggregate(filters);
        List<CancellationReasonStat> reasons = cancellationReasons(filters);

        return new KpiDashboardResponse(
            filters.from(),
            filters.to(),
            round2(aggregate.gmv()),
            aggregate.orderVolume(),
            round2(aggregate.conversionRatePercent()),
            round2(aggregate.averageOrderValue()),
            round2(aggregate.repeatPurchaseRatePercent()),
            round2(aggregate.fulfillmentTimelinessPercent()),
            reasons
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(KpiDashboardResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("metric,value\n");
        sb.append("from,").append(response.from()).append('\n');
        sb.append("to,").append(response.to()).append('\n');
        sb.append("gmv,").append(response.gmv()).append('\n');
        sb.append("order_volume,").append(response.orderVolume()).append('\n');
        sb.append("conversion_rate_percent,").append(response.conversionRatePercent()).append('\n');
        sb.append("average_order_value,").append(response.averageOrderValue()).append('\n');
        sb.append("repeat_purchase_rate_percent,").append(response.repeatPurchaseRatePercent()).append('\n');
        sb.append("fulfillment_timeliness_percent,").append(response.fulfillmentTimelinessPercent()).append('\n');
        sb.append('\n');
        sb.append("cancellation_reason,count\n");
        for (CancellationReasonStat reason : response.cancellationReasons()) {
            sb.append(csv(reason.reason())).append(',').append(reason.count()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportXlsx(KpiDashboardResponse response) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet summarySheet = workbook.createSheet("kpi_summary");
            int rowIndex = 0;
            rowIndex = writeRow(summarySheet, rowIndex, "Metric", "Value");
            rowIndex = writeRow(summarySheet, rowIndex, "From", response.from().toString());
            rowIndex = writeRow(summarySheet, rowIndex, "To", response.to().toString());
            rowIndex = writeRow(summarySheet, rowIndex, "GMV", String.valueOf(response.gmv()));
            rowIndex = writeRow(summarySheet, rowIndex, "Order Volume", String.valueOf(response.orderVolume()));
            rowIndex = writeRow(summarySheet, rowIndex, "Conversion Rate %", String.valueOf(response.conversionRatePercent()));
            rowIndex = writeRow(summarySheet, rowIndex, "Average Order Value", String.valueOf(response.averageOrderValue()));
            rowIndex = writeRow(summarySheet, rowIndex, "Repeat Purchase Rate %", String.valueOf(response.repeatPurchaseRatePercent()));
            writeRow(summarySheet, rowIndex, "Fulfillment Timeliness %", String.valueOf(response.fulfillmentTimelinessPercent()));

            XSSFSheet reasonsSheet = workbook.createSheet("cancellations");
            int r = 0;
            r = writeRow(reasonsSheet, r, "Reason", "Count");
            for (CancellationReasonStat reason : response.cancellationReasons()) {
                r = writeRow(reasonsSheet, r, reason.reason(), String.valueOf(reason.count()));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ValidationException("Failed to generate XLSX report");
        }
    }

    @Scheduled(cron = "0 10 0 * * *")
    @Transactional(readOnly = true)
    public void generateNightlyReportPackage() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Instant from = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusSeconds(1).toInstant();

        KpiDashboardResponse response = dashboard(from.toString(), to.toString(), null, null, null, null, null);

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd").format(yesterday);
        Path dir = Paths.get(reportingProperties.getSharedFolder());
        try {
            Files.createDirectories(dir);
            cleanupOldPackages(dir, reportingProperties.getRetentionDays());

            Files.write(
                dir.resolve("kpi-package-" + stamp + ".csv"),
                exportCsv(response),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            Files.write(
                dir.resolve("kpi-package-" + stamp + ".xlsx"),
                exportXlsx(response),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            log.info("event=nightly_report_package_generated date={} folder={}", stamp, dir.toAbsolutePath());
        } catch (IOException ex) {
            log.error("event=nightly_report_package_failed date={} reason={}", stamp, ex.getMessage());
            throw new ValidationException("Failed to write nightly report package: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<String> reportPackages() {
        Path dir = Paths.get(reportingProperties.getSharedFolder());
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .filter(this::isValidReportPackageFileName)
                .sorted((a, b) -> b.compareToIgnoreCase(a))
                .limit(100)
                .toList();
        } catch (IOException ex) {
            log.warn("event=report_package_list_failed reason={}", ex.getMessage());
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public byte[] loadReportPackage(String fileNameRaw) {
        String fileName = sanitizeFileName(fileNameRaw);
        Path dir = Paths.get(reportingProperties.getSharedFolder());
        Path target = dir.resolve(fileName).normalize();
        if (!target.startsWith(dir.normalize())) {
            throw new ValidationException("Invalid report package path");
        }
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new ValidationException("Report package not found");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            log.warn("event=report_package_read_failed file={} reason={}", fileName, ex.getMessage());
            throw new ValidationException("Failed to read report package");
        }
    }

    private int writeRow(XSSFSheet sheet, int rowIndex, String left, String right) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(left);
        row.createCell(1).setCellValue(right);
        return rowIndex;
    }

    private Aggregate aggregate(KpiFilters filters) {
        List<Object> params = new ArrayList<>();
        String where = whereClause(filters, params);
        String baseQuery = "FROM order_facts o " + where;

        double gmv = queryForDouble(
            "SELECT COALESCE(SUM(CASE WHEN o.order_status <> 'CANCELLED' THEN (o.unit_price * o.quantity) ELSE 0 END), 0) " + baseQuery,
            params
        );
        long orderVolume = queryForLong("SELECT COALESCE(COUNT(DISTINCT o.order_number), 0) " + baseQuery, params);
        long buyerCount = queryForLong("SELECT COALESCE(COUNT(DISTINCT o.user_username), 0) " + baseQuery, params);
        long repeatBuyerCount = queryForLong(
            "SELECT COALESCE(COUNT(*), 0) FROM (" +
                "SELECT o.user_username FROM order_facts o " + where +
                " GROUP BY o.user_username HAVING COUNT(DISTINCT o.order_number) > 1" +
            ") t",
            params
        );
        long fulfilledCount = queryForLong("SELECT COALESCE(COUNT(*), 0) FROM order_facts o " + where + " AND o.order_status = 'FULFILLED'", params);
        long timelyFulfilled = queryForLong(
            "SELECT COALESCE(COUNT(*), 0) FROM order_facts o " + where +
                " AND o.order_status = 'FULFILLED' AND o.fulfilled_at IS NOT NULL " +
                "AND TIMESTAMPDIFF(HOUR, o.ordered_at, o.fulfilled_at) <= 48",
            params
        );

        long visits = queryForLong(
            "SELECT COALESCE(COUNT(*), 0) FROM analytics_events e WHERE e.event_type = 'STORE_VISIT' " + eventWhereClause(filters),
            eventWhereParams(filters)
        );

        double aov = KpiMathPolicy.averageOrderValue(gmv, orderVolume);
        double conversion = KpiMathPolicy.conversionRatePercent(orderVolume, visits);
        double repeatRate = KpiMathPolicy.repeatPurchaseRatePercent(repeatBuyerCount, buyerCount);
        double fulfillmentTimeliness = KpiMathPolicy.fulfillmentTimelinessPercent(timelyFulfilled, fulfilledCount);

        return new Aggregate(gmv, orderVolume, conversion, aov, repeatRate, fulfillmentTimeliness);
    }

    private List<CancellationReasonStat> cancellationReasons(KpiFilters filters) {
        List<Object> params = new ArrayList<>();
        String where = whereClause(filters, params);
        return jdbcTemplate.query(
            "SELECT COALESCE(o.cancellation_reason, 'UNKNOWN') AS reason, COUNT(*) AS cnt " +
                "FROM order_facts o " + where + " AND o.order_status = 'CANCELLED' " +
                "GROUP BY COALESCE(o.cancellation_reason, 'UNKNOWN') ORDER BY cnt DESC",
            (rs, i) -> new CancellationReasonStat(rs.getString("reason"), rs.getLong("cnt")),
            params.toArray()
        );
    }

    private String whereClause(KpiFilters filters, List<Object> params) {
        StringBuilder where = new StringBuilder("WHERE o.ordered_at BETWEEN ? AND ? ");
        params.add(Timestamp.from(filters.from()));
        params.add(Timestamp.from(filters.to()));

        if (hasText(filters.category())) {
            where.append("AND o.category_name = ? ");
            params.add(filters.category());
        }
        if (hasText(filters.product())) {
            where.append("AND o.product_sku = ? ");
            params.add(filters.product());
        }
        if (hasText(filters.channel())) {
            where.append("AND o.channel = ? ");
            params.add(filters.channel());
        }
        if (hasText(filters.role())) {
            where.append("AND o.user_role = ? ");
            params.add(filters.role());
        }
        if (hasText(filters.region())) {
            where.append("AND o.region_code = ? ");
            params.add(filters.region());
        }
        return where.toString();
    }

    private String eventWhereClause(KpiFilters filters) {
        StringBuilder where = new StringBuilder("AND e.created_at BETWEEN ? AND ? ");
        if (hasText(filters.channel())) {
            where.append("AND e.channel = ? ");
        }
        if (hasText(filters.role())) {
            where.append("AND e.user_role = ? ");
        }
        if (hasText(filters.region())) {
            where.append("AND e.region_code = ? ");
        }
        return where.toString();
    }

    private Object[] eventWhereParams(KpiFilters filters) {
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.from(filters.from()));
        params.add(Timestamp.from(filters.to()));
        if (hasText(filters.channel())) {
            params.add(filters.channel());
        }
        if (hasText(filters.role())) {
            params.add(filters.role());
        }
        if (hasText(filters.region())) {
            params.add(filters.region());
        }
        return params.toArray();
    }

    private long queryForLong(String sql, List<Object> params) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return value == null ? 0 : value;
    }

    private long queryForLong(String sql, Object[] params) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, params);
        return value == null ? 0 : value;
    }

    private double queryForDouble(String sql, List<Object> params) {
        Double value = jdbcTemplate.queryForObject(sql, Double.class, params.toArray());
        return value == null ? 0 : value;
    }

    private void cleanupOldPackages(Path dir, int retentionDays) throws IOException {
        Instant cutoff = Instant.now().minusSeconds(Math.max(retentionDays, 1) * 24L * 60L * 60L);
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Instant modified = Files.getLastModifiedTime(path).toInstant();
                        if (modified.isBefore(cutoff)) {
                            Files.deleteIfExists(path);
                            log.info("event=report_package_deleted file={}", path.getFileName());
                        }
                    } catch (IOException ex) {
                        log.warn("event=report_package_delete_failed file={} reason={}", path.getFileName(), ex.getMessage());
                    }
                });
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String sanitizeFileName(String value) {
        String fileName = trimToNull(value);
        if (fileName == null) {
            throw new ValidationException("file is required");
        }
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new ValidationException("Invalid report package path");
        }
        if (!isValidReportPackageFileName(fileName)) {
            throw new ValidationException("Invalid report package file name");
        }
        return fileName;
    }

    private boolean isValidReportPackageFileName(String fileName) {
        return REPORT_PACKAGE_NAME_PATTERN.matcher(fileName).matches();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record Aggregate(
        double gmv,
        long orderVolume,
        double conversionRatePercent,
        double averageOrderValue,
        double repeatPurchaseRatePercent,
        double fulfillmentTimelinessPercent
    ) {}
}
