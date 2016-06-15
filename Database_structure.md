# Database structure

The database structure will be split per data source.

The structure will be shown as follows:

-   Table
    -   Attribute
    -   Attribute
    -   Attribute


-   Table
    -   Attribute
    -   Attribute

## Jira

-   issues
    -   id - INT
    -   key - VARCHAR (10)
    -   title - VARCHAR(250)
    -   type - INT
    -   priority - INT
    -   resolution - INT
    -   fixVersion - INT
    -   bugfix - INT
    -   watchers - INT
    -   created_at - DATE
    -   updated_at - DATE
    -   description - VARCHAR
    -   duedate - DATE
    -   project_id - INT
    -   status - INT
    -   delta_comments - VARCHAR
    -   reporter - INT
    -   assignee - INT
    -   attachments - INT
    -   additional_information - VARCHAR
    -   review_comments - VARCHAR

Context tables:

-   issueTypes
    -   id
    -   name
    -   description


-   status
    -   id
    -   name
    -   description


-   resolutions
    -   id
    -   name
    -   description


-   developers
    -   id
    -   name
    -   display_name


-   fixVersions
    -   id
    -   name
    -   description
    -   release_date


-   priorities
    -   id
    -   name


-   relationshipTypes
    -   id
    -   name
    -   description

Relationship tables:

-   issueLinks
    -   id_from
    -   id_to
    -   relationship_type


-   subtasks
    -   id_parent
    -   id_subtask
