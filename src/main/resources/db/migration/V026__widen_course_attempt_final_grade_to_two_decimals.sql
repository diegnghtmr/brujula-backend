-- V026__widen_course_attempt_final_grade_to_two_decimals.sql
-- Forward-only migration. NEVER edit this file after it is committed.
-- Widens progress.course_attempt.final_grade from NUMERIC(3,1) to NUMERIC(3,2) so
-- course grades preserve two decimal places (e.g. 4.91), matching the official UQ
-- grade sheets. Existing one-decimal values are kept as-is (4.9 -> 4.90).
-- The grade/status CHECK constraint (PASSED 3.0–5.0 / FAILED 0.0–2.9) is unaffected.

ALTER TABLE progress.course_attempt
    ALTER COLUMN final_grade TYPE NUMERIC(3, 2);
