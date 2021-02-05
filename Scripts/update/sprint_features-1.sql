-- %%
-- schema: gros
-- table: sprint_features
-- keys:
-- - name: pk_sprint_features_id
--   action: drop
-- columns:
-- - name: component
--   action: alter
--   "null": true
-- %%

ALTER TABLE gros.sprint_features DROP CONSTRAINT "pk_sprint_features_id";
ALTER TABLE gros.sprint_features ALTER COLUMN "component" SET NULL;
