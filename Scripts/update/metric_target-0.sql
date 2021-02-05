-- %%
-- schema: gros
-- table: metric_target
-- columns:
-- - name: type
--   action: alter
--   "null": true
-- %%

ALTER TABLE gros.metric_target ALTER COLUMN "type" SET NULL;
