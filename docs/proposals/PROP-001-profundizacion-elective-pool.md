# PROP-001: Profundización as a Pool of Distinct Interchangeable Electives

**Status**: Accepted (open decisions resolved 2026-07-22)
**Date**: 2026-07-22
**Type**: Change proposal + requirements (SDD propose/spec)
**Related**: ADR-005 (PostgreSQL/Flyway), simulation VAL-005/VAL-006, migrations V011/V012/V015/V016/V022

---

## 1. Context / Problem

The "Profundización" (deepening electives) area is modeled such that a student cannot build a valid simulation, and progress cannot be counted, once more than one profundización is involved.

Verified against the live `planner_db`:

- The curriculum has **4 distinct slot courses**: Profundización I `c0080000-…-004`, II `c0080000-…-005`, III `c0090000-…-004`, IV `c0090000-…-005` (3 credits each).
- The 2026-2 offering seed (`V022`) maps ~14 distinct concrete elective offerings (Diseño Centrado en el Usuario, Introducción al Big Data, Visualización de Datos, Arquitectura de Microservicios, Computación en la Nube, Desarrollo de Apps Móviles, Usabilidad y Experiencia de Usuario, Visión Artificial, …) onto a **single `course_id`** (Prof I `…004`), a few onto Prof III `…004`, and leaves **Prof II and Prof IV with zero offerings** — currently impossible to take.
- The concrete elective's identity lives **only** in `offering.course_offering.course_name` (free text), never in a distinct `course_id`.

Consequences (all verified):

- `simulation/domain/DuplicateSelectionRule.java:28-29` dedupes by `courseId` only → two different electives sharing the collapsed id are falsely blocked as `SIMULATION_DUPLICATE_SELECTION`.
- `simulation/domain/AlreadyPassedRule.java:23` also keys on `courseId` only → once one profundización is passed, every other elective is falsely blocked as `SIMULATION_COURSE_ALREADY_PASSED`.
- Progress counts by `course_id`: `progress.course_attempt` has no `course_name`/`offering_id` column; `GetStudentCourseHistoryService`/`GetSubscriptionCurriculumService` group attempts by `courseId` and `ProgressSummaryCalculator` adds credits once per distinct `courseId`. Two distinct electives filed under one slot id **silently collapse to one PASSED entry**; the real elective name is never shown; a student can never reach "4 distinct".
- `curriculum.course_requirement` has active, non-inferred edges **Prof III requires Prof I**, **Prof IV requires Prof II**, and **Trabajo de Grado requires Prof III and Prof IV** — a rigid slot→slot sequence that (a) forbids simultaneity and interchangeability, and (b) is unsatisfiable for the II→IV→thesis path because Prof II/IV have no offerings.

## 2. Confirmed Domain Rule (product owner)

- **Interchangeable electives**: any concrete profundización elective from the menu can satisfy any of the four profundización requirements.
- **Ordinal with simultaneity**: the four requirements have an order (I → II → III → IV); several may be taken in the **same** period; a later requirement may **not** be taken in an **earlier** period than an earlier one (you cannot take "III" before "I"). This is a non-strict ordering that permits concurrency, not a strict `COURSE_PASSED` prerequisite.
- **Distinctness**: a student must complete **4 distinct** electives; the same elective cannot count twice.
- **Thesis gate**: Trabajo de Grado requires the profundización requirement complete (4 distinct done), not specific numbered slots.

## 3. Goals / Non-goals

**Goals**
- A simulation may contain multiple distinct profundización electives without a false duplicate/already-passed block.
- Progress records and displays *which* concrete elective satisfied each requirement, and counts 4 distinct electives correctly.
- All profundización electives are selectable for all four requirements (no empty Prof II/IV).
- Prerequisite/ordering reflects the real rule (ordinal + simultaneity), and the thesis gate reflects "4 distinct done".

**Non-goals**
- No change to non-profundización courses, credit-limit logic, schedule-clash logic, or the Keycloak/session layer.
- No change to how offerings are imported from xlsx beyond what is needed for stable elective identity.

## 4. Requirements & Acceptance Scenarios

- **REQ-1 (distinct identity)**: each concrete profundización elective is a distinct, addressable curriculum course.
- **REQ-2 (interchangeable)**: any elective can satisfy any of the four profundización requirements.
- **REQ-3 (distinctness)**:
  - GIVEN a simulation with elective "Diseño Centrado en el Usuario", WHEN "Introducción al Big Data" and "Visualización de Datos" are added, THEN all are accepted (no `SIMULATION_DUPLICATE_SELECTION`).
  - GIVEN a simulation already containing "Big Data", WHEN "Big Data" is added again, THEN it is blocked with `SIMULATION_DUPLICATE_SELECTION`.
