-- %%
-- schema: gros
-- table: tfs_sprint
-- columns:
-- - name: repo_id
--   action: alter
--   "null": true
-- - name: team_id
--   action: alter
--   "null": true

ALTER TABLE gros.tfs_sprint ALTER COLUMN "repo_id" SET NULL;
ALTER TABLE gros.tfs_sprint ALTER COLUMN "team_id" SET NULL;
