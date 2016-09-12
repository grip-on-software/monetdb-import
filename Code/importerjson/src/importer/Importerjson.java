/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;
/**
 *
 * @author Enrique
 */
public class Importerjson {
    
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {

        int projectID = 0;
        
        ImpDataIssue data = new ImpDataIssue();
        data.parser();
        
        ImpProject impProject = new ImpProject();
        projectID = impProject.parser();
   
        ImpDataType impDataType = new ImpDataType();
        impDataType.parser();
        
        ImpDataStatus impDataStatus = new ImpDataStatus();
        impDataStatus.parser();
        
        ImpDataResolution impDataResolution = new ImpDataResolution();
        impDataResolution.parser();
         
        ImpDataRelationshipType impDataRelationshipType = new ImpDataRelationshipType();
        impDataRelationshipType.parser();
        
        ImpDataPriority impDataPriority = new ImpDataPriority();
        impDataPriority.parser();
        
        ImpDataFixVersion impDataFixVersion = new ImpDataFixVersion();
        impDataFixVersion.parser();
         
        ImpDataIssueLink impDataIssueLink = new ImpDataIssueLink();
        impDataIssueLink.parser();
      
        //ImpMetricValue impmetricvalue = new ImpMetricValue();
        //impmetricvalue.parser();
        
        ImpSprint impsprint = new ImpSprint();
        impsprint.parser(projectID);
    
    }

}
