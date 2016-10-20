/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import util.BaseImport;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrique
 */
public class CommentDb extends BaseImport{
    
    public void insert_comment(String issue, String message, String author, String sdate) throws PropertyVetoException{
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            con = DataSource.getInstance().getConnection();
            st = con.createStatement();
            
            sql = "insert into gros.comment(issue_id,message,author,date) values ('"+issue+"','"+message+"','"+author+"','"+sdate+"');";
                    
            st.executeUpdate(sql);
            
        } catch (SQLException | IOException ex) {
            Logger.getLogger(CommentDb.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
}
    
