package dpx;

/** An error that is sent to the client with an HTTP status code. */
public class ApiException extends RuntimeException {
    public final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }
}
