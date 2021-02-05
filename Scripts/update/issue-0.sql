-- %%
-- schema: gros
-- table: issue
-- columns:
-- - name: watchers
--   action: alter
--   "null": false
-- - name: labels
--   action: alter
--   "null": false
-- - name: resolution_date
--   action: alter
--   "null": true
-- - name: sprint_id
--   action: alter
--   "null": true
-- - name: updated_by
--   action: alter
--   "null": true
-- - name: environment
--   action: add
-- - name: external_project
--   action: add
-- %%

-- Update NULLable fields
-- Note that a later update reverts the changes to expected_ltcs, expected_phtcs
-- and test_execution_time. Thus they are not in the schema above.

ALTER TABLE gros.issue ALTER COLUMN "watchers" SET NOT NULL;
ALTER TABLE gros.issue ALTER COLUMN "labels" SET NOT NULL;
ALTER TABLE gros.issue ALTER COLUMN "expected_ltcs" SET NOT NULL;
ALTER TABLE gros.issue ALTER COLUMN "expected_phtcs" SET NOT NULL;
ALTER TABLE gros.issue ALTER COLUMN "test_execution_time" SET NOT NULL;

ALTER TABLE gros.issue ALTER COLUMN "resolution_date" SET NULL;
ALTER TABLE gros.issue ALTER COLUMN "sprint_id" SET NULL;
ALTER TABLE gros.issue ALTER COLUMN "updated_by" SET NULL;

-- Add external_project, environment field

ALTER TABLE gros.issue ADD COLUMN "environment" VARCHAR(100) NULL;
ALTER TABLE gros.issue ADD COLUMN "external_project" VARCHAR(20) NULL;

-- Move encryption to last column
ALTER TABLE gros.issue ADD COLUMN "new_encryption" INTEGER DEFAULT 0;
UPDATE gros.issue SET new_encryption=encryption;
ALTER TABLE gros.issue DROP COLUMN "encryption";
ALTER TABLE gros.issue ADD COLUMN "encryption" INTEGER DEFAULT 0;
UPDATE gros.issue SET encryption=new_encryption;
ALTER TABLE gros.issue DROP COLUMN "new_encryption";

-- Convert nullable fields that have '0' values
UPDATE gros.issue SET type = NULL WHERE type = 0;
UPDATE gros.issue SET priority = NULL WHERE priority = 0;
UPDATE gros.issue SET resolution = NULL WHERE resolution = 0;
UPDATE gros.issue SET fixversion = NULL WHERE fixversion = 0;
UPDATE gros.issue SET sprint_id = NULL WHERE sprint_id = 0;
UPDATE gros.issue SET ready_status = NULL WHERE ready_status = 0;
UPDATE gros.issue SET version = NULL WHERE version = 0;

UPDATE gros.issue SET additional_information = NULL WHERE additional_information = '0';
UPDATE gros.issue SET review_comments = NULL WHERE review_comments = '0';
UPDATE gros.issue SET updated_by = NULL WHERE updated_by = '0';
UPDATE gros.issue SET ready_status_reason = NULL WHERE ready_status_reason = '0';
UPDATE gros.issue SET test_given = NULL WHERE test_given = '0';
UPDATE gros.issue SET test_when = NULL WHERE test_when = '0';
UPDATE gros.issue SET test_then = NULL WHERE test_then = '0';
