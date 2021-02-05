-- %%
-- schema: gros
-- table: source_id
-- columns:
-- - name: domain_type
--   action: add
-- %%

ALTER TABLE "gros"."source_id" ADD COLUMN "domain_type" VARCHAR(32) NULL;
