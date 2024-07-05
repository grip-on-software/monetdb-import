/**
 * Seat importer.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2024 Leon Helwerda
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package importer;

import dao.SeatDb;
import dao.SprintDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for seat counts.
 * @author Leon Helwerda
 */
public class ImpSeats extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int projectId = this.getProjectID();
 
        try (
            SeatDb seatDb = new SeatDb();
            SprintDb sprintDb = new SprintDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String month = (String) jsonObject.get("month");
                Object value = jsonObject.get("seats");
                float seats;
                if (value instanceof Long) {
                    seats = ((Long) value).floatValue();
                }
                else if (value instanceof Double) {
                    seats = ((Double) value).floatValue();
                }
                else if (value instanceof String) {
                    seats = Double.valueOf((String) value).floatValue();
                }
                else {
                    throw new ImporterException("Cannot parse seats value for month " + month + ", unexpected type of value: " + value.getClass().getSimpleName());
                }
                
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
                Date date = format.parse(month);
                
                Calendar start = new GregorianCalendar();
                start.setTime(date);
                start.set(Calendar.DAY_OF_MONTH, 1);
                Calendar end = new GregorianCalendar();
                end.setTime(date);
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
                end.set(Calendar.HOUR_OF_DAY, end.getMaximum(Calendar.HOUR_OF_DAY));
                end.set(Calendar.MINUTE, end.getMaximum(Calendar.MINUTE));
                end.set(Calendar.SECOND, end.getMaximum(Calendar.SECOND));
                
                Timestamp start_date = new Timestamp(start.getTimeInMillis());
                Timestamp end_date = new Timestamp(end.getTimeInMillis());
                
                Set<Integer> sprints = sprintDb.find_sprints(projectId, start_date, end_date);
                for (Integer sprint : sprints) {
                    Float currentSeats = seatDb.check(projectId, sprint, start_date);
                    if (currentSeats == null) {
                        seatDb.insert(projectId, sprint, start_date, seats);
                    }
                    else if (currentSeats != seats) {
                        seatDb.update(projectId, sprint, start_date, seats);
                    }
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
        return "seat counts";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_seats.json"};
    }
    
}
