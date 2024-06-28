-- %%
-- schema: gros
-- table: metric_default
-- columns:
-- - name: scale
--   action: add
-- %%

ALTER TABLE "gros"."metric_default" ADD COLUMN "scale" VARCHAR(16) NULL;

-- %%
-- schema: gros
-- table: metric_target
-- columns:
-- - name: direction
--   action: add
-- - name: target
--   action: alter
--   "null": true
-- - name: low_target
--   action: alter
--   "null": true
-- - name: type
--   action: drop
-- - name: debt_target
--   action: add
-- - name: scale
--   action: add
-- %%

ALTER TABLE "gros"."metric_target" ADD COLUMN "direction" BOOLEAN NULL;

ALTER TABLE "gros"."metric_target" ADD COLUMN "new_target" FLOAT NULL;
UPDATE "gros"."metric_target" SET new_target = target;
ALTER TABLE "gros"."metric_target" DROP COLUMN "target";
ALTER TABLE "gros"."metric_target" RENAME COLUMN "new_target" TO "target";

ALTER TABLE "gros"."metric_target" ADD COLUMN "new_low_target" FLOAT NULL;
UPDATE "gros"."metric_target" SET new_low_target = low_target;
ALTER TABLE "gros"."metric_target" DROP COLUMN "low_target";
ALTER TABLE "gros"."metric_target" RENAME COLUMN "new_low_target" TO "low_target";

ALTER TABLE "gros"."metric_target" ADD COLUMN "debt_target" FLOAT NULL;
UPDATE "gros"."metric_target" SET debt_target = target  WHERE "type" = 'TechnicalDebtTarget';

ALTER TABLE "gros"."metric_target" DROP COLUMN "type";
ALTER TABLE "gros"."metric_target" ADD COLUMN "scale" VARCHAR(16) NULL;

-- %%
-- schema: gros
-- table: metric_version
-- columns:
-- - name: developer
--   action: alter
--   "null": true
-- %%

ALTER TABLE "gros"."metric_version" ALTER COLUMN "developer" SET NULL;
