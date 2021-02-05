-- %%
-- schema: gros
-- table: sprint
-- columns:
-- - name: goal
--   action: add
-- %%

ALTER TABLE gros.sprint ADD COLUMN "goal" VARCHAR(500) NULL;
