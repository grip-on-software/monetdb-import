/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import util.BaseDb;

/**
 *
 * @author Leon Helwerda
 */
public class SaltDb extends BaseDb implements AutoCloseable {
    BatchedStatement insertStmt = null;
    PreparedStatement checkStmt = null;

    public class SaltPair {
        private String salt;
        private String pepper;
        
        public SaltPair(String salt, String pepper) {
            this.salt = salt;
            this.pepper = pepper;
        }
        
        public String getSalt() {
            return salt;
        }
        
        public String getPepper() {
            return pepper;
        }
    }
    
    public SaltDb() {
        String sql = "insert into gros.project_salt(project_id,salt,pepper) values (?,?,?);";
        insertStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select salt, pepper from gros.project_salt where project_id=?;");
        }
    }
    
    private SaltPair create_pair() {
        Random source = new SecureRandom();
        byte[] salt = new byte[24];
        source.nextBytes(salt);
        
        byte[] pepper = new byte[24];
        source.nextBytes(pepper);
        
        return new SaltPair(Base64.encode(salt), Base64.encode(pepper));
    }
    
    public SaltPair get_salt(int project_id) throws SQLException, IOException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, project_id);
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                return new SaltPair(rs.getString("salt"), rs.getString("pepper"));
            }
        }

        SaltPair pair = create_pair();
        insert_salt(project_id, pair);
        return pair;
    }
    
    public void insert_salt(int project_id, SaltPair pair) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setInt(1, project_id);
        pstmt.setString(2, pair.getSalt());
        pstmt.setString(3, pair.getPepper());
        
        insertStmt.batch();
    }

    @Override
    public void close() throws SQLException {
        insertStmt.execute();
        insertStmt.close();
        
        if (checkStmt != null) {
            checkStmt.close();
            checkStmt = null;
        }
    }
    
}
