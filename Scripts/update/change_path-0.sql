-- %%
-- schema: gros
-- table: change_path
-- columns:
-- - name: size
--   action: add
-- %%
ALTER TABLE gros.change_path ADD COLUMN "size" INTEGER NOT NULL default 0;
