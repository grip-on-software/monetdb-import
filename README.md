# MonetDB import and management

This repository contains a Java application which can interact with a MonetDB 
database for a Grip on Software data collection in order to import JSON 
representations of source information.

The repository also contains scripts that validate schemas, perform schema 
upgrades, allow imports of backups and exchange formats, as well as generate 
exports, partially via the 
[monetdb-dumper](https://github.com/grip-on-software/monetdb-dumper) 
repository.

## Importer application 

### Requirements, configuration and building

The importer application has been tested with Semeru OpenJDK 21. In order to 
build the application, we use Ant 1.10.1+ with the JDK (a package with 
`javac`). Make sure your `JAVA_HOME` environment variable points to the correct 
JDK directory if you have multiple possible installations.

Before building, ensure you have create a file in the path 
`Code/importerjson/nbproject/private/config.properties`, possibly by copying 
the `config.properties.example` file to there and editing it, containing the 
following properties:

```
importer.url=jdbc:monetdb://MONETDB_HOST/gros
importer.user=MONETDB_USER
importer.password=MONETDB_PASSWORD
importer.relPath=export
importer.email_domain=EMAIL_DOMAIN
```

Replace the values to properly connect to the MonetDB database and define the 
internal domain used at the organization. Take care to use a proper 
JDBC-MonetDB URL for the `importer.url` property, including the correct port to 
connect to the database and the name of the database (`gros` by default). The 
`importer.relPath` property should be a relative path from the working 
directory when the application is run. These properties can be overridden 
during execution using defines.

Now run the following command in order to build the MonetDB import application: 
```
ant -buildfile Code/importerjson/build.xml \
    -propertyfile Code/importerjson/nbproject/private/config.properties
```

The JAR is then made available in `Code/importerjson/dist/importerjson.jar`.

### Running importer

The MonetDB importer works on JSON data files that contain representations of 
data acquired from software development systems which have been processed by 
a [data-gathering](https://github.com/grip-on-software/data-gathering) agent. 
JSON schemas for those files are also available in that repository. Typically, 
a released version of the importer is compatible with the same version of the 
data-gathering agent, with backward compatibility for the same major version.

Run the application as follows:

```
java -Dimporter.log=LEVEL [...other defines] -jar \
    Code/importerjson/dist/importerjson.jar PROJECT TASKS
```

In this command, replace `LEVEL` with an appropriate log level (for example 
`INFO`), the `PROJECT` with a project key to import for (must be a subdirectory 
of the relative path), and the `TASKS` with the import tasks (`all` by 
default). In case there is a complex selection of tasks required, then they may 
be provided as a comma-separated list of tasks or group names, where a minus 
sign before a task or group excludes that task from operation again. Special 
tasks (often data corrections) are not performed by default and should be added 
to the list if they are to be performed. If the project argument is `--`, then 
only special tasks may be performed and they are done as organization-wide 
changes (if not already). The project argument can also be `--files`, in which 
case a list of files involved for the selected tasks is printed instead of 
performing them. The import tasks and configuration aspects are also described 
when `--help` is provided for the project argument, which then also exits out.

You may possibly add other defines at the start of the command, including 
replacement values for the properties defined in the `config.properties` 
property file included during the build (`importer.url`, `importer.user`, 
`importer.password`, `importer.relPath` and `importer.email_domain`), as well 
as the following:

- `importer.update`: A space-separated list of update tracker files to import 
  for the `update` task, such that subsequent data gathering can continue from 
  this state and thus support incremental collection and import. Update 
  trackers are also used by some GROS visualizations to determine source age.
- `importer.encrypt_tables`: A comma-separated list of table names to perform 
  encryption of personally identifying information on for the `encrypt` task. 
  By default, this task encrypts project-specific tables with developer 
  information with the project encryption key if a project is selected or 
  organization-common tables with the global encryption key if no project is 
  selected (with `--` as `PROJECT` argument).

### Testing

Tests can be performed during the build using:

```
ant -buildfile Code/importerjson/build.xml \
    -propertyfile Code/importerjson/nbproject/private/config.properties test
```

Note that one test is an integration test, which requires a few things to be 
set up beforehand, otherwise it will detect and skip the test:

- Import data files should be placed in directories 
  `Code/importerjson/export/TEST1` through `Code/importerjson/export/TEST10`, 
  as well as `Code/importerjson/data_vcsdev_to_dev.json`.
- A MonetDB database instance should be running on `localhost` on the default 
  port (`50000`) and a database with the name `gros_test` should be created and 
  pre-filled with the database schema.

These two steps can be simplified by [running scripts](#running-scripts), 
respectively the `generate_test_files.py` and `recreate_database.py` scripts. 
Note that the check whether a database is available may take a long time before 
skipping, as this depends on database pooling options which try to reconnect to 
the database several times until it gives up.

Test output should indicate the successful, failed and skipped tests. Once the 
test is complete, test result and coverage information is made available in 
`Code/importerjson/build/test`, with JUnit XML files in `junit/junit.xml` in 
that directory and JaCoCo coverage XML in `jacoco.xml` and HTML reports in 
`jacoco/index.html`.

## Management scripts

### Requirements

The scripts can be run using Python 3.7+ and Bash. The Python installation 
should have Pip or virtualenv installed so that dependencies can be installed, 
for example using `pip install -r Scripts/requirements.txt`. If there are not 
enough permissions to install the dependencies on the system, then you can add 
`--user` in the command, after `pip install`. Otherwise, you can create 
a `virtualenv ENV`, activate it with `source ENV/bin/activate` and install the 
dependencies there.

### Configuration

Copy the `Scripts/settings.cfg.example` to `Scripts/settings.cfg`. From this 
point on, we assume you are in the `Scripts` working directory, otherwise place 
the `settings.cfg` in another working directory and adjust any further paths to 
the scripts.

In the `settings.cfg` file, replace the variables in the configuration items of 
the groups in order to properly configure the environment of the scripts:

- `monetdb`: MonetDB database connection settings
  - `hostname` (`$MONETDB_HOSTNAME`): Domain name of the database host.
  - `passphrase` (`$MONETDB_PASSPHRASE`): Passphrase for administrative remote 
    control (not the same as the client password).
  - `username`: (`$MONETDB_USERNAME`): Username that has authorization to 
    create and alter tables.
  - `password` (`$MONETDB_PASSWORD`): Password of the user that can create and 
    alter tables.
  - `database` (`$MONETDB_DATABASE`): Database name that can be (re)created or 
    have its schema altered.
- `jenkins`: Jenkins connection settings
  - `host` (`$JENKINS_HOST`): Base URL of the Jenkins instance.
  - `job` (`$JENKINS_JOB`): Jenkins job that has a workspace that can be 
    deleted in order to clean up any leftover tracking data.
  - `username` (`$JENKINS_USERNAME`): Username to log in to Jenkins.
  - `token` (`$JENKINS_TOKEN`): Password or API token to log in to Jenkins.
  - `crumb`: Whether to request a CSRF crumb before performing other API 
    requests. This should be "yes", as Jenkins instances that did not support 
    (or require) this are ancient.
- `schema`: Database table schema validation
  - `url` (`$SCHEMA_URL`): URL to an external MediaWiki or JSON resource that 
    documents the table schema.
  - `path`: The path to an SQL file that can be used to create the table schema 
    in an empty database.
  - `verify` (`$SCHEMA_VERIFY`): Whether to verify the SSL certificate when 
    obtaining the schema documentation from an external URL. If this is set to 
    a file path, then that file is used to verify the certificate with.
  - `username` (`$SCHEMA_USERNAME`): Username to use for Basic authorization 
    when obtaining the schema documentation from an external URL realm.
  - `password` (`$SCHEMA_PASSWORD`): Password to use for Basic authorization 
    when obtaining the schema documentation from an external URL realm.

Some configuration can be adjusted through command line arguments in the 
scripts (and some scripts do not use the configuration file).

### Running scripts

The following scripts are available to manage the database:

- `dump_tables.sh`: Perform a (partial) database dump using compressed SQL/CSV 
  files plus the schema, placed into a timestamped output directoryy by default 
  (uses the `monetdb-dumper` application)
- `import_tables.sh`: Perform an import of a database dump (assumes an empty 
  database as this also creates the schema provided with the dump)
- `recreate_database.py`: Destroy the database and create it again, usually 
  with the current schema, possibly wiping out a Jenkins workspace as well
- `update_database.py`: Perform schema upgrades on an existing database.
- `validate_schema.py`: Compare a documentation resource against the database 
  table schema file in order to check for validation errors and differences.

Use the `--help` argument for the scripts to receive more details on running 
the scripts and their arguments.

Additionally, the file `generate_test_files.py` is usable for setting up the 
integration tests for the importer application by creating generated files 
based on JSON Schemas of the data imports, which requires a clone of the 
[data-gathering](https://github.com/grip-on-software/data-gathering) repository 
to be available.

The script `workbench_group.py` only works within the MySQL Workbench Scripting 
Shell and is meant to alter the model file for entity-relationship diagrams.

### Schema documentation

Within the `Scripts` directory, several versions of documentation of the 
database schema can be found:

- `Database_structure.md`: Exhaustive documentation on tables, keys, attributes 
  and references, including what each means and in which cases columns can be 
  `NULL` or other specific values.
- `Sensitive_data.md`: Additional documentation on what (future) steps can be 
  taken for specific fields stored within the database to keep 
  project-sensitive data and personal data secure.
- `create-tables.sql`: The actual schema for MonetDB in `CREATE TABLE` SQL 
  statements.
- `database-model.mwb`: A MySQL Workbench file containing a converted version 
  of the schema.

Some of these files are used by the scripts in order to perform validation or 
conversion to other formats, such as JSON.

## License

The MonetDB importer is licensed under the Apache 2.0 License. Dependency 
libraries are included in object form (some libraries are only used in tests) 
and have the following licenses:

- CopyLibs: Part of NetBeans, distributed under Apache 2.0 License
- [ahocorasick](https://github.com/robert-bor/aho-corasick): Apache 2.0 License
- [c3p0](https://github.com/swaldman/c3p0): LGPL v2.1 (or any later version) or 
  EPL v1.0
- [joda-time](https://github.com/JodaOrg/joda-time): Apache 2.0 License
- [json-simple](https://github.com/fangyidong/json-simple): Apache 2.0 License
- [mchange-commons-java](https://github.com/swaldman/mchange-commons-java): 
  LGPL v2.1 (or any later version) or EPL v1.0
- [monetdb-jdbc](https://github.com/MonetDB/monetdb-java): MPL v2.0, available 
  from [MonetDB Java Download Area](https://www.monetdb.org/downloads/Java/)

Test libraries:

- [hamcrest-core](https://github.com/hamcrest/JavaHamcrest): BSD License, see 
  [LICENSE.txt](Code/importerjson/lib/hamcrest/LICENSE.txt)
- [jacoco](https://github.com/jacoco/jacoco) (agent and ant task): EPL v2.0
- [junit4](https://github.com/junit-team/junit4): EPL v1.0
