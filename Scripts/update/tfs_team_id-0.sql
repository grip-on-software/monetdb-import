ALTER TABLE "gros"."commits" ADD COLUMN "team_id" INTEGER NULL;
ALTER TABLE "gros"."project_developer" ADD COLUMN "team_id" INTEGER NULL;
ALTER TABLE "gros"."project_developer" DROP CONSTRAINT "pk_project_developer_id";
ALTER TABLE "gros"."project_developer" ADD CONSTRAINT "pk_project_developer_id" PRIMARY KEY ("project_id", "team_id", "developer_id");
