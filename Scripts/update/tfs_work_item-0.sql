-- %%
-- schema: gros
-- table: tfs_work_item
-- columns:
-- - name: encryption
--   action: add
-- %%

ALTER TABLE gros.tfs_work_item ADD COLUMN "encryption" INTEGER DEFAULT 0;
