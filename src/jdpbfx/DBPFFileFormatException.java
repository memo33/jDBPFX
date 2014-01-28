package jdpbfx;

import java.io.IOException;

/**
 * Signals that a file attempted to be read as DBPF file does not conform to
 * the DBPF file format.
 *
 * @author memo
 */
public class DBPFFileFormatException extends IOException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@code DBPFFileFormatException} with {@code null} as its
     * error detail message.
     */
    public DBPFFileFormatException() {
        super();
    }

    /**
     * Constructs a {@code DBPFFileFormatException} with the specified detail
     * message.
     */
    public DBPFFileFormatException(String message) {
        super(message);
    }

}
