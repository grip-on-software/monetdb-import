-- %%
-- schema: gros
-- table: source_environment
-- columns:
-- - name: version
--   action: add
-- %%

ALTER TABLE gros.source_environment ADD COLUMN "version" VARCHAR(32) NOT NULL DEFAULT '';
