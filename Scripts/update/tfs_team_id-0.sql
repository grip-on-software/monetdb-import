ALTER TABLE "gros"."commits" ADD COLUMN "team_id" INTEGER NULL;
ALTER TABLE "gros"."project_developer" ADD COLUMN "team_id" INTEGER NULL;
ALTER TABLE "gros"."project_developer" DROP CONSTRAINT "pk_project_developer_id";
CREATE UNIQUE INDEX "pk_project_developer_id" ON "gros"."project_developer" ("project_id", "team_id", "developer_id");
