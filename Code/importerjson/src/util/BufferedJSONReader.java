/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Leon Helwerda
 */
public class BufferedJSONReader extends LineNumberReader {
    private final JSONParser parser;
    private boolean inArray = false;

    public BufferedJSONReader(Reader in) {
        super(in);
        parser = new JSONParser();
    }
    
    public BufferedJSONReader(Reader in, int sz) {
        super(in, sz);
        parser = new JSONParser();
    }
    
    /**
     * Read a JSON object (or string if on first line) from the file.
     * This method returns the parsed object without reading the entire file,
     * and can be called again to read the next object until the end of the file
     * is correctly reached.
     * The accepted JSON data is either a string or an array of objects.
     * The data must be formatted in such as way that we can detect starts and
     * ends of strings, arrays and objects easily: no whitespace or empty lines,
     * array starts and ends on their own line, and object ends with "},".
     * @return Object
     * @throws IOException
     * @throws ParseException 
     */
    public Object readObject() throws IOException, ParseException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = this.readLine()) != null) {
            if (!inArray && this.getLineNumber() == 1) {
                if (line.startsWith("\"") && line.endsWith("\"")) {
                    return (Object) parser.parse(line);
                }
                if ("[]".equals(line)) {
                    return null;
                }
                if ("[".equals(line)) {
                    inArray = true;
                    continue;
                }
            }
            if ("]".equals(line)) {
                inArray = false;
                continue;
            }
            
            if (!inArray) {
                throw new EOFException("Expecting end of JSON stream at line " + this.getLineNumber());
            }
            sb.append(line.trim());

            String json = "";
            if (sb.length() > 1 && sb.substring(sb.length()-1).equals("}")) {
                json = sb.toString();
            }
            else if (sb.length() > 2 && sb.substring(sb.length()-2).equals("},")) {
                json = sb.substring(0, sb.length()-1);
            }
            if (!json.isEmpty()) {
                Object jsonObject = (Object) parser.parse(json);
                sb.setLength(0);
                return jsonObject;
            }
        }
        if (inArray) {
            throw new EOFException("Unexpected end of file while parsing JSON object");
        }
        return null;
    }
}

