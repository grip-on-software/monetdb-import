/**
 * Application entry point.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2023 Leon Helwerda
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package importer;

import dao.RepositoryDb;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.BaseImport;

/**
 * The main importer application entry point.
 * @author Enrique
 * @author Thomas Helling
 * @author Leon Helwerda
 */
public class Importerjson {
    private static String projectName = "";
    private static int projectID = 0;
    private static Set<String> problematicImports = new TreeSet<>();
    
    private final static HashMap<String, List<String>> GROUPED_TASKS = retrieveGroupedTasks();

    private static HashMap<String, List<String>> retrieveGroupedTasks() {
        HashMap<String, List<String>> groupedTasks = new HashMap<>();
        
        // JIRA
        groupedTasks.put("jira", Arrays.asList(
            "issue", "issuetype", "status", "status_category", "resolution", 
            "priority", "fixVersion", "ready_status", "test_execution",
            "relationshiptype", "issuelink", "subtask",
            "sprint", "comment", "developer", "component"
        ));

        // Quality dashboard metrics
        groupedTasks.put("metrics", Arrays.asList(
            "metric_target", "metric_value", "metric_version"
        ));

        // Version control systems and review systems (Git, GitHub, GitLab, TFS, SVN)
        groupedTasks.put("vcs", Arrays.asList(
            "commit", "change_path", "tag", "vcs_event",
            "gitlab_repo", "github_repo", "github_issue", "github_issue_note",
            "tfs_team", "tfs_team_member", "tfs_developer", "tfs_sprint",
            "tfs_work_item",
            "merge_request", "merge_request_review", "merge_request_note", "commit_comment"
        ));
        
        return groupedTasks;
    }
    
    private final static List<String> DEFAULT_TASKS = retrieveDefaultTasks();
    
    private static List<String> retrieveDefaultTasks() {
        List<String> defaultTasks = new ArrayList<>();
        defaultTasks.addAll(GROUPED_TASKS.get("jira"));
        defaultTasks.addAll(GROUPED_TASKS.get("metrics"));
        defaultTasks.addAll(GROUPED_TASKS.get("vcs"));
        
        // LDAP
        defaultTasks.add("ldap_developer");
        // Source environments
        defaultTasks.add("environment");
        // Source identifiers
        defaultTasks.add("source_id");
        // Jenkins
        defaultTasks.add("jenkins");
        // Self-Service Desk
        defaultTasks.add("reservation");
        // Seats
        defaultTasks.add("seats");
        // Tracking
        defaultTasks.add("update");
        
        // Additional tasks
        defaultTasks.add("developerlink");
        
        return defaultTasks;
    }
    
    private final static List<String> SPECIAL_TASKS = Arrays.asList(
        "sprintlink", "developerproject", "developerlink",
        "metric_domain_name", "metric_default_target",
        "repo_sources", "issue_changelog_id",
        "encrypt"
    );
        
    private final static HashMap<String, Class<? extends BaseImport>> TASK_IMPORTERS = retrieveImporters();
    
    private final static Logger LOGGER = Logger.getLogger("importer");

