package de.fraunhofer.camunda.javaserver.exceptions;

public class DiagramStateException extends RuntimeException {
    public DiagramStateException(String s) {
        super(s);
    }

    public DiagramStateException(String s, Throwable cause) {
        super(s, cause);
    }
}
