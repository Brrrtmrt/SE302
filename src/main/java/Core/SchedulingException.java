package Core;

public class SchedulingException extends Exception {
    private final String errorDetail;

    public SchedulingException(String message, String errorDetail) {
        super(message);
        this.errorDetail = errorDetail;
    }

    public String getErrorDetail() {
        return errorDetail;
    }
}
