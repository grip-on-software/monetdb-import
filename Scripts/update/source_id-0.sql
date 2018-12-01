CREATE TABLE "gros"."source_id" (
	"project_id" INTEGER NOT NULL,
	"domain_name" VARCHAR(100) NOT NULL,
	"url" VARCHAR(255) NOT NULL,
	"source_id" VARCHAR(100) NOT NULL,
		CONSTRAINT "pk_source_id" PRIMARY KEY ("project_id", "domain_name", "url")
);
