package org.znerd.confluence.client.support;

public class JsonParseRuntimeException extends RuntimeException {
    public JsonParseRuntimeException() {
    }

    public JsonParseRuntimeException(final String message) {
        super(message);
    }

    public JsonParseRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
