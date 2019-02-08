/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import dao.DeveloperDb;
import dao.RepositoryDb;
import dao.SaltDb;
import dao.TeamDb;
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
public class ImpTfsTeamMember extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project = getProjectID();
 
        try (
            FileReader fr = new FileReader(getMainImportPath());
            TeamDb teamDb = new TeamDb();
            RepositoryDb repoDb = new RepositoryDb();
            DeveloperDb devDb = new DeveloperDb()
        ) {            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String team_name = (String) jsonObject.get("team_name");
                String repo_name = (String) jsonObject.get("repo_name");
                String user = (String) jsonObject.get("user");
                String username = (String) jsonObject.get("username");
                String encrypted = (String) jsonObject.get("encrypted");
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                
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
                if (!teamDb.check_tfs_team_member(team, username)) {
                    DeveloperDb.Developer dev = new DeveloperDb.Developer(username, user, null);
                    Integer alias_id = devDb.update_vcs_developer(project, dev, encryption);
                    teamDb.insert_tfs_team_member(team, alias_id, username, user, encryption);
                    devDb.insert_project_developer(project, alias_id, dev, team.getTeamId());
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
        return "TFS team members";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_tfs_team_member.json"};
    }
    
}
