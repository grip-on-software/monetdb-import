ALTER TABLE gros.sprint_features DROP CONSTRAINT "pk_sprint_features_id";
ALTER TABLE gros.sprint_features ALTER COLUMN "component" SET NULL;
