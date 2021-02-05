-- %%
-- schema: gros
-- table: project
-- columns:
-- - name: jira_name
--   action: add
-- %%

ALTER TABLE gros.project ADD COLUMN "jira_name" VARCHAR(100) NULL;
