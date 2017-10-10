-- Comment

ALTER TABLE "gros"."comment" ALTER COLUMN "message" SET NOT NULL;
ALTER TABLE "gros"."comment" ALTER COLUMN "date" SET NOT NULL;
ALTER TABLE "gros"."comment" ALTER COLUMN "updater" SET NOT NULL;
ALTER TABLE "gros"."comment" ALTER COLUMN "updated_date" SET NOT NULL;
