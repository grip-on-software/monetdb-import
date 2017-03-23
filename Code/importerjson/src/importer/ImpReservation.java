/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.RepositoryDb;
import dao.ReservationDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 *
 * @author Leon Helwerda
 */
public class ImpReservation extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project_id = getProjectID();
 
        try (
            ReservationDb reservationDb = new ReservationDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_reservations.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
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
                    
                    reservationDb.insert_reservation(reservation_id, project_id, requester, number_of_people, description, start_date, end_date, prepare_date, close_date);
                }
            }
        }
        catch (FileNotFoundException ex) {
            System.out.println("Cannot import reservations: " + ex.getMessage());
        }
        catch (Exception ex) {
            logException(ex);
        }
            
    }
    
}
