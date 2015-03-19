/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
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

    public ComputationExceptionCause getExceptionCause() {
        return cause;
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
