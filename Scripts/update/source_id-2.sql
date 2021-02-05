-- %%
-- schema: gros
-- table: source_id
-- columns:
-- - name: source_id
--   action: alter
--   "null": false
-- %%

-- Adjust source_id type and move to last column
ALTER TABLE gros.source_id ADD COLUMN "new_source_id" TEXT;
UPDATE gros.source_id SET new_source_id=source_id;
ALTER TABLE gros.source_id DROP COLUMN "source_id";
ALTER TABLE gros.source_id ADD COLUMN "source_id" TEXT;
UPDATE gros.source_id SET source_id=new_source_id;
ALTER TABLE gros.source_id DROP COLUMN "new_source_id";
ALTER TABLE gros.source_id ALTER COLUMN "source_id" SET NOT NULL;
