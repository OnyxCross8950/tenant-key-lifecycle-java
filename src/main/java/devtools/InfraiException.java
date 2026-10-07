package devtools;

public final class InfraiException extends RuntimeException {
    private final String code;
    public InfraiException(String code, String message) { super(message); this.code = code; }
    public String code() { return code; }
}
