/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

/**
 *
 * @author Enrique & Thomas Helling
 */
public class Importerjson {
        
    private static void showCompleteTask(String taskName, long startTime) {
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        
        String line = taskName + " in " + (elapsedTime / 1000) + " seconds";
        System.out.println(line);
    }
    
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        
        int projectID;
        String projectName;
        long startTime;
        
        projectName = args[0].trim();
        
        startTime = System.currentTimeMillis();
        
        ImpProject impProject = new ImpProject();
        projectID = impProject.parser(projectName);
        
        showCompleteTask("Imported project", startTime);
        
        startTime = System.currentTimeMillis();
        
        ImpDataIssue impIssue = new ImpDataIssue();
        impIssue.parser(projectID, projectName);
        
        showCompleteTask("Imported issues", startTime);
        
        startTime = System.currentTimeMillis();
                
        ImpDataType impDataType = new ImpDataType();
        impDataType.parser(projectName);
        
        showCompleteTask("Imported issue types", startTime);
        
        startTime = System.currentTimeMillis();
        
        ImpDataStatus impDataStatus = new ImpDataStatus();
        impDataStatus.parser(projectName);
        
        showCompleteTask("Imported status types", startTime);
        
        startTime = System.currentTimeMillis();
                
        ImpDataResolution impDataResolution = new ImpDataResolution();
        impDataResolution.parser(projectName);
        
        showCompleteTask("Imported resolution types", startTime);
        
        startTime = System.currentTimeMillis();
                
        ImpDataRelationshipType impDataRelationshipType = new ImpDataRelationshipType();
        impDataRelationshipType.parser(projectName);
        
        showCompleteTask("Imported relationship types", startTime);
        
        startTime = System.currentTimeMillis();
        
        ImpDataPriority impDataPriority = new ImpDataPriority();
        impDataPriority.parser(projectName);
        
        showCompleteTask("Imported priortity types", startTime);
        
        startTime = System.currentTimeMillis();
        
        ImpDataFixVersion impDataFixVersion = new ImpDataFixVersion();
        impDataFixVersion.parser(projectName);
        
        showCompleteTask("Imported fixVersions", startTime);
        
        startTime = System.currentTimeMillis();
        
        ImpReadyStatus impReadyStatus = new ImpReadyStatus();
        impReadyStatus.parser(projectName);
        
        showCompleteTask("Imported ready status names", startTime);
        
        startTime = System.currentTimeMillis();
        
        ImpDataIssueLink impDataIssueLink = new ImpDataIssueLink();
        impDataIssueLink.parser(projectName);
        
        showCompleteTask("Imported issue links", startTime);

        startTime = System.currentTimeMillis();
        
        ImpMetricValue impmetricvalue = new ImpMetricValue();
        impmetricvalue.parser(projectID, projectName);
        
        showCompleteTask("Imported metric values", startTime);
        
        startTime = System.currentTimeMillis();
        
        ImpSprint impsprint = new ImpSprint();
        impsprint.parser(projectID, projectName);
        
        showCompleteTask("Imported sprints", startTime);
        
        startTime = System.currentTimeMillis();
         
        ImpComment impComment = new ImpComment();
        impComment.parser(projectName);

        showCompleteTask("Imported comments", startTime);
        
        startTime = System.currentTimeMillis();
        
        ImpDeveloper impDeveloper = new ImpDeveloper();
        impDeveloper.parser(projectName);
        
        showCompleteTask("Imported developers", startTime);
        
        startTime = System.currentTimeMillis();
 
        ImpCommit impCommit = new ImpCommit();
        impCommit.parser(projectID, projectName);
        
        showCompleteTask("Imported commits", startTime);
        
        //startTime = System.currentTimeMillis();
        
        //impCommit.updateJiraID(); // fix developer linking manually (out of json file) after all projects are checked.
        //impCommit.printUnknownDevs();
        
        // Encryption
        //impCommit.hashNames();
        
        //showCompleteTask("Sanitized data", startTime);
    }

}
