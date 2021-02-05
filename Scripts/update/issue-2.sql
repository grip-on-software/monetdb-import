-- %%
-- schema: gros
-- table: issue
-- columns:
-- - name: expected_ltcs
--   action: alter
--   "null": true
-- - name: expected_phtcs
--   action: alter
--   "null": true
-- - name: test_execution_time
--   action: alter
--   "null": true
-- %%

ALTER TABLE gros.issue ALTER COLUMN "expected_ltcs" SET NULL;
ALTER TABLE gros.issue ALTER COLUMN "expected_phtcs" SET NULL;
ALTER TABLE gros.issue ALTER COLUMN "test_execution_time" SET NULL;
