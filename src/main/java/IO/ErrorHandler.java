package IO;

import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {

    private static ErrorHandler instance;

    // Requirements: Hold error count and error messages
    private ArrayList<String> errorMessages;
    private int errorCount;

    // Private constructor for Singleton
    private ErrorHandler() {
        this.errorMessages = new ArrayList<>();
        this.errorCount = 0;
    }

    // Global access point
    public static ErrorHandler getInstance() {
        if (instance == null) {
            instance = new ErrorHandler();
        }
        return instance;
    }

    /**
     * Logs an error message and increments the error count.
     * @param message The error description
     */
    public void logError(String message) {
        this.errorMessages.add(message);
        this.errorCount++;
        // Optional: Print to console immediately so you don't lose track during debugging
        System.err.println("[ErrorHandler] " + message);
    }

    /**
     * @return The total number of errors recorded.
     */
    public int getErrorCount() {
        return this.errorCount;
    }

    /**
     * @return The list of error messages.
     */
    public ArrayList<String> getErrorMessages() {
        return this.errorMessages;
    }

    /**
     * Clears all errors (useful before starting a new validation process).
     */
    public void clear() {
        this.errorMessages.clear();
        this.errorCount = 0;
    }

    /**
     * @return True if there are any errors, false otherwise.
     */
    public boolean hasErrors() {
        return errorCount > 0;
    }
}
