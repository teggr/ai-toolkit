# Screaming Architecture Workflow

This guide operationalizes feature-first architecture so package structure "screams" domain intent.

## Article Anchors

- Screaming Architecture: prioritize use-case readability; treat frameworks, web, and databases as replaceable details.
- Whoops! Where did my architecture go: model vertical business slices explicitly in packages and keep cross-slice/public surface small to control dependency risk.

## Outcome

Default output:

- Quick architecture checklist (findings + top refactors).
- Candidate package tree (feature-first target).
- Screaming Scorecard (overall score + per-criterion scores).

Optional expanded output when requested:

- Architecture review report with findings and prioritized fixes.
- Migration plan from layered packaging to feature-first slices.

## Procedure

### 1) Build a capability map

List product capabilities in business terms.

Good examples:

- orders
- billing
- customer-profile
- onboarding

Weak examples:

- controllers
- services
- repositories
- utils

### 2) Assess current structure

Classify top-level packages:

- Feature-centric (good): names map to capabilities.
- Technical-layer-centric (risk): names map to implementation layers.
- Mixed (common): some features, some horizontal technical buckets.

### 3) Define target layout

Prefer a shape like:

- feature-a/
- feature-b/
- feature-c/
- shared-kernel/ (utility-only)
- platform/ (framework integration, thin adapters)

Inside each feature slice, allow local layering only as needed (api, application, domain, infrastructure).

Balanced policy: allow top-level platform/integration modules when required, but keep them thin so features remain the primary top-level signal.

See the **Example Layouts** section below for concrete before/after package trees.

### 4) Validate dependency rules

Check these invariants:

- Domain/application policy is not dependent on framework glue.
- Cross-feature calls occur through explicit interfaces/events.
- Shared-kernel contents are limited to utility functions/classes only.

### 5) Apply branching guidance

#### Case A: Pure layered structure exists

Actions:

1. Create feature slices based on capabilities.
2. Move use-case logic into feature application/domain packages.
3. Keep existing technical layers as temporary facades while migrating.
4. Remove obsolete horizontal layer packages.

#### Case B: Mixed structure exists

Actions:

1. Keep already healthy feature slices.
2. Migrate only ambiguous/shared code.
3. Split large shared packages by ownership and volatility.
4. Move non-utility shared behavior back into owning features.

#### Case C: Framework forces certain layout

Actions:

1. Keep required framework folders minimal.
2. Delegate to feature modules immediately.
3. Restrict framework annotations and wiring to edge adapters.

### 6) Produce actionable output

Always include:

- Keep/Rename/Move/Split/Merge list.
- Before/After package tree (condensed).
- Dependency risk notes.
- Phased migration plan (small batches).
- Suggested architecture tests/linters to prevent regression.

## Quality Checks

Use this checklist before final answer:

- Can a new teammate infer product purpose from top-level package names?
- Is each major feature clearly owned by a module/slice?
- Are technical concerns nested under features rather than owning top-level namespace?
- Is shared-kernel strictly utility-only?
- Is migration sequencing explicit and low risk?

## Screaming Scorecard

Rate each criterion from 0 to 5, then apply weight.

- Feature visibility in top-level packages, weight 30%
	- 0: top-level is mostly technical layers.
	- 3: mixed, feature intent visible but diluted.
	- 5: top-level clearly communicates business capabilities.
- Framework isolation at boundaries, weight 20%
	- 0: framework concerns dominate core packages.
	- 3: partial isolation with frequent leakage.
	- 5: framework/web/db concerns are thin edge adapters.
- Slice autonomy and dependency direction, weight 25%
	- 0: heavy cross-slice coupling and cycles.
	- 3: mostly directional but with notable violations.
	- 5: clear inward dependency flow and controlled cross-slice access.
- Shared-kernel purity (utility-only), weight 15%
	- 0: shared-kernel contains business rules/use-case orchestration.
	- 3: mostly utility but a few policy leaks.
	- 5: strictly utility functions/classes only.
- Public surface minimization per slice, weight 10%
	- 0: many public internals exposed by default.
	- 3: some visibility control, inconsistent boundaries.
	- 5: small explicit public slice APIs, internals hidden.

Formula:

- Overall score = sum((criterion_score / 5) * criterion_weight)
- Report as percentage from 0 to 100.

Interpretation bands:

- 85-100: architecture clearly screams domain intent.
- 70-84: good direction, targeted refactors needed.
- 50-69: mixed architecture, significant restructuring advised.
- 0-49: framework/layer-first architecture, high refactor priority.

### Scorecard Template

Use this structure in review output.

```markdown
## Screaming Scorecard

| Criterion | Weight | Score (0-5) | Weighted Points | Notes |
|---|---:|---:|---:|---|
| Feature visibility in top-level packages | 30% | X | Y | Short evidence |
| Framework isolation at boundaries | 20% | X | Y | Short evidence |
| Slice autonomy and dependency direction | 25% | X | Y | Short evidence |
| Shared-kernel purity (utility-only) | 15% | X | Y | Short evidence |
| Public surface minimization per slice | 10% | X | Y | Short evidence |
| **Total** | **100%** |  | **ZZ / 100** |  |

Overall Band: {85-100 | 70-84 | 50-69 | 0-49}

Top Deductions:
1. {Biggest deduction and why}
2. {Second deduction and why}
3. {Third deduction and why}
```

Calculation notes:

- Weighted Points per criterion = (Score / 5) * Weight
- Example: score 4 with weight 30 gives 24 points.

## Example Layouts

Use these concrete examples when producing or reviewing package trees.

### Example 1: Drotbohm CRM — before and after (Java)

Source: https://odrotbohm.de/2013/01/whoops-where-did-my-architecture-go/

