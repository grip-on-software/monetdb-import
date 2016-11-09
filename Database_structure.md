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
    -   issue_id - INT
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
    -   story_points - INT
    -   resolution_date - DATE
    -   sprint_id - INT
    -   updated_by - VARCHAR

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


-   sprints
    -   sprint_id
    -   project_id
    -   name
    -   start_date
    -   end_date


-   project
    -   project_id
    -   name


-   Comments
    -   comment_id
    -   issue_id
    -   message
    -   author
    -   date

Relationship tables:

-   issueLinks
    -   id_from
    -   id_to
    -   relationship_type


-   subtasks
    -   id_parent
    -   id_subtask

## Gitlab

-   Commits
    -   commit_id
    -   project_id
    -   commit_date
    -   sprint_id
    -   developer_id
    -   message
    -   size_of_commit
    -   message
    -   insertions
    -   deletions
    -   number_of_files
    -   number_of_lines
    -   type

## History Files (Jenkins)

-   Metrics
    -   metric_id
    -   metric_name


-   Metric_values
    -   metric_id
    -   value
    -   date
    -   project_id

## History file

## Gitlab

-   commits
    -   commit_id
    -   commit_date
    -   sprint_id
    -   developer_id
    -   message
    -   size_of_commit
    -   insertions
    -   deletions
    -   number_of_files
    -   number_of_lines
    -   type
