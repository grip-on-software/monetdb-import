-- %%
-- schema: gros
-- table: metric
-- columns:
-- - name: domain_type
--   action: add
-- %%

ALTER TABLE "gros"."metric" ADD COLUMN "domain_type" VARCHAR(32) NULL;
