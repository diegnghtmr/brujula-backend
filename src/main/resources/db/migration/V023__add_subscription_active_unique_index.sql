-- V023__add_subscription_active_unique_index.sql
-- Forward-only migration. NEVER edit this file after it is committed.
-- Adds a partial unique index on progress.student_program_subscription so the DB enforces
-- the correct uniqueness invariant: (user_id, program_id, reference_journey_code) must be
-- unique among ACTIVE rows.
--
-- This reflects the corrected business rule (SUB-1 fix):
--   - The uniqueness key is (programId + referenceJourneyCode), NOT programId alone.
--   - Diurna AND Nocturna of the SAME program are TWO valid distinct subscriptions.
--   - DELETED rows are excluded from the constraint (soft-delete is the archival strategy).
--
-- The partial predicate `WHERE status = 'ACTIVE'` matches the status values used by
-- V015__create_progress_tables.sql and the SubscriptionStatus enum ('ACTIVE' / 'DELETED').

CREATE UNIQUE INDEX idx_subscription_active_unique
    ON progress.student_program_subscription (user_id, program_id, reference_journey_code)
    WHERE status = 'ACTIVE';
