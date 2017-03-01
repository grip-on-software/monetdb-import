/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Objects;
import util.BaseDb;

/**
 *
 * @author Leon Helwerda
 */
public class IssueLinkDb extends BaseDb implements AutoCloseable {    
    private static class Link {
        public final String from_key;
        public final String to_key;
        public final int relationship_type;
        public final boolean outward;
        
        public Link(String from_key, String to_key, int relationship_type) {
            this.from_key = from_key;
            this.to_key = to_key;
            this.relationship_type = relationship_type;
            this.outward = true;
        }
        
        public Link(String from_key, String to_key, int relationship_type, boolean outward) {
            this.from_key = from_key;
            this.to_key = to_key;
            this.relationship_type = relationship_type;
            this.outward = outward;
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof Link) {
                Link otherLink = (Link) other;
                return (from_key.equals(otherLink.from_key) &&
                        to_key.equals(otherLink.to_key) &&
                        relationship_type == otherLink.relationship_type);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = Objects.hashCode(this.from_key);
            hash = 31 * hash + Objects.hashCode(this.to_key);
            hash = 17 * hash + this.relationship_type;
            hash = 13 * hash + Boolean.hashCode(this.outward);
            return hash;
        }        
    }
    
    public static class LinkDates {
        public Timestamp start_date;
        public Timestamp end_date;
        
        public LinkDates() {
            this.start_date = null;
            this.end_date = null;
        }
        
        public LinkDates(Timestamp start_date, Timestamp end_date) {
            this.start_date = start_date;
            this.end_date = end_date;
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof LinkDates) {
                LinkDates otherDates = (LinkDates) other;
                return ((start_date == null ? otherDates.start_date == null : start_date.equals(otherDates.start_date)) &&
                        (end_date == null ? otherDates.end_date == null : end_date.equals(otherDates.end_date)));
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.start_date);
            hash = 53 * hash + Objects.hashCode(this.end_date);
            return hash;
        }
    }
    
    public static class CheckResult {
        public enum State { MISSING, DIFFERS, EXISTS };
        public State state = State.MISSING;
        public LinkDates dates = null;
    };
    
    HashMap<Link, LinkDates> linkCache = null;
    
    BatchedStatement insertStmt = null;
    PreparedStatement checkStmt = null;
    BatchedStatement updateStmt = null;

    public IssueLinkDb() {
        String sql = "insert into gros.isselink (from_key,to_key,relationship_type,outward,start_date,end_date) values (?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);

        sql = "update gros.issuelink set start_date=?, end_date=? where from_key=? and to_key=? and relationship_type=? and outward=?;";
        updateStmt = new BatchedStatement(sql);
    }

    private void getCheckStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select start_date, end_date from gros.issuelink where from_key=? and to_key=? and relationship_type? and outward=?;");
        }
    }

    private void fillLinkCache() throws SQLException, IOException, PropertyVetoException {
        if (linkCache != null) {
            return;
        }
        linkCache = new HashMap<>();
        
        Connection con = insertStmt.getConnection();
        String sql = "SELECT from_key,to_key,relationship_type,outward,start_date,end_date FROM gros.issuelink";
        ResultSet rs;
        try (Statement stmt = con.createStatement()) {
            rs = stmt.executeQuery(sql);
            while(rs.next()) {
                Link link = new Link(rs.getString("from_key"), rs.getString("to_key"), rs.getInt("relationship_type"), rs.getBoolean("outward"));
                LinkDates dates = new LinkDates(rs.getTimestamp("start_date"), rs.getTimestamp("end_date"));
                linkCache.put(link, dates);
            }
        }
        rs.close();
    }
    
    private boolean compareTimestamps(Timestamp date, Timestamp current_date, boolean allowEarlier) {
        if (date == null) {
            return (current_date == null);
        }
        if (allowEarlier && current_date != null && date.after(current_date)) {
            return true;
        }
        return date.equals(current_date);
    }

    private CheckResult compareLinkDates(LinkDates dates, LinkDates current_dates) {
        CheckResult result = new CheckResult();
        result.dates = current_dates;
        if (compareTimestamps(dates.start_date, current_dates.start_date, true) &&
                compareTimestamps(dates.end_date, current_dates.end_date, false)) {
            result.state = CheckResult.State.EXISTS;
        }
        else {
            result.state = CheckResult.State.DIFFERS;
        }
        return result;
    }

    public CheckResult check_link(String from_key, String to_key, int relationship_type, boolean outward, Timestamp start_date, Timestamp end_date) throws SQLException, IOException, PropertyVetoException {
        getCheckStmt();
        fillLinkCache();
        
        Link link = new Link(from_key, to_key, relationship_type, outward);
        LinkDates dates = new LinkDates(start_date, end_date);
                
        LinkDates cache_dates = linkCache.get(link);
        if (cache_dates != null) {
            return compareLinkDates(dates, cache_dates);
        }
        
        CheckResult result = new CheckResult();
        
        checkStmt.setString(1, from_key);
        checkStmt.setString(2, to_key);
        checkStmt.setInt(3, relationship_type);
        checkStmt.setBoolean(4, outward);
        try (ResultSet rs = checkStmt.executeQuery()) {
            while (rs.next()) {
                LinkDates row_dates = new LinkDates(rs.getTimestamp("start_date"), rs.getTimestamp("end_date"));
                result = compareLinkDates(dates, row_dates);
                linkCache.put(link, row_dates);
            }
        }
        
        return result;
    }
    
    private void setTimestamp(PreparedStatement pstmt, int index, Timestamp date) throws SQLException {
        if (date == null) {
            pstmt.setNull(index, java.sql.Types.TIMESTAMP);
        }
        else {
            pstmt.setTimestamp(index, date);
        }
    }
    
    public void insert_link(String from_key, String to_key, int relationship_type, boolean outward, Timestamp start_date, Timestamp end_date) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setString(1, from_key);
        pstmt.setString(2, to_key);
        pstmt.setInt(3, relationship_type);
        pstmt.setBoolean(4, outward);
        setTimestamp(pstmt, 5, start_date);
        setTimestamp(pstmt, 6, end_date);
                             
        insertStmt.batch();
        
        if (linkCache != null) {
            Link link = new Link(from_key, to_key, relationship_type, outward);
            LinkDates dates = new LinkDates(start_date, end_date);
            linkCache.put(link, dates);
        }
    }
    
    public void update_link(String from_key, String to_key, int relationship_type, boolean outward, Timestamp start_date, Timestamp end_date) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        setTimestamp(pstmt, 1, start_date);
        setTimestamp(pstmt, 2, end_date);
        pstmt.setString(3, from_key);
        pstmt.setString(4, to_key);
        pstmt.setInt(5, relationship_type);
        pstmt.setBoolean(6, outward);

        updateStmt.batch();
        
        if (linkCache != null) {
            Link link = new Link(from_key, to_key, relationship_type, outward);
            LinkDates dates = new LinkDates(start_date, end_date);
            linkCache.put(link, dates);
        }
    }
    
    @Override
    public void close() throws SQLException {
        insertStmt.execute();
        insertStmt.close();
        
        updateStmt.execute();
        updateStmt.close();
        
        if (checkStmt != null) {
            checkStmt.close();
            checkStmt = null;
        }
    }
}
