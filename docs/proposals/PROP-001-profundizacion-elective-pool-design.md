# Design: Profundización as a Pool of Distinct Interchangeable Electives

**Change**: PROP-001-profundizacion-elective-pool
**Status**: Design (technical). No code, no migrations, no DB changes produced here.
**Source proposal**: `PROP-001-profundizacion-elective-pool.md` (Accepted). Chosen Approach = Option (i): per-elective identity + pool requirement.
**Mode**: STRICT TDD. Every behavior change below names its RED test(s) and layer.

---

## 0. CORRECTION (2026-07-22, authoritative — supersedes D3's gatekeeper handling)

Product-owner rulings + the official source `01 - Propuesta Profundizaciones o electivas 2026-2.pdf` (in repo root) refine the model:

- **Ordering**: DROPPED (confirmed). The slot ordinal self-satisfies under interchangeability; operative rule = **4 distinct, max 4**. Still ARCHIVE the slot→slot edges (III←I, IV←II). ✔ unchanged from D3.
- **Entry prerequisite (CORRECTS D3 / R-gatekeeper)**: electives are interchangeable across SLOTS, but **each elective keeps ITS LINE's prerequisite**. Do NOT null the gatekeepers and leave electives edge-free. Instead **relocate each line's gatekeeper onto its electives** as one `COURSE_PASSED` `course_requirement` edge per elective. The existing `PrerequisiteEvaluator` handles `COURSE_PASSED` with **no code change** — only different data. REQ-2 therefore means "interchangeable across slots", NOT "no prerequisite".

**Official elective → line → prerequisite (plan nuevo; Diego is on Plan 21/Reforma 2016 = plan nuevo):**

| Elective | Line (Área) | Prerequisite edge (COURSE_PASSED) |
|---|---|---|
| Diseño Centrado en el Usuario | Ing. del Software y HCI | Ingeniería de Software I (`ISC-S5-INGENIERIADESOFTWAREI`) |
| Desarrollo de Aplicaciones para Dispositivos Móviles | Ing. del Software y HCI | Ingeniería de Software I |
| Usabilidad y Experiencia de Usuario | Ing. del Software y HCI | Ingeniería de Software I |
| Computación en la Nube | Infraestructura de T.I. | Administración de Infraestructura de T.I. (`ISC-S7-ADMINISTRACIONDEINFRAESTRUCTURADETI`) |
| Arquitectura Orientada a Microservicios | Infraestructura de T.I. | Administración de Infraestructura de T.I. |
| Visualización de Datos | Gestión de la Información | Bases de Datos I (`ISC-S6-BASESDEDATOSI`) |
| Visión Artificial | Gestión de la Información | Bases de Datos I |
| Introducción al Big Data | Gestión de la Información | Bases de Datos I |

**Migration impact (updates §5/§9 V028)**: V028 additionally inserts 8 `COURSE_PASSED` `course_requirement` edges (elective → line gatekeeper), and NULLs the 3 V014 track gatekeepers (logic relocated, not removed). **Test impact**: REQ-2 RED test becomes "elective accepted when its line prereq is met / blocked (SIMULATION_PREREQUISITE_NOT_MET-equivalent) when not"; add per-elective prereq assertions. Everything else in this doc stands.

---

## 1. Technical Approach (summary)

Give each concrete profundización elective its own `curriculum.course` identity (`type = DEEPENING`, stable slug key). Re-point every `offering.course_offering.course_id` from the four shared slot ids to the specific elective id. Because the simulation candidate's `courseId` is taken verbatim from the offering (`OfferingReadAdapter.resolveScheduleOption().courseId()`), and because `DuplicateSelectionRule`/`AlreadyPassedRule` and progress grouping already key on `courseId`, **the two false-block bugs and the progress-collapse bug disappear with no domain-rule code change** — they were symptoms of the collapsed id. The only genuinely new logic is the thesis gate + distinctness count on the progress side and the malla "4 numbered slots" display mapping.

