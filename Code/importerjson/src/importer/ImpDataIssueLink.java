/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import java.io.FileReader;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import dao.IssueLinkDb;
import util.BaseImport;
import util.BaseLinkDb.CheckResult;

/**
 * Importer for JIRA issue links.
 * @author Enrique
 */
public class ImpDataIssueLink extends BaseImport {
    
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
 
        try (
            FileReader fr = new FileReader(getMainImportPath());
            IssueLinkDb linkDb = new IssueLinkDb()
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String from_key = (String) jsonObject.get("from_key");
                String to_key = (String) jsonObject.get("to_key");
                String relationshiptype = (String) jsonObject.get("relationshiptype");
                String outward = (String) jsonObject.get("outward");
                String start_date = (String) jsonObject.get("start_date");
                String end_date = (String) jsonObject.get("end_date");
                
                int relationship_type = Integer.parseInt(relationshiptype);
                boolean is_outward = outward.equals("1");
                Timestamp start;
                if (start_date.equals("0")) {
                    start = null;
                }
                else {
                    start = Timestamp.valueOf(start_date);
                }
                Timestamp end;
                if (end_date.equals("0")) {
                    end = null;
                }
                else {
                    end = Timestamp.valueOf(end_date);
                }
                
                CheckResult result = linkDb.check_link(from_key, to_key, relationship_type, is_outward, start, end);
                if (result.state == CheckResult.State.MISSING) {
                    linkDb.insert_link(from_key, to_key, relationship_type, is_outward, start, end);
                }
                else if (result.state == CheckResult.State.DIFFERS) {
                    // Ensure we take the earliest start date and latest end date.
                    if (result.dates.start_date != null &&
                            (start == null || start.after(result.dates.start_date))) {
                        start = result.dates.start_date;
                    }
                    if (result.dates.end_date != null && end != null && end.before(result.dates.end_date)) {
                        end = result.dates.end_date;
                    }
                    linkDb.update_link(from_key, to_key, relationship_type, is_outward, start, end);
                }
            }
        }            
        catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "JIRA issue links";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_issuelinks.json"};
    }
}
    
