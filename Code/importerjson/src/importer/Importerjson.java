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
    
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {

        int projectID = 0;
        String projectName="";
        
        projectName = args[0].trim();
       /*
        ImpDataIssue data = new ImpDataIssue();
        data.parser(projectName);
        */
        ImpProject impProject = new ImpProject();
        projectID = impProject.parser(projectName);
        /*
        ImpDataType impDataType = new ImpDataType();
        impDataType.parser(projectName);
        
        ImpDataStatus impDataStatus = new ImpDataStatus();
        impDataStatus.parser(projectName);
        
        ImpDataResolution impDataResolution = new ImpDataResolution();
        impDataResolution.parser(projectName);
         
        ImpDataRelationshipType impDataRelationshipType = new ImpDataRelationshipType();
        impDataRelationshipType.parser(projectName);
        
        ImpDataPriority impDataPriority = new ImpDataPriority();
        impDataPriority.parser(projectName);
        
        ImpDataFixVersion impDataFixVersion = new ImpDataFixVersion();
        impDataFixVersion.parser(projectName);
         
        ImpDataIssueLink impDataIssueLink = new ImpDataIssueLink();
        impDataIssueLink.parser(projectName);
        
        ImpMetricValue impmetricvalue = new ImpMetricValue();
        impmetricvalue.parser(projectName);
        
        ImpSprint impsprint = new ImpSprint();
        impsprint.parser(projectID,projectName); 
        
        ImpDeveloper impDeveloper = new ImpDeveloper();
        impDeveloper.parser(projectName);
        
        */
        ImpCommit impCommit = new ImpCommit();
        //impCommit.parser(projectID,projectName);
        impCommit.updateJiraID(projectID, projectName); // fix developer linking manually (out of json file).        
        
        /*
        ImpComment impComment = new ImpComment();
        impComment.parser(projectName);
        */
    }

}
