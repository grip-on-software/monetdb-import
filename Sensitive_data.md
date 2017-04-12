# Sensitive data

Project sensitive data and personal data that need to be handled
differently for this reason.

Legend of [DataSources](DataSources.md): JIRA = Issue tracker,
VCS = Version control system, SSD = Self-Service Desk (reservations), \*
= multiple sources. See [Database
structure](Database_structure.md) for tables and fields.

| Source | Table              | Field            | Reason                            | Solution                                                                                                          |
|--------|--------------------|------------------|-----------------------------------|-------------------------------------------------------------------------------------------------------------------|
| JIRA   | project            | name             | Recongizable project abbreviation | Encrypt with global salt afterward                                                                                |
| JIRA   | issue              | reporter         | Recongizable user shorthand       | Encrypt with project salt                                                                                         |
| JIRA   | issue              | assignee         | Recongizable user shorthand       | Encrypt with project salt                                                                                         |
| JIRA   | issue              | updated_by       | Recongizable user shorthand       | Encrypt with project salt                                                                                         |
| JIRA   | developer          | name             | Recongizable user shorthand       | Encrypt with project salt, and separately store global version for the purpose of matching users between projects |
| JIRA   | developer          | display_name     | Personal name                     | Encrypt with project salt, and separately store global version for the purpose of matching users between projects |
| HQ     | metric_version     | developer        | Recongizable user shorthand       | Encrypt with project salt                                                                                         |
| JIRA   | comment            | author           | Recongizable user shorthand       | Encrypt with project salt                                                                                         |
| \*     | \*                 | TIMESTAMP fields | Property of project (time span)   | Adapt all data afterward to store differences compared to first date within project (epoch)                       |
| VCS    | vcs_developer      | display_name     | Personal name                     | Encrypt with project salt beforehand                                                                              |
| VCS    | repo               | repo_name        | Recognizable project artifact     | Encrypt with project salt beforehand                                                                              |
| VCS    | tag                | tag_name         | Recognizable project artifact     | Encrypt with project salt beforehand                                                                              |
| VCS    | gitlab_repo        | description      | Recognizable project artifact     | Encrypt with project salt beforehand                                                                              |
| VCS    | merge_request      | author           | Personal name                     | Encrypt with project salt beforehand                                                                              |
| VCS    | merge_request      | assignee         | Personal name                     | Encrypt with project salt beforehand                                                                              |
| VCS    | merge_request_note | author           | Personal name                     | Encrypt with project salt beforehand                                                                              |
| VCS    | commit_comment     | author           | Personal name                     | Encrypt with project salt beforehand                                                                              |
| SSD    | reservation        | requester        | Personal name                     | Encrypt with project salt                                                                                         |
| SSD    | reservation        | description      | Recognizable project artifact     | Encrypt with project salt                                                                                         |

Additionally, from VCS:

-   Salt changed paths (commit_comment.file, commit_path.path)
-   Filter source code by replacing each character with a zero
    character, in all versions, using [BFG repo
    cleaner](https://rtyley.github.io/bfg-repo-cleaner/) with custom
    filter options (see filter-sourcecode-projects job)
