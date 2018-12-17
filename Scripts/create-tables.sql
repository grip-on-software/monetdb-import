CREATE TABLE "gros"."issue" (
	"issue_id"       INTEGER     NOT NULL,
	"changelog_id"   INTEGER     NOT NULL,
	"key"            VARCHAR(20)   NOT NULL,
	"title"          TEXT   NOT NULL,
	"type"           INTEGER    NULL,
	"priority"       INTEGER    NULL,
	"resolution"     INTEGER    NULL,
	"fixversion"     INTEGER    NULL,
	"bugfix"         BOOLEAN    NULL,
	"watchers"       INTEGER    NOT NULL,
	"created"     TIMESTAMP  NULL,
	"updated"     TIMESTAMP  NULL,
	"description"    TEXT  NULL,
	"duedate"        DATE  NULL,
	"project_id"     INTEGER    NOT NULL,
	"status"         INTEGER    NULL,
	"reporter"       VARCHAR(64)    NULL,
	"assignee"       VARCHAR(64)    NULL,
	"attachments"    INTEGER    NOT NULL,
	"additional_information" TEXT  NULL,
	"review_comments" TEXT  NULL,
	"story_points"   DECIMAL(5,2)    NULL,
	"resolution_date"        TIMESTAMP    NULL,
	"sprint_id"      INTEGER    NULL,
	"updated_by"     VARCHAR(64)    NULL,
	"rank_change"    BOOLEAN    NULL,
	"epic"           VARCHAR(20)    NULL,
	"impediment"     BOOLEAN     NOT NULL,
	"ready_status"   INTEGER    NULL,
	"ready_status_reason" TEXT  NULL,
	"approved"       BOOLEAN    NULL,
	"approved_by_po" BOOLEAN    NULL,
	"labels"         INTEGER    NOT NULL,
	"version"        INTEGER    NULL,
	"expected_ltcs"  INTEGER    NULL,
	"expected_phtcs" INTEGER    NULL,
	"test_given"     TEXT    NULL,
	"test_when"      TEXT    NULL,
	"test_then"      TEXT    NULL,
	"test_execution" INTEGER NULL,
	"test_execution_time" INTEGER   NULL,
	"environment"    VARCHAR(100)   NULL,
	"external_project" VARCHAR(20) NULL,
	"encryption"     INTEGER   DEFAULT 0,
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
	"category_id"     INTEGER     NULL,
        CONSTRAINT "pk_status_id" PRIMARY KEY ("id")
);

CREATE TABLE "gros"."status_category" (
	"category_id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"key"            VARCHAR(32)   NOT NULL,
	"name"           VARCHAR(100)   NOT NULL,
	"color"          VARCHAR(32)     NULL,
        CONSTRAINT "pk_status_category_id" PRIMARY KEY ("category_id")
);


CREATE TABLE "gros"."resolution" (
	"id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
	"description"     VARCHAR(500)   NULL,
        CONSTRAINT "pk_resolution_id" PRIMARY KEY ("id")
);

CREATE TABLE "gros"."developer" (
	"id"              INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(64)   NOT NULL,
	"display_name"     VARCHAR(100)   NULL,
	"email"           VARCHAR(100)   NULL,
	"local_domain"    BOOLEAN NOT NULL,
	"encryption"     INTEGER   DEFAULT 0,
        CONSTRAINT "pk_developer_id" PRIMARY KEY ("id")
);

CREATE TABLE "gros"."project_developer" (
	"project_id"     INTEGER NOT NULL,
	"developer_id"   INTEGER NOT NULL,
	"name"          VARCHAR(64) NOT NULL,
	"display_name"     VARCHAR(100)   NULL,
	"email"           VARCHAR(100)   NULL,
	"encryption"   INTEGER   DEFAULT 0,
	    CONSTRAINT "pk_project_developer_id" PRIMARY KEY ("project_id", "developer_id")
);

CREATE TABLE "gros"."project_salt" (
	"project_id"      INTEGER   NOT NULL,
	"salt"       VARCHAR(32)    NOT NULL,
	"pepper"     vARCHAR(32)    NOT NULL,
		CONSTRAINT "pk_project_salt_id" PRIMARY KEY ("project_id")
);

