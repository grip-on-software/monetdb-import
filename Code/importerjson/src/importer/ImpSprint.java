/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import dao.SprintDb;
import util.BaseImport;
import java.io.FileReader;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Importer for JIRA sprints.
 * @author Enrique
 */
public class ImpSprint extends BaseImport {
    
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project = getProjectID();
 
        try (
            FileReader fr = new FileReader(getMainImportPath());
            SprintDb sprintDb = new SprintDb()
        ) {            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                String start = (String) jsonObject.get("start_date");
                String end = (String) jsonObject.get("end_date");
                String complete = (String) jsonObject.get("complete_date");
                String goal = (String) jsonObject.get("goal");
                
                int sprint_id = Integer.valueOf(id);
                Timestamp start_date;
                if ((start.trim()).equals("0") || (start.trim()).equals("None")){
                    start_date = null;
                }
                else {
                    start_date = Timestamp.valueOf(start);
                }
                
                Timestamp end_date;
                if ((end.trim()).equals("0") || (end.trim()).equals("None")){
                    end_date = null;
                }
                else {
                    end_date = Timestamp.valueOf(end);
                }
                                
                Timestamp complete_date;
                if (complete == null || (complete.trim()).equals("0") || (end.trim()).equals("None")){
                    complete_date = null;
                }
                else {
                    complete_date = Timestamp.valueOf(complete);
                }
                                
                SprintDb.CheckResult result = sprintDb.check_sprint(sprint_id, project, name, start_date, end_date, complete_date, goal);
                if (result == SprintDb.CheckResult.MISSING) {
                    sprintDb.insert_sprint(sprint_id, project, name, start_date, end_date, complete_date, goal);
                }
                else if (result == SprintDb.CheckResult.DIFFERS) {
                    sprintDb.update_sprint(sprint_id, project, name, start_date, end_date, complete_date, goal);
                }
            }
        }
        catch (Exception ex) {
            logException(ex);
        }
        
    }

    @Override
    public String getImportName() {
        return "JIRA sprints";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_sprint.json"};
    }
        

}
    
