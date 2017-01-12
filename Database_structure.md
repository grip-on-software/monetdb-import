# Database structure

The database structure will be split per data source.

The structure will be shown as follows:

-   Table - Description
    -   **Attribute** - Type or other (pseudo)reference
    -   **Attribute**
    -   **Attribute**


-   Table
    -   **Attribute**
    -   **Attribute**

## Jira

-   **issue**: Entries from the JIRA database. Each row is one changelog
    item. Primary key is (issue_id, changelog_id)
    -   **issue_id** - INT, **there may be multiple rows with the same
        issue_id**
    -   **changelog_id** - INT
    -   **key** - VARCHAR (10)
    -   **title** - VARCHAR(250)
    -   **type** - INT - reference to issuetype.id
    -   **priority** - INT - reference to priority.id
    -   **resolution** - INT - reference to resolution.id
    -   **fixVersion** - INT - reference to fixversion.id
    -   **bugfix** - INT
    -   **watchers** - INT
    -   **created_at** - DATE
    -   **updated_at** - DATE
    -   **description** - VARCHAR
    -   **duedate** - DATE
    -   **project_id** - INT - reference to project.project_id
    -   **status** - INT - reference to status.id
    -   **delta_comments** - VARCHAR
    -   **reporter** - VARCHAR - reference to developer.name
    -   **assignee** - VARCHAR - reference to developer.name
    -   **attachments** - INT
    -   **additional_information** - VARCHAR
    -   **review_comments** - VARCHAR
    -   **story_points** - INT
    -   **resolution_date** - DATE
    -   **sprint_id** - INT - reference to sprint.sprint_id
    -   **updated_by** - VARCHAR - reference to developer.name

Context tables:

-   **issuetype**: The types of issues and their descriptive names,
    e.g., Bug, Task, Story or Epic.
    -   **id** - primary key
    -   **name**
    -   **description**


-   **status**: The statuses that issues can have, such as Closed,
    Opened or In Progress. Actual use may differ between project.
    -   **id** - primary key
    -   **name**
    -   **description**


-   **resolution**: Once an issue receives a status of Closed, an
    indicator of why it was closed (such as Fixed, Duplicate or Works as
    designed).
    -   **id** - primary key
    -   **name**
    -   **description**


-   **developer**: Names of JIRA users that performed some action in an
    issue, including short account name and long display name.
    -   **id** - primary key
    -   **name**
    -   **display_name**


-   **fixversion**: Indicators in JIRA issues of the version to fix the
    issue before that version is released.
    -   **id** - primary key
    -   **name**
    -   **description**
    -   **release_date**


-   **priority**: An indicator of the priority of the issue, on a scale
    of 1-5 (potentially unrelated to user story points or backlog
    prioritization)
    -   **id** - primary key
    -   **name**


-   **sprint**: Data regarding a sprint registered in JIRA, including
    the start and end dates.
    -   **sprint_id** - primary key
    -   **project_id** - reference to project.project_id
    -   **name**
    -   **start_date**
    -   **end_date**


-   project: The projects that were collected. The name is the JIRA key,
    the project ID is unrelated to internal JIRA IDs.
    -   **project_id** - primary key
    -   **name**


-   **comment**: Individual comment that was added to a JIRA issue
    -   **comment_id** - primary key
    -   **issue_id** - reference to issue.issue_id
    -   **message**
    -   **author** - VARCHAR - reference to developer.name
    -   **date**

Relationship tables:

-   **issuelink**: (Bi)directional links that exist between pairs of
    issues in JIRA. Only the links that exist when the data is collected
    are stored here (no changelogs). Primary key consists of all
    columns.
    -   **id_from** - reference to issue.issue_id
    -   **id_to** - reference to issue.issue_id
    -   **relationship_type** - reference to relationshiptype.id


-   **relationshiptype**: The types of relationships that can exist
    between issues, such as Blocks, Details or Duplicate
    -   **id**
    -   **name**
    -   **description**


-   subtasks: **Not implemented in the database and data gathering thus
    far**
    -   **id_parent**
    -   **id_subtask**

## Gitlab

-   **commits**: Data from individual commits in Git repositories
    -   **commit_id** - SHA hash, primary key
    -   **project_id** - reference to project.project_id
    -   **commit_date**
    -   **sprint_id** - reference to sprint.sprint_id (based on date
        intervals)
    -   **developer_id** - reference to git_developer.alias_id
    -   **message**
    -   **size_of_commit**
    -   **message**
    -   **insertions**
    -   **deletions**
    -   **number_of_files**
    -   **number_of_lines**
    -   **type**


-   **git_developer**: User names from Gitlab commits
    -   **alias_id** - primary key
    -   **jira_dev_id** - reference to developer.id (based on
        display_name or data_gitdev_to_dev.json)
    -   **display_name**

## History Files (Jenkins)

-   metric: Metric types
    -   **metric_id**
    -   **metric_name**


-   metric_value: Singular metric data from quality report
    -   **metric_id** - reference to metric.metric_id
    -   **value**
    -   **category** - red, yellow or green
    -   **date**
    -   **project_id**
