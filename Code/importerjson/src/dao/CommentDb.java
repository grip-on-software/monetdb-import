/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import util.BaseImport;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author Enrique
 */
public class CommentDb extends BaseImport {
    BatchedStatement bstmt = null;
    
    public CommentDb() {
        String sql = "insert into gros.comment(issue_id,message,author,date) values (?,?,?,?);";
        bstmt = new BatchedStatement(sql);
    }
    
    public void insert_comment(String issue, String message, String author, String sdate) throws SQLException, IOException, PropertyVetoException{    
        PreparedStatement pstmt = bstmt.getPreparedStatement();
        pstmt.setInt(1, Integer.parseInt(issue));
        pstmt.setString(2, message);
        pstmt.setString(3, author);
        pstmt.setTimestamp(4, Timestamp.valueOf(sdate));
                             
        bstmt.batch();
    }
    
    public void close() throws SQLException {
        bstmt.execute();
        bstmt.close();
    }
        
}
    
