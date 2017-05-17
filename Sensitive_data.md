# Sensitive data

Project sensitive data and personal data that need to be handled
differently for this reason.

Legend of [DataSources](DataSources.md): JIRA = Issue tracker,
VCS = Version control system, SSD = Self-Service Desk (reservations), \*
= multiple sources. See [Database
structure](Database_structure.md) for tables and fields.

| Source | Table             | Field            | Reason                             | Solution                                                                                    | Status                                                             |
|--------|-------------------|------------------|------------------------------------|---------------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| JIRA   | project           | name             | Recognizable project abbreviation  | Encrypt with global salt afterward                                                          | Not yet done                                                       |
| JIRA   | issue             | reporter         | Recognizable user shorthand        | Encrypt with project and/or global salt                                                     | Run task during import, or manual global encryption step afterward |
| JIRA   | issue             | assignee         | Recognizable user shorthand        | Encrypt with project and/or global salt                                                     | Run task during import, or manual global encryption step afterward |
| JIRA   | issue             | updated_by       | Recognizable user shorthand        | Encrypt with project and/or global salt                                                     | Run task during import, or manual global encryption step afterward |
| JIRA   | developer         | name             | Recognizable user shorthand        | Encrypt with global salt                                                                    | Manual global encryption step afterward                            |
| JIRA   | developer         | display_name     | Personal name                      | Encrypt with global salt                                                                    | Manual global encryption step afterward                            |
| JIRA   | developer         | email            | Personally identifying information | Encrypt with global salt                                                                    | Manual global encryption step afterward                            |
| JIRA   | project_developer | name             | Recognizable user shorthand        | Encrypt with project salt                                                                   | Automatic during developer imports                                 |
| JIRA   | project_developer | display_name     | Personal name                      | Encrypt with project salt                                                                   | Automatic during developer imports                                 |
| JIRA   | project_developer | email            | Personally identifying information | Encrypt with project salt                                                                   | Automatic during developer imports                                 |
| HQ     | metric            | name             | Recognizable project artifact      | Encrypt with project salt beforehand                                                        | Not yet done                                                       |
| HQ     | metric_version    | developer        | Recognizable user shorthand        | Encrypt with global salt                                                                    | Manual global encryption step afterward                            |
| JIRA   | comment           | author           | Recognizable user shorthand        | Encrypt with project and/or global salt                                                     | Run task during import, or manual global encryption step afterward |
| \*     | \*                | TIMESTAMP fields | Property of project (time span)    | Adapt all data afterward to store differences compared to first date within project (epoch) | Not yet done                                                       |
| VCS    | vcs_developer     | display_name     | Personal name                      | Encrypt with project salt beforehand                                                        | Run task during import, or manual global encryption step afterward |
| VCS    | repo              | repo_name        | Recognizable project artifact      | Encrypt with project salt beforehand                                                        | Not yet done                                                       |
| VCS    | tag               | tag_name         | Recognizable project artifact      | Encrypt with project salt beforehand                                                        | Not yet done                                                       |
| VCS    | gitlab_repo       | description      | Recognizable project artifact      | Encrypt with project salt beforehand                                                        | Not yet done                                                       |
| SSD    | reservation       | requester        | Personal name                      | Encrypt with project salt                                                                   | Run task during import, or manual global encryption step afterward |
| SSD    | reservation       | description      | Recognizable project artifact      | Encrypt with project salt                                                                   | Not yet done                                                       |

Additionally, from VCS:

-   Salt changed paths (commit_comment.file, commit_path.path) with
    project salt (Not yet done)
-   Filter source code by replacing each character with a zero
    character, in all versions, using [BFG repo
    cleaner](https://rtyley.github.io/bfg-repo-cleaner/) with custom
    filter options (see [filter-sourcecode-projects
    job](http://www.jenkins.example:8080/view/GROS/job/filter-sourcecode-projects/)).
    This is done for archived projects, and should be done for all
    projects in order to keep all necessary information available. The
    filters should be improved such that they also salt the changed
    paths.
