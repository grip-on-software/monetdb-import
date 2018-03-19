/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import dao.ReservationDb;
import dao.SprintDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for reservations.
 * @author Leon Helwerda
 */
public class ImpReservation extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project_id = getProjectID();
 
        try (
            ReservationDb reservationDb = new ReservationDb();
            SprintDb sprintDb = new SprintDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String reservation_id = (String) jsonObject.get("reservation_id");
                String requester = (String) jsonObject.get("requester");
                String people = (String) jsonObject.get("number_of_people");
                String description = (String) jsonObject.get("description");
                String start_time = (String) jsonObject.get("start_date");
                String end_time = (String) jsonObject.get("end_date");
                String prepare_time = (String) jsonObject.get("prepare_date");
                String close_time = (String) jsonObject.get("close_date");
                                
                if (!reservationDb.check_reservation(reservation_id)) {
                    int number_of_people = Integer.parseInt(people);
                    Timestamp start_date = Timestamp.valueOf(start_time);
                    Timestamp end_date = Timestamp.valueOf(end_time);
                    Timestamp prepare_date = Timestamp.valueOf(prepare_time);
                    Timestamp close_date = Timestamp.valueOf(close_time);
                    int sprint_id = sprintDb.find_sprint(project_id, start_date);
                    
                    reservationDb.insert_reservation(reservation_id, project_id, requester, number_of_people, description, start_date, end_date, prepare_date, close_date, sprint_id);
                }
            }
        }
        catch (FileNotFoundException ex) {
            getLogger().log(Level.WARNING, "Cannot import {0}: {1}", new Object[]{getImportName(), ex.getMessage()});
        }
        catch (Exception ex) {
            logException(ex);
        }
            
    }

    @Override
    public String getImportName() {
        return "reservations";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_reservations.json"};
    }
    
}