**Before — slice-then-layer naming (layer packages force internal types public)**

```
com.example.crm.account.domain
com.example.crm.account.presentation
com.example.crm.account.repository
com.example.crm.account.service
com.example.crm.core.domain
com.example.crm.customer.domain
com.example.crm.customer.presentation
com.example.crm.customer.service
com.example.crm.customer.repository
```

Problem: `CustomerRepository` must be `public` so the service layer can reference it, making it an accidental injection candidate everywhere in the codebase.

**After — flat slice packages with visibility control**

Legend: `+` = public, `o` = package-private.

```
com.example.crm.customer
  + Customer
  + CustomerManagement
  o CustomerManagementImpl
  o CustomerNumberGenerator
  o CustomerRepository
com.example.crm.account
  + Account
  + AccountManagement
  o AccountManagementImpl
  o AccountRepository
com.example.crm.core
  + Money
  + Address
```

Result: fewer than half the types need to be public. `CustomerRepository` is hidden from the rest of the codebase by the Java compiler without any additional tooling.

---

### Example 2: Maven Spring Boot web app — before and after

**Before — technical-layer-first (antipattern)**

```
src/
└── main/
    ├── java/com/example/webapp/
    │   ├── controller/
    │   │   ├── CustomerController.java
    │   │   ├── OrderController.java
    │   │   └── ProductController.java
    │   ├── service/
    │   │   ├── CustomerService.java
    │   │   ├── CustomerServiceImpl.java
    │   │   └── OrderService.java
    │   ├── repository/
    │   │   ├── CustomerRepository.java
    │   │   ├── OrderRepository.java
    │   │   └── ProductRepository.java
    │   ├── model/
    │   │   ├── Customer.java
    │   │   ├── Order.java
    │   │   └── Product.java
    │   └── dto/
    │       ├── CustomerDto.java
    │       └── OrderDto.java
    └── resources/
        ├── templates/
        │   ├── customer.html
        │   └── order.html
        └── application.properties
```

**After — feature-first with package-private internals**

Legend: `+` = public, `-` = package-private.

```
src/
└── main/
    ├── java/com/example/webapp/
    │   ├── customer/
    │   │   + Customer.java            (domain object — public)
    │   │   + CustomerManagement.java  (slice API interface — public)
    │   │   - CustomerController.java  (package-private)
    │   │   - CustomerService.java     (package-private)
    │   │   - CustomerRepository.java  (package-private)
    │   ├── orders/
    │   │   + Order.java               (domain object — public)
    │   │   + OrderManagement.java     (slice API interface — public)
    │   │   - OrderController.java     (package-private)
    │   │   - OrderService.java        (package-private)
    │   │   - OrderRepository.java     (package-private)
    │   ├── catalog/
    │   │   + Product.java             (domain object — public)
    │   │   + CatalogBrowser.java      (slice API interface — public)
    │   │   - ProductRepository.java   (package-private)
    │   ├── shared/                    (utility-only — no business logic)
    │   │   + Money.java
    │   │   + PageResult.java
    │   └── platform/                  (thin Spring wiring — no business logic)
    │       - WebMvcConfig.java
    │       - SecurityConfig.java
    │       - PersistenceConfig.java
    └── resources/
        ├── templates/                 (mirrors feature split)
        │   ├── customer/
        │   │   ├── list.html
        │   │   └── detail.html
        │   ├── orders/
        │   │   ├── list.html
        │   │   └── checkout.html
        │   └── catalog/
        │       └── product-list.html
        └── application.properties
```

Key decisions:
- Each feature slice exposes only its domain object and a slim interface; all controllers, services, and repositories are package-private.
- `shared/` is strictly utility (value types, pagination helpers). Move anything with business rules back into its owning feature.
- `platform/` holds only Spring configuration and thin adapters. It has no business logic.
- Template directories mirror the Java package split so a developer always knows where to look.

---

### Example 3: Growing a slice — internal sub-packaging

When a single feature slice becomes large (20+ classes), introduce internal sub-packages. Keep the public API surface at the slice root and hide sub-packages from outside callers.

```
com.example.webapp.orders/
  + Order.java                    (public domain object)
  + OrderManagement.java          (public slice API)
  application/
    - PlaceOrderUseCase.java      (package-private)
    - CancelOrderUseCase.java     (package-private)
  domain/
    - OrderLine.java              (package-private)
    - OrderStatus.java            (package-private)
    - PricingPolicy.java          (package-private)
  infrastructure/
    - JpaOrderRepository.java     (package-private)
    - OrderEmailNotifier.java     (package-private)
```

Rules for this pattern:
- Sub-packages (`application/`, `domain/`, `infrastructure/`) are an internal implementation detail of the slice only.
- Other slices still import only from `com.example.webapp.orders` (the root), never from sub-packages.
- Introduce sub-packages only when the flat slice becomes hard to navigate, not by default.

## Common Anti-patterns

- Top-level namespaces: controller/service/repository.
- Single massive shared/common package.
- Shared-kernel containing business policy or use-case orchestration.
- Cross-feature shortcuts through utility classes.
- Domain model coupled directly to framework annotations or transport models.

## Suggested Outputs

### Quick Review Mode

- 5-10 findings ranked by impact.
- 3 highest-priority refactors.
- Candidate package tree sketch.
- Scorecard table with overall score and top deduction reasons.

### Design Mode

- Proposed feature map.
- Proposed package tree.
- Dependency rules and ownership boundaries.

### Migration Mode

- Stepwise move plan.
- Risk controls (compatibility facades, test gates, rollback points).

## Source Material

- https://odrotbohm.de/2013/01/whoops-where-did-my-architecture-go/
- https://blog.cleancoder.com/uncle-bob/2011/09/30/Screaming-Architecture.html