package com.meridian.portal.discovery.service;

import com.meridian.portal.discovery.domain.Category;
import com.meridian.portal.discovery.domain.Product;
import com.meridian.portal.discovery.domain.ProductCondition;
import com.meridian.portal.discovery.domain.SearchQueryEvent;
import com.meridian.portal.discovery.domain.ZipReference;
import com.meridian.portal.discovery.dto.ActiveDiscoveryRule;
import com.meridian.portal.discovery.dto.ProductSearchRequest;
import com.meridian.portal.discovery.dto.ProductSummaryResponse;
import com.meridian.portal.discovery.dto.SuggestionResponse;
import com.meridian.portal.dto.CursorPageResponse;
import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.discovery.repository.CategoryRepository;
import com.meridian.portal.discovery.repository.ProductRepository;
import com.meridian.portal.discovery.repository.SearchQueryEventRepository;
import com.meridian.portal.discovery.repository.ZipReferenceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscoveryService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ZipReferenceRepository zipReferenceRepository;
    private final SearchQueryEventRepository searchQueryEventRepository;
    private final DiscoveryRuleService discoveryRuleService;

    public DiscoveryService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        ZipReferenceRepository zipReferenceRepository,
        SearchQueryEventRepository searchQueryEventRepository,
        DiscoveryRuleService discoveryRuleService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.zipReferenceRepository = zipReferenceRepository;
        this.searchQueryEventRepository = searchQueryEventRepository;
        this.discoveryRuleService = discoveryRuleService;
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(Category::getName).toList();
    }

    @Transactional(readOnly = true)
    public List<String> conditions() {
        return List.of(ProductCondition.NEW.name(), ProductCondition.USED.name(), ProductCondition.REFURBISHED.name());
    }

    @Transactional
    public PagedResponse<ProductSummaryResponse> search(ProductSearchRequest request, Authentication auth) {
        ParsedFilters filters = parseFilters(request);
        List<ActiveDiscoveryRule> rules = discoveryRuleService.activeRules();
        Set<String> excludedSkus = rules.stream()
            .filter(r -> "EXCLUDE_SKU".equalsIgnoreCase(r.ruleType()))
            .map(ActiveDiscoveryRule::targetValue)
            .collect(Collectors.toSet());
        Map<String, Integer> pinnedSkuPriority = rules.stream()
            .filter(r -> "PIN_SKU".equalsIgnoreCase(r.ruleType()))
            .collect(Collectors.toMap(
                ActiveDiscoveryRule::targetValue,
                ActiveDiscoveryRule::priority,
                Integer::min
            ));

        Pageable pageable = PageRequest.of(filters.page(), filters.size(), Sort.by(Sort.Direction.DESC, "postedAt"));
        Specification<Product> spec = Specification.where((root, query, cb) -> cb.isTrue(root.get("enabled")));

        if (hasText(filters.keyword())) {
            String keywordPattern = "%" + filters.keyword().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), keywordPattern),
                cb.like(cb.lower(root.get("sku")), keywordPattern),
                cb.like(cb.lower(root.get("description")), keywordPattern)
            ));
        }

        if (hasText(filters.category())) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("category").get("name")), filters.category().toLowerCase(Locale.ROOT)));
        }

        if (filters.minPrice() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), filters.minPrice()));
        }

        if (filters.maxPrice() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), filters.maxPrice()));
        }

        if (filters.condition() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("condition"), filters.condition()));
        }

        if (filters.postedFrom() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("postedAt"), filters.postedFrom()));
        }

        if (filters.postedTo() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("postedAt"), filters.postedTo()));
        }
        if (!excludedSkus.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.not(root.get("sku").in(excludedSkus)));
        }

        ZipReference originZip = null;
        List<String> nearbyZipCodes = List.of();
        if (filters.zipCode() != null || filters.distanceMiles() != null) {
            if (filters.zipCode() == null || filters.distanceMiles() == null) {
                throw new ValidationException("Both zipCode and distanceMiles are required when using distance filtering");
            }
            originZip = zipReferenceRepository.findById(filters.zipCode())
                .orElseThrow(() -> new ValidationException("Unknown ZIP code: " + filters.zipCode()));
            nearbyZipCodes = zipReferenceRepository.findZipCodesWithinMiles(
                originZip.getLatitude().doubleValue(),
                originZip.getLongitude().doubleValue(),
                filters.distanceMiles()
            );
            if (nearbyZipCodes.isEmpty()) {
                return new PagedResponse<>(List.of(), filters.page(), filters.size(), 0, 0, true, true);
            }
            List<String> finalNearbyZipCodes = nearbyZipCodes;
            spec = spec.and((root, query, cb) -> root.get("zipCode").in(finalNearbyZipCodes));
        }

        Page<Product> page = productRepository.findAll(spec, pageable);

        Map<String, ZipReference> zipMap = zipReferenceRepository.findByZipCodeIn(
            page.getContent().stream().map(Product::getZipCode).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(ZipReference::getZipCode, z -> z));

        ZipReference finalOriginZip = originZip;
        List<ProductSummaryResponse> content = page.getContent().stream()
            .map(product -> toSummary(product, finalOriginZip, zipMap.get(product.getZipCode())))
            .toList();
        if (!pinnedSkuPriority.isEmpty()) {
            content = content.stream()
                .sorted((a, b) -> {
                    int ap = pinnedSkuPriority.getOrDefault(a.sku(), Integer.MAX_VALUE);
                    int bp = pinnedSkuPriority.getOrDefault(b.sku(), Integer.MAX_VALUE);
                    if (ap != bp) {
                        return Integer.compare(ap, bp);
                    }
                    return b.postedAt().compareTo(a.postedAt());
                })
                .toList();
        }

        recordSearchEvent(filters, auth);

        return new PagedResponse<>(
            content,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }

    @Transactional
    public CursorPageResponse<ProductSummaryResponse> searchLazy(ProductSearchRequest request, String cursor, Authentication auth) {
        ParsedFilters filters = parseFilters(request);
        List<ActiveDiscoveryRule> rules = discoveryRuleService.activeRules();
        Set<String> excludedSkus = rules.stream()
            .filter(r -> "EXCLUDE_SKU".equalsIgnoreCase(r.ruleType()))
            .map(ActiveDiscoveryRule::targetValue)
            .collect(Collectors.toSet());
        Map<String, Integer> pinnedSkuPriority = rules.stream()
            .filter(r -> "PIN_SKU".equalsIgnoreCase(r.ruleType()))
            .collect(Collectors.toMap(
                ActiveDiscoveryRule::targetValue,
                ActiveDiscoveryRule::priority,
                Integer::min
            ));

        int requestedSize = filters.size();
        Pageable pageable = PageRequest.of(0, requestedSize + 1, Sort.by(Sort.Direction.DESC, "postedAt", "id"));
        Specification<Product> spec = Specification.where((root, query, cb) -> cb.isTrue(root.get("enabled")));

        if (hasText(filters.keyword())) {
            String keywordPattern = "%" + filters.keyword().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), keywordPattern),
                cb.like(cb.lower(root.get("sku")), keywordPattern),
                cb.like(cb.lower(root.get("description")), keywordPattern)
            ));
        }

        if (hasText(filters.category())) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("category").get("name")), filters.category().toLowerCase(Locale.ROOT)));
        }

        if (filters.minPrice() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), filters.minPrice()));
        }

        if (filters.maxPrice() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), filters.maxPrice()));
        }

        if (filters.condition() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("condition"), filters.condition()));
        }

        if (filters.postedFrom() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("postedAt"), filters.postedFrom()));
        }

        if (filters.postedTo() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("postedAt"), filters.postedTo()));
        }
        if (!excludedSkus.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.not(root.get("sku").in(excludedSkus)));
        }

        ZipReference originZip = null;
        if (filters.zipCode() != null || filters.distanceMiles() != null) {
            if (filters.zipCode() == null || filters.distanceMiles() == null) {
                throw new ValidationException("Both zipCode and distanceMiles are required when using distance filtering");
            }
            originZip = zipReferenceRepository.findById(filters.zipCode())
                .orElseThrow(() -> new ValidationException("Unknown ZIP code: " + filters.zipCode()));
            List<String> nearbyZipCodes = zipReferenceRepository.findZipCodesWithinMiles(
                originZip.getLatitude().doubleValue(),
                originZip.getLongitude().doubleValue(),
                filters.distanceMiles()
            );
            if (nearbyZipCodes.isEmpty()) {
                return new CursorPageResponse<>(List.of(), null, false);
            }
            List<String> finalNearbyZipCodes = nearbyZipCodes;
            spec = spec.and((root, query, cb) -> root.get("zipCode").in(finalNearbyZipCodes));
        }

        CursorKey cursorKey = parseCursor(cursor);
        if (cursorKey != null) {
            spec = spec.and((root, query, cb) -> cb.or(
                cb.lessThan(root.get("postedAt"), cursorKey.postedAt()),
                cb.and(
                    cb.equal(root.get("postedAt"), cursorKey.postedAt()),
                    cb.lessThan(root.get("id"), cursorKey.id())
                )
            ));
        }

        Page<Product> page = productRepository.findAll(spec, pageable);
        List<Product> fetched = page.getContent();
        boolean hasMore = fetched.size() > requestedSize;
        List<Product> selected = hasMore ? fetched.subList(0, requestedSize) : fetched;

        Map<String, ZipReference> zipMap = zipReferenceRepository.findByZipCodeIn(
            selected.stream().map(Product::getZipCode).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(ZipReference::getZipCode, z -> z));

        ZipReference finalOriginZip = originZip;
        List<ProductSummaryResponse> content = selected.stream()
            .map(product -> toSummary(product, finalOriginZip, zipMap.get(product.getZipCode())))
            .toList();
        if (!pinnedSkuPriority.isEmpty()) {
            content = content.stream()
                .sorted((a, b) -> {
                    int ap = pinnedSkuPriority.getOrDefault(a.sku(), Integer.MAX_VALUE);
                    int bp = pinnedSkuPriority.getOrDefault(b.sku(), Integer.MAX_VALUE);
                    if (ap != bp) {
                        return Integer.compare(ap, bp);
                    }
                    return b.postedAt().compareTo(a.postedAt());
                })
                .toList();
        }

        String nextCursor = null;
        if (hasMore && !selected.isEmpty()) {
            Product last = selected.get(selected.size() - 1);
            nextCursor = encodeCursor(last.getPostedAt(), last.getId());
        }
        recordSearchEvent(filters, auth);
        return new CursorPageResponse<>(content, nextCursor, hasMore);
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponse> suggestions(String query, Authentication auth) {
        if (!hasText(query) || query.trim().length() < 2) {
            return List.of();
        }

        String q = query.trim();
        String username = auth.getName();

        LinkedHashMap<String, SuggestionResponse> merged = new LinkedHashMap<>();

        productRepository
            .findTop10ByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrderByPostedAtDesc(q, q)
            .forEach(product -> {
                merged.putIfAbsent("sku:" + product.getSku(), new SuggestionResponse(product.getSku(), "SKU"));
                merged.putIfAbsent("name:" + product.getName(), new SuggestionResponse(product.getName(), "PRODUCT"));
            });

        categoryRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(q)
            .forEach(category -> merged.putIfAbsent("category:" + category.getName(), new SuggestionResponse(category.getName(), "CATEGORY")));

        searchQueryEventRepository.findUserSearchHistoryMatching(username, q, 10)
            .forEach(term -> merged.putIfAbsent("query:" + term, new SuggestionResponse(term, "HISTORY")));

        return merged.values().stream().limit(10).toList();
    }

    @Transactional(readOnly = true)
    public List<String> trendingSearches(int limit) {
        if (limit < 1 || limit > 20) {
            throw new ValidationException("limit must be between 1 and 20");
        }
        Instant fromTs = Instant.now().minusSeconds(7L * 24 * 60 * 60);
        return searchQueryEventRepository.findTrendingQueries(fromTs, limit);
    }

    @Transactional(readOnly = true)
    public List<String> userSearchHistory(Authentication auth, int limit) {
        if (limit < 1 || limit > 50) {
            throw new ValidationException("limit must be between 1 and 50");
        }
        return searchQueryEventRepository.findUserSearchHistory(auth.getName(), limit);
    }

    @Transactional
    public int clearUserSearchHistory(Authentication auth) {
        return searchQueryEventRepository.deleteByUsername(auth.getName());
    }

    private void recordSearchEvent(ParsedFilters filters, Authentication auth) {
        SearchQueryEvent event = new SearchQueryEvent();
        event.setUsername(auth.getName());
        event.setQueryText(filters.keyword());
        event.setCategoryName(filters.category());
        event.setMinPrice(filters.minPrice());
        event.setMaxPrice(filters.maxPrice());
        event.setConditionStatus(filters.condition() == null ? null : filters.condition().name());
        event.setZipCode(filters.zipCode());
        event.setDistanceMiles(filters.distanceMiles());
        searchQueryEventRepository.save(event);
    }

    private ProductSummaryResponse toSummary(Product product, ZipReference origin, ZipReference target) {
        Double distance = null;
        if (origin != null && target != null) {
            distance = haversineMiles(
                origin.getLatitude().doubleValue(),
                origin.getLongitude().doubleValue(),
                target.getLatitude().doubleValue(),
                target.getLongitude().doubleValue()
            );
        }

        return new ProductSummaryResponse(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getCategory().getName(),
            product.getPrice(),
            product.getCondition().name(),
            product.getPostedAt(),
            product.getZipCode(),
            distance == null ? null : Math.round(distance * 100.0) / 100.0
        );
    }

    private ParsedFilters parseFilters(ProductSearchRequest request) {
        String keyword = trimToNull(request.keyword());
        String category = trimToNull(request.category());
        BigDecimal minPrice = parsePrice(request.minPrice(), "minPrice");
        BigDecimal maxPrice = parsePrice(request.maxPrice(), "maxPrice");
        ProductCondition condition = parseCondition(trimToNull(request.condition()));
        Instant postedFrom = parseDateTime(trimToNull(request.postedFrom()), "postedFrom", false);
        Instant postedTo = parseDateTime(trimToNull(request.postedTo()), "postedTo", true);
        String zipCode = trimToNull(request.zipCode());
        Integer distanceMiles = parseInteger(trimToNull(request.distanceMiles()), "distanceMiles", 1, 500);
        int page = validatePage(request.page());
        int size = validatePageSize(request.size());

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ValidationException("minPrice must be less than or equal to maxPrice");
        }

        if (postedFrom != null && postedTo != null && postedFrom.isAfter(postedTo)) {
            throw new ValidationException("postedFrom must be earlier than or equal to postedTo");
        }

        return new ParsedFilters(
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
        );
    }

    private BigDecimal parsePrice(String raw, String field) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException(field + " must be >= 0");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new ValidationException("Invalid number for " + field);
        }
    }

    private Integer parseInteger(String raw, String field, int min, int max) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) {
                throw new ValidationException(field + " must be between " + min + " and " + max);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new ValidationException("Invalid integer for " + field);
        }
    }

    private Instant parseDateTime(String raw, String field, boolean endOfMinute) {
        if (!hasText(raw)) {
            return null;
        }

        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime local = LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            Instant instant = local.atZone(ZoneId.systemDefault()).toInstant();
            return endOfMinute ? instant.plusSeconds(59) : instant;
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Invalid datetime for " + field);
        }
    }

    private ProductCondition parseCondition(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            return ProductCondition.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid condition value");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw new ValidationException("page must be greater than or equal to 0");
        }
        return page;
    }

    private int validatePageSize(int size) {
        if (size < 1 || size > 100) {
            throw new ValidationException("size must be between 1 and 100");
        }
        return size;
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String encodeCursor(Instant postedAt, Long id) {
        return postedAt.toEpochMilli() + ":" + id;
    }

    private CursorKey parseCursor(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        String[] parts = raw.trim().split(":");
        if (parts.length != 2) {
            throw new ValidationException("Invalid cursor format");
        }
        try {
            long epochMillis = Long.parseLong(parts[0]);
            long id = Long.parseLong(parts[1]);
            if (id <= 0) {
                throw new ValidationException("Invalid cursor format");
            }
            return new CursorKey(Instant.ofEpochMilli(epochMillis), id);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Invalid cursor format");
        }
    }

    private double haversineMiles(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3959 * c;
    }

    private record ParsedFilters(
        String keyword,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ProductCondition condition,
        Instant postedFrom,
        Instant postedTo,
        String zipCode,
        Integer distanceMiles,
        int page,
        int size
    ) {}

    private record CursorKey(Instant postedAt, long id) {}
}
