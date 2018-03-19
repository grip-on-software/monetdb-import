/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

/**
 * Generic exception class for exceptions thrown during an import task.
 * @author Leon Helwerda
 */
public class ImporterException extends RuntimeException {    
    public ImporterException(String string) {
        super(string);
    }
}
