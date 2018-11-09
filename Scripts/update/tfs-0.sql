-- TFS items

CREATE TABLE "gros"."tfs_developer" (
	"project_id"   INTEGER NOT NULL,
	"display_name" VARCHAR(100) NOT NULL,
	"email"        VARCHAR(100) NULL,
	"alias_id"  INTEGER NULL,
	"encryption"     INTEGER   DEFAULT 0,
        CONSTRAINT "pk_tfs_developer_id" PRIMARY KEY ("project_id", "display_name")
);

CREATE TABLE "gros"."tfs_sprint" (
	"sprint_id" INTEGER NOT NULL AUTO_INCREMENT,
	"project_id"   INTEGER NOT NULL,
	"repo_id" INTEGER NOT NULL,
	"team_id" INTEGER NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"start_date"     TIMESTAMP      NULL,
	"end_date"       TIMESTAMP      NULL,
        CONSTRAINT "pk_tfs_sprint_id" PRIMARY KEY ("sprint_id")
);

CREATE TABLE "gros"."tfs_team" (
	"team_id" INTEGER NOT NULL AUTO_INCREMENT,
	"project_id" INTEGER NOT NULL,
	"repo_id" INTEGER NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"description" VARCHAR(500) NULL,
		CONSTRAINT "pk_tfs_team_id" PRIMARY KEY ("team_id")
);

CREATE TABLE "gros"."tfs_team_member" (
	"team_id" INTEGER NOT NULL,
	"repo_id" INTEGER NOT NULL,
	"alias_id" INTEGER NULL,
	"name" VARCHAR(100) NOT NULL,
	"display_name" VARCHAR(100) NOT NULL,
	"encryption"     INTEGER   DEFAULT 0,
        CONSTRAINT "pk_tfs_team_member_id" PRIMARY KEY ("team_id", "name")
);

CREATE TABLE "gros"."tfs_work_item" (
	"issue_id"       INTEGER     NOT NULL,
	"changelog_id"   INTEGER     NOT NULL,
	"title"          TEXT   NOT NULL,
	"type"           VARCHAR(64)    NULL,
	"priority"       INTEGER    NULL,
	"created"     TIMESTAMP  NULL,
	"updated"     TIMESTAMP  NULL,
	"description"    TEXT  NULL,
	"duedate"        DATE  NULL,
	"project_id"     INTEGER    NOT NULL,
	"status"         VARCHAR(64)    NULL,
	"reporter"       VARCHAR(100)    NULL,
	"assignee"       VARCHAR(100)    NULL,
	"attachments"    INTEGER    NOT NULL,
	"additional_information" TEXT  NULL,
	"sprint_id"      INTEGER    NULL,
	"team_id"        INTEGER    NULL,
	"updated_by"     VARCHAR(100)    NULL,
	"labels"         INTEGER    NULL,
        CONSTRAINT "pk_work_item_id" PRIMARY KEY ("issue_id", "changelog_id")
);
