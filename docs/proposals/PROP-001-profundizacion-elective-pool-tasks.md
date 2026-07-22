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

- [x] 1.1 RED (new `FlywayV028CurriculumProfundizacionPoolTest`): assert 8 DEEPENING elective courses exist with stable official_codes (design §3 table) → GREEN: `V028__curriculum_profundizacion_pool.sql`, idempotent `WHERE NOT EXISTS` insert of the 8 electives (type=DEEPENING, credits=3, plan `33333333-…-001`). [REQ-1, design §3/§5]
- [x] 1.2 RED (new `FlywayV028CurriculumProfundizacionPoolTest`): assert the 4 superseded ordering edges — slot→slot (Prof III←I, IV←II) + 2 thesis-slot edges (TdG←III, TdG←IV) — are `status=ARCHIVED` → GREEN: same V028 sets those 4 rows to ARCHIVED (`V012:733-746`). **Deviation**: ground-truth V012 has exactly 4 course_requirement rows in that range (2 slot→slot + 2 thesis-slot), not 6 — verified directly against V012:732-746; task text's "6 archived edges" phrasing overcounted. [REQ-4, design D3]
- [x] 1.3 RED (new `FlywayV028CurriculumProfundizacionPoolTest`): assert one `CONFIGURABLE` `course_requirement` row on Trabajo de Grado, `required_credits=4`, `required_course_id=NULL` → GREEN: same V028 inserts the pool-gate row. [REQ-8, design D1]
- [x] 1.4 RED (new `FlywayV028CurriculumProfundizacionPoolTest` + updated `FlywayV014DeepeningGatekeeperTest` + updated `CurriculumReadAdapterIT`): assert 8 `COURSE_PASSED` edges (elective → line gatekeeper per design §0 table) exist, and the 3 V014 track gatekeepers are NULL → GREEN: same V028 inserts the 8 edges and NULLs `sequential_track.gatekeeper_course_id`. [REQ-2, design §0 CORRECTION]
- [x] 1.5 RED/GREEN: updated `FlywayV012SistemasSeedTest` ISC course-count assertion from 57→65 — true once V028 lands, no prod code. [design §7]
- [x] 1.6 Verified `SequentialTrackEvaluatorTest`: `deepeningCourse_noGatekeeperDefined_returnsEmpty()` already covers the NULL-gatekeeper case pre-existing this change; confirmed still green, no evaluator code change needed. [design D3]
- [x] 1.7 Verify: `./gradlew spotlessApply spotlessCheck test --tests "*CurriculumReadAdapterIT*" --tests "*SequentialTrackEvaluatorTest*" --tests "*FlywayV028CurriculumProfundizacionPoolTest*" --tests "*FlywayV014DeepeningGatekeeperTest*" --tests "*FlywayV012SistemasSeedTest*"` → BUILD SUCCESSFUL, all GREEN. Full `./gradlew test` suite also run — BUILD SUCCESSFUL, no regressions.

## PR1b — Offering Re-point (V029) + Selection Backfill (V030) + Seed Verify

Base: PR1a branch

- [x] 2.1 RED (new `OfferingRepointV029IT`): assert every 2026-2 profundización `course_offering.course_id` resolves to one of the 8 elective ids via the normalized-name map (design §3 table); assert zero unmapped offerings → GREEN: `V029__repoint_2026_2_profundizacion_offerings.sql`, idempotent by offering id, fails loudly on any unmatched offering. [REQ-7, design D6, §11 R-offering-map]
- [x] 2.2 RED (new `SimulationSelectionBackfillV030IT`): assert existing `simulation.simulation_selection` rows on old shared-slot ids remap to the correct elective id by normalized `course_name`, with a legacy/unknown fallback for unmatched rows → GREEN: `V030__backfill_simulation_selection_electives.sql`. [design D6, §11 R1]
- [x] 2.3 Update offering-adapter test fixtures still referencing shared slot ids as `course_id`. **Deviation**: only `FlywayV022OfferingSeedFromXlsxTest` needed updating (it asserted 18 offerings on the shared slot ids by construction) — `CurriculumReadAdapterIT`'s use of the same UUID constant is a `curriculum.course` id (the slot course, kept ACTIVE as a display anchor per design D2), not an `offering.course_offering.course_id`, so it is unaffected by V029 and needed no change.
- [ ] 2.4 Re-run root seed `seed-diego-simulacros-2026-2.sql`; verify the 32-selection assertion still holds (manual/seed run, not gradlew). [design D6] — **DEFERRED to orchestrator** (dev-DB apply + seed verification is out of scope for this apply batch per explicit instruction; not run against the dev DB by this agent).
- [x] 2.5 Verify: `./gradlew spotlessApply spotlessCheck test --tests "*OfferingRepointV029IT*" --tests "*SimulationSelectionBackfillV030IT*"` → BUILD SUCCESSFUL, all GREEN. Full `./gradlew test --offline` also run — BUILD SUCCESSFUL, 1517 tests, 0 failures, 0 errors, 14 pre-existing skips (unrelated shell-script contract tests).

