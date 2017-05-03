/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.BaseImport;

/**
 *
 * @author Enrique
 * @author Thomas Helling
 * @author Leon Helwerda
 */
public class Importerjson {
    private static String projectName = "";
    private static int projectID = 0;
    private final static List<String> DEFAULT_TASKS = Arrays.asList(new String[]{
        // JIRA
        "issue", "issuetype", "status", "resolution", "relationshiptype",
        "priority", "fixVersion", "ready_status", "issuelink", "test_execution",
        "sprint", "comment", "developer",
        // Quality dashboard metrics
        "metric_value", "metric_version", "metric_target",
        // Version control systems (Git, GitLab, SVN)
        "commit", "change_path", "tag",
        "gitlab_repo", "merge_request", "merge_request_note", "commit_comment",
        // Self-Service Desk
        "reservation",
        // Tracking
        "update",
        // Additional tasks
        "developerlink" //, "encrypt"
    });
    private final static List<String> SPECIAL_TASKS = Arrays.asList(new String[]{
        "developerproject", "developerlink", "encrypt"
    });
        
    private final static HashMap<String, Class<? extends BaseImport>> TASK_IMPORTERS = retrieveImporters();

    private static HashMap<String, Class<? extends BaseImport>> retrieveImporters() {
        HashMap<String, Class<? extends BaseImport>> importers = new HashMap<>();
        importers.put("issue", ImpDataIssue.class);
        importers.put("fixVersion", ImpDataFixVersion.class);
        importers.put("issuelink", ImpDataIssueLink.class);

        importers.put("issuetype", ImportTable.class);
        importers.put("status", ImportTable.class);
        importers.put("resolution", ImportTable.class);
        importers.put("relationshiptype", ImportTable.class);
        importers.put("priority", ImportTable.class);
        importers.put("ready_status", ImportTable.class);
        importers.put("test_execution", ImportTable.class);
        
        importers.put("sprint", ImpSprint.class);
        importers.put("comment", ImpComment.class);
        importers.put("developer", ImpDeveloper.class);
        
        importers.put("metric_value", ImpMetricValue.class);
        importers.put("metric_version", ImpMetricVersion.class);
        importers.put("metric_target", ImpMetricTarget.class);
        
        importers.put("commit", ImpCommit.class);
        importers.put("change_path", ImpChangePath.class);
        importers.put("tag", ImpTag.class);
        importers.put("gitlab_repo", ImpGitLabRepo.class);
        importers.put("merge_request", ImpMergeRequest.class);
        importers.put("merge_request_note", ImpMergeRequestNote.class);
        importers.put("commit_comment", ImpCommitComment.class);
        
        importers.put("reservation", ImpReservation.class);
        importers.put("update", ImpUpdateTracker.class);
        
        return importers;
    }
    
    private final static HashMap<String, String[]> IMPORTER_ARGUMENTS = retrieveImporterArguments();
    
    private static HashMap<String, String[]> retrieveImporterArguments() {
        HashMap<String, String[]> importers = new HashMap<>();
        importers.put("issuetype", new String[]{"issuetype", "name", "description", "issue types"});
        importers.put("status", new String[]{"status", "name", "description", "status types"});
        importers.put("resolution", new String[]{"resolution", "name", "description", "resolution types"});
        importers.put("relationshiptype", new String[]{"relationshiptype", "name", "relationship types"});
        importers.put("priority", new String[]{"priority", "name", "priority types"});
        importers.put("ready_status", new String[]{"ready_status", "name", "ready status names"});
        importers.put("test_execution", new String[]{"test_execution", "value", "test execution methods"});
        return importers;
    }

    private static void performTask(BaseImport importer, String importType) {
        long startTime;
        
        startTime = System.currentTimeMillis();
        
        importer.setProjectName(projectName);
        importer.setProjectID(projectID);
        importer.parser();
        
        showCompleteTask("Imported " + importType, startTime);
    }
        
    private static void showCompleteTask(String taskName, long startTime) {
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        
        Logger.getLogger("importer").log(Level.INFO, "{0} in {1} seconds", new Object[]{taskName, elapsedTime / 1000});
    }
    
