-- %%
-- schema: gros
-- table: sprint_features
-- action: create
-- %%

-- Sprint features cache table

CREATE TABLE "gros"."sprint_features" (
    "project_id" INTEGER NOT NULL,
    "sprint_id" INTEGER NOT NULL,
    "component" VARCHAR(100) NULL,
    "name" VARCHAR(200) NOT NULL,
    "value" FLOAT NULL,
    "details" TEXT NULL,
    "update_date" TIMESTAMP NULL,
        CONSTRAINT "pk_sprint_features_id" PRIMARY KEY ("project_id", "sprint_id", "component", "name")
);