## PR2 — Domain Rules Regression + Progress Pool Gate

Base: PR1b branch

- [x] 3.1 RED (`DuplicateSelectionRuleTest`, new cases): two distinct electives (real ids) both accepted; same elective twice blocked → confirmed `DuplicateSelectionRule.java` needs no change, GREEN immediately. [REQ-3, design D4]
- [x] 3.2 RED (`AlreadyPassedRuleTest`, new cases): passed elective A + select distinct B accepted; select A again blocked → confirmed `AlreadyPassedRule.java` needs no change, GREEN immediately. [REQ-5, design D4]
- [x] 3.3 RED (`AddSimulationSelectionServiceTest`): elective blocked while its line's `COURSE_PASSED` gatekeeper (design §0 table) is unmet; accepted once met, regardless of numbered slot → GREEN: confirmed `PrerequisiteEvaluator` needs no code change (data-only, from PR1a). [REQ-2, design §0 CORRECTION]
- [x] 3.4 RED (`AddSimulationSelectionServiceTest`): two electives added to the same simulation period both accepted (simultaneity) → GREEN: confirmed no evaluator change needed. [REQ-4]
- [x] 3.5 RED (`CourseAvailabilityServiceTest`, new cases): Trabajo de Grado BLOCKED with <4 distinct PASSED pool members; AVAILABLE with ≥4 → GREEN: replaced the `:88-92` skip-branch in `CourseAvailabilityService.java` with CONFIGURABLE pool-gate evaluation via a new backward-compatible 4-arg `compute(..., poolMemberCourseIds)` overload. [REQ-8, design D1]
- [x] 3.6 RED (flag F1 — unit test on `CourseRequirement.configurable(...)`): `required_credits` was dropped by the 2-arg factory (used by `CreateCourseRequirementService`) → GREEN: added a 3-arg `configurable(id, targetCourseId, requiredCredits)` overload; `reconstitute()` already carried it correctly (JPA mapper unaffected). [design §11 F1]
- [x] 3.7 RED (`GetSubscriptionCurriculumServiceTest`): 4 distinct passed electives → 4 named entries, no collapse; k-th passed pool elective (by `period_id`/`registered_at`) maps to display slot k → GREEN: implemented pool-set computation + slot mapping in `GetSubscriptionCurriculumService.java`. **Deviation**: required additive backward-compatible fields on `PlanMallaView` (`poolCourses`), `PlanMallaView.RequirementSummary` (`requiredCredits`), `CourseRequirementEdge` (`requiredCredits`), and `CurriculumCourseData` (`filledElectiveName`) — design §6 "no DTO change" was incorrect against ground truth (V028 electives have zero `course_recommended_semester` rows, so the existing malla channel never carried them). [REQ-6, design D5]
- [x] 3.8 RED (`GetStudentCourseHistoryServiceTest`): distinct courseIds no longer collapse into one PASSED entry (regression guard) — confirmed no code change needed. [REQ-6]
- [x] 3.9 Searched all test directories for the old shared slot ids: only 5 legitimate PR1a/PR1b migration/adapter tests remain (correct usage, not a grouping bug); no progress-service test needed updating.
- [x] 3.10 Verify: `./gradlew spotlessApply spotlessCheck test --tests "*DuplicateSelectionRuleTest*" --tests "*AlreadyPassedRuleTest*" --tests "*CourseAvailabilityServiceTest*" --tests "*GetSubscriptionCurriculumService*" --tests "*GetStudentCourseHistoryService*" --offline` → BUILD SUCCESSFUL. Full `./gradlew test --offline` → BUILD SUCCESSFUL, 1538 tests, 0 failures, 0 errors, 14 pre-existing unrelated skips. **Bug found+fixed during full-suite run**: `GetSubscriptionCurriculumService.describeUnmet()` NPE'd on the new null `requiredCourseId` (pool-gate unmet entries) — reproduced via the real `ProgressCurriculumIntegrationTest` (real seeded CONFIGURABLE row on Trabajo de Grado), fixed with a null-guard, RED→GREEN confirmed.

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
