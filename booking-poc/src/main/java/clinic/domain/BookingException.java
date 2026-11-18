package clinic.domain;

/** Raised when a system operation's precondition is not met. */
public class BookingException extends RuntimeException {
    public BookingException(String message) {
        super(message);
    }
}
