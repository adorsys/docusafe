package org.adorsys.resource.server.exceptions;

/**
 * Created by peter on 23.12.17 at 18:11.
 */
public class BaseExceptionHandler {
    private final static String PREFIX = "org.adorsys.resource.server.exceptions";

    static public BaseException handle (Throwable t) {
        if (t.getClass().getName().startsWith(PREFIX)) {
            throw (RuntimeException) t;
        }
        throw new BaseException(t);
    }

    static public String getCauseChain(Throwable t) {
        String message = t.getMessage(); // + "(" + t.getClass().toString() + ")";
        if (t.getCause() != null) {
            return getCauseChain(t.getCause()) + "\nthis caused :" + message;
        }
        return message;
    }

    static public String getLastCauseOfChain(Throwable t) {
        if (t.getCause() != null) {
            return getLastCauseOfChain(t.getCause());
        }
        StringBuilder sb = new StringBuilder();

        sb.append("Exception message:");
        sb.append(t.getMessage());
        sb.append("\n");

        sb.append("Exception class:");
        sb.append(t.getClass().toString());
        sb.append("\n");

        sb.append("Exception stack:");
        sb.append("\n");
        for (StackTraceElement el : t.getStackTrace()) {
            if (el.toString().startsWith(PREFIX)) {
                sb.append("\t");
                sb.append(el.toString());
                sb.append("\n");
            }
        }
        return sb.toString();

    }

    static public String ThrowableToString(int layer, Throwable t, boolean debug) {
        String tab = "";
        for (int i = 0; i < layer; i++) {
            tab = tab + "\t";
        }
        StringBuilder sb = new StringBuilder();

        sb.append(tab);
        sb.append("Exception message:");
        sb.append(t.getMessage());
        sb.append("\n");

        sb.append(tab);
        sb.append("Exception class:");
        sb.append(t.getClass().toString());
        sb.append("\n");

        sb.append(tab);
        sb.append("Exception stack:");
        sb.append("\n");

        if (debug) {
            for (StackTraceElement el : t.getStackTrace()) {
                sb.append(tab);
                sb.append("\t");
                sb.append(el.toString().startsWith(PREFIX) ? "-> " : "   ");
                sb.append(el.toString());
                sb.append("\n");
            }
        } else {
            for (StackTraceElement el : t.getStackTrace()) {
                if (el.toString().startsWith(PREFIX)) {
                    sb.append(tab);
                    sb.append("\t");
                    sb.append(el.toString());
                    sb.append("\n");
                }
            }
        }

        if (t.getCause() != null) {
            sb.append(tab);
            sb.append("caused by:");
            sb.append(ThrowableToString(layer + 1, t.getCause(), debug));
        }

        return sb.toString();
    }
}

