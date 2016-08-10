/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importerjson;
/**
 *
 * @author Enrique
 */
public class Importerjson {
    
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        
        ImpData data = new ImpData();
        data.parser();
        
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
        
        ImpMetricValue impmetricvalue = new ImpMetricValue();
        impmetricvalue.parser();
    
    }

}
