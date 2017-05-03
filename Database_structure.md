# Database structure

The database structure will be split per data source.

The structure will be shown as follows:

-   **Table**: Description
    -   **Attribute** - TYPE - (pseudo)reference: Description
    -   **Attribute** - TYPE: Description
    -   **Attribute** - TYPE(Special field type)
    -   **Attribute** - TYPE

## Special field types

The following types are aliases for an actual MonetDB type
specification. They describe the constraints set on the values stored in
such fields more thoroughly and uniformly.

-   **VARCHAR(Issue key)**: A field containing a JIRA issue key. The
    maximum length limitation of this field is currently 20 characters,
    which should hold most project keys and issue sequence numbers.
-   **VARCHAR(JIRA developer)**: A field containing a developer short
    name from JIRA. These are usually not more than 6 characters in the
    source data, but the limit is set to 64 characters for all fields
    for encryption purposes.
-   **VARCHAR(Git branch)**: A field containing a Git branch object
    name. The maximum length limitation of this field is 255 characters.
-   **INTEGER(row encryption)**: A field specifying which encryption
    level is applied to certain fields in the given row has encrypted
    fields. This field exists in tables with [sensitive
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

## Issue tables (JIRA)

-   **issue**: Entries from the JIRA database. Each row is one changelog
    item. Primary key is (issue_id, changelog_id)
    -   **issue_id** - INT: Internal JIRA identifier. **There may be
        multiple rows with the same issue_id.**
    -   **changelog_id** - INT: Version number deduced from the
        changelog. The earliest version is has a changelog id of zero.
    -   **key** - VARCHAR (Issue key): The JIRA issue key. **There may
        be multiple rows with the same key.**
    -   **title** - VARCHAR(250): The human-readable title of the issue.
        Can be updated in changes.
    -   **type** - INT - reference to issuetype.id: The issue type
        (Story, Bug, Use Case) as an internal JIRa identifier.
    -   **priority** - INT - reference to priority.id: The issue
        priority (Low, Medium High) as an internal scale.
    -   **resolution** - INT - reference to resolution.id: The issue
        resolution (Fixed, Duplicate, Works as designed) as an internal
        JIRA identifier
    -   **fixVersion** - INT - reference to fixversion.id: The earliest
        version in which the issue is fixed as an internal JIRA
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
    -   **project_id** - INT - reference to project.project_id: The JIRA
        project in which this issue is contained.
    -   **status** - INT - reference to status.id: The issue status
        (Open, In Progress, Resolved, Closed) as an internal JIRA
        identifier.
    -   **reporter** - VARCHAR(JIRA developer) - reference to
        developer.name: The reporter of the issue according to the issue
        data. Usually, this is the same developer as *updated_by* on the
        first version, but this may be different due to cloned issues or
        intermediate changes.
    -   **assignee** - VARCHAR(JIRA developer) - reference to
        developer.name: The developer that should resolve the issue, or
        NULL if none is assigned thus far.
    -   **attachments** - INT: The number of attachments in the issue.
        Updated according to changed in the changelog.
    -   **additional_information** - TEXT: Human-readable text that is
        shown within a tab beside the description, or NULL if it is not
        filled in. Often used for extra implementation/functional
        details for user stories.
    -   **review_comments** - TEXT: Human-readable text that is shown in
        a Review tab beside the description, or '0' if it is not filled
        in. Updated by reviewing developers to check whether the story
        is complete enough to start with it or that an issue is fixed.
    -   **story_points** - DECIMAL(5,2): The number of points assigned
        to a story after the developers meet in a refinement and
        determine the difficulty of the story. If not yet set, then this
        is NULL. Maximum value is 100 and two digits after the decimal
        point is allowed.
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
    -   **updated_by** - VARCHAR(JIRA developer) - reference to
        developer.name: The developer that made a change in this version
        of the issue.
    -   **rank_change** - BOOL: The rank change performed in this
        change. Possible values are *true*, meaning an increase in rank,
        *false*, meaning a decrease in rank, or NULL, which means that
        the rank was not changed. The actual rank cannot be deduced from
        the changes due to dependencies on the ranks of other issues.
    -   **epic** - VARCHAR(Issue key): The issue key of the Epic link.
    -   **impediment** - BOOL: Whether the issue is currently marked as
        being blocked by an Impediment.
    -   **ready_status** - INTEGER - reference to ready_status.id: The
        refinement ready status as an internal JIRA identifier.
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
        an internal JIRA identifier.
    -   **test_execution_time** - INT: Units of time that the test
        execution appears to take. This is often set after the use case
        is resolved and tested. If this is not set, then it is the
        integer 0.
    -   **encryption** - INTEGER(row encryption)

### Context tables

-   **sprint**: Data regarding a sprint registered in JIRA, including
    the start and end dates.
    -   **sprint_id** - INT - primary key: identifier for the given
        sprint
    -   **project_id** - INT - reference to project.project_id: Project
        in which the given sprint occurs
    -   **name** - VARCHAR(500): Human-readable name of the sprint, may
        include a sequence number (not unique) and/or the sprint topic
        if it is a specialized sprint
    -   **start_date** - TIMESTAMP: Moment in time at which the sprint
        starts or is set to start. May differ from the actual start time
        (but usually not by more than a day).
    -   **end_date** - TIMESTAMP: Moment in time at which the sprint
        ends or is set to end. May be a projected date from the start
        and thus has a time which is not always correct, but is usually
        close to the real date.
    -   **complete_date** - TIMESTAMP: Moment in time at which the tasks
        in the sprint are completed. This may be different than the
        latest change to an issue within the sprint, and may be not
        provided for all sprints.


-   **project**: The projects that were collected. The name is the JIRA
    key, the project ID is unrelated to internal JIRA IDs.
    -   **project_id** - INT - primary key: Sequence number of the
        project when it is inserted into the database.
    -   **name** - VARCHAR(100): JIRA key prefix abbreviation.


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
    -   **updated_date** - TIMESTAMP: Most recent time at which the
        message was edited.
    -   **encryption** - INTEGER(row encryption)

### Metadata tables

-   **issuetype**: The types of issues and their descriptive names,
    e.g., Bug, Task, Story or Epic.
    -   **id** - INT - primary key: Internal JIRA identifier
    -   **name** - VARCHAR(100)
    -   **description** - VARCHAR(500)


-   **status**: The statuses that issues can have, such as Closed,
    Resolved, Opened or In Progress. Actual use may differ between
    project based on Sprint board setup.
    -   **id** - INT - primary key: Internal JIRA identifier
    -   **name** - VARCHAR(100)
    -   **description** - VARCHAR(500)


-   **resolution**: Once an issue receives a status of Resolved or
    Closed, an indicator of why it was closed (such as Fixed, Duplicate
    or Works as designed).
    -   **id** - INT - primary key: Internal JIRA identifier
    -   **name** - VARCHAR(100)
    -   **description** - VARCHAR(500)


-   **developer**: Names of JIRA users that performed some action in an
    issue, including short account name and long display name.
    -   **id** - INT - primary key: Auto-incrementing identifier.
    -   **name** - VARCHAR(JIRA developer): Abbreviation of the JIRA
        developer.
    -   **display_name** - VARCHAR(100): Name of the JIRA developer as
        displayed in the JIRA interface.
    -   **email** - VARCHAR(100): The email address of the JIRA
        developer.
    -   **encryption** - INTEGER(row encryption)


-   **project_developer**: **Not yet used.** Names of JIRA users,
    encrypted using the project-specific salt rather than a global salt.
    Only use this for matching against (encrypted) developer names from
    other sources. The fields duplicate those of *developer*, but the
    values are encrypted differently and some of the fields of the
    latter table may be removed.
    -   **project_id** - INT - reference to project.id: Project that the
        JIRA developer worked on.
    -   **developer_id** - INT - reference to developer.id: Global
        version of the developer.
    -   **name** - VARCHAR(JIRA developer): Abbreviation of the JIRA
        developer.
    -   **display_name** - VARCHAR(100): Name of the JIRA developer as
        displayed in the JIRA interface.
    -   **email** - VARCHAR(100): The email address of the JIRA
        developer.
    -   **encryption** - INTEGER(row encryption)


-   **fixversion**: Indicators in JIRA issues of the version to fix the
    issue before that version is released.
    -   **id** - INT - primary key: Internal JIRA identifier
    -   **project_id** - INT - reference to project.id: The project this
        version belongs to.
    -   **name** - VARCHAR(100): The name (version numbering scheme) of
        the release version.
    -   **description** - VARCHAR(500): Description provided to the
        release version.
    -   **start_date** - DATE: Day on which work started on this fix
        version according to JIRA aggregate information.
    -   **release_date** - DATE: Day on which the version is released or
        is supposed to be released.
    -   **released** - BOOL: Whether the fix version has been released
        already.


-   **priority**: An indicator of the priority of the issue, on a scale
    of 1-5, with names like Low, Medium, High. This is not necessarily
    related to user story points or backlog prioritization.
    -   **id** - INT - primary key: Internal JIRA identifier
    -   **name** - VARCHAR(100)


-   **ready_status**: An indicator of the pre-refinement ready state:
    Whether the user story can be pulled into the next stage such as
    Workshops, Design meetings, and Refinements. It may also be Blocked
    for or by any of these stages.
    -   **id** - INT - primary key: Internal JIRA identifier
    -   **name** - VARCHAR(100): Human-readable short description of the
        ready status: 'Ready for refinement', 'Blocked for design'


-   **test_execution**: The state of how a use case is tested.
    -   **id** - INT - primary key: Internal JIRA identifier
    -   **value** - VARCHAR(100): 'Manual', 'Automated' or 'Will not be
        executed'

### Relationship tables

-   **issuelink**: Bidirectional links that exist between pairs of
    issues in JIRA. Only the links that exist when the data is collected
    are stored here (no changelogs). Primary key consists of (from_key,
    to_key, relationship_type, outward).
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
        or NULL if not known.
    -   **end_date** - TIMESTAMP - reference to issue.updated: Point in
        time when the link was last removed from the *from_key* issue,
        or NULL if the link still exists.


-   **relationshiptype**: The types of relationships that can exist
    between issues, such as Blocks, Details or Duplicate
    -   **id** - INT - primary key: The Jira identifier of this issue
        link relationship type
    -   **name** - VARCHAR(100): Textual name of the relationship, as a
        noun or verb

    :   **The following fields do not yet exist, but may be added at a
        later stage:**

    -   **outward** - VARCHAR: Phrase used to describe the outward
        relation, such as 'blocks' or 'duplicates'
    -   **inward** - VARCHAR: Phrase used to describe the inward
        relation, such as 'is blocked by' or 'is duplicated by'


-   **subtasks**: Links that exists between issues and their subtasks,
    different from the issue links.
    -   **id_parent** - INT - reference to issue.issue_id: The parent
        issue.
    -   **id_subtask** - INT - reference to issue.issue_id: The subtask
        issue

## Version control system tables (Git, Subversion)

These tables include data from Gitlab/Git and Subversion.

-   **commits**: Data from individual commits in VCS (Git or Subversion)
    repositories used by the development team.
    -   **version_id** - VARCHAR(100): SHA hash or revision number
        belonging to this code version. The version number is unique for
        the repository the change is made in.
    -   **project_id** - INT - reference to project.project_id: The
        project this code version belongs to.
    -   **commit_date** - TIMESTAMP: point in time at which the commit
        was made (in case of Git: the commit date, not the push date).
    -   **sprint_id** - INT - reference to sprint.sprint_id: The sprint
        in which this commit was made, based on date intervals.
    -   **developer_id** - INT - reference to git_developer.alias_id:
        The developer that made the commit.
    -   **message** - TEXT: The full commit message that is shown in
        version control logs.
    -   **size_of_commit** - INT: The byte size of the commit. The
        semantic meaning differs between version control system, namely
        whether it includes the diff or other data.
    -   **insertions** - INT: Number of lines "added" or changed but not
        deleted in this commit.
    -   **deletions** - INT: Number of lines "deleted" or changed in
        this commit.
    -   **number_of_files** - INT: Number of files touched by this
        commit.
    -   **number_of_lines** - INT: Number of lines touched by this
        commit, be it added, deleted or changed.
    -   **type** - VARCHAR(100): The type of code change made (commit,
        revert, merge). Deduced from auxiliary data and possibly the
        commit message.
    -   **repo_id** - INT - reference to repo.id: The repository in
        which the code change is made.


-   **git_developer**: User names from VCS commits
    -   **alias_id** - INT - primary key: Sequential number assigned to
        the developer.
    -   **jira_dev_id** - INT - reference to developer.id. The matching
        is based on the VCS developer's display name and the JIRA
        developer display name or short name. The matching is also
        manually tweaked using data_gitdev_to_dev.json.
    -   **display_name** - VARCHAR(500): The name of the developer used
        in the version control system.
    -   **email** - VARCHAR(100): Email address that the developer uses.
    -   **encryption** - INTEGER(row encryption)


-   **repo**: Repository names
    -   **repo_id** - INT - primary key: Auto-incrementing identifier.
    -   **repo_name** - VARCHAR(1000): Readable name of the repository
        extracted from one of the data sources (GitLab, project
        definition, path name).


-   **change_path**: Statistics on files that are changed in versions in
    the repository.
    -   **repo_id** - INT - reference to repo.repo_id: The repository in
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


-   **tag**: Release tags specified in the repository.
    -   **repo_id** - INT - reference to repo.repo_id: The repository in
        which the tag is added.
    -   **tag_name** - VARCHAR(100): The name of the tag.
    -   **version_id** - VARCHAR(100) - reference to commits.version_id:
        The version in which the tag is added (Subversion) or which the
        tag references (Git).
    -   **message** - TEXT: Message that is added to the tag when it is
        created, separate from the commit message. Only available for
        Git repositories, and optional.
    -   **tagged_date** - TIMESTAMP: Date on which the tag is created
        (Git) or most recently updated (Subversion). Note that tags
        should not be altered once they are created, so the tagged date
        should reflect the commit date for Subversion. The date may be
        missing for incomplete Git tags.
    -   **tagger_id** - INT - reference to vcs_developer.id: The
        developer that created the tag. If the developer cannot be
        deduced from tag information, then this is null.

### GitLab tables

-   **gitlab_repo**: GitLab-specific metadata of a repository. Primary
    key is (repo_id, gitlab_id).
    -   **repo_id** - INT - reference to repo.id: Repository that the
        GitLab repo stores.
    -   **gitlab_id** - INT: internal GitLab identifier of the
        repository.
    -   **description** - TEXT: Description of the repository.
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


-   **merge_request**: A request within the development team to merge a
    Git branch into another. Primary key is (repo_id, request_id).
    -   **repo_id** - INT - reference to repo.id: Repository whose
        branches are the topic of the request.
    -   **request_id** - INT: Internal GitLab identifier which is unique
        for the GitLab instance.
    -   **title** - TEXT: The short header message describing what the
        merge request is about, e.g., what branches/commits it merges.
    -   **description** - TEXT: The contents of the request message.
    -   **source_branch** - VARCHAR(Git branch): The (feature) branch
        from which commits should be merged.
    -   **target_branch** - VARCHAR(Git branch): The (main) branch at
        which the commits should be merged into.
    -   **author** - VARCHAR(500): The GitLab user name of the developer
        that started the request.
    -   **assignee** - VARCHAR(500): The GitLab user name of the
        developer that should review the request. This is NULL if nobody
        is assigned.
    -   **upvotes** - INT: Number of votes from the development team in
        support of the merge request.
    -   **downvotes** - INT: Number of votes from the development team
        against the merge request.
    -   **created_date** - TIMESTAMP: Time at which the merge request is
        created.
    -   **updated_date** - TIMESTAMP: Time at which the merge request
        received an update (a merge request note or update to the
        request details).
    -   **encryption** - INTEGER(row encryption)


-   **merge_request_note**: A comment or automated message attached to a
    merge request, which is a part of a discussion about the request or
    describes changes to request details or branch commits. Primary key
    is (repo_id, request_id, note_id).
    -   **repo_id** - INT - reference to repo.id: Repository for which
        this note is added.
    -   **request_id** - INT - reference to merge_request.request_id:
        Merge request to which this note is added.
    -   **note_id** - INT: Internal GitLab identifier which is unique
        for the GitLab instance.
    -   **author** - VARCHAR(500): The GitLab user name of the developer
        that wrote the comment or on whose regard the automated comment
        is added.
    -   **comment** - TEXT: Plain text comment message of the note.
        Automated notes have a first line which is surrounded with
        underscores, or matches the regex "Added \\d commits?:" with the
        remaining lines either empty or starting with star-bullets.
    -   **created_date** - TIMESTAMP: Time at which the comment is added
        to the merge request.
    -   **encryption** - INTEGER(row encryption)


-   **commit_comment**: A comment attached to a version in the Git
    repository. Note that the comment may be part of a review of a merge
    request, but this is not directly visible from the data.
    -   **repo_id** - INT - reference to repo.id: Repository for which
        this note is added.
    -   **version_id** - VACHAR(100) - reference to commits.version_id:
        Commit to which this note is added.
    -   **file** - VARCHAR(1000): Path to a file in the repository that
        is changed in the commit and is discussed by the comment. If
        this is NULL, then the comment belongs to the entire version.
    -   **line** - INT: Line number of the file that is discussed by the
        comment. If this is NULL, then the comment belongs to the entire
        version.
    -   **line_type** - VARCHAR(100): The type of line being discussed
        by the comment: 'old' or 'new'. If this is NULL, then the
        comment belongs to the entire version.
    -   **encryption** - INTEGER(row encryption)

## Metrics tables (Quality dashboard history)

These tables include data from the metrics history files and the quality
dashboard project definition.

-   **metric**: Metric types
    -   **metric_id** - INT - primary key: Sequential number assigned to
        the metric.
    -   **metric_name** - VARCHAR(100): Amalgamated name of the metric,
        based on the type of metric and the component, product, team or
        other domain object it measures.


-   **metric_value**: Singular metric data from quality report
    -   **metric_id** - INT - reference to metric.metric_id: The metric
        that is measured
    -   **value** - INT: The raw value. This is -1 if there is a problem
        with measuring the metric (category is grey, missing, or
        missing_source).
    -   **category** - VARCHAR(100): 'red' (below low target), 'yellow'
        (below target), green (at or above target), perfect (cannot be
        improved), grey (disabled), missing (internal problem),
        missing_source (external problem).
    -   **date** - TIMESTAMP: Time at which the measurement took place.
    -   **since_date** - TIMESTAMP - reference to metric_value.date:
        Time since which the metric has the same value.
    -   **project_id** - INT - reference to project.project_id: The
        project for which the measurement was made


-   **metric_version**: Versions of the project definition in which
    changes to target norms of a project are made. Primary key is
    (project_id, version_id)
    -   **project_id** - INT - reference to project.project_id
    -   **version_id** - INT: Subversion revision number
    -   **developer** - VARCHAR(64): Developer or quality lead that made
        the change
    -   **message** - TEXT: Commit message describing the change
    -   **commit_date** - TIMESTAMP: Time at which the target change
        took place
    -   **encryption** - INTEGER(row encryption)


-   **metric_target**: Manual changes to the metric targets of a project
    -   **project_id** - INT - reference to project.project_id
    -   **version_id** - INT - reference to metric_version.version_id
    -   **metric_id** - INT - reference to metric.metric_id: Metric
        whose norms are changed
    -   **type** - VARCHAR(100): Type of change: 'options',
        'old_options', 'TechnicalDebtTarget'
    -   **target** - INT: Norm value at which the category changes from
        green to yellow.
    -   **low_target** - INT: Norm value at which the category changes
        from yellow to red.
    -   **comment** - TEXT: Comment for technical debt targets
        describing the reason of the norm change

## Reservation tables (Topdesk)

-   **reservation**: A reservation that is planned in the Topdesk
    self-service tool.
    -   **reservation_id** - VARCHAR(10) - primary key: Reservation
        identifier as a formatted number.
    -   **project_id** - INT - reference to project.id: Project to which
        the reservation belongs according to whitelist/blacklist
        matching.
    -   **requester** - VARCHAR(500): Name of the person who requests
        the reservation.
    -   **number_of_people** - INT: Number of people that the
        reservation encompasses. This may be 0 if not filled in, and
        other values may not be exact or meaningful either.
    -   **description** - TEXT: The text accompanying the reservation,
        usually a description of what type of meeting it is and possibly
        who is involved.
    -   **start_date** - TIMESTAMP: Time (up to minute) at which the
        reservation starts.
    -   **end_date** - TIMESTAMP: Time (up to minute) at which the
        reservation ends.
    -   **prepare_date** - TIMESTAMP: Time (up to minute) at which the
        reservation is booked to perform setup in the room. If no
        preparation is needed, then this is the same as start_date.
    -   **close_date** - TIMESTAMP: Time (up to minute) until which the
        reservation is booked to break down any setup in the room. If no
        dismantling is needed, then this is the same as end_date.
    -   **encryption** - INTEGER(row encryption)

## Internal trackers

-   **update_tracker**: Files that keep track of where to start
    gathering data from for incremental updates and database
    synchronization.
    -   **project_id** - INT - reference to project.id: The project to
        which the tracker file belongs.
    -   **filename** - VARCHAR(255): The name of the file (without path)
        that keeps track of the update state.
    -   **contents** - TEXT: The textual (JSON or otherwise readable)
        contents of the file that tracks the update state.
    -   **update_date** - TIMESTAMP: The latest modification date of the
        file.


-   **project_salt**: Project-specific hash pairs that are used for
    one-way encryption of [sensitive data](sensitive_data).
    -   **project_id** - INT - reference to project.id: The project for
        which the hash holds. If this is 0, then it indicates a global
        hash pair.
    -   **salt** - VARCHAR(32): First salt of the project data.
    -   **pepper** - VARCHAR(32): Second salt of the project data.