    private static HashMap<String, Class<? extends BaseImport>> retrieveImporters() {
        HashMap<String, Class<? extends BaseImport>> importers = new HashMap<>();
        
        // JIRA
        importers.put("issue", ImpDataIssue.class);
        importers.put("fixVersion", ImpDataFixVersion.class);
        importers.put("issuelink", ImpDataIssueLink.class);
        importers.put("subtask", ImpDataSubtask.class);
        
        importers.put("status", ImpJiraStatus.class);
        importers.put("status_category", ImpJiraStatusCategory.class);

        importers.put("issuetype", ImportTable.class);
        importers.put("resolution", ImportTable.class);
        importers.put("relationshiptype", ImportTable.class);
        importers.put("priority", ImportTable.class);
        importers.put("ready_status", ImportTable.class);
        importers.put("test_execution", ImportTable.class);
        
        importers.put("sprint", ImpSprint.class);
        importers.put("comment", ImpComment.class);
        importers.put("developer", ImpDeveloper.class);
        importers.put("component", ImpComponent.class);
        
        // Quality dashboard metrics
        importers.put("metric_value", ImpMetricValue.class);
        importers.put("metric_version", ImpMetricVersion.class);
        importers.put("metric_target", ImpMetricTarget.class);
        
        // Version control systems and review systems
        importers.put("commit", ImpCommit.class);
        importers.put("change_path", ImpChangePath.class);
        importers.put("tag", ImpTag.class);
        importers.put("vcs_event", ImpVcsEvent.class);
        
        importers.put("gitlab_repo", ImpGitLabRepo.class);
        
        importers.put("github_repo", ImpGitHubRepo.class);
        importers.put("github_issue", ImpGitHubIssue.class);
        importers.put("github_issue_note", ImpGitHubIssueNote.class);
        
        importers.put("tfs_team", ImpTfsTeam.class);
        importers.put("tfs_team_member", ImpTfsTeamMember.class);
        importers.put("tfs_developer", ImpTfsDeveloper.class);
        importers.put("tfs_sprint", ImpTfsSprint.class);
        importers.put("tfs_work_item", ImpTfsWorkItem.class);
        
        importers.put("merge_request", ImpMergeRequest.class);
        importers.put("merge_request_review", ImpMergeRequestReview.class);
        importers.put("merge_request_note", ImpMergeRequestNote.class);
        importers.put("commit_comment", ImpCommitComment.class);
        
        // Other import tasks
        importers.put("ldap_developer", ImpLdapDeveloper.class);
        importers.put("environment", ImpEnvironment.class);
        importers.put("source_id", ImpSourceId.class);
        importers.put("jenkins", ImpJenkins.class);
        importers.put("reservation", ImpReservation.class);
        importers.put("seats", ImpSeats.class);
        importers.put("update", ImpUpdateTracker.class);
        
        return importers;
    }
    
    private final static HashMap<String, String[]> IMPORTER_ARGUMENTS = retrieveImporterArguments();
    
    private static HashMap<String, String[]> retrieveImporterArguments() {
        HashMap<String, String[]> importers = new HashMap<>();
        importers.put("issuetype", new String[]{"issuetype", "name", "description", "issue types"});
        importers.put("resolution", new String[]{"resolution", "name", "description", "resolution types"});
        importers.put("relationshiptype", new String[]{"relationshiptype", "name", "relationship types"});
        importers.put("priority", new String[]{"priority", "name", "priority types"});
        importers.put("ready_status", new String[]{"ready_status", "name", "ready status names"});
        importers.put("test_execution", new String[]{"test_execution", "value", "test execution methods"});
        return importers;
    }
    
    private static abstract class ImportTask {
        public abstract void performTask(BaseImport importer);
    }

    private static class PerformImport extends ImportTask {
        @Override
        public void performTask(BaseImport importer) {
            long startTime;

            startTime = System.currentTimeMillis();

            importer.setProjectName(projectName);
            importer.setProjectID(projectID);
            importer.setProblematicImports(problematicImports);
            importer.parser();
            if (importer.hasExceptions()) {
                if (problematicImports.equals(importer.getProblematicImports())) {
                    problematicImports.addAll(Arrays.asList(importer.getImportFiles()));
                }
                else {
                    problematicImports.addAll(importer.getProblematicImports());
                }
                showCompleteTask("Encountered problems while importing " + importer.getImportName(), startTime, Level.WARNING);
            }
            else {
                showCompleteTask("Imported " + importer.getImportName(), startTime);
            }
        }
    }
    
    private static class FileCollector extends ImportTask {
        private ArrayList<String> files = new ArrayList<>();
        
        @Override
        public void performTask(BaseImport importer) {
            String[] importFiles = importer.getImportFiles();
            LOGGER.log(Level.INFO, "Importer {0} uses files {1}", new Object[]{importer.getImportName(), Arrays.toString(importFiles)});
            files.addAll(Arrays.asList(importFiles));
        }
        
        public List<String> getFiles() {
            return files;
        }
    }
        
    private static void showCompleteTask(String taskName, long startTime, Level logLevel) {
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        
        LOGGER.log(logLevel, "{0} in {1} seconds", new Object[]{taskName, elapsedTime / 1000});
    }
    
    private static void showCompleteTask(String taskName, long startTime) {
        showCompleteTask(taskName, startTime, Level.INFO);
    }
    
