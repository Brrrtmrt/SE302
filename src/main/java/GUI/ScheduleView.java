package GUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ScheduleView extends GridPane {

    private final String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    // Default settings
    private LocalTime startTime = LocalTime.of(8, 30);
    private LocalTime endTime = LocalTime.of(18, 30); // Adjust end time as needed
    private int slotDurationMinutes = 60; // Default

    public ScheduleView() {
        this.setPadding(new Insets(20));
        this.setHgap(5);
        this.setVgap(5);
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Fill screen

        // Initial build
        setupGrid();
    }

    /**
     * Call this to change the interval (e.g., 30, 45, 60 minutes)
     */
    public void setSlotDuration(int minutes) {
        this.slotDurationMinutes = minutes;
        setupGrid(); // Rebuild grid
    }

    private void setupGrid() {
        // 1. Clear everything to rebuild
        this.getChildren().clear();
        this.getColumnConstraints().clear();
        this.getRowConstraints().clear();

        // 2. Setup Columns (Time + 5 Days)
        ColumnConstraints timeCol = new ColumnConstraints();
        timeCol.setPercentWidth(10);
        this.getColumnConstraints().add(timeCol);

        for (int i = 0; i < days.length; i++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setPercentWidth(18); // 90% / 5 days
            this.getColumnConstraints().add(dayCol);
        }

        // 3. Add Day Headers (Row 0)
        this.add(createHeaderLabel("Time"), 0, 0);
        for (int i = 0; i < days.length; i++) {
            this.add(createHeaderLabel(days[i]), i + 1, 0);
        }

        // 4. Generate Rows Dynamically
        LocalTime currentTime = startTime;
        int row = 1;

        while (currentTime.isBefore(endTime)) {
            // Add Row Constraint to make it grow
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            this.getRowConstraints().add(rc);

            // Time Label
            String timeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            this.add(createHeaderLabel(timeStr), 0, row);

            // Empty Cells
            for (int col = 0; col < days.length; col++) {
                StackPane cell = new StackPane();
                cell.setStyle("-fx-border-color: lightgray; -fx-background-color: white;");
                this.add(cell, col + 1, row);
            }

            // Increment time
            currentTime = currentTime.plusMinutes(slotDurationMinutes);
            row++;
        }

        // Add header row constraint
        RowConstraints headerRow = new RowConstraints();
        headerRow.setMinHeight(30);
        this.getRowConstraints().add(0, headerRow);
    }

    public void addLesson(String lessonName, int dayIndex, int timeIndex, Color color) {
        StackPane lessonPane = new StackPane();
        lessonPane.setStyle("-fx-background-color: " + toHexString(color) +
                "; -fx-border-color: darkgray; -fx-border-width: 1;");
        Label lbl = new Label(lessonName);
        lbl.setStyle("-fx-font-weight: bold;");
        lessonPane.getChildren().add(lbl);
        lessonPane.setUserData("lesson");

        this.add(lessonPane, dayIndex + 1, timeIndex + 1);
    }

    public void clearLessons() {
        getChildren().removeIf(node -> "lesson".equals(node.getUserData()));
    }

    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        // Fallback style
        label.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0; -fx-border-color: #ccc;");
        return label;
    }

    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }
}