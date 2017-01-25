/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
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
        "priority", "fixVersion", "ready_status", "issuelink",
        "metric_value", "metric_version", "metric_target",
        "sprint", "comment", "developer", "commit",
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
        
        String line = taskName + " in " + (elapsedTime / 1000) + " seconds";
        System.out.println(line);
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
        if (args.length <= 0) {
            throw new RuntimeException("Usage: java -jar importerjson <project> [tasks]");
        }
        projectName = args[0].trim();
        
        SortedSet<String> tasks;
        if (args.length > 1) {
            tasks = retrieveTasks(args[1].trim().split(","));
        }
        else {
            tasks = defaultTasks;
        }
        
        System.out.println("Tasks to run: " + Arrays.toString(tasks.toArray()));
        
        // Always perform project import so that project ID is known to exist
        ImpProject impProject = new ImpProject();
        performTask(impProject, "project");
        projectID = impProject.getProjectID();
               
        if (tasks.contains("issue")) {
            ImpDataIssue impIssue = new ImpDataIssue();
            performTask(impIssue, "issues");
        }
                
        if (tasks.contains("issuetype")) {
            ImpDataType impDataType = new ImpDataType();
            performTask(impDataType, "issue types");
        }
                
        if (tasks.contains("status")) {
            ImpDataStatus impDataStatus = new ImpDataStatus();
            performTask(impDataStatus, "status types");
        }
                
        if (tasks.contains("resolution")) {
            ImpDataResolution impDataResolution = new ImpDataResolution();
            performTask(impDataResolution, "resolution types");
        }
        
        if (tasks.contains("relationshiptype")) {
            ImpDataRelationshipType impDataRelationshipType = new ImpDataRelationshipType();
            performTask(impDataRelationshipType, "relationship types");
        }
        
        if (tasks.contains("priority")) {
            ImpDataPriority impDataPriority = new ImpDataPriority();
            performTask(impDataPriority, "priority types");
        }
        
        if (tasks.contains("fixVersion")) {
            ImpDataFixVersion impDataFixVersion = new ImpDataFixVersion();
            performTask(impDataFixVersion, "fixVersions");
        }
        
        if (tasks.contains("ready_status")) {
            ImpReadyStatus impReadyStatus = new ImpReadyStatus();
            performTask(impReadyStatus, "ready status names");
        }
        
        if (tasks.contains("issuelink")) {
            ImpDataIssueLink impDataIssueLink = new ImpDataIssueLink();
            performTask(impDataIssueLink, "issue links");
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
