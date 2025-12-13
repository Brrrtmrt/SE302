package GUI;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private BorderPane mainContainer;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //preparing the schedule view
        ScheduleView schedule = new ScheduleView();
        schedule.addLesson("Java Lab", 0, 0, Color.LIGHTBLUE);
        StackPane.setAlignment(schedule, javafx.geometry.Pos.CENTER);
        schedule.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        mainContainer.setCenter(schedule); //the timetable
    }
}
