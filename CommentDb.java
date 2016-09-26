/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import util.BaseImport;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrique
 */
public class CommentDb extends BaseImport{
    
    public void insert_comment(String issue, String message, String author, String sdate){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
       
            st = con.createStatement();
            
            sql = "insert into gros.comment(issue_id,message,author,date) values ('"+issue+"','"+message+"','"+author+"','"+sdate+"');";
                    
            st.executeUpdate(sql);
            
            con.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(CommentDb.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(CommentDb.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    
    }
        
}
    
