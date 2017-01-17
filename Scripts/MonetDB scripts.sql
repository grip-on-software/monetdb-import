CREATE TABLE "gros"."issue" (
	"issue_id"       INTEGER     NOT NULL,
	"changelog_id"   INTEGER     NOT NULL,
	"key"            VARCHAR(10)   NOT NULL,
	"title"          TEXT   NOT NULL,
	"type"           INTEGER    NULL,
	"priority"       INTEGER    NULL,
	"resolution"     INTEGER    NULL,
	"fixVersion"     INTEGER    NULL,
	"bugfix"         INTEGER    NULL,
	"watchers"       INTEGER    NULL,
	"created"     TIMESTAMP  NULL,
	"updated"     TIMESTAMP  NULL,
	"description"    TEXT  NULL,
	"duedate"        TIMESTAMP  NULL,
	"project_id"     INTEGER    NOT NULL,
	"status"         INTEGER    NULL,
	"delta_comments" TEXT  NULL,
	"reporter"       VARCHAR(15)    NULL,
	"assignee"       VARCHAR(15)    NULL,
	"attachments"    INTEGER    NULL,
	"additional_information" TEXT  NULL,
	"review_comments" TEXT  NULL,
	"story_points"   INTEGER    NULL,
	"resolution_date"        TIMESTAMP,
	"sprint_id"      INTEGER    NOT NULL,
	"updated_by"     VARCHAR(100)    NOT NULL,
        CONSTRAINT "pk_issue_id" PRIMARY KEY ("issue_id","changelog_id")
);

CREATE TABLE "gros"."issuetype" (
	"id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
	"description"     VARCHAR(500)   NULL,
        CONSTRAINT "pk_issuetype_id" PRIMARY KEY ("id")
);


CREATE TABLE "gros"."status" (
	"id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
	"description"     VARCHAR(500)   NULL,
        CONSTRAINT "pk_status_id" PRIMARY KEY ("id")
);


CREATE TABLE "gros"."resolution" (
	"id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
	"description"     VARCHAR(500)   NULL,
        CONSTRAINT "pk_resolution_id" PRIMARY KEY ("id")
);

CREATE TABLE "gros"."developer" (
	"id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
	"display_name"     VARCHAR(100)   NULL,
        CONSTRAINT "pk_developer_id" PRIMARY KEY ("id")
);

CREATE TABLE "gros"."fixversion" (
	"id"              INTEGER     NOT NULL,
	"name"            VARCHAR(100)   NOT NULL,
	"description"     VARCHAR(500)   NULL,
	"release_date"    DATE  NULL,
        CONSTRAINT "pk_fixversion_id" PRIMARY KEY ("id")
);

CREATE TABLE "gros"."priority" (
	"id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
        CONSTRAINT "pk_priority_id" PRIMARY KEY ("id")
);

CREATE TABLE "gros"."relationshiptype" (
	"id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
        CONSTRAINT "pk_relationshiptype_id" PRIMARY KEY ("id")
);

CREATE TABLE "gros"."issuelink" (
	"id_from"              INTEGER   NOT NULL,
	"id_to"                INTEGER   NOT NULL,
	"relationship_type"    INTEGER   NOT NULL,
        CONSTRAINT "pk_issuelink_id" PRIMARY KEY ("id_from","id_to","relationship_type")
);

CREATE TABLE "gros"."subtask" (
	"id_parent"             INTEGER   NOT NULL,
	"id_subtask"            INTEGER   NOT NULL,
        CONSTRAINT "pk_subtask_id" PRIMARY KEY ("id_parent","id_subtask")
);

CREATE TABLE "gros"."metric" (
	"metric_id"       INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL
);

CREATE TABLE "gros"."metric_value" (
	"metric_id"       INTEGER     NOT NULL,
	"value"           INTEGER     NOT NULL,
	"category"        VARCHAR(100) NOT NULL,
	"date"            TIMESTAMP  NULL,
	"since_date"      TIMESTAMP  NULL,
	"project_id"      INTEGER     NOT NULL
);

CREATE TABLE "gros"."sprint" (
	"sprint_id"      INTEGER     NOT NULL,
	"project_id"     INTEGER     NOT NULL,
	"name"           VARCHAR(500)   NOT NULL,
	"start_date"     TIMESTAMP      NULL,
	"end_date"       TIMESTAMP      NULL,
        CONSTRAINT "pk_sprint_id" PRIMARY KEY ("sprint_id")
);

CREATE TABLE "gros"."project" (
	"project_id"      INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
        CONSTRAINT "pk_project_id" PRIMARY KEY ("project_id")
);

CREATE TABLE "gros"."commits" (
	"commit_id"     VARCHAR(100)     NOT NULL,
	"project_id"    INTEGER         NOT NULL,
	"commit_date"   TIMESTAMP       NOT NULL,
	"sprint_id"     INTEGER         NOT NULL,
	"developer_id"  INTEGER         NOT NULL,
	"message"       TEXT            NOT NULL,
	"size_of_commit" INTEGER        NOT NULL,
	"insertions"    INTEGER         NOT NULL,
	"deletions"     INTEGER         NOT NULL,
	"number_of_files"   INTEGER     NOT NULL,
	"number_of_lines"   INTEGER     NOT NULL,
	"type"              VARCHAR(100)  NOT NULL,
	"repo_id"       INTEGER         NOT NULL
);

CREATE TABLE "gros"."comment" (
	"comment_id"     INTEGER        NOT NULL AUTO_INCREMENT,
	"issue_id"       INTEGER        NOT NULL,
	"message"        TEXT           NULL,
	"author"         VARCHAR(200)   NOT NULL,
	"date"           TIMESTAMP      NULL,
        CONSTRAINT "pk_comment_id" PRIMARY KEY ("comment_id")
);

CREATE TABLE "gros"."git_developer" (
	"alias_id"     INTEGER       NOT NULL AUTO_INCREMENT,
	"jira_dev_id"  INTEGER,
	"display_name" VARCHAR(500),
        CONSTRAINT "pk_alias_id" PRIMARY KEY ("alias_id")
);

CREATE TABLE "gros"."git_repo" (
	"id"       INTEGER       NOT NULL AUTO_INCREMENT,
	"git_name" VARCHAR(1000) NOT NULL,
        CONSTRAINT "pk_repo_id" PRIMARY KEY ("id")
);
