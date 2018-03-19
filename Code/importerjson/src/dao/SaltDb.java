/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package dao;

import java.beans.PropertyVetoException;
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
 * Management of project-specific and global encryption salts.
 * @author Leon Helwerda
 */
public class SaltDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
    
    public final static class Encryption {
        public final static int NONE = 0;
        public final static int PROJECT = 1;
        public final static int GLOBAL = 2;
        public final static int BOTH = 3;

        /**
         * Parse an encryption flag.
         * @param encrypted The encryption flag encoded in a string.
         * @return The encryption flag integer.
         * @throws NumberFormatException If the string does not contain a valid
         * encryption flag.
         */
        public static int parseInt(String encrypted) {
            if (encrypted == null) {
                return NONE;
            }
            
            int encryption = Integer.parseInt(encrypted);
            check(encryption);
            return encryption;
        }
        
        /**
         * Check whether an encryption flag is valid.
         * @param encryption The encryption flag integer
         * @throws NumberFormatException If the integer does not contain a valid
         * encryption flag.
         */
        public static void check(int encryption) {
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
        private final String salt;
        private final String pepper;
        
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
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select salt, pepper from gros.project_salt where project_id=?;");
        }
    }
    
    private SaltPair createPair() {
        Random source = new SecureRandom();
        byte[] salt = new byte[24];
        source.nextBytes(salt);
        
        byte[] pepper = new byte[24];
        source.nextBytes(pepper);
        
        Base64.Encoder encoder = Base64.getEncoder();
        return new SaltPair(encoder.encodeToString(salt), encoder.encodeToString(pepper));
    }
    
    /**
     * Retrieve the salt and pepper pair for the provided project. If the pair
     * does not yet exist, it is created, inserted and returned.
     * @param project_id The project to retrieve the pair for. If this is 0, then
     * the global salt and pepper are retrieved.
     * @return A SaltPair with the salt and pepper.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public SaltPair get_salt(int project_id) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, project_id);
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                return new SaltPair(rs.getString("salt"), rs.getString("pepper"));
            }
        }

        SaltPair pair = createPair();
        insert_salt(project_id, pair);
        return pair;
    }
    
    /**
     * Insert a salt pair into de project salts table.
     * @param project_id The project ID to add the salt and pepper for, or 0 to
     * insert a global salt pair.
     * @param pair The salt and pepper pair.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_salt(int project_id, SaltPair pair) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setInt(1, project_id);
        pstmt.setString(2, pair.getSalt());
        pstmt.setString(3, pair.getPepper());
        
        insertStmt.batch();
    }

    private static String sha256(String base) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(base.getBytes("UTF-8"));
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
    
    /**
     * Perform a one-way encryption step using SHA-256 on the provided string.
     * @param value A string to encrypt
     * @param pair The salt and pepper to use in the one-way encryption.
     * @return The encrypted string
     */
    public String hash(String value, SaltPair pair) {
        // Keep null values as is, since there is no sensitive information in this case.
        if (value == null) {
            return null;
        }
        try {
            return sha256(pair.getSalt() + value + pair.getPepper());
        }
        catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
            logException(ex);
            return null;
        }
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
