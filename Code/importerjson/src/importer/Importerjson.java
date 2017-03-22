/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import java.util.Arrays;
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
    private final static SortedSet<String> defaultTasks = retrieveTasks(new String[]{
        "issue", "issuetype", "status", "resolution", "relationshiptype",
        "priority", "fixVersion", "ready_status", "issuelink", "test_execution",
        "metric_value", "metric_version", "metric_target",
        "sprint", "comment", "developer", "commit", "gitlab_repo",
        // Additional tasks
        "developerlink" //, "encrypt"
    });
    
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
                tasks.addAll(defaultTasks);
            }
            else if (task.startsWith("-")) {
                // Remove task from the list (overriding earlier additions).
                // Also, if this is the first option in the list of tasks,
                // then add all default tasks and remove it from there.
                if (tasks.isEmpty()) {
                    tasks.addAll(defaultTasks);
                }
                tasks.remove(task.substring(1));
            }
            else {
                tasks.add(task);
            }
        }
        return tasks;
    }
    
    public static void main(String[] args) {
        String usage = "\nUsage: java [-Dimporter.log=LEVEL] -jar importerjson <project> [tasks]";
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
        projectName = args[0].trim();
        
        SortedSet<String> tasks;
        if (args.length > 1) {
            tasks = retrieveTasks(args[1].trim().split(","));
        }
        else {
            tasks = defaultTasks;
        }
        
        Logger.getLogger("importer").log(Level.INFO, "Tasks to run: {0}", Arrays.toString(tasks.toArray()));
        
        // Always perform project import so that project ID is known to exist
        ImpProject impProject = new ImpProject();
        performTask(impProject, "project");
        projectID = impProject.getProjectID();
               
        if (tasks.contains("issue")) {
            ImpDataIssue impIssue = new ImpDataIssue();
            performTask(impIssue, "issues");
        }
                
        if (tasks.contains("issuetype")) {
            ImportTable impType = new ImportTable("issuetype", "name", "description");
            performTask(impType, "issue types");
        }
                
        if (tasks.contains("status")) {
            ImportTable impStatus = new ImportTable("status", "name", "description");
            performTask(impStatus, "status types");
        }
                
        if (tasks.contains("resolution")) {
            ImportTable impResolution = new ImportTable("resolution", "name", "description");
            performTask(impResolution, "resolution types");
        }
        
        if (tasks.contains("relationshiptype")) {
            ImportTable impRelationshipType = new ImportTable("relationshiptype", "name");
            performTask(impRelationshipType, "relationship types");
        }
        
        if (tasks.contains("priority")) {
            ImportTable impPriority = new ImportTable("priority", "name");
            performTask(impPriority, "priority types");
        }
        
        if (tasks.contains("fixVersion")) {
            ImpDataFixVersion impDataFixVersion = new ImpDataFixVersion();
            performTask(impDataFixVersion, "fixVersions");
        }
        
        if (tasks.contains("ready_status")) {
            ImportTable impReadyStatus = new ImportTable("ready_status", "name");
            performTask(impReadyStatus, "ready status names");
        }
        
        if (tasks.contains("issuelink")) {
            ImpDataIssueLink impDataIssueLink = new ImpDataIssueLink();
            performTask(impDataIssueLink, "issue links");
        }
        
        if (tasks.contains("test_execution")) {
            ImportTable impTestExecution = new ImportTable("test_execution", "value");
            performTask(impTestExecution, "test execution methods");
        }
        
        if (tasks.contains("metric_value")) {
            ImpMetricValue impMetricValue = new ImpMetricValue();
            performTask(impMetricValue, "metric values");
        }
        
        if (tasks.contains("metric_version")) {
            ImpMetricVersion impMetricVersion = new ImpMetricVersion();
            performTask(impMetricVersion, "metric versions");
        }
        
        if (tasks.contains("metric_target")) {
            ImpMetricTarget impMetricTarget = new ImpMetricTarget();
            performTask(impMetricTarget, "metric targets");
        }
        
        if (tasks.contains("sprint")) {
            ImpSprint impSprint = new ImpSprint();
            performTask(impSprint, "sprints");
        }
        
        if (tasks.contains("comment")) {
            ImpComment impComment = new ImpComment();
            performTask(impComment, "comments");
        }
        
        if (tasks.contains("developer")) {
            ImpDeveloper impDeveloper = new ImpDeveloper();
            performTask(impDeveloper, "developers");
        }
        
        ImpCommit impCommit = new ImpCommit();
        impCommit.setProjectName(projectName);
        impCommit.setProjectID(projectID);
        if (tasks.contains("commit")) {
            performTask(impCommit, "commits");
        }
        
        if (tasks.contains("gitlab_repo")) {
            ImpGitLabRepo impGitLabRepo = new ImpGitLabRepo();
            performTask(impGitLabRepo, "GitLab repositories");
        }
        
        if (tasks.contains("developerlink")) {
            long startTime = System.currentTimeMillis();
            
            impCommit.updateJiraID(); // fix developer linking manually (out of json file) after all projects are checked.
            impCommit.printUnknownDevs();
            
            showCompleteTask("Fixed JIRA and Git developer linking", startTime);
        }
        
        if (tasks.contains("encrypt")) {        
            long startTime = System.currentTimeMillis();
            
            impCommit.hashNames();

            showCompleteTask("Encrypted personal information", startTime);
        }
    }

}