CREATE TABLE "gros"."fixversion" (
	"id"              INTEGER     NOT NULL,
	"project_id"      INTEGER     NOT NULL,
	"name"            VARCHAR(100)   NOT NULL,
	"description"     VARCHAR(500)   NULL,
	"start_date"      DATE  NULL,
	"release_date"    DATE  NULL,
	"released"        BOOLEAN  NOT NULL,
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

CREATE TABLE "gros"."component" (
	"project_id"      INTEGER     NOT NULL,
	"component_id"    INTEGER     NOT NULL,
	"name"            VARCHAR(100)   NOT NULL,
	"description"     VARCHAR(500)   NULL,
        CONSTRAINT "pk_component_id" PRIMARY KEY ("project_id", "component_id")
);

CREATE TABLE "gros"."issue_component" (
	"issue_id"        INTEGER NOT NULL,
	"component_id"    INTEGER NOT NULL,
	"start_date"      TIMESTAMP   NULL,
	"end_date"        TIMESTAMP   NULL,
        CONSTRAINT "pk_issue_component_id" PRIMARY KEY ("issue_id","component_id")
);

CREATE TABLE "gros"."issuelink" (
	"from_key"        VARCHAR(20) NOT NULL,
	"to_key"          VARCHAR(20) NOT NULL,
	"relationship_type"     INTEGER NOT NULL,
	"outward"         BOOLEAN    NOT NULL,
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
	"name"            VARCHAR(100)   NOT NULL,
	"base_name"       VARCHAR(100)   NULL,
	"domain_name"     VARCHAR(100)   NULL
);

CREATE TABLE "gros"."metric_value" (
	"metric_id"       INTEGER     NOT NULL,
	"value"           FLOAT       NOT NULL,
	"category"        VARCHAR(100) NOT NULL,
	"date"            TIMESTAMP  NULL,
	"sprint_id"       INTEGER    NULL,
	"since_date"      TIMESTAMP  NULL,
	"project_id"      INTEGER     NOT NULL
);

CREATE TABLE "gros"."metric_version" (
	"project_id"     INTEGER     NOT NULL,
	"version_id"     VARCHAR(100)     NOT NULL,
	"developer"      VARCHAR(64) NOT NULL,
	"message"        TEXT      NULL,
	"commit_date"    TIMESTAMP NOT NULL,
	"sprint_id"      INTEGER   NULL,
	"encryption"     INTEGER   DEFAULT 0,
        CONSTRAINT "pk_metric_version_id" PRIMARY KEY ("project_id","version_id")
);

CREATE TABLE "gros"."metric_target" (
	"project_id"     INTEGER     NOT NULL,
	"version_id"     VARCHAR(100)     NOT NULL,
	"metric_id"      INTEGER     NOT NULL,
	"type"           VARCHAR(100) NOT NULL,
	"target"         INTEGER     NOT NULL,
	"low_target"     INTEGER     NOT NULL,
	"comment"        TEXT     NULL
);

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

CREATE TABLE "gros"."sprint" (
	"sprint_id"      INTEGER     NOT NULL,
	"project_id"     INTEGER     NOT NULL,
	"name"           VARCHAR(500)   NOT NULL,
	"start_date"     TIMESTAMP      NULL,
	"end_date"       TIMESTAMP      NULL,
	"complete_date"  TIMESTAMP      NULL,
	"goal"           VARCHAR(500)   NULL,
	"board_id"       INTEGER        NULL,
        CONSTRAINT "pk_sprint_id" PRIMARY KEY ("sprint_id", "project_id")
);

CREATE TABLE "gros"."project" (
	"project_id"      INTEGER     NOT NULL AUTO_INCREMENT,
	"name"            VARCHAR(100)   NOT NULL,
	"main_project"    VARCHAR(100)   NULL,
	"github_team"     VARCHAR(100)   NULL,
	"gitlab_group"    VARCHAR(100)   NULL,
	"quality_name"    VARCHAR(100)   NULL,
	"quality_display_name"    VARCHAR(100)    NULL,
	"is_support_team" BOOLEAN        NULL,
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
	"repo_id"       INTEGER         NOT NULL,
	"author_date"   TIMESTAMP       NULL,
	"branch"        VARCHAR(255)    NULL,
        CONSTRAINT "pk_commit_id" PRIMARY KEY ("version_id", "repo_id")
);

CREATE TABLE "gros"."comment" (
	"comment_id"     INTEGER        NOT NULL AUTO_INCREMENT,
	"issue_id"       INTEGER        NOT NULL,
	"message"        TEXT           NOT NULL,
	"author"         VARCHAR(64)   NOT NULL,
	"date"           TIMESTAMP      NOT NULL,
	"updater"        VARCHAR(64)   NOT NULL,
	"updated_date"   TIMESTAMP      NOT NULL,
	"encryption"     INTEGER   DEFAULT 0,
        CONSTRAINT "pk_comment_id" PRIMARY KEY ("comment_id")
);

CREATE TABLE "gros"."ldap_developer" (
	"project_id"   INTEGER NOT NULL,
	"name"         VARCHAR(64) NOT NULL, 
	"display_name" VARCHAR(100),
	"email"        VARCHAR(100) NULL,
	"jira_dev_id"  INTEGER NULL,
	"encryption"     INTEGER   DEFAULT 0,
        CONSTRAINT "pk_ldap_developer_id" PRIMARY KEY ("project_id", "name")
);

CREATE TABLE "gros"."vcs_developer" (
	"alias_id"     INTEGER       NOT NULL AUTO_INCREMENT,
	"jira_dev_id"  INTEGER,
	"display_name" VARCHAR(500),
	"email"        VARCHAR(100) NULL,
	"encryption"     INTEGER   DEFAULT 0,
        CONSTRAINT "pk_alias_id" PRIMARY KEY ("alias_id")
);

CREATE TABLE "gros"."repo" (
	"id"       INTEGER       NOT NULL AUTO_INCREMENT,
	"repo_name" VARCHAR(1000) NOT NULL,
	"project_id" INTEGER       NOT NULL,
	"type"    VARCHAR(32)    NULL,
	"url"     VARCHAR(255)   NULL,
        CONSTRAINT "pk_repo_id" PRIMARY KEY ("id")
);

CREATE TABLE "gros"."change_path" (
	"repo_id" INTEGER NOT NULL,
	"version_id" VARCHAR(100) NOT NULL,
	"file" VARCHAR(1000) NOT NULL,
	"insertions" INTEGER NOT NULL,
	"deletions" INTEGER NOT NULL,
	"type" VARCHAR(1) NOT NULL default 'M',
	"size" INTEGER NOT NULL default 0,
        CONSTRAINT "pk_change_path_id" PRIMARY KEY ("repo_id", "version_id", "file")
);

CREATE TABLE "gros"."tag" (
	"repo_id" INTEGER NOT NULL,
	"tag_name" VARCHAR(100) NOT NULL,
	"version_id" VARCHAR(100) NOT NULL,
	"message" TEXT NULL,
	"tagged_date" TIMESTAMP NULL,
	"tagger_id" INTEGER NULL,
	"sprint_id" INTEGER NULL,
		CONSTRAINT "pk_tag_id" PRIMARY KEY ("repo_id", "tag_name")
);

CREATE TABLE "gros"."vcs_event" (
	"repo_id" INTEGER NOT NULL,
	"action" VARCHAR(20) NOT NULL,
	"kind" VARCHAR(20) NOT NULL,
	"version_id" VARCHAR(100) NOT NULL,
	"ref" VARCHAR(100) NOT NULL,
	"date" TIMESTAMP NOT NULL,
	"developer_id" INTEGER NOT NULL
);

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

CREATE TABLE "gros"."gitlab_repo" (
	"repo_id" INTEGER NOT NULL,
	"gitlab_id" INTEGER NOT NULL,
	"description" TEXT NULL,
	"create_date" TIMESTAMP NOT NULL,
	"archived" BOOLEAN NOT NULL,
	"has_avatar" BOOLEAN NOT NULL,
	"star_count" INTEGER NOT NULL,
		CONSTRAINT "pk_gitlab_repo_id" PRIMARY KEY ("repo_id", "gitlab_id")
);

CREATE TABLE "gros"."github_repo" (
	"repo_id" INTEGER NOT NULL,
	"github_id" INTEGER NOT NULL,
	"description" TEXT NULL,
	"create_date" TIMESTAMP NOT NULL,
	"private" BOOLEAN NOT NULL,
	"forked" BOOLEAN NOT NULL,
	"star_count" INTEGER NOT NULL,
	"watch_count" INTEGER NOT NULL,
        CONSTRAINT "pk_github_repo_id" PRIMARY KEY ("repo_id", "github_id")
);

CREATE TABLE "gros"."github_issue" (
	"repo_id" INTEGER NOT NULL,
	"issue_id" INTEGER NOT NULL,
	"title" TEXT NULL,
	"description" TEXT NULL,
	"status" VARCHAR(100) NULL,
	"author_id" INTEGER NOT NULL,
	"assignee_id" INTEGER NULL,
	"created_date" TIMESTAMP NULL,
	"updated_date" TIMESTAMP NULL,
	"pull_request_id" INTEGER NULL,
	"labels" INTEGER NULL,
	"closed_date" TIMESTAMP NULL,
	"closer_id" INTEGER NULL,
		CONSTRAINT "pk_github_issue_id" PRIMARY KEY ("repo_id", "issue_id")
);

CREATE TABLE "gros"."github_issue_note" (
	"repo_id" INTEGER NOT NULL,
	"issue_id" INTEGER NOT NULL,
	"note_id" INTEGER NOT NULL,
	"author_id" INTEGER NOT NULL,
	"comment" TEXT NULL,
	"created_date" TIMESTAMP NULL,
	"updated_date" TIMESTAMP NULL,
		CONSTRAINT "pk_github_issue_note_id" PRIMARY KEY ("repo_id", "issue_id", "note_id")
);

CREATE TABLE "gros"."merge_request" (
	"repo_id" INTEGER NOT NULL,
	"request_id" INTEGER NOT NULL,
	"title" TEXT NULL,
	"description" TEXT NULL,
	"status" VARCHAR(100) NULL,
	"source_branch" VARCHAR(255) NOT NULL,
	"target_branch" VARCHAR(255) NOT NULL,
	"author_id" INTEGER NOT NULL,
	"assignee_id" INTEGER NULL,
	"upvotes" INTEGER NULL,
	"downvotes" INTEGER NULL,
	"created_date" TIMESTAMP NULL,
	"updated_date" TIMESTAMP NULL,
	"sprint_id" INTEGER NULL,
		CONSTRAINT "pk_merge_request_id" PRIMARY KEY ("repo_id", "request_id")
);

CREATE TABLE "gros"."merge_request_review" (
	"repo_id" INTEGER NOT NULL,
	"request_id" INTEGER NOT NULL,
	"reviewer_id" INTEGER NOT NULL,
	"vote" INTEGER NOT NULL,
		CONSTRAINT "pk_merge_request_review_id" PRIMARY KEY ("repo_id", "request_id", "reviewer_id")
);

CREATE TABLE "gros"."merge_request_note" (
	"repo_id" INTEGER NOT NULL,
	"request_id" INTEGER NOT NULL,
	"thread_id" INTEGER NOT NULL,
	"note_id" INTEGER NOT NULL,
	"parent_id" INTEGER NULL,
	"author_id" INTEGER NOT NULL,
	"comment" TEXT NULL,
	"created_date" TIMESTAMP NULL,
	"updated_date" TIMESTAMP NULL,
		CONSTRAINT "pk_merge_request_note_id" PRIMARY KEY ("repo_id", "request_id", "thread_id", "note_id")
);

CREATE TABLE "gros"."commit_comment" (
	"repo_id" INTEGER NOT NULL,
	"version_id" VARCHAR(100) NOT NULL,
	"request_id" INTEGER NOT NULL,
	"thread_id" INTEGER NOT NULL,
	"note_id" INTEGER NOT NULL,
	"parent_id" INTEGER NULL,
	"author_id" INTEGER NOT NULL,
	"comment" TEXT NULL,
	"file" VARCHAR(1000) NULL,
	"end_line" INTEGER NULL,
	"line" INTEGER NULL,
	"line_type" VARCHAR(100) NULL,
	"created_date" TIMESTAMP NULL,
	"updated_date" TIMESTAMP NULL
);

CREATE TABLE "gros"."source_environment" (
	"project_id" INTEGER NOT NULL,
	"source_type" VARCHAR(32) NOT NULL,
	"url" VARCHAR(255) NOT NULL,
	"environment" VARCHAR(500) NOT NULL,
	"version" VARCHAR(32) NOT NULL DEFAULT '',
		CONSTRAINT "pk_source_environment_id" PRIMARY KEY ("project_id", "environment")
);

CREATE TABLE "gros"."source_id" (
	"project_id" INTEGER NOT NULL,
	"domain_name" VARCHAR(100) NOT NULL,
	"url" VARCHAR(255) NOT NULL,
	"source_id" VARCHAR(100) NOT NULL,
		CONSTRAINT "pk_source_id" PRIMARY KEY ("project_id", "domain_name", "url")
);

CREATE TABLE "gros"."jenkins" (
	"project_id" INTEGER NOT NULL,
	"host" VARCHAR(255) NOT NULL,
	"jobs" INTEGER NOT NULL,
	"views" INTEGER NOT NULL,
	"nodes" INTEGER NOT NULL,
		CONSTRAINT "pk_jenkins_id" PRIMARY KEY ("project_id", "host")
);

CREATE TABLE "gros"."bigboat_status" (
	"project_id" INTEGER NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"checked_date" TIMESTAMP NOT NULL,
	"ok" BOOLEAN NOT NULL,
	"value" FLOAT NULL,
	"max" FLOAT NULL
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
	"sprint_id" INTEGER NULL,
	"encryption"     INTEGER   DEFAULT 0,
		CONSTRAINT "pk_reservation_id" PRIMARY KEY ("reservation_id")
);

CREATE TABLE "gros"."seats" (
	"project_id" INTEGER NOT NULL,
	"sprint_id" INTEGER NOT NULL,
	"date" TIMESTAMP NOT NULL,
	"seats" FLOAT NULL,
		CONSTRAINT "pk_seat_range_id" PRIMARY KEY ("project_id", "sprint_id", "date")
);

CREATE TABLE "gros"."update_tracker" (
	"project_id" INTEGER NOT NULL,
	"filename" VARCHAR(255) NOT NULL,
	"contents" TEXT NOT NULL,
	"update_date" TIMESTAMP NULL,
		CONSTRAINT "pk_update_tracker_id" PRIMARY KEY ("project_id", "filename")
);
