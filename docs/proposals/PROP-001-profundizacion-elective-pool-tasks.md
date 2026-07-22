# Tasks: Profundización as a Pool of Distinct Interchangeable Electives

**Change**: PROP-001-profundizacion-elective-pool
**Mode**: STRICT TDD — every behavior task is RED (failing test) → GREEN (implementation) → formatting/lint stays green (`spotlessApply`/`spotlessCheck`).
No code, no migrations produced here — checklist only.

---

## Review Workload Forecast

| Field | Value |
|---|---|
| Estimated changed lines | PR1a ~200–280, PR1b ~150–220, PR2 ~200–320, PR3 ~120–260 (higher if client regen is large) |
| 400-line budget risk | Medium overall (PR1a is closest to budget; none should exceed 400 if split holds) |
| Chained PRs recommended | Yes |
| Suggested split | PR1a → PR1b → PR2 → PR3 (feature-branch chain) |
| Delivery strategy | ask-on-risk (default; not overridden in this session) |
| Chain strategy | feature-branch-chain (already committed in proposal §6 / design §10) |

```text
Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: Medium
```

`Decision needed: Yes` because delivery strategy defaults to `ask-on-risk` — confirm the PR1a/PR1b split and the feature-branch-chain base sequence before `sdd-apply` starts.

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|---|---|---|---|---|---|
| 1 | 8 DEEPENING electives + pool CONFIGURABLE gate + 8 per-elective prereq edges + archived ordering/thesis edges | PR1a | `./gradlew test --tests "*CurriculumReadAdapterIT*"` | Testcontainers Postgres, real Flyway run of V028 | Revert `V028__…sql` only; nothing downstream yet depends on it |
| 2 | Offering re-point (V029) + selection backfill (V030) + seed verify | PR1b | `./gradlew test --tests "*OfferingRepointV029IT*" --tests "*SimulationSelectionBackfillV030IT*"` | Re-run root seed `seed-diego-simulacros-2026-2.sql` against local DB (real scenario) | Revert V029+V030 only; V028 course rows stay intact |
| 3 | Progress pool gate + malla slot mapping + F1 tweak + domain-rule regression tests | PR2 | `./gradlew test --tests "*CourseAvailabilityServiceTest*" --tests "*GetSubscriptionCurriculumService*" --tests "*DuplicateSelectionRuleTest*" --tests "*AlreadyPassedRuleTest*"` | Manual check of Diego's "Resumen de avance" once ≥4 electives PASSED, or seeded fixture | Revert `CourseAvailabilityService`/`GetSubscriptionCurriculumService`/`CourseRequirement` only; migrations remain valid independently |
| 4 | OpenAPI pool semantics + optional `filledElectiveName` + client regen + residual sweep | PR3 | Repo's OpenAPI generate/validate task + `./gradlew test` (full suite) | N/A — additive/nullable contract field, no new runtime path beyond PR2's IT | Revert yaml + generated client only; server behavior unaffected |

---

## PR1a — Curriculum Migration (V028)

Base: `feature/PROP-001-profundizacion-elective-pool` (tracker branch)

