package com.javapp.api;

/** Error de API con causa tipada (para mensajes UI genéricos en AUTH). */
public class ApiException extends RuntimeException {

    public enum Kind {
        AUTH,
        VALIDATION,
        NOT_FOUND
    }

    private final Kind kind;

    public ApiException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
