-- Encryption fields and developer display name / link ID nullability

-- %%
-- schema: gros
-- table: issue
-- columns:
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros.issue ALTER COLUMN "encryption" SET NOT NULL;

-- %%
-- schema: gros
-- table: developer
-- columns:
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros.developer ALTER COLUMN "encryption" SET NOT NULL;

-- %%
-- schema: gros
-- table: project_developer
-- columns:
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros.project_developer ALTER COLUMN "encryption" SET NOT NULL;

-- %%
-- schema: gros
-- table: metric_version
-- columns:
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros.metric_version ALTER COLUMN "encryption" SET NOT NULL;

-- %%
-- schema: gros
-- table: comment
-- columns:
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros."comment" ALTER COLUMN "encryption" SET NOT NULL;

-- %%
-- schema: gros
-- table: ldap_developer
-- columns:
--  - name: display_name
--    action: alter
--    "null": false
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros.ldap_developer ALTER COLUMN "display_name" SET NOT NULL;
ALTER TABLE gros.ldap_developer ALTER COLUMN "encryption" SET NOT NULL;

-- %%
-- schema: gros
-- table: vcs_developer
-- columns:
--  - name: jira_dev_id
--    action: alter
--    "null": false
--  - name: display_name
--    action: alter
--    "null": false
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros.ldap_developer ALTER COLUMN "jira_dev_id" SET NOT NULL;
ALTER TABLE gros.ldap_developer ALTER COLUMN "display_name" SET NOT NULL;
ALTER TABLE gros.ldap_developer ALTER COLUMN "encryption" SET NOT NULL;

-- %%
-- schema: gros
-- table: tfs_developer
-- columns:
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros.tfs_developer ALTER COLUMN "encryption" SET NOT NULL;

-- %%
-- schema: gros
-- table: tfs_team_member
-- columns:
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros.tfs_team_member ALTER COLUMN "encryption" SET NOT NULL;

-- %%
-- schema: gros
-- table: tfs_work_item
-- columns:
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros.tfs_work_item ALTER COLUMN "encryption" SET NOT NULL;

-- %%
-- schema: gros
-- table: reservation
-- columns:
--  - name: encryption
--    action: alter
--    "null": false
-- %%
ALTER TABLE gros.reservation ALTER COLUMN "encryption" SET NOT NULL;
