CREATE TABLE "gros"."metric_default" (
	"base_name"      VARCHAR(100) NOT NULL,
	"version_id"     VARCHAR(100) NOT NULL,
	"commit_date"    TIMESTAMP NOT NULL,
	"direction"      BOOLEAN      NULL,
	"perfect"        FLOAT     NULL,
	"target"         FLOAT     NULL,
	"low_target"     FLOAT     NULL,
	    CONSTRAINT "pk_metric_default_id" PRIMARY KEY ("base_name", "version_id")
);
