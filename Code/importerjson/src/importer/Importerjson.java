/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import java.io.File;

/**
 *
 * @author Enrique & Thomas Helling
 */
public class Importerjson {
    
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        
        int projectID = 0;
        String projectName="";
        
        projectName = args[0].trim();
        
        ImpProject impProject = new ImpProject();
        projectID = impProject.parser(projectName);
        
        System.out.println("Imported project");
        
        ImpDataIssue data = new ImpDataIssue();
        data.parser(projectName);
        
        System.out.println("Imported Issues");
        
        ImpDataType impDataType = new ImpDataType();
        impDataType.parser(projectName);
        
        System.out.println("Imported issue types");

        ImpDataStatus impDataStatus = new ImpDataStatus();
        impDataStatus.parser(projectName);
        
        System.out.println("Imported status types");
        
        ImpDataResolution impDataResolution = new ImpDataResolution();
        impDataResolution.parser(projectName);
        
        System.out.println("Imported resolution types");
        
        ImpDataRelationshipType impDataRelationshipType = new ImpDataRelationshipType();
        impDataRelationshipType.parser(projectName);
        
        System.out.println("Imported relationship types");
        
        ImpDataPriority impDataPriority = new ImpDataPriority();
        impDataPriority.parser(projectName);
        
        System.out.println("Imported priority types");
        
        ImpDataFixVersion impDataFixVersion = new ImpDataFixVersion();
        impDataFixVersion.parser(projectName);
        
        System.out.println("Imported fixVersions");
        
        ImpDataIssueLink impDataIssueLink = new ImpDataIssueLink();
        impDataIssueLink.parser(projectName);
        
        System.out.println("Imported Issue links");
        
        ImpMetricValue impmetricvalue = new ImpMetricValue();
        impmetricvalue.parser(projectID, projectName);
        
        System.out.println("Imported metric values");
        
        ImpSprint impsprint = new ImpSprint();
        impsprint.parser(projectID, projectName);
        
        System.out.println("Imported Sprints");
         
        ImpComment impComment = new ImpComment();
        impComment.parser(projectName);

        System.out.println("Imported comments");
        
        ImpDeveloper impDeveloper = new ImpDeveloper();
        impDeveloper.parser(projectName);
        
        System.out.println("Imported developers");
 
        ImpCommit impCommit = new ImpCommit();
        impCommit.parser(projectID, projectName);
        
        System.out.println("Imported commits");
        
        //impCommit.updateJiraID(); // fix developer linking manually (out of json file) after all projects are checked.
        //impCommit.printUnknownDevs();
        
        // Encryption
        //impCommit.hashNames();
    }

}
