## 1. Phase 1 scope boundaries

- Question: Should Phase 1 include recommendations and nightly report packages, or focus first on auth/RBAC/discovery/notifications?
- Understanding: The prompt includes all modules, but delivery sequencing is not defined.
- Solution: Start with auth/RBAC/discovery/notifications in Phase 1; add recommendations/report packages in Phase 2 unless you confirm all-in-one delivery.

## 2. Deployment tenancy model

- Question: Is this for one retail organization only, or multiple business units with strict isolation?
- Understanding: "On-prem retail organization" suggests single-tenant, but internal multi-unit use is possible.
- Solution: Default to single-tenant with a future-ready org/store partitioning key.

## 3. ZIP distance calculation method

- Question: Should distance filtering use ZIP centroid coordinates (Haversine) or a fixed ZIP-to-mile approximation table?
- Understanding: Offline requirement allows either, but precision and maintenance differ.
- Solution: Use offline ZIP centroid data plus Haversine for better accuracy without internet dependency.

## 4. SKU data source of truth

- Question: Are products created and managed only in this portal, or imported from ERP/POS files?
- Understanding: Discovery requires a reliable product data pipeline not explicitly described.
- Solution: Support both manual management and batch import adapters (CSV first, ERP connector later).

## 5. Type-ahead data scope

- Question: Should suggestions include SKU IDs, product names, categories, and prior queries, or product fields only?
- Understanding: Type-ahead is required but source dimensions are undefined.
- Solution: Include product names/SKUs/categories plus recent query terms for better recall.

## 6. Trending search segmentation

- Question: Should trending searches be global, region-specific, role-specific, or all views?
- Understanding: Last-7-day trend is required, but audience segmentation is unspecified.
- Solution: Default to region-level trending with optional global toggle.

## 7. Recommendation placement surfaces

- Question: Where should recommendations appear: search page, home dashboard, product detail, or all?
- Understanding: Recommendation logic is specified, placement is not.
- Solution: Launch on search and home first; add product detail once engagement is validated.

## 8. Recommendation refresh cadence

- Question: Should recommendations update in real-time, near-real-time batch, or nightly?
- Understanding: Offline systems can do either streaming-like or scheduled pipelines.
- Solution: Start with near-real-time (every 5-15 minutes) to balance freshness and operational cost.

## 9. Notification channel boundaries

- Question: Is v1 strictly in-app notifications, or should SMS/email extensibility be planned now?
- Understanding: The prompt defines in-app behavior only.
- Solution: Build in-app now with a pluggable channel abstraction for later SMS/email.

## 10. Check-in window configuration model

- Question: Are check-in windows fixed from shift templates, or dynamic per store/day?
- Understanding: Notification timing depends on schedule source design.
- Solution: Implement template-based defaults with per-store/day overrides.

## 11. Do Not Disturb handling

- Question: During DND, should reminders be queued for later or dropped if stale/non-critical?
- Understanding: DND is defined, suppression semantics are not.
- Solution: Queue actionable notifications and drop expired reminder-only messages.

## 12. Exception approval taxonomy

- Question: What exception request types exist, and which require Ops Manager approval?
- Understanding: Approval outcomes are referenced but workflow types are undefined.
- Solution: Define explicit exception categories with configurable approval routing.

## 13. KPI formula governance

- Question: Can we lock exact formulas for GMV, conversion, AOV, repeat rate, timeliness, and cancellation causes?
- Understanding: Metrics are listed but formula source-of-truth is not.
- Solution: Publish a KPI definition spec and version it in code and report metadata.

## 14. Region hierarchy definition

- Question: What hierarchy is required for filtering (region/city/store/channel)?
- Understanding: Region is in scope but levels are unspecified.
- Solution: Implement region to city to store hierarchy with optional channel cross-cut.

## 15. Export permission granularity

- Question: Should Analysts export only aggregates, or also row-level datasets?
- Understanding: Exports are required but least-privilege level is unclear.
- Solution: Default Analysts to aggregate exports only; row-level export reserved for Admin/Ops unless approved.

## 16. Report package storage policy

- Question: What is the exact local shared-folder path, filename pattern, and retention duration?
- Understanding: Nightly package generation is required but storage policy is missing.
- Solution: Configure path via environment, use timestamped naming, and enforce retention (for example, 90 days).

## 17. Sensitive field catalog

- Question: Which exact fields must be encrypted at rest and masked in the UI?
- Understanding: Employee IDs/contact fields are examples, not a complete inventory.
- Solution: Create a data-classification matrix and implement field-level policies from it.

## 18. Offline key management source

- Question: Where should encryption keys be stored offline: OS keystore, env files, HSM, or internal vault?
- Understanding: Encryption is mandatory, and key custody determines security posture.
- Solution: Prefer OS keystore/HSM; fallback to encrypted env-secret file with rotation policy.

## 19. Audit immutability requirements

- Question: Do we need immutable audit trails for role changes, approvals, templates, and event rules?
- Understanding: Compliance and dispute resolution may require tamper evidence.
- Solution: Implement append-only audit logs with a signed hash chain if immutability is required.

## 20. Compliance framework target

- Question: Are there specific policy frameworks or local regulatory controls to enforce?
- Understanding: Security controls are strong, but formal compliance mapping is not stated.
- Solution: Map controls to your chosen framework early to avoid redesign later.

