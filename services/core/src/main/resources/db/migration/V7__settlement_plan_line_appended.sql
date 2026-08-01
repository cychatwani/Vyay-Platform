-- =====================================================================
-- V7__settlement_plan_line_appended.sql
-- Store "was this line appended?" instead of deriving it from timestamps.
--
-- V6 asserted that "an appended line is simply one whose created_at > its
-- plan's created_at — no separate appended flag needed". That is withdrawn.
-- The derivation is true only while every generation-batch line is stamped with
-- the PLAN's created_at rather than its own, which made a display flag depend on
-- a write-time trick: any line inserted with its own Instant.now() — the obvious
-- thing for a future caller to do — takes a timestamp later than the plan's and
-- silently reports itself as appended. Worse, it is a comparison of two clocks
-- for a fact the writer knows outright at insert time.
--
-- So the writer records it. Generation writes false, the append path writes true,
-- and nothing downstream has to infer intent from a timestamp.
--
-- Backfill: DEFAULT false is correct for every existing row. No append path has
-- shipped yet, so every line currently in the table came from a generation batch.
-- =====================================================================

ALTER TABLE settlement_plan_line
    ADD COLUMN appended boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN settlement_plan_line.appended IS
    'False for lines written by plan generation (netted or carried); true for a line '
    'appended to a live plan after a balance change. Set by the writer, never derived.';