Key verified facts that drive the design:
- `simulation.simulation_selection` already persists `course_name` per row (root seed `seed-diego-simulacros-2026-2.sql:114`); candidate `courseId` flows from the offering (`OfferingReadAdapter.java:57-58`).
- `progress.course_attempt` has **no** `course_name`/`offering_id` column (`V015__create_progress_tables.sql:33-61`); OpenAPI `CourseAttempt.courseName` is **required** (`brujula-api.v1.yaml:2843,2854`) → it is resolved from `curriculum.course.name` at read time. Filing attempts under the real elective id gives the concrete name for free.
- `MallaRequirement.type` already includes `CONFIGURABLE` and `simultaneityAllowed` (`brujula-api.v1.yaml:3468-3487`); the malla channel (`PlanMallaView.RequirementSummary`) already carries requirement edges into progress `CourseAvailabilityService`.
- `RequirementType.CONFIGURABLE` is reserved and **skipped** by both evaluators with a TODO (`PrerequisiteEvaluator.java:38`, `CourseAvailabilityService.java:88-92`).

---

## 2. Architecture Decisions (ADRs)

### D1 — Requirement-model mechanism (the pool)

**Choice**: Model the four requirements as a **pool on `curriculum.course_requirement`, reusing the reserved `CONFIGURABLE` type**. Pool membership = the set of `ACTIVE`, `DEEPENING` elective courses in the plan. The thesis gate becomes **one** `CONFIGURABLE` requirement row on Trabajo de Grado meaning "≥ 4 distinct passed pool members" (count carried in the existing `required_credits` column, reinterpreted as "distinct-completions required"). No slot→slot edges.

| Option | Integrates prereq eval? | Integrates progress counting? | New cross-module plumbing | Verdict |
|--------|-------------------------|-------------------------------|---------------------------|---------|
| (a) `sequential_track_level_option` for 4 levels × full menu | Yes (sim reads tracks in `CurriculumReadAdapter.sequentialPredecessor`) | **No** — progress never reads tracks | New progress→curriculum track port + DTO + mapping | Rejected: most new code on the progress side; interchangeability makes the 3 per-line gatekeepers ambiguous |
| **(b) `CONFIGURABLE` pool rule on `course_requirement`** | Yes — `requirementsForCourse`→`PrereqRequirement` (skips CONFIGURABLE = no false block) | **Yes** — rides `PlanMallaView.requirements`→`CourseRequirementEdge`→`CourseAvailabilityService` | **None** (existing channel); one new eval branch | **Chosen** |
| (c) Dedicated elective-group table + aggregate | Requires new ports both sides | Requires new ports both sides | New table, aggregate, 2 ports | Rejected: over-engineered; `type=DEEPENING`+plan already identifies the pool |

**Rationale**: `course_requirement` is the *only* curriculum read path already consumed by **both** subsystems. The thesis gate inherently needs pool-count semantics in `CourseAvailabilityService`, whose skip-branch (`:88-92`) is the natural home. Simulation needs no evaluator change because distinctness there is already `DuplicateSelectionRule`'s job on real ids. This is the least-special-casing option and the one the proposal foreshadows ("repurposing the reserved CONFIGURABLE").

### D2 — Elective identity & slot fate