- **REQ-4 (ordinal + simultaneity)**:
  - GIVEN two profundización electives in the same period, THEN both are accepted.
  - GIVEN a profundización elective placed in a period earlier than a not-yet-taken earlier requirement, THEN it is blocked by the ordering rule.
- **REQ-5 (already-passed by elective)**: GIVEN the student passed "Big Data", WHEN a different elective is selected, THEN it is NOT blocked as already passed; WHEN "Big Data" itself is selected, THEN it IS blocked.
- **REQ-6 (progress traceability & count)**: GIVEN 4 distinct passed electives, THEN progress shows the 4 elective names and counts the requirement as complete; two different electives never collapse into one entry.
- **REQ-7 (offering availability)**: every profundización elective offering is selectable toward any of the four requirements; Prof II and Prof IV are no longer empty.
- **REQ-8 (thesis gate)**: Trabajo de Grado becomes available when 4 distinct profundizaciones are complete, independent of numbered-slot identity.

## 5. Chosen Approach

**Option (i): give each concrete elective its own curriculum-course identity, and model the four requirements as a pool ("choose 4 distinct from the profundización menu").**

- Each real elective becomes a `curriculum.course` row (type `DEEPENING`) with a stable natural key.
- The four "Profundización I–IV" requirements become a **pool requirement** (choose 4 distinct from the menu), reusing existing but currently-dormant plumbing where it fits: `curriculum.sequential_track_level_option` already models "N real options per level", and `RequirementType` already reserves `MINIMUM_CREDITS`/`CONFIGURABLE` (unused, `CourseAvailabilityService` skips them with a TODO). Final mechanism chosen in the design phase.
- `offering.course_offering.course_id` points to the **specific elective** course, not the shared slot id.
- The strict `COURSE_PASSED` slot→slot edges (III←I, IV←II) are **relaxed to ordinal-with-simultaneity** — the `course_requirement.simultaneity_allowed` column already exists for exactly this.
- With a real per-elective `courseId`, `DuplicateSelectionRule`, `AlreadyPassedRule`, and progress grouping become correct **without any DEEPENING special-casing**.

**Why not Option (ii) (special-case DEEPENING to key on `course_name`)**: it treats the symptom, leaves the curriculum model claiming 4 courses when there are ~15, keeps a permanent two-identity-axes special case in the rules, relies on unstable free-text names (e.g. "…(Sincrónico)" variants), and still requires reworking the contradicting requirement edges. Rejected as architecturally leaky.

## 6. Blast Radius & Delivery Plan (feature-branch chain)

- **PR 1 — Curriculum + offering data (low code risk, data-focused)**: new Flyway migration(s) adding per-elective `DEEPENING` courses, the pool requirement, relaxed ordinal edges, thesis-gate rework, and a re-point of the 2026-2 offering seed to specific elective ids.
- **PR 2 — Domain rules + progress**: verify `DuplicateSelectionRule`/`AlreadyPassedRule` are correct against the new real ids; update progress counting/distinctness; add `course_name`/`offering_id` traceability to `course_attempt` if the design requires it.
- **PR 3 — Contract / frontend / backfill (if needed)**: OpenAPI pool concept, malla/progress display, and best-effort backfill of any pre-existing `course_attempt`/`simulation_selection` rows on the old shared ids.

## 7. Data Safety

- Diego's account has **zero** profundización `course_attempt` rows (verified) → no personal data migration.
- Existing `simulation.simulation_selection` rows on old shared ids (e.g. the re-seeded 2026-2 simulacros) will be re-seeded/backfilled as part of PR 1/PR 3.

## 8. Risks & Mitigations

- **R1 — remap of pre-existing rows on shared ids** (lossy, since old rows don't record the concrete elective): scope is only seed/test data (Diego has none); use best-effort remap or a "legacy/unknown elective" fallback.
- **R2 — re-import stability**: define a stable natural key (e.g. a slug of the elective's Spanish name) so per-period xlsx re-imports don't create duplicate courses.
- **R3 — ordinal-with-simultaneity in `PrerequisiteEvaluator`**: ensure the evaluator honors `simultaneity_allowed` and the pool count instead of specific slot passes.

## 9. Resolved Decisions (product owner, 2026-07-22)

1. **"(Sincrónico)" variants**: RESOLVED — modality variants (Sincrónico/Asincrónico) of the same subject are the **SAME** elective; they count once and cannot both be taken toward distinctness. Elective identity ignores the modality suffix.
2. **Student-facing display**: RESOLVED — keep **four numbered slots** (I–IV), each showing the concrete elective that filled it (e.g. "Profundización I: PASSED — Diseño Centrado en el Usuario"). A single "Profundización (4 required)" pool bar may be adopted later; not in scope now.
3. **Menu ownership / natural key**: RESOLVED — each elective gets a **stable slug** derived from its normalized Spanish name (modality suffix stripped, per decision 1) as its natural key, so per-period xlsx re-imports reconcile to the same course instead of duplicating.