    private static SortedSet<String> retrieveTasks(String[] taskList) {
        SortedSet<String> tasks = new TreeSet<>();
        for (String task : taskList) {
            if (task.equals("all")) {
                tasks.addAll(DEFAULT_TASKS);
            }
            else if (task.startsWith("-")) {
                // Remove task from the list (overriding earlier additions).
                // Also, if this is the first option in the list of tasks,
                // then add all default tasks and remove it from there.
                if (tasks.isEmpty()) {
                    tasks.addAll(DEFAULT_TASKS);
                }
                tasks.remove(task.substring(1));
            }
            else {
                if (!DEFAULT_TASKS.contains(task)) {
                    Logger.getLogger("importerjson").log(Level.WARNING, "Task {0} not in the default tasks", task);
                }
                tasks.add(task);
            }
        }
        return tasks;
    }
    
    public static void main(String[] args) {
        String usage = "\nUsage: java [-Dimporter.log=LEVEL] -jar importerjson.jar <project> [tasks]";
        String logLevel = System.getProperty("importer.log", "WARNING");
        System.setProperty("java.util.logging.SimpleFormatter.format",
                           "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS:%4$s:%2$s:%5$s%6$s%n");
        try {
            Logger.getLogger("importer").setLevel(Level.parse(logLevel));
        }
        catch (IllegalArgumentException ex) {
            throw new RuntimeException("Illegal importer.log argument: " + ex.getMessage() + usage);
        }
        if (args.length <= 0 || "--help".equals(args[0])) {
            throw new RuntimeException(usage);
        }
        
        SortedSet<String> tasks;
        if (args.length > 1) {
            tasks = retrieveTasks(args[1].trim().split(","));
        }
        else {
            tasks = new TreeSet<>(DEFAULT_TASKS);
        }
        
        if ("--".equals(args[0])) {
            // Only allow special tasks that may run project-independently
            if (!SPECIAL_TASKS.containsAll(tasks)) {
                throw new RuntimeException("Project must given for the provided tasks" + usage);
            }
        }
        else {
            projectName = args[0].trim();
        }
        
        Logger.getLogger("importer").log(Level.INFO, "Tasks to run: {0}", Arrays.toString(tasks.toArray()));
        
        // Perform project import so that project ID is known to exist
        if (!projectName.isEmpty()) {
            ImpProject impProject = new ImpProject();
            performTask(impProject, "project");
            projectID = impProject.getProjectID();
        }
    
        for (String task : DEFAULT_TASKS) {
            if (tasks.contains(task)) {
                if (TASK_IMPORTERS.containsKey(task)) {
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
                            Logger.getLogger("importer").log(Level.SEVERE, "While instatiating importer for task " + task, ex);
                        }
                    }
                    else {
                        try {
                            importer = importClass.newInstance();
                        } catch (InstantiationException | IllegalAccessException ex) {
                            Logger.getLogger("importer").log(Level.SEVERE, "While instatiating importer for task " + task, ex);
                        }
                    }
                    if (importer != null) {
                        performTask(importer, importer.getImportName());
                    }
                }
            }
        }
        
        ImpCommit impCommit = new ImpCommit();
        impCommit.setProjectName(projectName);
        impCommit.setProjectID(projectID);
        if (tasks.contains("developerproject")) {
            long startTime = System.currentTimeMillis();
            
            impCommit.fillProjectDevelopers();
            
            showCompleteTask("Fixed project-specific developer linking", startTime);
        }
        
        if (tasks.contains("developerlink")) {
            long startTime = System.currentTimeMillis();
            
            impCommit.updateJiraID(); // fix developer linking manually (out of json file) after all projects are checked.
            impCommit.printUnknownDevs();
            
            showCompleteTask("Fixed JIRA and VCS developer linking", startTime);
        }
        
        if (tasks.contains("encrypt")) {        
            long startTime = System.currentTimeMillis();
            
            impCommit.hashNames();

            showCompleteTask("Encrypted personal information", startTime);
        }
    }

}
