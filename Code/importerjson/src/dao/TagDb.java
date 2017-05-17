/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Objects;
import util.BaseDb;

/**
 * Database access management for the version control system tags table.
 * @author Leon Helwerda
 */
public class TagDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private BatchedStatement updateStmt = null;
    private PreparedStatement cacheStmt = null;
    private HashMap<Integer, HashMap<String, TagInfo>> tagRepoCache = null;
    
    /**
     * Object that holds properties of a certain tag.
     */
    private static class TagInfo {
        public String version_id;
        public String message;
        public Timestamp tagged_date;
        public Integer tagger_id;
        
        public TagInfo() {
            this.version_id = null;
            this.message = null;
            this.tagged_date = null;
            this.tagger_id = null;
        }
        
        public TagInfo(String version_id, String message, Timestamp tagged_date, Integer tagger_id) {
            this.version_id = version_id;
            this.message = message;
            this.tagged_date = tagged_date;
            this.tagger_id = tagger_id;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof TagInfo) {
                TagInfo otherTag = (TagInfo) other;
                return ((version_id == null ? otherTag.version_id == null : version_id.equals(otherTag.version_id)) &&
                        (message == null ? otherTag.message == null : message.equals(otherTag.message)) &&
                        (tagged_date == null ? otherTag.tagged_date == null : tagged_date.equals(otherTag.tagged_date)) &&
                        (tagger_id == null ? otherTag.tagger_id == null : tagger_id.equals(otherTag.tagger_id)));
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.version_id);
            hash = 89 * hash + Objects.hashCode(this.message);
            hash = 89 * hash + Objects.hashCode(this.tagged_date);
            hash = 89 * hash + Objects.hashCode(this.tagger_id);
            return hash;
        }
    }
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };
    
    public TagDb() {
        String sql = "insert into gros.tag(repo_id,tag_name,version_id,message,tagged_date,tagger_id,sprint_id) values (?,?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.tag set version_id=?, message=?, tagged_date=?, tagger_id=?, sprint_id=? where repo_id=? and tag_name=?;";
        updateStmt = new BatchedStatement(sql);
        
        tagRepoCache = new HashMap<>();
    }
        
    private void fillTagCache(int repo_id) throws SQLException, PropertyVetoException {
        if (tagRepoCache.containsKey(repo_id)) {
            return;
        }
        
        if (cacheStmt == null) {
            Connection con = insertStmt.getConnection();
            String sql = "SELECT repo_id, tag_name, version_id, message, tagged_date, tagger_id FROM gros.tag WHERE repo_id = ?";
            cacheStmt = con.prepareStatement(sql);
        }
        
        HashMap<String, TagInfo> cache = new HashMap<>();
        
        cacheStmt.setInt(1, repo_id);
        try (ResultSet rs = cacheStmt.executeQuery()) {
            while (rs.next()) {
                String tag_name = rs.getString("tag_name");
                TagInfo tagInfo = new TagInfo();
                tagInfo.version_id = rs.getString("version_id");
                tagInfo.message = rs.getString("message");
                tagInfo.tagged_date = rs.getTimestamp("tagged_date");
                if (rs.getObject("tagger_id") == null) {
                    tagInfo.tagger_id = null;
                }
                else {
                    tagInfo.tagger_id = rs.getInt("tagger_id");
                }
                cache.put(tag_name, tagInfo);
            }
        }
        tagRepoCache.put(repo_id, cache);
    }
    
    private void updateTagCache(int repo_id, String tag_name, String version_id, String message, Timestamp tagged_date, Integer tagger_id) {
        HashMap<String, TagInfo> cache = tagRepoCache.get(repo_id);
        TagInfo tagInfo = new TagInfo(version_id, message, tagged_date, tagger_id);
        cache.put(tag_name, tagInfo);
    }
    
    /**
     * Check whether a tag exists in the database and that it has the same
     * properties as the provided parameters.
     * @param repo_id Identifier of the repository in which the tag is added
     * @param tag_name The name of the tag
     * @param version_id The version in which the tag is added (Subversion) or
     * which the tag references (Git)
     * @param message Message that is added to the tag when it is created, separate
     * from the commit message. Only available for Git repositories. If this is null,
     * then the tag has no separate message.
     * @param tagged_date Timestamp on which the tag is created (Git) or most
     * recently updated (Subversion). May be null for incomplete tags.
     * @param tagger_id Idenitifier of the VCS developer that created the tag.
     * If this is null, then the developer could not be deduced from tag information
     * @return An indicator of the state of the database regarding the given tag.
     * This is CheckResult.MISSING if the tag with the provided repository and name
     * does not exist. This is CheckResult.DIFFERS if there is a row with the provided
     * repsoitory identifier and tag name in the database, but it has different values
     * in its other fields. This is CheckResult.EXISTS if there is an merge request
     * in the database that matches all the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_tag(int repo_id, String tag_name, String version_id, String message, Timestamp tagged_date, Integer tagger_id) throws SQLException, PropertyVetoException {
        fillTagCache(repo_id);
        HashMap<String, TagInfo> cache = tagRepoCache.get(repo_id);
        TagInfo currentTagInfo = cache.get(tag_name);
        if (currentTagInfo != null) {
            TagInfo tagInfo = new TagInfo(version_id, message, tagged_date, tagger_id);
            // Check if the fields differ
            if (currentTagInfo.equals(tagInfo)) {
                return CheckResult.EXISTS;
            }
            else {
                return CheckResult.DIFFERS;
            }
        }
        else {
            // Cache is always complete (unless we have competing jobs for this repo), so a cache miss indicates a missing tag.
            return CheckResult.MISSING;
        }
    }
    
    /**
     * Insert a new tag in the database.
     * @param repo_id Identifier of the repository in which the tag is added
     * @param tag_name The name of the tag
     * @param version_id The version in which the tag is added (Subversion) or
     * which the tag references (Git)
     * @param message Message that is added to the tag when it is created, separate
     * from the commit message. Only available for Git repositories. If this is null,
     * then the tag has no separate message.
     * @param tagged_date Timestamp on which the tag is created (Git) or most
     * recently updated (Subversion). May be null for incomplete tags.
     * @param tagger_id Idenitifier of the VCS developer that created the tag.
     * If this is null, then the developer could not be deduced from tag information
     * @param sprint_id Idenitifer of the sprint in which the tag is created
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_tag(int repo_id, String tag_name, String version_id, String message, Timestamp tagged_date, Integer tagger_id, int sprint_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setInt(1, repo_id);
        pstmt.setString(2, tag_name);
        pstmt.setString(3, version_id);
        setString(pstmt, 4, message);
        setTimestamp(pstmt, 5, tagged_date);
        setInteger(pstmt, 6, tagger_id);
        pstmt.setInt(7, sprint_id);
        
        updateTagCache(repo_id, tag_name, version_id, message, tagged_date, tagger_id);
        
        insertStmt.batch();
    }
    
    /**
     * Update an existing tag in the database with new parameter values.
     * @param repo_id Identifier of the repository in which the tag is added
     * @param tag_name The name of the tag
     * @param version_id The version in which the tag is added (Subversion) or
     * which the tag references (Git)
     * @param message Message that is added to the tag when it is created, separate
     * from the commit message. Only available for Git repositories. If this is null,
     * then the tag has no separate message.
     * @param tagged_date Timestamp on which the tag is created (Git) or most
     * recently updated (Subversion). May be null for incomplete tags.
     * @param tagger_id Idenitifier of the VCS developer that created the tag.
     * If this is null, then the developer could not be deduced from tag information
     * @param sprint_id Idenitifer of the sprint in which the tag is created
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_tag(int repo_id, String tag_name, String version_id, String message, Timestamp tagged_date, Integer tagger_id, int sprint_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        
        pstmt.setString(1, version_id);
        setString(pstmt, 2, message);
        setTimestamp(pstmt, 3, tagged_date);
        setInteger(pstmt, 4, tagger_id);
        pstmt.setInt(5, sprint_id);
        
        pstmt.setInt(6, repo_id);
        pstmt.setString(7, tag_name);
        
        updateTagCache(repo_id, tag_name, version_id, message, tagged_date, tagger_id);
        
        updateStmt.batch();
    }
    
    @Override
    public void close() throws SQLException {
        insertStmt.execute();
        insertStmt.close();
        
        updateStmt.execute();
        updateStmt.close();
        
        if (tagRepoCache != null) {
            for (HashMap<String, TagInfo> cache : tagRepoCache.values()) {
                cache.clear();
            }
            tagRepoCache.clear();
        }
    }

}
