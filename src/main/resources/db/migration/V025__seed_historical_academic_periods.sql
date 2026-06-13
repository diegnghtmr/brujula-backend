-- V025__seed_historical_academic_periods.sql
-- Forward-only migration. NEVER edit this file after it is committed.
-- Seeds historical academic periods 2023-1 .. 2026-1 (CLOSED) so the /periods
-- dropdown lets students declare past attempts. 2026-2 (V017) is left untouched.

INSERT INTO offering.academic_period (id, name, start_date, end_date, status, version)
VALUES
    ('00000002-0000-4000-a000-000000000001', '2023-1', '2023-01-16', '2023-06-02', 'CLOSED', 0),
    ('00000003-0000-4000-a000-000000000001', '2023-2', '2023-07-17', '2023-11-24', 'CLOSED', 0),
    ('00000004-0000-4000-a000-000000000001', '2024-1', '2024-01-15', '2024-06-07', 'CLOSED', 0),
    ('00000005-0000-4000-a000-000000000001', '2024-2', '2024-07-15', '2024-11-29', 'CLOSED', 0),
    ('00000006-0000-4000-a000-000000000001', '2025-1', '2025-01-20', '2025-06-06', 'CLOSED', 0),
    ('00000007-0000-4000-a000-000000000001', '2025-2', '2025-07-14', '2025-11-28', 'CLOSED', 0),
    ('00000008-0000-4000-a000-000000000001', '2026-1', '2026-01-19', '2026-06-05', 'CLOSED', 0)
ON CONFLICT (name) DO NOTHING;
