package dat.exceptions;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends Exception {
    
    /**
     * Constructs a new ValidationException with the specified detail message.
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new ValidationException with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}