    private static SortedSet<String> retrieveTasks(String[] taskList, Collection<String> validateTasks) {
        SortedSet<String> tasks = new TreeSet<>();
        for (String task : taskList) {
            if (task.equals("all")) {
                tasks.addAll(DEFAULT_TASKS);
            }
            else if (GROUPED_TASKS.containsKey(task)) {
                List<String> groupTasks = GROUPED_TASKS.get(task);
                if (!validateTasks.containsAll(groupTasks)) {
                    LOGGER.log(Level.WARNING, "Some tasks of group {0} could not be added or removed", task);                    
                }
                tasks.addAll(groupTasks);
            }
            else if (task.startsWith("-")) {
                // Remove task from the set (overriding earlier additions).
                // Validate against the current set to check if tasks are
                // being needlessly removed (swhich may indicate input problem).
                // Also, if this is the first option in the list of tasks,
                // then add all default tasks and remove it from there.
                if (tasks.isEmpty()) {
                    tasks.addAll(DEFAULT_TASKS);
                }
                SortedSet<String> removeTasks = retrieveTasks(new String[]{task.substring(1)}, tasks);
                tasks.removeAll(removeTasks);
            }
            else {
                if (!validateTasks.contains(task)) {
                    LOGGER.log(Level.WARNING, "Task {0} could not be added or removed", task);
                }
                tasks.add(task);
            }
        }
        return tasks;
    }
    
    private static String formatUsage() {
        return formatUsage(false);
    }
    
    private static String formatUsage(boolean all) {
        StringBuilder usage = new StringBuilder("\nUsage: java [-Dimporter.log=LEVEL] -jar importerjson.jar <project> [tasks]");
        
        if (all) {
            usage.append("\n\nTask groups and tasks:\n\n - all: All default (non-special) tasks\n");
            
            List<String> otherTasks = new ArrayList<>(DEFAULT_TASKS);
            for (Map.Entry<String, List<String>> group : GROUPED_TASKS.entrySet()) {
                usage.append("\n - ").append(group.getKey()).append(": ");
                usage.append(String.join(", ", group.getValue()));
                
                otherTasks.removeAll(group.getValue());
            }
            
            usage.append("\n\n - Other (default) tasks: ").append(String.join(", ", otherTasks));
            usage.append("\n - Special tasks: ").append(String.join(", ", SPECIAL_TASKS));
        }
        
        return usage.toString();
    }
    
    public static void main(String[] args) {
        String logLevel = System.getProperty("importer.log", "WARNING");
        System.setProperty("java.util.logging.SimpleFormatter.format",
                           "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS:%4$s:%2$s:%5$s%6$s%n");
        try {
            Level level = Level.parse(logLevel);
            Handler handler = new ConsoleHandler();
            handler.setLevel(level);
            LOGGER.addHandler(handler);
            LOGGER.setLevel(level);
            LOGGER.setUseParentHandlers(false);
            LOGGER.log(Level.FINE, "Set log level to {0}", level.getName());
        }
        catch (IllegalArgumentException ex) {
            throw new ImporterException("Illegal importer.log argument: " + ex.getMessage() + formatUsage());
        }
        if (args.length <= 0 || "--help".equals(args[0])) {
            throw new ImporterException(formatUsage(true));
        }
        
        // Determine a set of tasks to run. With a missing argumet, we run all
        // the default tasks. Otherwise, we add all of the tasks and task groups
        // (or remove them when prefixed with a minus sign) from the argument.
        // Warnings are logged if added tasks are not registered at all, or if
        // a task is removed that was not in the list before it.
        SortedSet<String> tasks;
        SortedSet<String> allTasks = new TreeSet<>(DEFAULT_TASKS);
        if (args.length > 1) {
            allTasks.addAll(SPECIAL_TASKS);
            tasks = retrieveTasks(args[1].trim().split(","), allTasks);
        }
        else {
            tasks = allTasks;
        }
                
        if ("--files".equals(args[0])) {
            FileCollector performer = new FileCollector();
            projectName = "ANY";
            performTasks(tasks, performer);
            String files = String.join(" ", performer.getFiles());
            // Output the file list to standard output.
            System.out.println(files); //NOSONAR
            return;
        }
        else if ("--".equals(args[0])) {
            // Only allow special tasks that may run project-independently
            if (!SPECIAL_TASKS.containsAll(tasks)) {
                throw new ImporterException("Project must be given for the provided tasks" + formatUsage());
            }
        }
        else {
            projectName = args[0].trim();
        }
        
        LOGGER.log(Level.INFO, "Tasks to run: {0}", Arrays.toString(tasks.toArray()));
        
        // Perform project import so that project ID is known to exist
        performTasks(tasks, new PerformImport());
        performSpecialTasks(tasks);
    }
    
