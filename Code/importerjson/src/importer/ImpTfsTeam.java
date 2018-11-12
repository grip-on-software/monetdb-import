/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import dao.RepositoryDb;
import dao.TeamDb;
import dao.TeamDb.Team;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for TFS teams.
 * @author Leon Helwerda
 */
public class ImpTfsTeam extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project = getProjectID();
 
        try (
            FileReader fr = new FileReader(getMainImportPath());
            TeamDb teamDb = new TeamDb();
            RepositoryDb repoDb = new RepositoryDb();
        ) {            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String team_name = (String) jsonObject.get("team_name");
                String repo_name = (String) jsonObject.get("repo_name");
                String description = (String) jsonObject.get("description");
                
                Integer repo_id = repoDb.check_repo(repo_name, project);
                if (repo_id == 0) {
                    repoDb.insert_repo(repo_name, project);
                    repo_id = repoDb.check_repo(repo_name, project);
                }
                
                Team currentTeam = teamDb.check_tfs_team(team_name, project);
                if (currentTeam == null) {
                    teamDb.insert_tfs_team(team_name, project, repo_id, description);
                }
                else {
                    Team newTeam = new Team(currentTeam.getTeamId(), team_name, project, repo_id, description);
                    if (!newTeam.equals(currentTeam)) {
                        teamDb.update_tfs_team(newTeam);
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
        return "TFS teams";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_tfs_team.json"};
    }
    
}
