ALTER TABLE	gros.status ADD COLUMN "category_id" INTEGER NULL;

CREATE TABLE "gros"."status_category" (
	"category_id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"key"            VARCHAR(32)   NOT NULL,
	"name"           VARCHAR(100)   NOT NULL,
	"color"          VARCHAR(32)     NULL,
        CONSTRAINT "pk_status_category_id" PRIMARY KEY ("category_id")
);
