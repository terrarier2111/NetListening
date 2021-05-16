package de.terrarier.netlistening.api;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public enum Type {

    BOOLEAN, BYTE, BYTEARRAY, CHAR, INT, SHORT, STRING, OBJECT, LONG, DOUBLE, UUID, FLOAT;

    public int getId() {
        return ordinal() + 1;
    }

}
