/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Random;
import util.BaseDb;

/**
 *
 * @author Leon Helwerda
 */
public class SaltDb extends BaseDb implements AutoCloseable {
    BatchedStatement insertStmt = null;
    PreparedStatement checkStmt = null;
    
    public final static class Encryption {
        public final static int NONE = 0;
        public final static int PROJECT = 1;
        public final static int GLOBAL = 2;
        public final static int BOTH = 3;

        public static int parseInt(String encrypted) throws NumberFormatException {
            if (encrypted == null) {
                return NONE;
            }
            
            int encryption = Integer.parseInt(encrypted);
            check(encryption);
            return encryption;
        }
        
        public static void check(int encryption) throws NumberFormatException {
            if (encryption < NONE || encryption > BOTH) {
                throw new NumberFormatException("Encryption level " + encryption + " is not supported");
            }
        }
                
        public static int add(int encryption, int new_encryption) {
            check(encryption);
            check(new_encryption);
            return encryption | new_encryption;
        }
        
        private Encryption() {
        }
    }

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
        
        Base64.Encoder encoder = Base64.getEncoder();
        return new SaltPair(encoder.encodeToString(salt), encoder.encodeToString(pepper));
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

    private static String sha256(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(UnsupportedEncodingException | NoSuchAlgorithmException ex){
           throw new RuntimeException(ex);
        }
    }
    
    public String hash(String value, SaltPair pair) {
        // Keep null values as is, since there is no sensitive information in this case.
        if (value == null) {
            return null;
        }
        return sha256(pair.getSalt() + value + pair.getPepper());
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
