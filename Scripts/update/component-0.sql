-- %%
-- schema: gros
-- table: component
-- action: create
-- %%

CREATE TABLE "gros"."component" (
	"project_id"      INTEGER     NOT NULL,
	"component_id"    INTEGER     NOT NULL,
	"name"            VARCHAR(100)   NOT NULL,
	"description"     VARCHAR(500)   NULL,
        CONSTRAINT "pk_component_id" PRIMARY KEY ("project_id", "component_id")
);

-- %%
-- schema: gros
-- table: issue_component
-- action: create
-- %%

CREATE TABLE "gros"."issue_component" (
	"issue_id"        INTEGER NOT NULL,
	"component_id"    INTEGER NOT NULL,
	"start_date"      TIMESTAMP   NULL,
	"end_date"        TIMESTAMP   NULL,
        CONSTRAINT "pk_issue_component_id" PRIMARY KEY ("issue_id","component_id")
);
