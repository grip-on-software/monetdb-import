CREATE TABLE "gros"."issue" (
	"issue_id"       INTEGER     NOT NULL,
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
        "sprint_id"      INTEGER    NOT NULL 
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
	"value"           VARCHAR(500)   NOT NULL,
        "date"            TIMESTAMP  NULL,
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

CREATE TABLE "gros"."git_features" (
	"feature_id"      INTEGER     NOT NULL AUTO_INCREMENT,
	"feature_name"    VARCHAR(500)   NOT NULL,
	"feature_value"   DOUBLE   NULL,
	"sprint_id"       INTEGER  NULL,
        "user_id"         INTEGER  NULL DEFAULT 0,
        "user_name"       VARCHAR(500)  NULL   
);


