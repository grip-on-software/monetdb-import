-- %%
-- schema: gros
-- table: ldap_developer
-- action: create
-- %%

CREATE TABLE "gros"."ldap_developer" (
	"project_id"   INTEGER NOT NULL,
	"name"         VARCHAR(64) NOT NULL, 
	"display_name" VARCHAR(100),
	"email"        VARCHAR(100) NULL,
	"jira_dev_id"  INTEGER NULL,
	"encryption"     INTEGER   DEFAULT 0,
        CONSTRAINT "pk_ldap_developer_id" PRIMARY KEY ("project_id", "name")
);
