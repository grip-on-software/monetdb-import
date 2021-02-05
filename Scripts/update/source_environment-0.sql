-- %%
-- schema: gros
-- table: source_environment
-- keys:
-- - name: pk_source_environment_id
--   action: alter
--   objects: [project_id, environment]
-- %%

ALTER TABLE gros.source_environment DROP CONSTRAINT "pk_source_environment_id";
ALTER TABLE gros.source_environment ADD CONSTRAINT "pk_source_environment_id" PRIMARY KEY ("project_id", "environment");
