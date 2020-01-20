# Database structure

The database structure is shown here divided by the data sources from
which we retrieve the contents. The structure of each table is shown as
follows:

-   **Table**: Description
    -   **Attribute** - TYPE - (pseudo)reference: Description
    -   **Attribute** - TYPE: Description with mention of NULL if the
        field is nullable in some conditions
    -   **Attribute** - TYPE(Special field type)
    -   **Attribute** - TYPE

The schema documentation on this page is parsed by
[validate_schema.py](http://git.liacs.nl/gros/monetdb-import/blob/master/Scripts/validate_schema.py)
to compare against the schema definition
[create-tables.sql](http://git.liacs.nl/gros/monetdb-import/blob/master/Scripts/create-tables.sql).
Therefore, all table structures adhere to this format.

Some attributes may mention their fields as being nullable by indicating
the term `NULL` in their description. In some cases the NULL values are
actually not in use because the data gatherer and importer do not
provide such values to the database. However, in the future, the NULL
values could be in use for the condition as is reserved on this page.
Also note the (current) use of other types of values to indicate missing
relations or unobtainable data, such as zeroes or empty strings.

Additional details about how the data is imported can be found by
browsing the
[Javadoc](http://www.jenkins.example:8080/job/build-monetdb-import/job/master/Javadoc/)
of the importer, especially the dao package.

Development of new schemas on branches of the monetdb-import repository
may also create a development subpage for validation of the schema
changes in the build in comparison to the documentation on this page as
well as its overrides in the subpage. Additionally, an upgrade path is
provided through new files in the
[update](http://git.liacs.nl/gros/monetdb-import/tree/master/Scripts/update)
tree on the branch. When the branch is merged, the subpage should be
merged into this page and deleted.

## Special field types

The following types are aliases for an actual MonetDB type
specification. They describe the constraints set on the values stored in
such fields more thoroughly and uniformly.

-   **VARCHAR(Issue key)**: A field containing a JIRA issue key. The
    maximum length limitation is 20 characters. This field holds project
    keys, a one-character delimiter and issue sequence numbers.
-   **VARCHAR(JIRA developer)**: A field containing a developer short
    name from JIRA. The maximum length limitation is 64 characters for
    all fields for encryption purposes, even though the unencrypted
    source data is usually not more than 6 characters.
-   **VARCHAR(Git branch)**: A field containing a Git branch object
    name. The maximum length limitation is 255 characters.
-   **INT(row encryption)**: A field specifying which encryption level
    is applied to certain fields in the given row has encrypted fields.
    This field exists in tables with [sensitive
    data](sensitive_data), specifically those with personally
    identifying information. The field levels bitmask is simply an
    integer, where 0 means no encryption, 1 is encryption using
    project-specific salts, 2 is encryption with global
    (project-independent) salt and 3 means project-specific then global
    salt encryption. This can be extended later on with other masks to
    indicate which parts of the row are encrypted, e.g., only personal
    names, project-identifying data. The field can be used to check
    whether encryption still needs to be done, or that the sensitive
    fields can only be used in an encrypted fashion. We can only check
    whether such as field holds a certain value if we have the original
    value as well as the global or project-specific salts. The field can
    be matched against other fields with the same encryption level.

Where applicable, we describe the semantics of the field types in text.
If a field can be NULL, then we explain the circumstances when this
occurs. Some fields instead store a zero value for missing data, either
as integer or string, but NULL should be the preferred storage for this
purpose.

## Issue tables (JIRA)

-   **issue**: Entries from the JIRA database. Each row is one changelog
    item, and contains fields derived from the global issue information
    or the changelog version. Primary key is (issue_id, changelog_id).
    -   **issue_id** - INT: Internal JIRA identifier. **There may be
        multiple rows with the same issue_id.**
    -   **changelog_id** - INT: Version number deduced from the
        changelog. The earliest version is has a changelog id of 0.
    -   **key** - VARCHAR(Issue key): The JIRA issue key. **There may be
        multiple rows with the same key.**
    -   **title** - TEXT: The human-readable title of the issue. This
        reflects the title at the moment the issue version existed,
        based on the changelog.
    -   **type** - INT - reference to issuetype.id: The issue type
        (Story, Bug, Use Case) as an internal JIRA identifier, or NULL
        if this information is not provided by the API.
    -   **priority** - INT - reference to priority.id: The issue
        priority (Low, Medium High) as an internal scale, or NULL if
        this information is not provided by the API.
    -   **resolution** - INT - reference to resolution.id: The issue
        resolution (Fixed, Duplicate, Works as designed) as an internal
        JIRA identifier, or NULL if this information is not provided by
        the API.
    -   **fixversion** - INT - reference to fixversion.id: The earliest
        version in which the issue is fixed as an internal JIRA
        identifier, or NULL if not provided.
    -   **bugfix** - BOOL: Whether this is a bugfix issue (possibly
        deduced from the issue type), or NULL if not provided.
    -   **watchers** - INT: The number of watchers of this issue at the
        moment that the data is scraped.
    -   **created** - TIMESTAMP: The time at which the issue was
        created, equal to updated_at of the first version. Provided as a
        convenience on all versions. This is NULL if the information is
        not provided by the API.
    -   **updated** - TIMESTAMP: The time at which the change in this
        version of the issue was made. Equal to *created* for the first
        version. This is NULL if the information is not provided by the
        API.
    -   **description** - TEXT: The human-readable description that is
        shown at the top of the issue by default. Can be updated in
        changes. This is NULL if not provided.
    -   **duedate** - DATE: The due date of this issue, or NULL if not
        provided. Some projects use due dates for milestone and issue
        prioritization, in addition to sprint constraints.
    -   **project_id** - INT - reference to project.project_id: The JIRA
        project in which this issue is contained. This may **differ from
        the project in the latest version of the issue** if the issue
        was created in a different project before being moved to this
        project. If such a projects is otherwise not in the database,
        then this points to a dummy project which is a subproject of the
        later project.
    -   **status** - INT - reference to status.id: The issue status
        (Open, In Progress, Resolved, Closed) as an internal JIRA
        identifier, or NULL if this information is not provided by the
        API.
    -   **reporter** - VARCHAR(JIRA developer) - reference to
        developer.name: The reporter of the issue according to the issue
        data. Usually, this is the same developer as *updated_by* on the
        first version, but this may be different due to cloned issues or
        intermediate changes. This is NULL if this information is not
        provided by the API.
    -   **assignee** - VARCHAR(JIRA developer) - reference to
        developer.name: The developer that should resolve the issue, or
        NULL if none is assigned thus far.
    -   **attachments** - INT: The number of attachments in the issue.
        This tracks updates in the changelog.
    -   **additional_information** - TEXT: Human-readable text that is
        shown within a tab beside the description, or NULL if it is not
        filled in. Often used for extra implementation/functional
        details for user stories.
    -   **review_comments** - TEXT: Human-readable text that is shown in
        a Review tab beside the description, or NULL if it is not filled
        in. Updated by reviewing developers to check whether the story
        is complete enough to start with it or that an issue is fixed.
    -   **story_points** - DECIMAL(5,2): The number of points assigned
        to a story after the developers meet in a refinement and
        determine the difficulty of the story. If not yet set, then this
        is NULL. Maximum value is 999 and two digits after the decimal
        point is allowed.
    -   **resolution_date** - TIMESTAMP: The time at which the issue was
        marked as resolved. This may be manually adjusted, but should
        match roughly with the time at which a change is made to mark
        the issue as Resolved or Closed. If the issue is (still) open in
        this version, then this is NULL.
    -   **sprint_id** - INT - reference to sprint.sprint_id: The sprint
        that this issue is pulled into. The sprint can differ by version
        if a change is made or if the issue has multiple sprints
        attached to it (due to mismatch with parent tasks or manual
        changes). In the case of multiple candidate sprints, the latest
        sprint which contains the changelog version updated date is
        used. If the issue is not yet explicitly added into a sprint,
        then this is NULL. For example, subtasks may have a sprint ID of
        0 while only the parent task is in a sprint.
    -   **updated_by** - VARCHAR(JIRA developer) - reference to
        developer.name: The developer that made a change in this version
        of the issue. This is NULL if this information is not provided
        by the API.
    -   **rank_change** - BOOL: The rank change performed in this
        change. Possible values are *true*, meaning an increase in rank,
        *false*, meaning a decrease in rank, or NULL, which means that
        the rank was not changed. The actual rank cannot be deduced from
        the changes due to dependencies on the ranks of other issues.
    -   **epic** - VARCHAR(Issue key): The issue key of the Epic link,
        or NULL if the issue is not linked to an Epic.
    -   **impediment** - BOOL: Whether the issue is currently marked as
        being blocked by an Impediment.
    -   **ready_status** - INT - reference to ready_status.id: The
        refinement ready status as an internal JIRA identifier, or NULL
        if the issue does not have a ready status.
    -   **ready_status_reason** - TEXT: Additional text provided in the
        Review tab of the issue to describe why it currently has the
        given ready status. This is NULL if the issue has no ready
        status or reason.
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
        bug of. If this is not set, then the field value is NULL.
    -   **expected_ltcs** - INT: The number of expected logical test
        cases that are needed to sufficiently test the implementation of
        this story, use case or issue. If this is not set, then the
        field value is NULL.
    -   **expected_phtcs** - INT: The number of expected physical test
        cases that are needed to sufficiently test the implementation of
        this story, use case or issue. If this is not set, then the
        field value is NULL.
    -   **test_given** - TEXT: The human-readable description of the
        Given part of a test model for a particular use case, or NULL if
        the issue does not have a test description.
    -   **test_when** - TEXT: The human-readable description of the When
        part of a test model for a particular use case, or NULL if the
        issue does not have a test description.
    -   **test_then** - TEXT: The human-readable description of the Then
        part of a test model for a particular use case, or NULL if the
        issue does not have a test description.
    -   **test_execution** - INT - reference to test_execution.id: The
        test execution model (Manual, Automated, Will not be tested) as
        an internal JIRA identifier, or NULL if the issue does not have
        a test model.
    -   **test_execution_time** - INT: Units of time that the test
        execution appears to take. This is often set after the use case
        is resolved and tested. If this is not set, then the field value
        is NULL.
    -   **environment** - VARCHAR(100): The environment to which the
        issue applies. This can apply to a DTAP environment, a component
        of the application, or other kinds of environments that the team
        may define. This is NULL if the issue does not have an
        environment specified.
    -   **external_project** - VARCHAR(20): The JIRA project key prefix
        for which this issue was created, in case the issue is created
        in a support team board. This is NULL if the external project is
        not defined. This may reference another project in the *project*
        table, although this is a weak reference and projects that have
        no data of its own may either not exist there or have
        *main_project* set to this issue's project.
    -   **encryption** - INT(row encryption)

### Context tables

-   **sprint**: Data regarding a sprint registered in JIRA, including
    the start and end dates.
    -   **sprint_id** - INT - primary key: Internal JIRA identifier of
        the sprint.
    -   **project_id** - INT - reference to project.project_id: Project
        in which the sprint occurs. There may be multiple projects which
        use the same sprint ID, for example in subprojects and projects
        that are being developed by the same teams.
    -   **name** - VARCHAR(500): Human-readable name of the sprint.
        Depending on the project and the person who creates the sprints,
        this may include a sequence number (not unique), the
        sprint/story owner (Scrum master), week/calendar date, target
        version and/or the sprint topic/goal if it is a specialized
        sprint.
    -   **start_date** - TIMESTAMP: Moment in time at which the sprint
        starts or is set to start. May differ from the actual start time
        (but usually not by more than a day). This is NULL if the start
        date is not yet known.
    -   **end_date** - TIMESTAMP: Moment in time at which the sprint
        ends or is set to end. May be a projected date from the start
        and thus has a time which is not always correct, but is usually
        close to the real date. This is NULL if the end date is not yet
        known.
    -   **complete_date** - TIMESTAMP: Moment in time at which the tasks
        in the sprint are completed. This may be different from the
        latest change to an issue within the sprint, and is NULL if it
        is not provided for the sprint.
    -   **goal** - VARCHAR(500): Human-readable description of what the
        primary goal of the sprint is. This may be related to product
        delivery or the main task for the sprint. Previously this was
        sometimes provided in the sprint name instead. This is NULL if
        no goal is provided to the sprint
    -   **board_id** - INT: Internal JIRA identifier of the primary
        board to which this sprint is added. This is NULL if the
        relation to the board is not available.


-   **project**: The projects that were collected and additional
    metadata. The name is the JIRA key, the project ID is unrelated to
    internal JIRA IDs.
    -   **project_id** - INT - primary key: Sequence number of the
        project when it is inserted into the database.
    -   **name** - VARCHAR(100): JIRA key prefix abbreviation.
    -   **main_project** - VARCHAR(100): JIRA key of the main project in
        case that this project is a subproject of another. If this
        project is a main project or we have no information about
        project relations, then this is NULL.
    -   **github_team** - VARCHAR(100): GitHub public identifier of a
        team within an organization that maintains the GitHub
        repositories of the project. If no such GitHub team is known to
        exist, then this is NULL.
    -   **gitlab_group** - VARCHAR(100): GitLab public identifier of the
        main group in which the GitLab repositories of the project are
        stored. This is determined by the sources and credentials
        available for the GitLab instance. This is NULL if there are no
        GitLab sources or if the information is not available.
    -   **quality_name** - VARCHAR(100): Public identifier of the
        project in the quality reporting dashboard, used by the project
        definition and history as storage location. This is NULL if the
        information is not available.
    -   **quality_display_name** - VARCHAR(100): Human-readable name of
        the project as shown in the quality reporting dashboard. This is
        NULL if the information is not available.
    -   **is_support_team** - BOOLEAN: Whether the project is maintained
        by a support team. This is NULL if the information is not
        available.
    -   **jira_name** - VARCHAR(100): Human-readable name of the project
        as shown in JIRA. This is NULL if the information is not
        available.


-   **comment**: Individual comment that was added to a JIRA issue.
    -   **comment_id** - INT - primary key: Internal JIRA identifier of
        the comment.
    -   **issue_id** - INT - reference to issue.issue_id: Issue to which
        the comment is posted.
    -   **message** - TEXT: The current message contents, including
        edits.
    -   **author** - VARCHAR(JIRA developer) - reference to
        developer.name: Developer that wrote the message initially.
    -   **date** - TIMESTAMP: Date at which the message was originally
        written.
    -   **updater** - VARCHAR(JIRA developer) - reference to
        developer.name: Developer that edited the message most recently.
        If the comment has not been updated, then this is equal to
        *author*.
    -   **updated_date** - TIMESTAMP: Most recent time at which the
        message was edited. This is equal to *date* if the comment has
        not been updated.
    -   **encryption** - INT(row encryption)

### Metadata tables

-   **issuetype**: The types of issues and their descriptive names,
    e.g., Bug, Task, Story or Epic.
    -   **id** - INT - primary key: Internal JIRA identifier for the
        issue type.
    -   **name** - VARCHAR(100): The human-readable name of the issue
        type.
    -   **description** - VARCHAR(500): The human-readable description
        of the issue type. This is the empty string if the issue type
        has no description or NULL if the description could not be
        obtained.


-   **status**: The statuses that issues can have, such as Closed,
    Resolved, Opened or In Progress. Actual use may differ between
    project based on Sprint board setup.
    -   **id** - INT - primary key: Internal JIRA identifier for the
        status.
    -   **name** - VARCHAR(100): The human-readable name of the status.
    -   **description** - VARCHAR(500): The human-readable description
        of the status. This is the empty string if the status has no
        description or NULL if the description could not be obtained.
    -   **category_id** - INT - reference to
        status_category.category_id: The identifier of the status
        category. This is NULL if the status is not linked to a status
        category.


-   **status_category**: The categories in which the statuses may fall,
    such as To-Do or Done.
    -   **category_id** - INT - primary key: Internal JIRA identifier
        for the status category.
    -   **key** - VARCHAR(32): Lowercase identifier of the status
        category.
    -   **name** - VARCHAR(100): The human-readable name of the status
        category.
    -   **color** - VARCHAR(32): The color name of the status category.
        This is NULL if the color was not obtained.


-   **resolution**: Once an issue receives a status of Resolved or
    Closed, an indicator of why it was closed (such as Fixed, Duplicate
    or Works as designed).
    -   **id** - INT - primary key: Internal JIRA identifier for the
        resolution.
    -   **name** - VARCHAR(100): The human-readable name of the
        resolution.
    -   **description** - VARCHAR(500): The human-readable description
        of the resolution. This is the empty string if the resolution
        has no description or NULL if the description could not be
        obtained.


-   **developer**: Names of JIRA users that performed some action in an
    issue, including short account name and long display name.
    -   **id** - INT - primary key: Auto-incrementing identifier.
    -   **name** - VARCHAR(JIRA developer): Abbreviation of the JIRA
        developer.
    -   **display_name** - VARCHAR(100): Name of the JIRA developer as
        displayed in the JIRA interface. This is NULL if there is no
        display name known for the developer.
    -   **email** - VARCHAR(100): The email address of the JIRA
        developer. This is NULL if there is no email address known for
        the developer due to missing source information.
    -   **local_domain** - BOOL: Whether the email address of the
        developer has a local domain as defined in the importer
        properties. In this case, whether the email ends with
        '@ictu.nl'.
    -   **encryption** - INT(row encryption)


-   **project_developer**: Names of JIRA or TFS users, encrypted using
    the project-specific salt rather than a global salt. Only use this
    for matching against (encrypted) developer names from other sources.
    The fields duplicate those of *developer*, but the values are
    encrypted differently and some of the fields of the latter table may
    be removed. The table has a unique index of (project_id, team_id,
    developer_id).
    -   **project_id** - INT - reference to project.project_id: Project
        that the developer worked on.
    -   **developer_id** - INT - reference to developer.id: Identifier
        of the developer. If there is no team_id then this is the
        internal JIRA developer identifier, otherwise it is the internal
        TFS developer identifier tfs_developer.alias_id.
    -   **name** - VARCHAR(JIRA developer): Encrypted, possibly
        abbreviated name of the developer.
    -   **display_name** - VARCHAR(100): Encrypted name of the developer
        as displayed in the JIRA or TFS interface. This is NULL if there
        is no source information for the display name.
    -   **email** - VARCHAR(100): The encrypted email address of the
        developer. This is NULL if there is no source information for
        the email address.
    -   **encryption** - INT(row encryption)
    -   **team_id** - INT - reference to tfs_team.team_id: Team that the
        TFS developer is part of, or NULL if the developer is not a part
        of a team. A developer may be a part of multiple team/project
        combinations and may thus appear multiple times (possibly with
        different encryptions) in this table.


-   **fixversion**: Indicators in JIRA issues of the version to fix the
    issue before that version is released.
    -   **id** - INT - primary key: Internal JIRA identifier
    -   **project_id** - INT - reference to project.project_id: The
        project this version belongs to.
    -   **name** - VARCHAR(100): The name (version numbering scheme) of
        the release version.
    -   **description** - VARCHAR(500): The human-readable description
        provided to the release version. This is the empty string if no
        description has been provided to the fix version, or NULL if the
        description could not be obtained.
    -   **start_date** - DATE: Day on which work started on this fix
        version according to JIRA aggregate information. This is NULL if
        no start date is known for the fix version.
    -   **release_date** - DATE: Day on which the version is released or
        is supposed to be released. This is NULL if no release date is
        known for the fix version.
    -   **released** - BOOL: Whether the fix version has been released
        already.


-   **priority**: An indicator of the priority of the issue, on a scale
    of 1-5, with names like Low, Medium, High. This is not necessarily
    related to user story points or backlog prioritization.
    -   **id** - INT - primary key: Internal JIRA identifier for the
        priority.
    -   **name** - VARCHAR(100)


-   **ready_status**: An indicator of the pre-refinement ready state:
    Whether the user story can be pulled into the next stage such as
    Workshops, Design meetings, and Refinements. It may also be Blocked
    for or by any of these stages.
    -   **id** - INT - primary key: Internal JIRA identifier for the
        ready status.
    -   **name** - VARCHAR(100): Human-readable short description of the
        ready status. Examples: 'Ready for refinement', 'Blocked for
        design'


-   **test_execution**: The state of how a use case is tested.
    -   **id** - INT - primary key: Internal JIRA identifier for the
        execution state.
    -   **value** - VARCHAR(100): Human-readable short description of
        the execution state: 'Manual', 'Automated' or 'Will not be
        executed'.

### Relationship tables

-   **component**: The components that exist in JIRA for a project.
    Primary key is (project_id, component_id).
    -   **project_id** - INT - reference to project.project_id:
        Identifier of the project in which the component exists.
    -   **component_id** - INT: Internal JIRA identifier for the
        component.
    -   **name** - VARCHAR(100): The (short) name of the component, as
        shown in the issues.
    -   **description** - VARCHAR(500): The description of the
        component, or NULL if no description is provided to the
        component.


-   **issue_component**: The links from issues to components. The links
    have temporal date ranges that indicate the largest range in which
    the link has existed based on changelog entries. Primary key
    consists of (issue_id, component_id).
    -   **issue_id** - INT - reference to issue.issue_id: Identifier of
        the issue that has a component.
    -   **component_id** - INT - reference to component.component_id:
        Identifier of the component in which the issue is contained.
    -   **start_date** - TIMESTAMP - reference to issue.updated: Point
        in time when the link was first added to the issue, or NULL if
        the link has existed since the issue was created.
    -   **end_date** - TIMESTAMP - reference to issue.updated: Point in
        time when the link was last removed from the issue, or NULL if
        the link still exists.


-   **issuelink**: Bidirectional links that exist between pairs of
    issues in JIRA. The links have temporal date ranges that indicate
    the largest range in which the link has existed based on changelog
    entries. Primary key consists of (from_key, to_key,
    relationship_type, outward).
    -   **from_key** - VARCHAR(Issue key) - reference to issue.key:
        Issue involved in the link.
    -   **to_key** - VARCHAR(Issue key) - reference to issue.key:
        Another issue involved in the link.
    -   **relationship_type** - INT - reference to relationshiptype.id:
        The type of the link relationship as an internal JIRA
        identifier.
    -   **outward** - BOOL: Whether the given entry is outward from
        *from_key* (true) or inward to it (false).
    -   **start_date** - TIMESTAMP - reference to issue.updated: Point
        in time when the link was first added to the *from_key* issue,
        or NULL if the link has existed since the issue was created.
    -   **end_date** - TIMESTAMP - reference to issue.updated: Point in
        time when the link was last removed from the *from_key* issue,
        or NULL if the link still exists.


-   **relationshiptype**: The types of relationships that can exist
    between issues, such as Blocks, Details or Duplicate
    -   **id** - INT - primary key: The JIRA identifier of this issue
        link relationship type.
    -   **name** - VARCHAR(100): Textual name of the relationship, as a
        noun or verb.

    :   **The following fields do not yet exist, but may be added at a
        later stage:**

    -   outward - VARCHAR: Phrase used to describe the outward relation,
        such as 'blocks' or 'duplicates'.
    -   inward - VARCHAR: Phrase used to describe the inward relation,
        such as 'is blocked by' or 'is duplicated by'.


-   **subtask**: Links that exists between issues and their subtasks,
    different from the issue links.
    -   **id_parent** - INT - reference to issue.issue_id: The parent
        issue.
    -   **id_subtask** - INT - reference to issue.issue_id: The subtask
        issue.

## Version control system tables (Git, Subversion)

These tables include data from Gitlab/Git and Subversion.

-   **commits**: Data from individual commits in VCS (Git or Subversion)
    repositories used by the development team. Primary key is
    (version_id, repo_id).
    -   **version_id** - VARCHAR(100): SHA hash or revision number
        belonging to this code version. The version number is unique for
        the repository the change is made in (indicated by the *repo_id*
        field).
    -   **project_id** - INT - reference to project.project_id: The
        project this code version belongs to.
    -   **commit_date** - TIMESTAMP: Point in time at which the commit
        was made. The commit date is when the latest publicly visible
        version of the changes made in the commit are finalized.
    -   **sprint_id** - INT - reference to sprint.sprint_id: The sprint
        in which this commit was made, based on date intervals. If the
        commit is not matched to a sprint, then this is 0. In the case
        of overlapping sprints, the latest sprint that still contains
        the commit is used.
    -   **developer_id** - INT - reference to vcs_developer.alias_id:
        The developer that made the commit.
    -   **message** - TEXT: The full commit message that is shown in
        version control logs.
    -   **size_of_commit** - INT: The byte size of the commit. The
        semantic meaning differs between version control system, namely
        whether it includes the diff or other data. This is NULL for
        commits in repositories which have disabled statistics lookups
        for efficiency reasons.
    -   **insertions** - INT: Number of lines "added" or changed but not
        deleted in this commit. This is NULL for commits in repositories
        which have disabled statistics lookups for efficiency reasons.
    -   **deletions** - INT: Number of lines "deleted" or changed in
        this commit. This is NULL for commits in repositories which have
        disabled statistics lookups for efficiency reasons.
    -   **number_of_files** - INT: Number of files touched by this
        commit. This is NULL for commits in repositories which have
        disabled statistics lookups for efficiency reasons.
    -   **number_of_lines** - INT: Number of lines touched by this
        commit, be it added, deleted or changed. This is NULL for
        commits in repositories which have disabled statistics lookups
        for efficiency reasons.
    -   **type** - VARCHAR(100): The type of code change made (commit,
        revert, merge). Deduced from auxiliary data and possibly the
        commit message.
    -   **repo_id** - INT - reference to repo.id: The repository in
        which the code change is made.
    -   **author_date** - TIMESTAMP: Point in time that the developer
        initially made the commit. This is NULL for version control
        systems that do not know a separate authored date.
    -   **branch** - VARCHAR(Git branch): The original branch that the
        commit was made on. The branch is deduced from the merge commit
        that brought the commit to the master branch. This is NULL for
        commits whose originating branch cannot be deduced, such as for
        Subversion repositories.
    -   **team_id** - INT - reference to tfs_team.team_id: The team of
        the developer that made the commit. This is NULL if we cannot
        determine in which team the developer was creating the commit.


-   **vcs_developer**: User names from VCS commits. The same developer
    (with the same *jira_dev_id*) may appear multiple times in the table
    if they have changed their name or email, for example in their Git
    configuration.
    -   **alias_id** - INT - primary key: Sequential number assigned to
        the developer.
    -   **jira_dev_id** - INT - reference to developer.id: The JIRA
        developer/ associated with the developer. The matching is based
        on the VCS developer's display name and email, and the JIRA
        developer display name, short name or email. The matching is
        also manually tweaked using the file data_vcsdev_to_dev.json
        (which is retrieved from ownCloud).
    -   **display_name** - VARCHAR(500): The name of the developer used
        in the version control system.
    -   **email** - VARCHAR(100): Email address that the developer uses.
        This is NULL if the version control system or review system does
        not provide an email address for the user.
    -   **encryption** - INT(row encryption)


-   **repo**: Repository names and projects
    -   **id** - INT - primary key: Auto-incrementing identifier.
    -   **repo_name** - VARCHAR(1000): Readable name of the repository
        extracted from one of the data sources (GitLab, project
        definition, path name).
    -   **project_id** - INT - reference to project.project_id: The
        project to which the repository belongs to. This, alongside the
        *id*, distinguishes repositories with the same name.
    -   **type** - VARCHAR(32): The literal type of the source related
        to the repository. This is NULL if the source information is not
        available.
    -   **url** - VARCHAR(255): The URL to the source related to the
        repository. This is NULL if the source information is not
        available.


-   **change_path**: Statistics on files that are changed in versions in
    the repository. Primary key is (repo_id, version_id, file).
    -   **repo_id** - INT - reference to repo.id: The repository in
        which the change was made.
    -   **version_id** - VARCHAR(100) - reference to commits.version_id:
        The version in which the change to the file was made.
    -   **file** - VARCHAR(1000): Path to the file that was changed.
    -   **insertions** - INT: Number of lines "added" or changed but not
        deleted in the file during the commit.
    -   **deletions** - INT: Number of lines "deleted" or changed in
        this commit.
    -   **type** - VARCHAR(1): The type of change made to the file. This
        may be 'M' (Modified), 'A' (Added), 'D' (Deleted) or 'R'
        (Replaced, Git only indicator for moved or copied files).
    -   **size** - INT: The byte size of the file or the changes. The
        semantic meaning differs between version control system, namely
        whether it includes the diff or if it is the new file size.


-   **tag**: Release tags specified in the repository.
    -   **repo_id** - INT - reference to repo.id: The repository in
        which the tag is added.
    -   **tag_name** - VARCHAR(100): The name of the tag.
    -   **version_id** - VARCHAR(100) - reference to commits.version_id:
        The version in which the tag is added (Subversion) or which the
        tag references (Git).
    -   **message** - TEXT: Message that is added to the tag when it is
        created, separate from the commit message. Only available for
        Git repositories, and is NULL if no message was created.
    -   **tagged_date** - TIMESTAMP: Date on which the tag is created
        (Git) or most recently updated (Subversion). Note that tags
        should not be altered once they are created, so the tagged date
        should reflect the commit date for Subversion. The date may be
        NULL for incomplete Git tags.
    -   **tagger_id** - INT - reference to vcs_developer.alias_id: The
        developer that created the tag. If the developer cannot be
        deduced from tag information, then this is NULL.
    -   **sprint_id** - INT - reference to sprint.sprint_id: The sprint
        in which the tag was created, based on date intervals. If the
        tag date is not matched to a sprint, then this is 0 or NULL. In
        the case of overlapping sprints, the latest sprint that still
        contains the date is used.

### Review system tables (GitHub, GitLab, TFS/Azure DevOps)

-   **vcs_event**: An event from an activity timeline of a repository.
    The events include pushes of commits or tags, or possibly also other
    actions such as deletions.
    -   **repo_id** - INT - reference to repo.id: Repository in which
        the event was made.
    -   **action** - VARCHAR(20): A slightly human-readable description
        of the action that the event performs. This describes what the
        target of the action is and how it is changed. Examples: 'pushed
        to' (commit to a branch), 'pushed new' (commit to new feature
        branch or a new tag), 'deleted' (branch)
    -   **kind** - VARCHAR(20): The type of event, which provides
        details of what the event adds, in addition to the action.
        Examples: 'push', 'tag_push'
    -   **version_id** - VARCHAR(100) - reference to commits.version_id:
        The version to which the event applies. For tag pushes, this is
        the same as *tag.version_id*.
    -   **ref** - VARCHAR(100): The Git reference that the event applies
        to. This can be a branch head reference (such as
        'refs/heads/master') or a tag reference ('refs/tags/0.1.2')
    -   **date** - TIMESTAMP: The moment in time when the event
        occurred.
    -   **developer_id** - INT - reference to vcs_developer.alias_id:
        The developer that performed the action in the version control
        repository.


-   **gitlab_repo**: GitLab-specific metadata of a repository. Primary
    key is (repo_id, gitlab_id).
    -   **repo_id** - INT - reference to repo.id: Repository that the
        GitLab repo stores.
    -   **gitlab_id** - INT: internal GitLab identifier of the
        repository.
    -   **description** - TEXT: Human-readable description of the
        repository. This is NULL if the repository has no description.
    -   **create_date** - TIMESTAMP: Time at which the GitLab repository
        was created.
    -   **archived** - BOOL: Whether the repository is marked as
        archived (read-only) by the development team.
    -   **has_avatar** - BOOL: Whether the development team has given
        the repository an avatar image.
    -   **star_count** - INT: Number of people in the team that have
        starred the repository such that it is shown more prominently on
        their personal dashboard. Some GitLab instances do not provide
        this and the value is then 0.


-   **github_repo**: GitHub-specific metadata of a repository. Primary
    key is (repo_id, github_id).
    -   **repo_id** - INT - reference to repo.id: Repository that the
        GitHub repo stores.
    -   **github_id** - INT: internal GitHub identifier of the
        repository.
    -   **description** - TEXT: Description of the repository. This is
        NULL if no description is provided.
    -   **create_date** - TIMESTAMP: Time at which the GitHub repository
        was created.
    -   **private** - BOOL: Whether the repository is private such that
        only the organization can see it.
    -   **forked** - BOOL: Whether the repository is forked from an
        upstream repository.
    -   **star_count** - INT: Number of people on GitHub that have
        starred the repository such that they have a bookmark of the
        repository and the repository is more appreciated by the
        community.
    -   **watch_count** - INT: Number of people on GitHub that watch the
        repository such that they may receive notifications about events
        in the repository.


-   **github_issue**: An issue within a GitHub repository. Primary key
    is (repo_id, issue_id).
    -   **repo_id** - INT - reference to repo.id: Repository which is
        the topic of the issue.
    -   **issue_id** - INT: GitHub identifier of the issue which is
        unique for the repository.
    -   **title** - TEXT: The short header message describing what the
        issue is about, e.g., what problems are encountered. This is
        NULL if no title is provided.
    -   **description** - TEXT: The contents of the issue message. This
        is the empty string if the issue message is empty or NULL if it
        could not be obtained.
    -   **status** - VARCHAR(100): The open/close state of the issue.
        This can be a word like 'open' or 'closed'. This is NULL if the
        status of the issue could not be obtained.
    -   **author_id** - INT - reference to vcs_developer.alias_id:
        Identifier of the developer that started the issue.
    -   **assignee_id** - INT - reference to vcs_developer.alias_id:
        Identifier of the developer that should address the issue. This
        is NULL if nobody is assigned to the issue.
    -   **created_date** - TIMESTAMP: Time at which the issue is
        created. This is NULL if the creation time of the issue could
        not be obtained.
    -   **updated_date** - TIMESTAMP: Time at which the issue received
        an update. This is NULL if the update time of the issue could
        not be obtained.
    -   **pull_request_id** - INT - reference to
        merge_request.request_id: The GitHub pull request identifier for
        a related pull request in the same repository. This is NULL if
        no such link has been created yet for the issue.
    -   **labels** - INT: Number of labels that are added to the issue.
        This is NULL if the number of labels for the issue could not be
        obtained.
    -   **closed_date** - TIMESTAMP: Time at which the issue is closed.
        This is NULL if the issue is not yet closed.
    -   **closer_id**: INT - reference to vcs_developer.alias_id:
        Identifier of the developer that closed the issue, either by
        fixing the problem or by considering it to not be an issue that
        should be solved. This is NULL if the issue is not yet closed.


-   **github_issue_note**: A comment attached to an issue on GitHub,
    which is a part of the discussion. Primary key is (repo_id,
    issue_id, note_id).
    -   **repo_id** - INT - reference to repo.id: Repository for which
        this note is added.
    -   **issue_id** - INT - reference to github_issue.issue_id: The
        GitHub issue identifier of the issue to which this note is
        added.
    -   **note_id** - INT: Internal identifier of the note which is
        unique for the GitHub instance.
    -   **author_id** - INT - reference to vcs_developer.alias_id:
        Identifier of the developer that wrote the comment.
    -   **comment** - TEXT: Plain text comment message of the note. This
        is NULL if the message could not be obtained.
    -   **created_date** - TIMESTAMP: Time at which the comment is added
        to the issue. This is NULL if the creation date of the note
        could not be obtained.
    -   **updated_date** - TIMESTAMP: Time at which the comment is most
        recently edited. This is NULL if the update date of the note
        could not be obtained.


-   **merge_request**: A request within the development team's review
    system to merge a Git branch into another. Primary key is (repo_id,
    request_id).
    -   **repo_id** - INT - reference to repo.id: Repository whose
        branches are the topic of the request.
    -   **request_id** - INT: Internal GitLab, GitHub or TFS identifier
        of the merge request which is unique for the instance.
    -   **title** - TEXT: The short header message describing what the
        merge request is about, e.g., what branches/commits it merges.
        This is NULL if the title of the request could not be obtained.
    -   **description** - TEXT: The contents of the request message.
        This is NULL if the description of the request could not be
        obtained.
    -   **status** - VARCHAR(100): The open/close state of the request.
        This can be a word like 'opened', 'merged' or 'closed' for
        GitLab, 'open', 'merged' or 'close' for GitHub, or 'active',
        'completed' or 'abandoned' for TFS. Note that merge procedures
        may differ by team, and as such a closed/abandoned pull request
        may have been manually merged. This is NULL if the status of the
        request could not be obtained.
    -   **source_branch** - VARCHAR(Git branch): The (feature) branch
        from which commits should be merged.
    -   **target_branch** - VARCHAR(Git branch): The (main) branch at
        which the commits should be merged into.
    -   **author_id** - INT - reference to vcs_developer.alias_id:
        Identifier of the developer that started the request.
    -   **assignee_id** - INT - reference to vcs_developer.alias_id:
        Identifier of the developer that should review the request. This
        is NULL if nobody is assigned, which is the case for TFS pull
        requests.
    -   **upvotes** - INT: Number of votes from the development team in
        support of the merge request. This is NULL if the review system
        does not support votes or they could not be obtained for the
        request.
    -   **downvotes** - INT: Number of votes from the development team
        against the merge request. This is NULL if the review system
        does not support votes or they could not be obtained for the
        request.
    -   **created_date** - TIMESTAMP: Time at which the merge request is
        created. This is NULL if the creation time of the request could
        not be obtained.
    -   **updated_date** - TIMESTAMP: Time at which the merge request
        received an update (a merge request note or update to the
        request details). This is NULL if the update time of the request
        could not be obtained.
    -   **sprint_id** - INT - reference to sprint.sprint_id: The sprint
        in which the merge request was made, based on date intervals. If
        the created date is not matched to a sprint, then this is 0 or
        NULL. In the case of overlapping sprints, the latest sprint that
        still contains the date is used.


-   **merge_request_review**: A vote given by a reviewer to a pull
    request in a GitHub or Team Foundation Server instance.
    -   **repo_id** - reference to repo.id: Repository for which the
        review was made.
    -   **request_id** - INT - reference to merge_request.request_id:
        Merge request in which the review was made.
    -   **reviewer_id** - INT - reference to vcs_developer.alias_id:
        Identifier of the developer who performed the review.
    -   **vote** - INT: The vote weight of the developer, which is
        positive for upvotes and acceptance, and negative for downvotes
        and requests for changes.


-   **merge_request_note**: A comment or automated message attached to a
    merge request in a review system. The comment is a part of a
    discussion about the request or describes changes to request details
    or branch commits. Primary key is (repo_id, request_id, thread_id,
    note_id).
    -   **repo_id** - INT - reference to repo.id: Repository for which
        this note is added.
    -   **request_id** - INT - reference to merge_request.request_id:
        Merge request to which this note is added.
    -   **thread_id** - INT: Identifier of the thread to which this
        comment belongs. If threading is not supported by the review
        system or the information is not provided by the API, then this
        is 0.
    -   **note_id** - INT: Internal identifier of the note which is
        unique within the scope of the thread, request, repository or
        instance.
    -   **parent_id** - INT - reference to merge_request_note.note_id:
        The note to which this comment is a reply. The parent note has
        the same repository, request and thread identifiers as this
        note. If threading is not supported by the review system or the
        information is not provided by the API, then this is 0 or NULL.
    -   **author_id** - INT - reference to vcs_developer.alias_id:
        Identifier of the developer that wrote the comment or on whose
        regard the automated comment is added.
    -   **comment** - TEXT: Plain text comment message of the note.
        Automated GitLab notes have a first line which is surrounded
        with underscores, or matches the regex "Added \\d commits?:"
        with the remaining lines either empty or starting with
        star-bullets. Automated TFS notes have been filtered out, and
        GitHub notes from the system are not included (bot comments are
        included though). This is NULL if the comment could not be
        obtained for the note.
    -   **created_date** - TIMESTAMP: Time at which the comment is added
        to the merge request. This is NULL if the creation date could
        not be obtained for the note.
    -   **updated_date** - TIMESTAMP: Time at which the comment is most
        recently edited. This is NULL if this information was not
        available from the API source.


-   **commit_comment**: A comment in the review system attached to a
    code version from the Git repository. Note that the comment may be
    part of a review of a merge request, but this may not always be
    directly visible from the data. The table has no keys; in order to
    check if a certain row already exists, all fields (except perhaps
    updated_date) should be included into the query.
    -   **repo_id** - INT - reference to repo.id: Repository for which
        this note is added.
    -   **version_id** - VARCHAR(100) - reference to commits.version_id:
        Commit to which this note is added.
    -   **request_id** - INT - reference to merge_request.request_id:
        Internal identifier of the merge request to which this note is
        added. This is 0 if the merge request cannot be deduced or the
        comment does not belong to a request.
    -   **thread_id** - INT: Identifier of the thread to which this
        comment belongs. If threading is not supported by the review
        system or the information is not provided by the API, then this
        is 0.
    -   **note_id** - INT: Internal identifier of the note. This is 0 if
        the instance does not provide unique identifiers to commit
        comments.
    -   **parent_id** - INT - reference to commit_comment.note_id: The
        note to which this comment is a reply. The parent note has the
        same repository, request and thread identifiers as this note. If
        threading is not supported by the review system or the
        information is not provided by the API, then this is 0 or NULL.
    -   **author_id** - INT - reference to vcs_developer.alias_id:
        Identifier of the developer who wrote the commit comment.
    -   **comment** - TEXT: Plain text comment message of the note. This
        is NULL if the message could not be obtained for the commit
        comment.
    -   **file** - VARCHAR(1000): Path to a file in the repository that
        is changed in the commit and is discussed by the comment. If
        this is NULL, then the comment belongs to the entire version.
    -   **line** - INT: Line number of the file that is discussed by the
        comment. If this is NULL, then the comment belongs to the entire
        version.
    -   **end_line** - INT: Line number of the end of the range in the
        file that is discussed by the comment. If this is NULL, then the
        comment does not belong to a range (because the review system
        does not support range comments). Note that the line ranges may
        not be exact for some review systems.
    -   **line_type** - VARCHAR(100): The type of line being discussed
        by the comment: 'old' or 'new'. If this is NULL, then the
        comment belongs to the entire version.
    -   **created_date** - TIMESTAMP: Time at which the comment is added
        to the commit. This is NULL if this information was not
        available from the API.
    -   **updated_date** - TIMESTAMP: Time at which the comment is most
        recently edited. This is NULL if this information was not
        available from the API.


-   **tfs_developer**: User names from TFS/Azure work items. Primary key
    is (project_id, display_name).
    -   **project_id** - INT - reference to project.project_id: Project
        in which the developer is working.
    -   **display_name** - VARCHAR(100): The name of the developer as
        displayed in TFS.
    -   **email** - VARCHAR(100): The email address of the developer.
        This may be a domain account name or an internal identifier of
        the account instead of an email address. This is NULL if the
        email address could not be obtained.
    -   **alias_id** - INT - reference to vcs_developer.alias_id: The
        VCS developer associated with the TFS developer. The matching is
        based on the TFS and VCS developer's display name. If no
        matching VCS developer is found or created, then this is NULL.
    -   **encryption** - INT(row encryption)


-   **tfs_sprint**: Data regarding a sprint registered in TFS/Azure,
    including the start and end dates.
    -   **sprint_id** - INT - primary key: Auto-incrementing identifier.
    -   **project_id** - INT - reference to project.project_id: Project
        in which the sprint occurs.
    -   **repo_id** - INT - reference to repo.id: Repository in which
        the sprint occurs. This is NULL if no repository could be
        determined for the sprint.
    -   **team_id** - INT - reference to tfs_team.team_id: Team which is
        working on the sprint. This is NULL if no team could be
        determined for the sprint.
    -   **name** - VARCHAR(100): Human-readable name of the sprint.
        Depending on the project, team and the person who creates the
        sprints, this may include a sequence number (not unique), the
        sprint/story owner (Scrum master), week/calendar date, target
        version and/or the sprint topic/goal if it is a specialized
        sprint.
    -   **start_date** - TIMESTAMP: Moment in time at which the sprint
        starts or is set to start. May differ from the actual start time
        (but usually not by more than a day). This is NULL if the start
        date is not yet known.
    -   **end_date** - TIMESTAMP: Moment in time at which the sprint
        ends or is set to end. May be a projected date from the start
        and thus has a time which is not always correct, but is usually
        close to the real date. This is NULL if the end date is not yet
        known.


-   **tfs_team**: A team working in a TFS/Azure repository.
    -   **team_id** - INT - primary key: Auto-incrementing identifier.
    -   **project_id** - INT - reference to project.project_id: Project
        in which the team exists.
    -   **repo_id** - INT - reference to repo.id: Repository in which
        the team is working.
    -   **name** - VARCHAR(100): Human-readable name of the team.
    -   **description** - VARCHAR(500): Description of the team. This is
        the empty string if no description is provided or NULL if the
        description could not be retrieved.


-   **tfs_team_member**: A team member working in a TFS/Azure team.
    Primary key is (team_id, name).
    -   **team_id** - INT - reference to tfs_team.team_id: Team in which
        the team member is working.
    -   **repo_id** - INT - reference to repo.id: Repository in which
        the team member is working.
    -   **alias_id** - INT - reference to vcs_developer.alias_id: The
        VCS developer associated with the TFS developer. The matching is
        based on the TFS team member's and VCS developer's display name.
        If no matching VCS developer is found or created, then this is
        NULL.
    -   **name** - VARCHAR(100): The internal name of the TFS team
        member.
    -   **display_name** - VARCHAR(100): The name of the team member as
        displayed in TFS.
    -   **encryption** - INT(row encryption)


-   **tfs_work_item**: A work item issue in a TFS/Azure repository.
    Primary key is (issue_id, changelog_id).
    -   **issue_id** - INT: TFS identifier of the work item. **There may
        be multiple rows with the same issue_id.**
    -   **changelog_id** - INT: Version number deduced from the
        changelog. The earliest version is has a changelog id of 0.
    -   **title** - TEXT: The human-readable title of the work item.
        This reflects the title at the moment the work item version
        existed, based on the changelog.
    -   **type** - VARCHAR(64): The work item type (Task, Bug, UseCase),
        or NULL if this information is not provided by the API.
    -   **priority** - INT: The work item priority (0-99), or NULL if
        this information is not provided by the API.
    -   **created** - TIMESTAMP: The time at which the work item was
        created, equal to updated_at of the first version. Provided as a
        convenience on all versions. This is NULL if the information is
        not provided by the API.
    -   **updated** - TIMESTAMP: The time at which the change in this
        version of the work item was made. Equal to *created* for the
        first version. This is NULL if the information is not provided
        by the API.
    -   **description** - TEXT: The human-readable description that is
        shown at the top of the work item by default. Can be updated in
        changes. This is NULL if not provided.
    -   **duedate** - DATE: The due date of this work item, or NULL if
        not provided. Some projects use due dates for milestones and
        prioritization, in addition to sprint constraints.
    -   **project_id** - INT - reference to project.project_id: The
        project in which this issue is contained.
    -   **status** - VARCHAR(64): The work item status (New, To Do,
        Open, In progress, Closed), or NULL if this information is not
        provided by the API.
    -   **reporter** - VARCHAR(100) - reference to
        tfs_developer.display_name: The reporter of the work item
        according to the work item data. Usually, this is the same
        developer as *updated_by* on the first version, but this may be
        different due to cloned issues or intermediate changes. This is
        NULL if this information is not provided by the API.
    -   **assignee** - VARCHAR(100) - reference to
        tfs_developer.display_name: The developer that should resolve
        the work item, or NULL if none is assigned thus far.
    -   **attachments** - INT: The number of attachments in the work
        item. This tracks updates in the changelog.
    -   **additional_information** - TEXT: Human-readable text that is
        shown within a tab beside the description, or NULL if it is not
        filled in. Often used for extra implementation/functional
        details.
    -   **story_points** - DECIMAL(5,2): The number of points assigned
        to a product backlog item after the developers meet in a
        refinement and determine the difficulty of the item. If not yet
        set, then this is NULL. Maximum value is 999 and two digits
        after the decimal point is allowed.
    -   **sprint_id** - INT - reference to tfs_sprint.sprint_id: The
        sprint that this work item is pulled into. The sprint can differ
        by version. If the work item is not yet explicitly added into a
        sprint, then this is NULL.
    -   **team_id** - INT - reference to tfs_team.team_id: The team that
        is working on this work item. This is NULL if no team could be
        obtained for this work item version.
    -   **updated_by** - VARCHAR(100) - reference to
        tfs_developer.display_name: The developer that made a change in
        this version of the work item. This is NULL if this information
        is not provided by the API.
    -   **labels** - INT: The number of labels that the work item
        currently has. This is NULL if the number of labels could not be
        obtained for this work item.

## Metrics tables (Quality dashboard history)

These tables include data from the metrics history files and the quality
dashboard project definition.

-   **metric**: Metric types
    -   **metric_id** - INT - primary key: Sequential number assigned to
        the metric.
    -   **name** - VARCHAR(100): Amalgamated name of the metric, based
        on the type of metric and the component, product, team or other
        domain object it measures.
    -   **base_name** - VARCHAR(100): Name of the metric that is being
        measured, based on the class name from the quality report
        repository used by the project definitions. There may be
        multiple rows with the same base_name. This is NULL if the base
        name has not been deduced yet.
    -   **domain_name** - VARCHAR(100): Domain name that the metric
        measures. This is often a component, product, team, build
        street, repository name or other domain object. For some metrics
        before August 2014, the domain name is the type of domain
        object, e.g., 'Product' or 'Street'. This is NULL if the domain
        name has not been deduced yet.


-   **metric_value**: Singular metric data from quality report
    -   **metric_id** - INT - reference to metric.metric_id: The metric
        that is measured
    -   **value** - FLOAT: The raw value. This is -1 if there is a
        problem with measuring the metric (category is grey, missing, or
        missing_source). Prior to the introduction of compact history
        (September 2017), all values are integers.
    -   **category** - VARCHAR(100): 'red' (below low target), 'yellow'
        (below target), 'green' (at or above target), 'perfect' (cannot
        be improved), 'grey' (disabled), 'missing' (internal problem),
        'missing_source' (external problem). The 'perfect' category is
        indicated by a light green color, while the 'missing' and
        'missing_source' are indicated by a grey color. The other
        categories use their category name as color in the quality
        report.
    -   **date** - TIMESTAMP: Time at which the measurement took place.
        This is NULL if the date could not be obtained.
    -   **sprint_id** - INT - reference to sprint.sprint_id: The sprint
        in which the metric was measured, based on date intervals. If
        the measurement date is not matched to a sprint, then this is 0
        or NULL. In the case of overlapping sprints, the latest sprint
        that still contains the date is used.
    -   **since_date** - TIMESTAMP - reference to metric_value.date:
        Time since which the metric has the same value. This is NULL if
        the date could not be obtained.
    -   **project_id** - INT - reference to project.project_id: The
        project for which the measurement was made


-   **metric_version**: Versions of the project definition in which
    changes to target norms of a project are made. Primary key is
    (project_id, version_id).
    -   **project_id** - INT - reference to project.project_id: The
        project to which the norm changes apply.
    -   **version_id** - VARCHAR(100): SHA hash or revision number
        belonging to the change of the target norms.
    -   **developer** - VARCHAR(64): Developer or quality lead that made
        the change.
    -   **message** - TEXT: Commit message describing the change. This
        is the empty string if the message is not provided or NULL if it
        could not be obtained.
    -   **commit_date** - TIMESTAMP: Time at which the target change
        took place.
    -   **sprint_id** - INT - reference to sprint.sprint_id: The sprint
        in which the target norms were changed, based on date intervals.
        If the commit date is not matched to a sprint, then this is 0 or
        NULL. In the case of overlapping sprints, the latest sprint that
        still contains the commit is used.
    -   **encryption** - INT(row encryption)


-   **metric_target**: Manual changes to the metric targets of a
    project.
    -   **project_id** - INT - reference to project.project_id: The
        project to which the target changes apply.
    -   **version_id** - VARCHAR(100) - reference to
        metric_version.version_id: SHA hash or revision number belonging
        to the change of the target norm.
    -   **metric_id** - INT - reference to metric.metric_id: Metric
        whose norms are changed.
    -   **type** - VARCHAR(100): Type of change: 'options',
        'old_options' or 'TechnicalDebtTarget'.
    -   **target** - INT: Norm value at which the category changes from
        green to yellow.
    -   **low_target** - INT: Norm value at which the category changes
        from yellow to red.
    -   **comment** - TEXT: Comment for technical debt targets
        describing the reason of the norm change. This is the empty
        string if no comment is provided or NULL if it could not be
        obtained.


-   **metric_default**: The default values of metric targets in the
    quality report repository at points in time that these values were
    introduced or changed. Primary key is (base_name, version_id).
    -   **base_name** - VARCHAR(100): Name of the metric that is being
        measured, based on the class name from the quality report
        repository used by the project definitions.
    -   **version_id** - VARCHAR(100): SHA hash or revision number
        belonging to the introduction or alteration of the default
        target values.
    -   **commit_date** - TIMESTAMP: Time at which the introduction or
        alteration took place.
    -   **direction** - BOOL: Whether the metric improves if the metric
        value increases. This is true if a higher value is better, false
        if a lower value is better, or NULL if it is not known or not
        applicable for the metric.
    -   **perfect** - FLOAT: The perfect value of the metric (there are
        no better values possible, indicated by a light green color in
        the quality report). This is NULL if the perfect value is not
        known.
    -   **target** - FLOAT: The target value of the metric (values equal
        or better to the target are indicated by a green color in the
        quality report). This is NULL if the target value is not known.
    -   **low_target** - FLOAT: The low target value of the metric
        (values equal or better than the low target, but worse than the
        target, are indicated by a yellow color in the quality report,
        while worse values are indicated by a red value). This is NULL
        if the low target value is not known.

## Docker dashboard tables (BigBoat)

-   **bigboat_status**: Environment health status metrics that are
    regularly retrieved from the Docker dashboard by the data gathering
    agent.
    -   **project_id** - INT - reference to project.project_id: The
        project to which the dashboard instance belongs.
    -   **name** - VARCHAR(100): The name of the health status metric as
        displayed in the dashboard.
    -   **checked_date** - TIMESTAMP: The time at which the health
        status information was refreshed.
    -   **ok** - BOOL: Whether the health status is OK, and the
        dashboard is thus functioning normally.
    -   **value** - FLOAT: The value of the health status metric at the
        time it was refreshed. This is NULL if the numeric value is not
        available for this metric or was unknown at the time of
        collection.
    -   **max** - FLOAT: The maximum reachable value of the health
        status metric at the time it was refreshed. This is NULL if the
        numeric value is not available for this metric or was unknown at
        the time of collection.

## Source tables (Quality dashboard project definitions)

-   **source_environment**: Environments that certain sources may share.
    Primary key is (project_id, environment).
    -   **project_id** - INT - reference to project.project_id: The
        project in which the environment lives.
    -   **source_type** - VARCHAR(32): The type of the representative
        source in the environment.
    -   **url** - VARCHAR(255): The URL to the source environment.
    -   **environment** - VARCHAR(500): The descriptor of the
        environment. This may be equal to the URL or it is a
        JSON-serialized array of elements that identify the environment.
        The environment is hashable and is unique for the project, such
        that sources that have the same environment descriptor belong to
        the same environment.
    -   **version** - VARCHAR(32): The version of the representative
        source in the environment. If the version is not known, then
        this is the empty string.


-   **source_id**: Identifiers used within certain sources that have
    domain names, such as analyzed applications/product components.
    Primary key is (project_id, domain_name, url).
    -   **project_id** - INT - reference to project.project_id: The
        project in which the source is used.
    -   **domain_name** - VARCHAR(100): Domain name that is measured by
        a metric that uses the source ID to locate the domain object,
        which could be an application, product, or component.
    -   **url** - VARCHAR(255): Base URL to the source that provides the
        domain object's metric data trough the source ID.
    -   **source_type** - VARCHAR(32): The type of the source. This is
        NULL if the source type could not be identified.
    -   **source_id** - TEXT: Identifier used by the source to provide
        access to the domain object's metric data.

## Build system tables (Jenkins)

-   **jenkins**: Generic usage statistics retrieved from Jenkins.
    Primary key is (project_id, host)
    -   **project_id** - INT - reference to project.project_id: The
        project to which the Jenkins instance belongs.
    -   **host** - VARCHAR(255): Base host URL of the Jenkins instance.
    -   **jobs** - INT: Number of jobs on the Jenkins instance.
    -   **views** - INT: Number of views on the Jenkins instance.
    -   **nodes** - INT: Number of computer nodes attached to the
        Jenkins instance.

## Reservation tables (Topdesk)

-   **reservation**: A reservation that is planned in the Topdesk
    self-service tool.
    -   **reservation_id** - VARCHAR(10) - primary key: Reservation
        identifier as a formatted number.
    -   **project_id** - INT - reference to project.project_id: Project
        to which the reservation belongs according to
        whitelist/blacklist matching.
    -   **requester** - VARCHAR(500): Name of the person who requests
        the reservation.
    -   **number_of_people** - INT: Number of people that the
        reservation encompasses. This may be 0 or NULL if not filled in,
        and other values may not be exact or meaningful either.
    -   **description** - TEXT: The text accompanying the reservation,
        usually a description of what type of meeting it is and possibly
        who is involved. This is NULL if the description could not be
        obtained for the reservation.
    -   **start_date** - TIMESTAMP: Time (up to minute) at which the
        reservation starts.
    -   **end_date** - TIMESTAMP: Time (up to minute) at which the
        reservation ends.
    -   **prepare_date** - TIMESTAMP: Time (up to minute) at which the
        reservation is booked to perform setup in the room. If no
        preparation is needed, then this is the same as start_date. This
        is NULL if the preparation time is not known.
    -   **close_date** - TIMESTAMP: Time (up to minute) until which the
        reservation is booked to break down any setup in the room. If no
        dismantling is needed, then this is the same as end_date. This
        is NULL if the closing time is not known.
    -   **sprint_id** - INT - reference to sprint.sprint_id: The sprint
        in which the reservation took place, based on date intervals. If
        the reservation date is not matched to a sprint, then this is 0
        or NULL. In the case of overlapping sprints, the latest sprint
        that still contains the date is used.
    -   **encryption** - INT(row encryption)

## User administration (LDAP)

-   **ldap_developer**: User names from an LDAP database.
    -   **project_id** - INT - reference to project.project_id: Project
        to which the developer has (administrative) access to by being a
        part of a group specifically created for the project.
    -   **name** - VARCHAR(64): The account (login) name of the
        developer in the LDAP database.
    -   **display_name** - VARCHAR(100): The common name of the
        developer in the LDAP database.
    -   **email** - VARCHAR(100): The email address of the developer in
        the LDAP database. This is NULL if the email address could not
        be obtained.
    -   **jira_dev_id** - INT - reference to developer.id: The JIRA
        developer associated with the LDAP developer. The matching is
        based on the VCS developer's display name and email, and the
        JIRA developer display name, short name or email. This is 0 or
        NULL if no matching JIRA developer was found.
    -   **encryption** - INT(row encryption)

## Seat counts

-   **seats**: Seat count for a project team. Primary key is
    (project_id, sprint_id, date).
    -   **project_id** - INT - reference to project.project_id: Project
        to which the seat count belongs.
    -   **sprint_id** - INT - reference to sprint.sprint_id: The sprint
        in which the seat count applies, based on date intervals.
    -   **date** - TIMESTAMP: Date (up to months) in which the seat
        count applies.
    -   **seats** - FLOAT: The number of seats for the given month. This
        is NULL if the seat count is not known for the given month.

## Internal trackers

-   **update_tracker**: Files that keep track of where to start
    gathering data from for incremental updates and database
    synchronization.
    -   **project_id** - INT - reference to project.project_id: The
        project to which the tracker file belongs.
    -   **filename** - VARCHAR(255): The name of the file (without path)
        that keeps track of the update state.
    -   **contents** - TEXT: The textual (JSON or otherwise readable)
        contents of the file that tracks the update state.
    -   **update_date** - TIMESTAMP: The latest modification date of the
        file. This is NULL if the modification time could not be
        obtained.


-   **project_salt**: Project-specific hash pairs that are used for
    one-way encryption of [sensitive data](sensitive_data).
    -   **project_id** - INT - reference to project.project_id: The
        project for which the hash holds. If this is 0, then it
        indicates a global hash pair.
    -   **salt** - VARCHAR(32): First salt of the project data.
    -   **pepper** - VARCHAR(32): Second salt of the project data.
