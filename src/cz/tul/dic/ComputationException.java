package cz.tul.dic;

/**
 *
 * @author Petr Jecmen
 */
public class ComputationException extends Exception {
    
    private final ComputationExceptionCause cause;

    /**
     * Creates a new instance of <code>ComputationException</code> without
     * detail message.
     * @param cause
     */
    public ComputationException(final ComputationExceptionCause cause) {
        super();
        
        this.cause = cause;
    }

    /**
     * Constructs an instance of <code>ComputationException</code> with the
     * specified detail message.
     *
     * @param cause
     * @param msg the detail message.
     */
    public ComputationException(final ComputationExceptionCause cause, final String msg) {
        super(msg);
        
        this.cause = cause;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(cause);
        sb.append(" - ");
        sb.append(getLocalizedMessage());
        
        return sb.toString();
    }
}
