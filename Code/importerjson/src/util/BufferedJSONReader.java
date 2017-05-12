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
 * A reader that emits JSON objects from an JSON-encoded input stream.
 * @author Leon Helwerda
 */
public class BufferedJSONReader extends LineNumberReader {
    private final JSONParser parser;
    private boolean inArray = false;

    /**
     * Create a new buffered JSON reader, using the default input-buffer size.
     * @param in A Reader object to provide the underlying stream
     */
    public BufferedJSONReader(Reader in) {
        super(in);
        parser = new JSONParser();
    }
    
    /**
     * Create a new buffered JSON reader, reading characters into a buffer
     * of the given size.
     * @param in A Reader object to provide the underlying stream
     * @param sz An int specifying the size of the buffer
     */
    public BufferedJSONReader(Reader in, int sz) {
        super(in, sz);
        parser = new JSONParser();
    }
    
    /**
     * Read a JSON object (or string if on first line) from the file.
     * This method returns the parsed object without reading the entire file,
     * and can be called again to read the next object until the end of the file
     * is correctly reached.
     * The accepted JSON data must be empty, a string or an array of objects.
     * The data must be formatted in such as way that we can detect starts and
     * ends of strings, arrays and objects easily: no whitespace or empty lines,
     * array starts and ends on their own line, and object ends with "},".
     * @return Object The JSON object: Either a JSONObject containing the map
     * of keys and values in the object, or a String if the input stream consists
     * of only a JSON-encoded string, or null if the input stream consists of
     * only an empty JSON array. This method also returns null if there is no
     * stream content, or the valid JSON array is exhausted of JSON objects.
     * @throws IOException If an I/O error occurs or the end of file is reached
     * before enough of the JSON data could be constructed to parse it.
     * @throws ParseException If a JSON parse error occurs.
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