- [ ] 1.1 RED (`CurriculumReadAdapterIT`, new case): assert 8 DEEPENING elective courses exist with stable slugs (design §3 table) → GREEN: `V028__curriculum_profundizacion_pool.sql`, idempotent `WHERE NOT EXISTS` insert of the 8 electives (type=DEEPENING, credits=3, plan `33333333-…-001`). [REQ-1, design §3/§5]
- [ ] 1.2 RED (`CurriculumReadAdapterIT`): assert slot→slot edges (Prof III←I, IV←II) + 2 thesis-slot edges are `status=ARCHIVED` → GREEN: same V028 sets those 6 rows to ARCHIVED (`V012:733-746`). [REQ-4, design D3]
- [ ] 1.3 RED (`CurriculumReadAdapterIT`): assert one `CONFIGURABLE` `course_requirement` row on Trabajo de Grado, `required_credits=4`, `required_course_id=NULL` → GREEN: same V028 inserts the pool-gate row. [REQ-8, design D1]
- [ ] 1.4 RED (`CurriculumReadAdapterIT`): assert 8 `COURSE_PASSED` edges (elective → line gatekeeper per design §0 table) exist, and the 3 V014 track gatekeepers are NULL → GREEN: same V028 inserts the 8 edges and NULLs `sequential_track.gatekeeper_course_id`. [REQ-2, design §0 CORRECTION]
- [ ] 1.5 RED/GREEN: update the ISC course-count assertion from 57→65 (wherever it currently asserts 57) — becomes true once V028 lands, no prod code. [design §7]
- [ ] 1.6 Update `SequentialTrackEvaluatorTest`: add a NULL-gatekeeper case; confirm all existing assertions stay green (no evaluator code change expected). [design D3]
- [ ] 1.7 Verify: `./gradlew spotlessApply spotlessCheck test --tests "*CurriculumReadAdapterIT*" --tests "*SequentialTrackEvaluatorTest*"`.

## PR1b — Offering Re-point (V029) + Selection Backfill (V030) + Seed Verify

Base: PR1a branch

- [ ] 2.1 RED (new `OfferingRepointV029IT`): assert every 2026-2 profundización `course_offering.course_id` resolves to one of the 8 elective ids via the normalized-name map (design §3 table); assert zero unmapped offerings → GREEN: `V029__repoint_2026_2_profundizacion_offerings.sql`, idempotent by offering id, fails loudly on any unmatched offering. [REQ-7, design D6, §11 R-offering-map]
- [ ] 2.2 RED (new `SimulationSelectionBackfillV030IT`): assert existing `simulation.simulation_selection` rows on old shared-slot ids remap to the correct elective id by normalized `course_name`, with a legacy/unknown fallback for unmatched rows → GREEN: `V030__backfill_simulation_selection_electives.sql`. [design D6, §11 R1]
- [ ] 2.3 Update offering-adapter test fixtures still referencing shared slot ids as `course_id`.
- [ ] 2.4 Re-run root seed `seed-diego-simulacros-2026-2.sql`; verify the 32-selection assertion still holds (manual/seed run, not gradlew). [design D6]
- [ ] 2.5 Verify: `./gradlew spotlessApply spotlessCheck test --tests "*OfferingRepointV029IT*" --tests "*SimulationSelectionBackfillV030IT*"`.

## PR2 — Domain Rules Regression + Progress Pool Gate

Base: PR1b branch

- [ ] 3.1 RED (`DuplicateSelectionRuleTest`, new cases): two distinct electives (real ids) both accepted; same elective twice blocked → confirm `DuplicateSelectionRule.java` needs no change, GREEN immediately. [REQ-3, design D4]
- [ ] 3.2 RED (`AlreadyPassedRuleTest`, new cases): passed elective A + select distinct B accepted; select A again blocked → confirm `AlreadyPassedRule.java` needs no change, GREEN immediately. [REQ-5, design D4]
- [ ] 3.3 RED (application add-selection IT): elective blocked while its line's `COURSE_PASSED` gatekeeper (design §0 table) is unmet; accepted once met, regardless of numbered slot → GREEN: confirm `PrerequisiteEvaluator` needs no code change (data-only, from PR1a). [REQ-2, design §0 CORRECTION]
- [ ] 3.4 RED (application IT): two electives added to the same simulation period both accepted (simultaneity) → GREEN: confirm no evaluator change needed. [REQ-4]
- [ ] 3.5 RED (`CourseAvailabilityServiceTest`, new cases): Trabajo de Grado BLOCKED with <4 distinct PASSED pool members; AVAILABLE with ≥4 → GREEN: replace the `:88-92` skip-branch in `CourseAvailabilityService.java` with the CONFIGURABLE pool-gate evaluation (pool = ACTIVE DEEPENING courses minus the 4 slot ids). [REQ-8, design D1]
- [ ] 3.6 RED (flag F1 — unit test on `CourseRequirement.configurable(...)`/reconstitute): `required_credits` is currently dropped for CONFIGURABLE rows → GREEN: minimal factory/reconstitute tweak to carry it. [design §11 F1]
- [ ] 3.7 RED (`GetSubscriptionCurriculumService` application IT): 4 distinct passed electives → 4 named entries, no collapse; k-th passed pool elective (by `period_id`/`registered_at`) maps to display slot k → GREEN: implement pool-set computation + slot mapping in `GetSubscriptionCurriculumService.java`. [REQ-6, design D5]
- [ ] 3.8 RED (`GetStudentCourseHistoryService` IT): distinct courseIds no longer collapse into one PASSED entry (regression guard for the original bug). [REQ-6]
- [ ] 3.9 Update existing progress service tests still grouping by the old shared slot id.
- [ ] 3.10 Verify: `./gradlew spotlessApply spotlessCheck test --tests "*DuplicateSelectionRuleTest*" --tests "*AlreadyPassedRuleTest*" --tests "*CourseAvailabilityServiceTest*" --tests "*GetSubscriptionCurriculumService*" --tests "*GetStudentCourseHistoryService*"`.

