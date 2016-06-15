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
    -   id
    -   key
    -   title
    -   type
    -   priority
    -   resolution
    -   fixVersion
    -   bugfix
    -   watchers
    -   created_at
    -   updated_at (this is the data of a transition)
    -   description
    -   due date
    -   project_id
    -   status
    -   delta_comments
    -   reporter
    -   assignee
    -   attachments
    -   additional_information
    -   review_comments

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
