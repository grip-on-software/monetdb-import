-- %%
-- schema: gros
-- table: sprint
-- columns:
-- - name: board_id
--   action: add
-- %%

ALTER TABLE gros.sprint ADD COLUMN "board_id" INTEGER NULL;