## PR3 — Contract / Display / Residual Checks

Base: PR2 branch

- [ ] 4.1 RED (OpenAPI contract lint/snapshot): document `CONFIGURABLE` pool semantics in `brujula-api.v1.yaml`; optionally add nullable `filledElectiveName` to `MallaCourse` → GREEN: schema update + regenerate client. [design §6/§10]
- [ ] 4.2 RED (progress/malla application IT): malla response exposes `filledElectiveName` per numbered slot when set → GREEN: wire the field from PR2's slot-mapping output through the DTO/mapper layer. [REQ-6, design D5]
- [ ] 4.3 Regenerate OpenAPI-generated client artifacts; verify no drift.
- [ ] 4.4 Residual sweep: confirm no remaining code/test references the 4 shared slot ids (`c0080000-…-004/-005`, `c0090000-…-004/-005`) as elective identity outside slot-display code.
- [ ] 4.5 Verify: `./gradlew spotlessApply spotlessCheck test` (full suite) + OpenAPI lint/generate task.

---

## Dependency Order

Strict sequential chain, each PR bases on the prior one's branch (feature-branch-chain, per proposal §6 / design §10):

`PR1a (V028 curriculum)` → `PR1b (V029 offering re-point + V030 backfill, needs elective ids from PR1a)` → `PR2 (domain/progress code, needs real ids in DB from PR1a+PR1b)` → `PR3 (contract/display, needs PR2's slot-mapping output)`.

No PR may merge/land ahead of its predecessor. V030 shares the same normalized-name mapping as V029 — keep the mapping logic consistent between them (combine into one migration file if review budget allows, per design §9).

---

## Residual Risks to Watch During Apply

- **F1**: `CourseRequirement.configurable(...)` ignores `required_credits` today — PR2 task 3.6 must land before task 3.5's GREEN step can pass. [design §11 F1]
- **R-offering-map**: V029's normalization must cover every V022 name variant (`Profundización`, `Profundización -`, `(Sincrónico)`); an unmatched offering must fail the migration loudly, not silently keep the shared id. [design §11 R-offering-map]
- **R-attempt-validation**: confirm `course_attempt` creation accepts pool-elective ids that have no `course_recommended_semester` row before relying on PR2's distinctness count. [design §11]
- **A1 (REQ-4)**: the "later-in-earlier-period blocked" scenario is vacuous under full interchangeability; simultaneity is the binding clause — confirmed by product owner per design §0, but keep the vacuous case documented in PR1a/PR2 test comments.
- **R-gatekeeper**: relaxing the 3 per-line gatekeepers to NULL (PR1a) removes `SIMULATION_SEQUENTIAL_ORDER` for profundización lines — intended per design §0 CORRECTION, but double-check no other consumer reads those 3 gatekeeper columns.