## 21. High availability and disaster recovery

- Question: Should deployment be single-node or active-passive, and what are RPO/RTO targets?
- Understanding: On-prem environments vary widely in resilience expectations.
- Solution: Start single-node with backup automation, then upgrade to active-passive if RPO/RTO demands it.

## 22. Performance and volume assumptions

- Question: What are expected users/day, peak concurrency, events/day, and orders/day?
- Understanding: Capacity assumptions drive indexing, caching, and scheduler design.
- Solution: Baseline load model now and size schema/index strategy accordingly.

## 23. Browser support baseline

- Question: Must we support only modern corporate browsers or legacy versions too?
- Understanding: UI feature choices depend on browser compatibility floor.
- Solution: Target latest Chrome/Edge unless legacy support is explicitly required.

## 24. Localization requirements

- Question: Is v1 English-only, or do we need multi-language, timezone, and currency localization?
- Understanding: Retail operations may span multiple regions/locales.
- Solution: Build i18n-ready templates now even if only English strings ship in v1.

## 25. Java runtime version

- Question: Should we standardize on Java 21 or use an internal baseline like Java 17?
- Understanding: The prompt specifies Spring Boot, not Java version.
- Solution: Use Java 21 unless internal platform policy requires Java 17.

## 26. Build system choice

- Question: Do we use Maven or Gradle for this program?
- Understanding: Tooling affects CI templates, onboarding, and plugin ecosystem.
- Solution: Default to Maven for enterprise Spring consistency unless your teams standardize on Gradle.

## 27. Database migration mechanism

- Question: Should schema changes be managed with Flyway or another migration system?
- Understanding: Migration tooling is necessary but not mandated in the prompt.
- Solution: Use Flyway for versioned, repeatable on-prem schema evolution.

## 28. Partial-page interaction approach

- Question: For lazy loading and fragment updates, should we use HTMX or vanilla JS only?
- Understanding: Thymeleaf SSR is required; the client interaction layer is open.
- Solution: Use Thymeleaf fragments plus minimal HTMX to reduce JS complexity.

## 29. UI framework selection

- Question: Should styling use Bootstrap, Tailwind, or an internal design system?
- Understanding: UX requirements are clear but the component framework is unspecified.
- Solution: Prefer the internal design system; otherwise use Bootstrap for speed and consistency.

## 30. Password hashing algorithm

- Question: Should we implement Argon2id or BCrypt for salted password hashing?
- Understanding: The prompt requires salted hashing, not a specific algorithm.
- Solution: Use Argon2id where policy permits; fallback to BCrypt with a strong work factor.

## 31. Scheduler architecture

- Question: Should scheduled jobs use Spring `@Scheduled` only or Quartz?
- Understanding: Nightly reports/notifications require robust scheduling.
- Solution: Start with `@Scheduled`; adopt Quartz if persistent job control/recovery is needed.

## 32. XLSX library standard

- Question: Which library should generate XLSX exports?
- Understanding: XLSX output is required, and library choice is open.
- Solution: Use Apache POI as the default enterprise-safe option.

## 33. Log format standardization

- Question: Should application logs be structured JSON or plain text?
- Understanding: The Health screen needs machine-readable local monitoring data.
- Solution: Use structured JSON logs for reliable parsing and threshold detection.

## 34. Health metric persistence model

- Question: Should health counters/alerts be stored in MySQL tables or file-based summaries?
- Understanding: The internal Health UI requires queryable historical metrics.
- Solution: Store aggregated health metrics in MySQL for dashboarding and retention controls.

## 35. Packaging and runtime model

- Question: Do we deploy as JAR/system service or containerized with Docker Compose?
- Understanding: On-prem operations maturity varies by organization.
- Solution: Provide both artifacts; prefer Compose if the ops team already supports containers.

## 36. Reverse proxy requirement

- Question: Should Nginx front Spring Boot, or should the app serve directly on LAN?
- Understanding: TLS termination, static caching, and header controls may need a proxy layer.
- Solution: Use Nginx when TLS/header controls are needed; otherwise direct deployment is acceptable.

## 37. Encryption layering strategy

- Question: Should we rely on app-level field encryption only, or combine with MySQL-at-rest encryption?
- Understanding: The prompt requires encrypted sensitive data at rest, not exact layering.
- Solution: Implement app-level field encryption as the mandatory baseline; add DB-at-rest encryption where available.

## 38. Integration test database strategy

- Question: Should integration tests use Testcontainers (ephemeral MySQL) or a shared static DB?
- Understanding: Test isolation and reproducibility depend on DB strategy.
- Solution: Use Testcontainers for deterministic, environment-independent integration tests.

## 39. End-to-end test scope

- Question: Is MockMvc-level API/UI fragment testing enough, or do we add browser E2E tests?
- Understanding: SSR plus JS interactions may need real-browser validation.
- Solution: Start with MockMvc plus service tests; add targeted E2E for critical workflows.

## 40. Offline dependency governance

- Question: How will dependencies be approved, mirrored, and updated in an internet-restricted environment?
- Understanding: Offline/on-prem delivery needs controlled artifact sourcing.
- Solution: Establish an internal artifact mirror/repository and a signed dependency approval process.
