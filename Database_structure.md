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

## Issue tables (Jira)

-   **issue**: Entries from the JIRA database. Each row is one changelog
    item. Primary key is (issue_id, changelog_id)
    -   **issue_id** - INT: Internal Jira identifier. **There may be
        multiple rows with the same issue_id.**
    -   **changelog_id** - INT: Version number deduced from the
        changelog. The earliest version is has a changelog id of zero.
    -   **key** - VARCHAR (10): The Jira key. **There may be multiple
        rows with the same key.**
    -   **title** - VARCHAR(250): The human-readable title of the issue.
        Can be updated in changes.
    -   **type** - INT - reference to issuetype.id: The issue type
        (Story, Bug, Use Case) as an internal Jira identifier.
    -   **priority** - INT - reference to priority.id: The issue
        priority (Low, Medium High) as an internal scale.
    -   **resolution** - INT - reference to resolution.id: The issue
        resolution (Fixed, Duplicate, Works as designed) as an internal
        Jira identifier
    -   **fixVersion** - INT - reference to fixversion.id: The earliest
        version in which the issue is fixed as an internal Jira
        identifier, or NULL if not provided.
    -   **bugfix** - BOOL: Whether this is a bugfix issue (possibly
        deduced from the issue type)
    -   **watchers** - INT: The number of watchers of this issue at the
        moment that the data is scraped.
    -   **created** - TIMESTAMP: The time at which the issue was
        created, equal to updated_at of the first version. Provided as a
        convenience on all versions.
    -   **updated** - TIMESTAMP: The time at which the change in this
        version of the issue was made. Equal to *created* for the first
        version.
    -   **description** - TEXT: The human-readable description that is
        shown at the top of the issue by default. Can be updated in
        changes.
    -   **duedate** - DATE: The due date of this issue, or NULL if not
        provided. Some projects use due dates for milestone and issue
        prioritization, in addition to sprint constraints.
    -   **project_id** - INT - reference to project.project_id: The Jira
        project in which this issue is contained.
    -   **status** - INT - reference to status.id: The issue status
        (Open, In Progress, Resolved, Closed) as an internal Jira
        identifier.
    -   **delta_comments** - TEXT: **Currently unused.**
    -   **reporter** - VARCHAR - reference to developer.name: The
        reporter of the issue according to the issue data. Usually, this
        is the same developer as *updated_by* on the first version, but
        this may be different due to cloned issues or intermediate
        changes.
    -   **assignee** - VARCHAR - reference to developer.name: The
        developer that should resolve the issue, or NULL if none is
        assigned thus far.
    -   **attachments** - INT: The number of attachments in the issue.
        Updated according to changed in the changelog.
    -   **additional_information** - TEXT: Human-readable text that is
        shown within a tab beside the description, or NULL if it is not
        filled in. Often used for extra implementation/functional
        details for user stories.
    -   **review_comments** - VARCHAR: Human-readable text that is shown
        in a Review tab beside the description, or '0' if it is not
        filled in. Updated by reviewing developers to check whether the
        story is complete enough to start with it or that an issue is
        fixed.
    -   **story_points** - DECIMAL: The number of points assigned to a
        story after the developers meet in a refinement and determine
        the difficulty of the story. If not yet set, then this is NULL.
        Maximum value is 100 and one digit after the decimal point is
        allowed.
    -   **resolution_date** - TIMESTAMP: The time at which the issue was
        marked as resolved. This may be manually adjusted, but should
        match roughly with the time at which a change is made to mark
        the issue as Resolved or Closed. If the issue is still open,
        then this is NULL.
    -   **sprint_id** - INT - reference to sprint.sprint_id: The sprint
        that this issue is pulled into. The sprint can differ by version
        if a change is made or if the issue has multiple sprints
        attached to it (due to mismatch with parent tasks or manual
        changes). If the issue is not yet (explicitly) added into a
        sprint, then this is the integer 0.
    -   **updated_by** - VARCHAR - reference to developer.name: The
        developer that made a change in this version of the issue.
    -   **rank_change** - BOOL: The rank change performed in this
        change. Possible values are *true*, meaning an increase in rank,
        *false*, meaning a decrease in rank, or NULL, which means that
        the rank was not changed. The actual rank cannot be deduced from
        the changes due to dependencies on the ranks of other issues.
    -   **epic** - VARCHAR: The issue key of the Epic link.
    -   **impediment** - BOOL: Whether the issue is currently marked as
        being blocked by an Impediment.
    -   **ready_status** - INTEGER - reference to ready_status.id: The
        refinement ready status as an internal Jira identifier.
    -   **ready_status_reason** - TEXT: Additional text provided in the
        Review tab of the issue to describe why it currently has the
        given ready status.
    -   **approved** - BOOL: The Yes/No value of the Approved field, or
        NULL if it is not set. Not all projects use this field,
        preferring review comments, ready states or similar fields.
    -   **approved_by_po** - BOOL: The Yes/No value of the Approved by
        Product Owner field, or NULL if it is not set. Not all projects
        use this field, preferring review comments, ready states or
        similar fields.
    -   **labels** - INT: The number of labels that the issue currently
        has.
    -   **version** - INT - reference to fixversion.id: The first
        reported affected version that the issue describes a particular
        bug of. If this is not set, then the field value is 0.
    -   **expected_ltcs** - INT: The number of expected logical test
        cases that are needed to sufficiently test the implementation of
        this story, use case or issue. If this is not set, then it is
        the integer 0.
    -   **expected_phtcs** - INT: The number of expected physical test
        cases that are needed to sufficiently test the implementation of
        this story, use case or issue. If this is not set, then it is
        the integer 0.
    -   **test_given** - TEXT: The human-readable description of the
        Given part of a test model for a particular use case.
    -   **test_when** - TEXT: The human-readable description of the When
        part of a test model for a particular use case.
    -   **test_then** - TEXT: The human-readable description of the Then
        part of a test model for a particular use case.
    -   **test_execution** - INT - reference to test_execution.id: The
        test execution model (Manual, Automated, Will not be tested) as
        an internal Jira identifier.
    -   **test_execution_time** - INT: Units of time that the test
        execution appears to take. This is often set after the use case
        is resolved and tested. If this is not set, then it is the
        integer 0.