    private static void performTasks(SortedSet<String> tasks, ImportTask performer) {
        if (!projectName.isEmpty()) {
            ImpProject impProject = new ImpProject();
            performer.performTask(impProject);
            projectID = impProject.getProjectID();
        }
        
        for (String task : DEFAULT_TASKS) {
            if (tasks.contains(task) && TASK_IMPORTERS.containsKey(task)) {
                Class<? extends BaseImport> importClass = TASK_IMPORTERS.get(task);
                BaseImport importer = null;
                if (IMPORTER_ARGUMENTS.containsKey(task)) {
                    String[] arguments = IMPORTER_ARGUMENTS.get(task);
                    try {
                        Class<?>[] typeSpec = new Class<?>[arguments.length];
                        Arrays.fill(typeSpec, String.class);
                        Constructor<? extends BaseImport> constructor = importClass.getDeclaredConstructor(typeSpec);
                        importer = constructor.newInstance((Object[]) arguments);
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex) {
                        LOGGER.log(Level.SEVERE, "While instatiating importer for task " + task, ex);
                    }
                }
                else {
                    try {
                        importer = importClass.newInstance();
                    } catch (InstantiationException | IllegalAccessException ex) {
                        LOGGER.log(Level.SEVERE, "While instantiating importer for task " + task, ex);
                    }
                }
                if (importer != null) {
                    performer.performTask(importer);
                }
            }
        }
    }
        
    private static void performSpecialTasks(SortedSet<String> tasks) {
        ImpCommit impCommit = new ImpCommit();
        impCommit.setProjectName(projectName);
        impCommit.setProjectID(projectID);
        ImpMetricTarget impMetric = new ImpMetricTarget();
        if (tasks.contains("sprintlink")) {
            long startTime = System.currentTimeMillis();
            
            impCommit.updateSprintLink();
            
            showCompleteTask("Fixed sprint linking", startTime);
        }
        
        if (tasks.contains("developerproject")) {
            long startTime = System.currentTimeMillis();
            
            impCommit.fillProjectDevelopers();
            
            showCompleteTask("Fixed project-specific developer linking", startTime);
        }
        
        if (tasks.contains("developerlink")) {
            long startTime = System.currentTimeMillis();
            
            impCommit.updateJiraID(); // fix developer linking manually (out of json file) after all projects are checked.
            impCommit.showUnknownDevs();
            
            showCompleteTask("Fixed JIRA and VCS developer linking", startTime);
        }
        
        if (tasks.contains("metric_domain_name")) {
            long startTime = System.currentTimeMillis();
            
            impMetric.updateDomainNames();
                        
            showCompleteTask("Updated metric domain names", startTime);
        }
        
        if (tasks.contains("metric_default_target")) {
            long startTime = System.currentTimeMillis();
            
            impMetric.updateDefaultTargets();
                        
            showCompleteTask("Updated metric default targets", startTime);
        }
        
        if (tasks.contains("repo_source")) {
            long startTime = System.currentTimeMillis();
            
            try (RepositoryDb repoDb = new RepositoryDb()) {
                repoDb.update_repo_sources(projectID);
                showCompleteTask("Updated repository sources", startTime);
            }
            catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Cannot update repository sources", ex);
            }
        }
        
        if (tasks.contains("issue_changelog_id")) {
            long startTime = System.currentTimeMillis();
            ImpDataIssue impIssue = new ImpDataIssue();
            impIssue.update_changelog_id(projectID);
            showCompleteTask("Updated issue changelog identifiers", startTime);
        }
        
        if (tasks.contains("encrypt")) {        
            long startTime = System.currentTimeMillis();
            
            impCommit.hashNames();

            showCompleteTask("Encrypted personal information", startTime);
        }
    }

}
