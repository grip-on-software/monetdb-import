-- %%
-- schema: gros
-- table: commits
-- columns:
-- - name: size_of_commit
--   action: alter
--   "null": true
-- - name: insertions
--   action: alter
--   "null": true
-- - name: deletions
--   action: alter
--   "null": true
-- - name: number_of_files
--   action: alter
--   "null": true
-- - name: number_of_lines
--   action: alter
--   "null": true
-- %%

ALTER TABLE gros.commits ALTER COLUMN "size_of_commit" SET NULL;
ALTER TABLE gros.commits ALTER COLUMN "insertions" SET NULL;
ALTER TABLE gros.commits ALTER COLUMN "deletions" SET NULL;
ALTER TABLE gros.commits ALTER COLUMN "number_of_files" SET NULL;
ALTER TABLE gros.commits ALTER COLUMN "number_of_lines" SET NULL;
