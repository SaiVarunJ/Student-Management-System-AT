package com.airtripe.studentmanagement.exception;

public class PersistenceException extends RuntimeException {
    public PersistenceException(String msg, Throwable cause) { super(msg, cause); }
    public PersistenceException(String msg) { super(msg); }
}

