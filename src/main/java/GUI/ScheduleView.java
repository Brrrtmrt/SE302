package GUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ScheduleView extends GridPane {

    // Dynamic settings
    private int totalDays = 5; // Default
    private LocalDate startDate = LocalDate.now(); // Start from Today
    private LocalTime startTime = LocalTime.of(8, 30);
    private LocalTime endTime = LocalTime.of(18, 30);
    private int slotDurationMinutes = 60;

    public ScheduleView() {
        this.setPadding(new Insets(20));
        this.setHgap(5);
        this.setVgap(5);
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        setupGrid();
    }

    // --- CONFIGURATION METHODS ---

    public void setDayCount(int days) {
        this.totalDays = days;
        setupGrid();
    }

    public void setSlotDuration(int minutes) {
        this.slotDurationMinutes = minutes;
        setupGrid();
    }

    public void setStartHour(int hour) {
        this.startTime = LocalTime.of(hour, 30);
        setupGrid();
    }

    public void setEndHour(int hour) {
        this.endTime = LocalTime.of(hour, 30);
        setupGrid();
    }

    // --- GRID GENERATION ---

    private void setupGrid() {
        this.getChildren().clear();
        this.getColumnConstraints().clear();
        this.getRowConstraints().clear();

        // 1. Time Column
        ColumnConstraints timeCol = new ColumnConstraints();
        timeCol.setPercentWidth(10);
        this.getColumnConstraints().add(timeCol);

        // 2. Day Columns (Dynamic)
        // We divide remaining 90% width by the number of days
        for (int i = 0; i < totalDays; i++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setPercentWidth(90.0 / totalDays);
            this.getColumnConstraints().add(dayCol);
        }

        // 3. Header Row (DATES instead of Names)
        this.add(createHeaderLabel("Time"), 0, 0);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int i = 0; i < totalDays; i++) {
            // Calculate date: Start Date + i days
            LocalDate date = startDate.plusDays(i);
            this.add(createHeaderLabel(date.format(dateFormatter)), i + 1, 0);
        }

        // 4. Time Rows
        LocalTime currentTime = startTime;
        int row = 1;

        if (startTime.isAfter(endTime)) return;

        while (currentTime.isBefore(endTime)) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            this.getRowConstraints().add(rc);

            String timeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            this.add(createHeaderLabel(timeStr), 0, row);

            // Empty Cells
            for (int col = 0; col < totalDays; col++) {
                StackPane cell = new StackPane();
                cell.setStyle("-fx-border-color: lightgray; -fx-background-color: white;");
                this.add(cell, col + 1, row);
            }

            currentTime = currentTime.plusMinutes(slotDurationMinutes);
            row++;
        }

        RowConstraints headerRow = new RowConstraints();
        headerRow.setMinHeight(30);
        this.getRowConstraints().add(0, headerRow);
    }

    public void addLesson(String lessonName, int dayIndex, int timeIndex, Color color) {
        // Ignore if lesson is outside the current view (e.g. Day 6 when showing 5 days)
        if (dayIndex >= totalDays) return;

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
        label.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0; -fx-border-color: #ccc;");
        return label;
    }

    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }
}