### Context tables

-   **sprint**: Data regarding a sprint registered in JIRA, including
    the start and end dates.
    -   **sprint_id** - primary key
    -   **project_id** - reference to project.project_id
    -   **name**
    -   **start_date**
    -   **end_date**


-   **project**: The projects that were collected. The name is the JIRA
    key, the project ID is unrelated to internal JIRA IDs.
    -   **project_id** - primary key
    -   **name**


-   **comment**: Individual comment that was added to a JIRA issue
    -   **comment_id** - primary key
    -   **issue_id** - reference to issue.issue_id
    -   **message**
    -   **author** - VARCHAR - reference to developer.name
    -   **date**

### Metadata tables

-   **issuetype**: The types of issues and their descriptive names,
    e.g., Bug, Task, Story or Epic.
    -   **id** - primary key
    -   **name**
    -   **description**


-   **status**: The statuses that issues can have, such as Closed,
    Resolved, Opened or In Progress. Actual use may differ between
    project based on Sprint board setup.
    -   **id** - primary key
    -   **name**
    -   **description**


-   **resolution**: Once an issue receives a status of Resolved or
    Closed, an indicator of why it was closed (such as Fixed, Duplicate
    or Works as designed).
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
    of 1-5, with names like Low, Medium, High. This is not necessarily
    related to user story points or backlog prioritization.
    -   **id** - primary key
    -   **name**


-   **ready_status**: An indicator of the pre-refinement ready state:
    Whether the user story can be pulled into the next stage such as
    Workshops, Design meetings, and Refinements. It may also be Blocked
    for or by any of these stages.
    -   **id** - INT - primary key
    -   **name** - VARCHAR: Human-readable short description of the
        ready status: 'Ready for refinement', 'Blocked for design'


-   **test_execution**: The state of how a use case is tested.
    -   **id** - INT - primary key
    -   **value** - VARCHAR: 'Manual', 'Automated' or 'Will not be
        executed'

### Relationship tables