**Choice**: Create **8 distinct `DEEPENING` courses** (see §3), each with a stable slug natural key derived from the accent-folded Spanish name with the modality suffix `(Sincrónico)`/`(Asincrónico)` and the `Profundización`/`Profundización -` prefix stripped (resolved decision #1 & #3 → variants collapse to one course). The 8 electives get **no `course_recommended_semester` rows**, so the malla assembler (which groups only courses that have a semester row for the journey) does not render them as extra grid cells. The **four slot courses are kept as ACTIVE display anchors** (they retain their sem-8/9 semester rows so the malla still shows "Profundización I–IV"), but are **removed from the pool** (no offerings point to them; they carry no attempts; they are excluded from the pool membership query by id).

**Alternatives**: retire the slot courses (ARCHIVE) and synthesize four numbered slots in the malla assembler — rejected as more display code for no data benefit; keeping them ACTIVE reuses the existing malla assembler unchanged and honors resolved decision #2.

### D3 — Ordering with simultaneity, and PrerequisiteEvaluator

**Choice**: **Drop** the strict slot→slot `COURSE_PASSED` edges `Prof III ← Prof I` and `Prof IV ← Prof II` (`V012:733-738`) by setting `status = ARCHIVED`. Under full interchangeability + simultaneity + uniform 3-credit electives, the ordinal I→IV rule collapses to **distinctness + a max-of-4 count**: within a single-period simulation everything is "same period" (simultaneity always satisfied), so no hard ordinal block is needed. `PrerequisiteEvaluator` and `SequentialTrackEvaluator` need **no code change**; the per-line gatekeeper (`sequential_track.gatekeeper_course_id`, V014) is **relaxed to NULL** for profundización because line identity dissolves under interchangeability.

**REQ-4 note (flagged assumption A1)**: the "later-in-earlier-period is blocked" acceptance scenario is *vacuous* under full interchangeability (no elective is intrinsically "III"). The simultaneity clause is the binding one and is guaranteed by dropping the edges. Confirm with product owner before implementation.

### D4 — Distinctness

- **Simulation**: `DuplicateSelectionRule` (`:28-29`) is already correct on real ids — no change. Add RED cases proving REQ-3 with real elective ids.
- **Progress**: distinctness = count of distinct `PASSED` pool-member `courseId`s. New branch in `CourseAvailabilityService` (thesis gate) + malla slot mapping.

### D5 — Progress traceability

**Choice**: **No new column** on `course_attempt`. The concrete elective *is* the `course_id`; `courseName` resolves from `curriculum.course.name`; the malla renders "Profundización I: PASSED — <elective>" by mapping the k-th passed pool elective (ordered by `period_id`/`registered_at`) onto display slot k in the progress curriculum service. Rejected: adding `elective_course_id`/`course_name`/`offering_id` — redundant once identity lives in `course_id` (resolved decision #2).

### D6 — Offering re-point & existing simulacros

**Choice**: Corrective forward migration UPDATEs `offering.course_offering.course_id` from the shared slot ids to the specific elective id, matched by a **normalized `course_name`** (strip prefix/suffix, collapse modality variants) → elective-id mapping (CASE/mapping CTE). Existing 2026-2 `simulation_selection` rows (the re-seeded simulacros) are remapped best-effort by the same normalized `course_name`→elective mapping (they already store `course_name`); a "legacy/unknown" fallback covers any unmatched row. Diego has **zero** `course_attempt` rows → no attempt backfill. The root seed `seed-diego-simulacros-2026-2.sql` auto-adapts (it inserts `co.course_id`, now the elective, and joins credits via `cr.id = co.course_id`); it must be **re-run/verified** (its 32-selection assertion still holds).

---

## 3. Concrete Elective Set (from V022, normalized)

Eight distinct electives (modality/format variants collapsed per decision #1). Proposed `official_code = ISC-DEEP-{SLUG}`, `type=DEEPENING`, `credits=3`, plan `33333333-…-001`.

| # | Normalized name | Slug natural key | V022 offerings collapsed |
|---|-----------------|------------------|--------------------------|
| 1 | Diseño Centrado en el Usuario | `profundizacion-diseno-centrado-en-el-usuario` | 02200091, 02200175 |
| 2 | Introducción al Big Data | `profundizacion-introduccion-al-big-data` | 02200087, 02200171 |
| 3 | Visualización de Datos | `profundizacion-visualizacion-de-datos` | 02200092, 02200172 |
| 4 | Computación en la Nube | `profundizacion-computacion-en-la-nube` | 02200085, 02200086(Sinc), 02200173, 02200174(Sinc) |
| 5 | Arquitectura Orientada a Microservicios | `profundizacion-arquitectura-orientada-a-microservicios` | 02200097(Sinc), 02200183 |
| 6 | Visión Artificial | `profundizacion-vision-artificial` | 02200098, 02200180 |
| 7 | Desarrollo de Aplicaciones para Dispositivos Móviles | `profundizacion-desarrollo-de-aplicaciones-para-dispositivos-moviles` | 02200100, 02200182 |
| 8 | Usabilidad y Experiencia de Usuario | `profundizacion-usabilidad-y-experiencia-de-usuario` | 02200101, 02200179 |

**Residual risk R-set (verify)**: V022 has ~1663 lines; the enumeration above is grep-confirmed for the shared slot ids `…c0080000-…004` and `…c0090000-…004`. Prof II (`…005`) and Prof IV (`…005`) have **zero** offerings (confirmed) — after re-point every elective is reachable, so REQ-7 (Prof II/IV no longer empty) is satisfied structurally.

---

## 4. Data Flow

```
Offering (course_offering.course_id → ELECTIVE id)
      │  OfferingReadAdapter.resolveScheduleOption().courseId
      ▼
Simulation CandidateSelection.courseId = ELECTIVE id
      ├─ DuplicateSelectionRule (distinct real ids)      → REQ-3 ✓ (no code change)
      ├─ AlreadyPassedRule (real ids)                    → REQ-5 ✓ (no code change)
      └─ PrerequisiteEvaluator (elective has no edges)   → interchangeable ✓

Progress course_attempt.course_id = ELECTIVE id
      │  GetStudentCourseHistoryService groups by courseId (distinct)
      ▼
CourseAvailabilityService
      ├─ pool members = ACTIVE DEEPENING courses in plan (minus 4 slot ids)
      ├─ CONFIGURABLE gate on Trabajo de Grado: distinct PASSED pool ≥ 4 → AVAILABLE  → REQ-8
      └─ malla mapping: k-th passed pool elective → display slot Prof k              → REQ-6
```

---

## 5. Per-Module Change List

| Module / File | Action | Purpose | Test (layer) |
|---|---|---|---|
| `db/migration/V028__…curriculum_profundizacion_pool.sql` | Create | 8 DEEPENING elective courses (idempotent on official_code/slug); ARCHIVE slot→slot edges (III←I, IV←II) and thesis slot edges (`V012:733-746`); add `CONFIGURABLE` pool gate on Trabajo de Grado (`required_credits=4`); NULL the 3 track gatekeepers | Migration/repo IT (adapter) |
| `db/migration/V029__…repoint_2026_2_profundizacion_offerings.sql` | Create | Re-point `course_offering.course_id` shared slot → elective id via normalized name map; idempotent | Offering read IT (adapter) |
| `db/migration/V030__…backfill_simulation_selection_electives.sql` | Create | Best-effort remap existing `simulation_selection.course_id` by `course_name`; legacy fallback | Data-migration IT (adapter) |
| `progress/domain/CourseAvailabilityService.java` | Modify | New branch: evaluate `CONFIGURABLE` pool gate (count distinct PASSED pool members ≥ threshold) instead of silently skipping (`:88-92`) | `CourseAvailabilityServiceTest` (domain unit) |
| `progress/application/service/GetSubscriptionCurriculumService.java` | Modify | Compute pool set; map k-th passed pool elective → numbered display slot; expose filled-elective name | service IT (application) |
| `simulation/domain/DuplicateSelectionRule.java` | **No change** | Correct on real ids | new cases in `DuplicateSelectionRuleTest` (domain unit) |
| `simulation/domain/AlreadyPassedRule.java` | **No change** | Correct on real ids | new cases in `AlreadyPassedRuleTest` (domain unit) |
| `simulation/domain/PrerequisiteEvaluator.java` / `SequentialTrackEvaluator.java` | **No change** | Edges dropped in data; CONFIGURABLE already skipped | `SequentialTrackEvaluatorTest` unchanged; `CurriculumReadAdapterIT` updated for NULL gatekeeper |
| `openapi/brujula-api.v1.yaml` | Modify (optional/minor) | Document pool semantics; optionally add nullable `filledElectiveName` to `MallaCourse` if display is server-driven | contract/OpenAPI lint |
| `seed-diego-simulacros-2026-2.sql` (repo root) | Verify/re-run | Auto-adapts to elective ids; confirm 32-selection assertion | manual/seed run |

---

## 6. Interfaces / Contracts

- **`CourseRequirement` pool gate** (data only): `type=CONFIGURABLE`, `target_course_id = Trabajo de Grado`, `required_course_id = NULL`, `required_credits = 4` (reinterpreted "distinct completions required"). No factory change needed — `CourseRequirement.configurable(...)` exists (`:155`), but a `required_credits`-carrying CONFIGURABLE variant may need a small factory/reconstitute tweak (flag F1).
- **Pool membership** delivered to progress through the existing `PlanMallaView.CourseSummary` (type=DEEPENING) — no DTO change.
- **OpenAPI**: `CourseAttempt` unchanged (`courseName` already resolved from curriculum). `MallaRequirement`/`RequirementType` already support `CONFIGURABLE` — no schema change required for the gate.

---

## 7. Testing Strategy (STRICT TDD — RED first, per requirement)

| REQ | RED test(s) written first | Layer |
|-----|---------------------------|-------|
| REQ-1 distinct identity | migration/repo IT: 8 elective `DEEPENING` courses exist with stable slugs | adapter integration |
| REQ-2 interchangeable | add-selection IT: an elective is accepted regardless of "slot"; elective has no blocking COURSE_PASSED edge | application integration + domain |
| REQ-3 distinctness (sim) | `DuplicateSelectionRuleTest`: two distinct electives (real ids) → allowed; same elective twice → blocked | domain unit |
| REQ-4 ordinal+simultaneity | add two electives in one simulation → both accepted; migration test: slot→slot edges ARCHIVED (A1 vacuous case documented) | application integration + migration |
| REQ-5 already-passed by elective | `AlreadyPassedRuleTest`: passed A, select distinct B → allowed; select A → blocked | domain unit |
| REQ-6 traceability & count | `GetSubscriptionCurriculumService`/`GetStudentCourseHistoryService` IT: 4 distinct passed → 4 named entries, no collapse; slot k shows filled elective | application integration |
| REQ-7 offering availability | offering read IT: every elective offering resolves to a distinct elective id; Prof II/IV reachable | adapter integration |
| REQ-8 thesis gate | `CourseAvailabilityServiceTest`: TdG BLOCKED with <4 distinct passed pool; AVAILABLE with ≥4 | domain unit |

**Existing tests to update**: `CurriculumReadAdapterIT` (NULL gatekeeper / archived edges); progress service tests (pool electives, malla slot mapping); any migration/repo test asserting the ISC course count (57 → 65). `DuplicateSelectionRuleTest`/`AlreadyPassedRuleTest`/`SequentialTrackEvaluatorTest` gain cases but existing assertions remain green.

---

## 8. Threat Matrix

**N/A** — this change touches no routing, shell command, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. It is a data-model + domain-logic + Flyway-migration change only.

---

## 9. Migration / Rollout (Flyway, forward-only, V028+)

1. **V028** — curriculum: insert 8 electives (idempotent `WHERE NOT EXISTS` on slug/official_code); ARCHIVE the 4 slot→slot + 2 thesis slot edges; add CONFIGURABLE pool gate on Trabajo de Grado; NULL track gatekeepers.
2. **V029** — offering: re-point 2026-2 `course_offering.course_id` to elective ids via normalized-name mapping (idempotent, keyed on offering id).
3. **V030** — simulation: best-effort remap `simulation_selection.course_id` by `course_name`; legacy fallback.
   No attempt backfill (Diego has zero rows). V029+V030 may be combined if review budget allows.

---

## 10. PR Chain & Review-Budget Forecast (400-line guard)

| PR | Scope | Est. authored ± | 400-line risk | Note |
|----|-------|------------------|---------------|------|
| **PR1** | Curriculum + offering data (V028+V029) + migration/repo tests + seed verify | 300–450 | **High** | Data-heavy (8 inserts + edge archives + pool gate + name-normalization re-point). **Recommend split**: PR1a = V028 curriculum; PR1b = V029 offering re-point + V030 backfill + seed |
| **PR2** | Domain/progress: `CourseAvailabilityService` pool branch + malla slot mapping + domain/app tests | 200–320 | Medium | Lens: `review-reliability` (behavior/state/determinism). Under budget |
| **PR3** | Contract/frontend/backfill residue: OpenAPI doc + optional `filledElectiveName` + generated client + residual data checks | 120–260 | Low | Lens: `review-readability`/`review-risk` if client regen large |

`Decision needed before apply: Yes` (PR1 split). `Chained PRs recommended: Yes`. `400-line budget risk: High` (PR1 only).

---

## 11. Open Questions / Residual Risks

- **A1 (REQ-4)**: Under full interchangeability the "later-in-earlier-period is blocked" scenario is unreachable; simultaneity is the binding clause. Confirm the product owner accepts distinctness+max-4 as the operative rule.
- **F1**: `CourseRequirement.configurable(...)` currently ignores `required_credits`; a minimal factory/reconstitute tweak is needed to carry the pool threshold (or store the threshold via a documented convention). Confirm in sdd-tasks.
- **R-gatekeeper**: Relaxing the 3 per-line gatekeepers to NULL removes the `SIMULATION_SEQUENTIAL_ORDER` block for profundización. Confirm this is intended (interchangeability implies it).
- **R-attempt-validation**: Confirm `course_attempt` creation accepts pool-elective ids that have no `course_recommended_semester` row (they are plan courses, but attempt-creation validation should be checked).
- **R-offering-map**: The V022 normalization must handle every name variant (`Profundización`, `Profundización -`, `(Sincrónico)`); an unmatched offering would keep the shared id — the migration must assert full coverage or fail loudly.
```
