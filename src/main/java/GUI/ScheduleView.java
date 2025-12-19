package GUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class ScheduleView extends GridPane {

    private final String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private final String[] times = {"08:30", "09:25", "10:20", "11:15", "12:10", "13:05", "14:10", "15.05", "16.00", "17.55", "18.50", "19.45", "20.40"};

    public ScheduleView() {
        this.setPadding(new Insets(20));
        this.setHgap(5);
        this.setVgap(5);

        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        setupGrid();
    }

    private void setupGrid() {
        // Headers
        for (int i = 0; i < days.length; i++) {
            this.add(createHeaderLabel(days[i]), i + 1, 0);
        }

        // Time slots
        for (int row = 0; row < times.length; row++) {
            this.add(createHeaderLabel(times[row]), 0, row + 1);
            for (int col = 0; col < days.length; col++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(100, 50);
                cell.setStyle("-fx-border-color: lightgray; -fx-background-color: white;");
                this.add(cell, col + 1, row + 1);
            }
        }
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
        // This removes only the nodes we tagged as "lesson"
        getChildren().removeIf(node -> "lesson".equals(node.getUserData()));
    }

    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.setStyle("-fx-font-weight: bold;");

        // to make dark mode
        label.getStyleClass().add("schedule-header");

        return label;
    }

    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}
