ALTER TABLE "gros"."source_id" ADD COLUMN "source_type" VARCHAR(32) NULL;
UPDATE "gros"."source_id" SET source_type = 'sonar' WHERE source_type IS NULL;
