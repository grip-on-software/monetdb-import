ALTER TABLE gros.source_environment DROP CONSTRAINT "pk_source_environment_id";
ALTER TABLE gros.source_environment ADD CONSTRAINT "pk_source_environment_id" PRIMARY KEY ("project_id", "environment");
