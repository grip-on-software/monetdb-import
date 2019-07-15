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
                
                Object id = jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                String start = (String) jsonObject.get("start_date");
                String end = (String) jsonObject.get("end_date");
                String complete = (String) jsonObject.get("complete_date");
                String goal = (String) jsonObject.get("goal");
                Object board = jsonObject.get("board_id");
                Integer board_id = null;
                if (board instanceof String) {
                    board_id = Integer.valueOf((String)board);
                }
                else if (board != null) {
                    board_id = ((Long) board).intValue();
                }
                
                int sprint_id = (id instanceof String ? Integer.valueOf((String)id) : (int)id);
                Timestamp start_date = null;
                if (start != null && !start.trim().equals("0") && !start.trim().equals("None")){
                    start_date = Timestamp.valueOf(start);
                }
                
                Timestamp end_date = null;
                if (end != null && !end.trim().equals("0") && !end.trim().equals("None")){
                    end_date = Timestamp.valueOf(end);
                }
                                
                Timestamp complete_date = null;
                if (complete != null && !complete.trim().equals("0") && !complete.trim().equals("None")){
                    complete_date = Timestamp.valueOf(complete);
                }
                                
                SprintDb.CheckResult result = sprintDb.check_sprint(sprint_id, project, name, start_date, end_date, complete_date, goal, board_id);
                if (result == SprintDb.CheckResult.MISSING) {
                    sprintDb.insert_sprint(sprint_id, project, name, start_date, end_date, complete_date, goal, board_id);
                }
                else if (result == SprintDb.CheckResult.DIFFERS) {
                    sprintDb.update_sprint(sprint_id, project, name, start_date, end_date, complete_date, goal, board_id);
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
    
