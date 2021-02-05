-- %%
-- schema: gros
-- table: status
-- columns:
-- - name: category_id
--   action: add
-- %%

ALTER TABLE	gros.status ADD COLUMN "category_id" INTEGER NULL;

-- %%
-- schema: gros
-- table: status_category
-- action: create
-- %%

CREATE TABLE "gros"."status_category" (
	"category_id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"key"            VARCHAR(32)   NOT NULL,
	"name"           VARCHAR(100)   NOT NULL,
	"color"          VARCHAR(32)     NULL,
        CONSTRAINT "pk_status_category_id" PRIMARY KEY ("category_id")
);
