-- %%
-- schema: gros
-- table: comment
-- columns:
-- - name: message
--   action: alter
--   "null": false
-- - name: date
--   action: alter
--   "null": false
-- - name: updater
--   action: alter
--   "null": false
-- - name: updated_date
--   action: alter
--   "null": false
-- %%

-- Comment

ALTER TABLE "gros"."comment" ALTER COLUMN "message" SET NOT NULL;
ALTER TABLE "gros"."comment" ALTER COLUMN "date" SET NOT NULL;
ALTER TABLE "gros"."comment" ALTER COLUMN "updater" SET NOT NULL;
ALTER TABLE "gros"."comment" ALTER COLUMN "updated_date" SET NOT NULL;
