-- %%
-- schema: gros
-- table: issue
-- columns:
-- - name: attachments
--   action: alter
--   "null": false
-- %%

ALTER TABLE gros.issue ALTER COLUMN "attachments" SET NOT NULL;
