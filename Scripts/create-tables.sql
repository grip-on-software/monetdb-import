CREATE TABLE "gros"."issue" (
	"issue_id"       INTEGER     NOT NULL,
	"changelog_id"   INTEGER     NOT NULL,
	"key"            VARCHAR(10)   NOT NULL,
	"title"          TEXT   NOT NULL,
	"type"           INTEGER    NULL,
	"priority"       INTEGER    NULL,
	"resolution"     INTEGER    NULL,
	"fixversion"     INTEGER    NULL,
	"bugfix"         BOOL    NULL,
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
	"story_points"   DECIMAL(4,1)    NULL,
	"resolution_date"        TIMESTAMP,
	"sprint_id"      INTEGER    NOT NULL,
	"updated_by"     VARCHAR(100)    NOT NULL,
	"rank_change"    BOOL    NULL,
	"epic"           VARCHAR(10)    NULL,
	"impediment"     BOOL     NOT NULL,
	"ready_status"   INTEGER    NULL,
	"ready_status_reason" TEXT  NULL,
	"approved"       BOOL    NULL,
	"approved_by_po" BOOL    NULL,
	"labels"         INTEGER    NULL,
	"version"        INTEGER    NULL,
	"expected_ltcs"  INTEGER    NULL,
	"expected_phtcs" INTEGER    NULL,
	"test_given"     TEXT    NULL,
	"test_when"      TEXT    NULL,
	"test_then"      TEXT    NULL,
	"test_execution" INTEGER NULL,
	"test_execution_time" INTEGER   NULL,
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
	"project_id"      INTEGER     NOT NULL,
	"name"            VARCHAR(100)   NOT NULL,
	"description"     VARCHAR(500)   NULL,
	"start_date"      DATE  NULL,
	"release_date"    DATE  NULL,
	"released"        BOOL  NOT NULL,
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
	"from_key"        VARCHAR(10) NOT NULL,
	"to_key"          VARCHAR(10) NOT NULL,
	"relationship_type"     INTEGER NOT NULL,
	"outward"         BOOL    NOT NULL,
	"start_date"      TIMESTAMP   NULL,
	"end_date"        TIMESTAMP   NULL,
        CONSTRAINT "pk_issuelink_id" PRIMARY KEY ("from_key","to_key","relationship_type","outward")
);

CREATE TABLE "gros"."subtask" (
	"id_parent"             INTEGER   NOT NULL,
	"id_subtask"            INTEGER   NOT NULL,
        CONSTRAINT "pk_subtask_id" PRIMARY KEY ("id_parent","id_subtask")
);

CREATE TABLE "gros"."ready_status" (
	"id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
        CONSTRAINT "pk_ready_status_id" PRIMARY KEY ("id")
);

CREATE TABLE "gros"."test_execution" (
	"id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"value"            VARCHAR(100)   NOT NULL,
        CONSTRAINT "pk_test_execution_id" PRIMARY KEY ("id")
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

CREATE TABLE "gros"."metric_version" (
	"project_id"     INTEGER     NOT NULL,
	"version_id"     INTEGER     NOT NULL,
	"developer"      VARCHAR(100) NOT NULL,
	"message"        TEXT      NULL,
	"commit_date"    TIMESTAMP NOT NULL,
        CONSTRAINT "pk_metric_version_id" PRIMARY KEY ("project_id","version_id")
);

CREATE TABLE "gros"."metric_target" (
	"project_id"     INTEGER     NOT NULL,
	"version_id"     INTEGER     NOT NULL,
	"metric_id"      INTEGER     NOT NULL,
	"type"           VARCHAR(100) NOT NULL,
	"target"         INTEGER     NOT NULL,
	"low_target"     INTEGER     NOT NULL,
	"comment"        TEXT     NULL
);

CREATE TABLE "gros"."sprint" (
	"sprint_id"      INTEGER     NOT NULL,
	"project_id"     INTEGER     NOT NULL,
	"name"           VARCHAR(500)   NOT NULL,
	"start_date"     TIMESTAMP      NULL,
	"end_date"       TIMESTAMP      NULL,
        CONSTRAINT "pk_sprint_id" PRIMARY KEY ("sprint_id", "project_id")
);

CREATE TABLE "gros"."project" (
	"project_id"      INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
        CONSTRAINT "pk_project_id" PRIMARY KEY ("project_id")
);

CREATE TABLE "gros"."commits" (
	"version_id"     VARCHAR(100)     NOT NULL,
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
	"updater"        VARCHAR(200)   NOT NULL,
	"updated_date"   TIMESTAMP      NULL,
        CONSTRAINT "pk_comment_id" PRIMARY KEY ("comment_id")
);

CREATE TABLE "gros"."vcs_developer" (
	"alias_id"     INTEGER       NOT NULL AUTO_INCREMENT,
	"jira_dev_id"  INTEGER,
	"display_name" VARCHAR(500),
        CONSTRAINT "pk_alias_id" PRIMARY KEY ("alias_id")
);

CREATE TABLE "gros"."repo" (
	"id"       INTEGER       NOT NULL AUTO_INCREMENT,
	"repo_name" VARCHAR(1000) NOT NULL,
        CONSTRAINT "pk_repo_id" PRIMARY KEY ("id")
);
