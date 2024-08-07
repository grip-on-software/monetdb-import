/**
 * TFS sprint importer.
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

import dao.RepositoryDb;
import dao.SprintDb;
import dao.TeamDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for TFS sprints.
 * @author Enrique, Leon Helwerda
 */
public class ImpTfsSprint extends BaseImport {
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project = getProjectID();
 
        try (
            FileReader fr = new FileReader(getMainImportPath());
            SprintDb sprintDb = new SprintDb();
            RepositoryDb repoDb = new RepositoryDb();
            TeamDb teamDb = new TeamDb()
        ) {            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String name = (String) jsonObject.get("sprint_name");
                String start = (String) jsonObject.get("start_date");
                String end = (String) jsonObject.get("end_date");
                String repo_name = (String) jsonObject.get("repo_name");
                String team_name = (String) jsonObject.get("team_name");
                
                Integer repo_id = repoDb.check_repo(repo_name, project);
                if (repo_id == 0) {
                    repoDb.insert_repo(repo_name, project);
                    repo_id = repoDb.check_repo(repo_name, project);
                }
                TeamDb.Team team = teamDb.check_tfs_team(team_name, project);
                if (team == null) {
                    teamDb.insert_tfs_team(team_name, project, repo_id, null);
                    team = teamDb.check_tfs_team(team_name, project);
                }
                
                Integer id = sprintDb.check_tfs_sprint(project, name, repo_id, team.getTeamId());
                if (id == null) {
                    Timestamp start_date = null;
                    if (start != null && !start.equals("0")) {
                        start_date = Timestamp.valueOf(start);
                    }

                    Timestamp end_date = null;
                    if (end != null && !end.equals("0")) {
                        end_date = Timestamp.valueOf(end);
                    }

                    sprintDb.insert_tfs_sprint(project, name, start_date, end_date, repo_id, team.getTeamId());
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
        return "TFS sprints";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_tfs_sprint.json"};
    }

}
