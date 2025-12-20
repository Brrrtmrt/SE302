package GUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class ScheduleView extends GridPane {

    // Settings
    private int totalDays = 5;
    private LocalDate startDate = LocalDate.now();
    private LocalTime startTime = LocalTime.of(8, 30);
    private LocalTime endTime = LocalTime.of(18, 30);
    private int slotDurationMinutes = 60;
    private ArrayList<Label> dayHeaderLabels = new ArrayList<>();

    private VBox[][] cells;

    public ScheduleView() {
        this.setPadding(new Insets(20));
        this.setHgap(5);
        this.setVgap(5);
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        setupGrid();
    }

    public void setStartDate(LocalDate date) {
        this.startDate = date;
        setupGrid();
    }

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

    private void setupGrid() {
        this.getChildren().clear();
        this.getColumnConstraints().clear();
        this.getRowConstraints().clear();

        long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        int totalRows = (int) (totalMinutes / slotDurationMinutes) + 1;

        cells = new VBox[totalDays][totalRows + 5];

        ColumnConstraints timeCol = new ColumnConstraints();
        timeCol.setPercentWidth(10);
        this.getColumnConstraints().add(timeCol);

        for (int i = 0; i < totalDays; i++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setPercentWidth(90.0 / totalDays);
            this.getColumnConstraints().add(dayCol);
        }

        this.add(createHeaderLabel("Time"), 0, 0);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy EEE", new Locale("tr", "TR"));

        for (int i = 0; i < totalDays; i++) {
            LocalDate date = startDate.plusDays(i);
            this.add(createHeaderLabel(date.format(dateFormatter)), i + 1, 0);
        }
        // -------------------------------------

        LocalTime currentTime = startTime;
        int row = 1;

        if (startTime.isAfter(endTime)) return;

        while (currentTime.isBefore(endTime)) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            rc.setMinHeight(60);
            this.getRowConstraints().add(rc);

            String timeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            this.add(createHeaderLabel(timeStr), 0, row);

            for (int col = 0; col < totalDays; col++) {

                VBox cellContent = new VBox(2);
                cellContent.setAlignment(Pos.TOP_CENTER);
                cellContent.setStyle("-fx-background-color: white;");

                ScrollPane scroller = new ScrollPane(cellContent);
                scroller.setFitToWidth(true);
                scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scroller.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: lightgray;");

                if (row - 1 < cells[col].length) {
                    cells[col][row - 1] = cellContent;
                }

                this.add(scroller, col + 1, row);
            }

            currentTime = currentTime.plusMinutes(slotDurationMinutes);
            row++;
        }

        RowConstraints headerRow = new RowConstraints();
        headerRow.setMinHeight(30);
        this.getRowConstraints().add(0, headerRow);
    }

    public void addLesson(String lessonName, int dayIndex, int timeIndex, Color color) {
        if (dayIndex < 0 || dayIndex >= totalDays) return;
        if (cells == null) return;
        if (timeIndex < 0 || timeIndex >= cells[dayIndex].length) return;

        VBox targetCell = cells[dayIndex][timeIndex];
        if (targetCell == null) return;

        Label lbl = new Label(lessonName);
        lbl.setWrapText(true);
        lbl.setTextAlignment(TextAlignment.CENTER);
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER);

        StackPane examBlock = new StackPane(lbl);
        examBlock.setStyle("-fx-background-color: " + toHexString(color) +
                "; -fx-background-radius: 4; -fx-border-color: gray; -fx-border-radius: 4; -fx-border-width: 1;");
        examBlock.setPadding(new Insets(3));
        examBlock.setUserData("lesson");

        targetCell.getChildren().add(examBlock);
    }

    public void clearLessons() {
        if (cells == null) return;
        for (int d = 0; d < cells.length; d++) {
            if (cells[d] == null) continue;
            for (int t = 0; t < cells[d].length; t++) {
                if (cells[d][t] != null) {
                    cells[d][t].getChildren().clear();
                }
            }
        }
    }

    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add("schedule-header");
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }
}