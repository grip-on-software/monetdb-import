CREATE TABLE "gros"."issue" (
	"issue_id"       INTEGER     NOT NULL,
	"changelog_id"   INTEGER     NOT NULL,
	"key"            VARCHAR(20)   NOT NULL,
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
	"duedate"        DATE  NULL,
	"project_id"     INTEGER    NOT NULL,
	"status"         INTEGER    NULL,
	"delta_comments" TEXT  NULL,
	"reporter"       VARCHAR(15)    NULL,
	"assignee"       VARCHAR(15)    NULL,
	"attachments"    INTEGER    NULL,
	"additional_information" TEXT  NULL,
	"review_comments" TEXT  NULL,
	"story_points"   DECIMAL(4,2)    NULL,
	"resolution_date"        TIMESTAMP,
	"sprint_id"      INTEGER    NOT NULL,
	"updated_by"     VARCHAR(100)    NOT NULL,
	"rank_change"    BOOL    NULL,
	"epic"           VARCHAR(20)    NULL,
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
	"from_key"        VARCHAR(20) NOT NULL,
	"to_key"          VARCHAR(20) NOT NULL,
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

CREATE TABLE "gros"."tag" (
	"repo_id" INTEGER NOT NULL,
	"tag_name" VARCHAR(100) NOT NULL,
	"version_id" VARCHAR(100) NOT NULL,
	"message" TEXT NULL,
	"tagged_date" TIMESTAMP NULL,
	"tagger_id" INTEGER NULL,
		CONSTRAINT "pk_tag_id" PRIMARY KEY ("repo_id", "tag_name")
);

CREATE TABLE "gros"."gitlab_repo" (
	"repo_id" INTEGER NOT NULL,
	"gitlab_id" INTEGER NOT NULL,
	"description" TEXT NULL,
	"create_date" TIMESTAMP NOT NULL,
	"archived" BOOL NOT NULL,
	"has_avatar" BOOL NOT NULL,
	"star_count" INTEGER NOT NULL,
		CONSTRAINT "pk_gitlab_repo_id" PRIMARY KEY ("repo_id", "gitlab_id")
);

CREATE TABLE "gros"."merge_request" (
	"repo_id" INTEGER NOT NULL,
	"request_id" INTEGER NOT NULL,
	"title" TEXT NULL,
	"description" TEXT NULL,
	"source_branch" VARCHAR(255) NOT NULL,
	"target_branch" VARCHAR(255) NOT NULL,
	"author" VARCHAR(500) NOT NULL,
	"assignee" VARCHAR(500) NULL,
	"upvotes" INTEGER NULL,
	"downvotes" INTEGER NULL,
	"created_date" TIMESTAMP NULL,
	"updated_date" TIMESTAMP NULL,
		CONSTRAINT "pk_merge_request_id" PRIMARY KEY("repo_id", "request_id")
);

CREATE TABLE "gros"."merge_request_note" (
	"repo_id" INTEGER NOT NULL,
	"request_id" INTEGER NOT NULL,
	"note_id" INTEGER NOT NULL,
	"author" VARCHAR(500) NOT NULL,
	"comment" TEXT NULL,
	"created_date" TIMESTAMP NULL,
		CONSTRAINT "pk_merge_request_note_id" PRIMARY KEY("repo_id", "request_id", "note_id")
);

CREATE TABLE "gros"."commit_comment" (
	"repo_id" INTEGER NOT NULL,
	"version_id" VARCHAR(100) NOT NULL,
	"author" VARCHAR(500) NOT NULL,
	"comment" TEXT NULL,
	"file" VARCHAR(1000) NULL,
	"line" INTEGER NULL,
	"line_type" VARCHAR(100) NULL
);

CREATE TABLE "gros"."reservation" (
	"reservation_id" VARCHAR(10) NOT NULL,
	"project_id" INTEGER NOT NULL,
	"requester" VARCHAR(500) NOT NULL,
	"number_of_people" INTEGER NULL,
	"description" TEXT NULL,
	"start_date" TIMESTAMP NOT NULL,
	"end_date" TIMESTAMP NOT NULL,
	"prepare_date" TIMESTAMP NULL,
	"close_date" TIMESTAMP NULL,
		CONSTRAINT "pk_reservation_id" PRIMARY KEY("reservation_id")
);

CREATE TABLE "gros"."update_tracker" (
	"project_id" INTEGER NOT NULL,
	"filename" VARCHAR(255) NOT NULL,
	"contents" TEXT NOT NULL,
	"update_date" TIMESTAMP NULL,
		CONSTRAINT "pk_update_tracker_id" PRIMARY KEY("project_id", "filename")
);
