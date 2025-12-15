package GUI;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private BorderPane mainContainer;

    //connection to the fxml items
    @FXML private ListView<String> studentList;
    @FXML private ListView<String> courseList;
    @FXML private ListView<String> classroomList;
    @FXML private ListView<String> attendanceList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //preparing the schedule view
        ScheduleView schedule = new ScheduleView();
        schedule.addLesson("Java Lab", 0, 0, Color.LIGHTBLUE);
        /*   I think we don't need this comment but i'm not sure so don't delete yet
        StackPane.setAlignment(schedule, javafx.geometry.Pos.CENTER);
        schedule.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        */
        studentList.getItems().addAll("Mert Utma", "Mert Saadet", "Abdullah Demir");
        courseList.getItems().addAll("Math 153", "Physics 100", "CE 302");
        classroomList.getItems().addAll("M102 (Cap: 50)", "C502 (Cap: 45)");
        attendanceList.getItems().addAll("Full", " 5 missing");

        //VERY CRUCIAL TO ENABLE DELETE BUTTONS
        studentList.setCellFactory(param -> new DeletableCell());
        courseList.setCellFactory(param -> new DeletableCell());
        classroomList.setCellFactory(param -> new DeletableCell());
        attendanceList.setCellFactory(param -> new DeletableCell());

        mainContainer.setCenter(schedule); //the timetable
    }

    //helper class
    static class DeletableCell extends ListCell<String> {
        HBox hbox = new HBox();
        Label label = new Label("");
        Pane pane = new Pane();
        Button button = new Button("X");

        public DeletableCell() {
            super();

            // configure how it looks
            hbox.getChildren().addAll(label, pane, button);
            HBox.setHgrow(pane, Priority.ALWAYS);


            button.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-background-color: transparent; -fx-cursor: hand;");

            // THE DELETE ACTION WE'LL ADD MORE HERE
            button.setOnAction(event -> {
                getListView().getItems().remove(getItem());
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(null);

            if (item != null && !empty) {
                label.setText(item);
                setGraphic(hbox);
            }
        }
    }
}