-   **issuelink**: Bidirectional links that exist between pairs of
    issues in JIRA. Only the links that exist when the data is collected
    are stored here (no changelogs). Primary key consists of all
    columns.
    -   **id_from** - reference to issue.issue_id
    -   **id_to** - reference to issue.issue_id
    -   **relationship_type** - reference to relationshiptype.id

    :   **Note: the issuelink table has been updated to provide more
        data, the new format is as follows:** Primary key consists of
        (from_key, to_key, outward, relationship_type).

    -   **from_key** - VARCHAR - reference to issue.key: Issue involved
        in the link.
    -   **to_key** - VARCHAR - reference to issue.key: Another issue
        involved in the link.
    -   **outward** - BOOL: Whether the given entry is outward from
        *from_key* (true) or inward to it (false).
    -   **relationship_type** - reference to relationshiptype.id: The
        type of the link relationship as an internal Jira identifier.
    -   **start_date** - TIMESTAMP - reference to issue.updated: Point
        in time when the link was first added to the *from_key* issue,
        or NULL if not known.
    -   **end_date** - TIMESTAMP - reference to issue.updated: Point in
        time when the link was last removed from the *from_key* issue,
        or NULL if the link still exists.


-   **relationshiptype**: The types of relationships that can exist
    between issues, such as Blocks, Details or Duplicate
    -   **id** - INT - primary key: The Jira identifier of this issue
        link relationship type
    -   **name** - VARCHAR: Textual name of the relationship, as a noun
        or verb

    :   **The following fields do not yet exist, but may be added at a
        later stage:**

    -   **outward** - VARCHAR: Phrase used to describe the outward
        relation, such as 'blocks' or 'duplicates'
    -   **inward** - VARCHAR: Phrase used to describe the inward
        relation, such as 'is blocked by' or 'is duplicated by'


-   **subtasks**: Links that exists between issues and their subtasks,
    different from the issue links.
    -   **id_parent** - reference to issue.issue_id: The parent issue.
    -   **id_subtask** - reference to issue.issue_id: The subtask issue

## Version control system tables

These tables include data from Gitlab/Git and Subversion.

-   **commits**: Data from individual commits in Git or repositories
    -   **version_id** - SHA hash, primary key
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


-   **git_developer**: User names from VCS commits
    -   **alias_id** - primary key
    -   **jira_dev_id** - reference to developer.id (based on
        display_name or data_gitdev_to_dev.json)
    -   **display_name**

## Metrics Files (Quality dashboard history)

-   **metric**: Metric types
    -   **metric_id**
    -   **metric_name**


-   **metric_value**: Singular metric data from quality report
    -   **metric_id** - INT - reference to metric.metric_id: The metric
        that is measured
    -   **value** - INT: The raw value. This is -1 if there is a problem
        with measuring the metric.
    -   **category** - VARCHAR: 'red' (below low target), 'yellow'
        (below target), green (at or above target), perfect (cannot be
        improved), grey (disabled), missing (internal problem),
        missing_source (external problem).
    -   **date** - TIMESTAMP: Time at which the measurement took place.
    -   **since_date** - TIMESTAMP: Time since which the metric has the
        same value.
    -   **project_id** - INT - reference to project.project_id


-   **metric_version**: Versions of the project definition in which
    changes to target norms of a project are made. Primary key is
    (project_id, version_id)
    -   **project_id** - INT - reference to project.project_id
    -   **version_id** - INT: Subversion revision number
    -   **developer** - VARCHAR: Developer or quality lead that made the
        change
    -   **message** - TEXT: Commit message describing the change
    -   **commit_date** - TIMESTAMP: Time at which the target change
        took place


-   **metric_target**: Manual changes to the metric targets of a project
    -   **project_id** - INT - reference to project.project_id
    -   **version_id** - INT - reference to metric_version.version_id
    -   **metric_id** - INT - reference to metric.metric_id: Metric
        whose norms are changed
    -   **type** - VARCHAR: Type of change: options, old_options,
        TechnicalDebtTarget
    -   **target** - INT
    -   **low_target** - INT
    -   **comment** - TEXT: Comment for technical debt targets
        describing the reason of the norm change
