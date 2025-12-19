package GUI;

import Core.ClassRoom;
import Core.Course;
import Core.Student;
import IO.Importer;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private BorderPane mainContainer;

    //connection to the fxml items
    @FXML private ListView<String> studentList;
    @FXML private ListView<String> courseList;
    @FXML private ListView<String> classroomList;
    @FXML private ListView<String> attendanceList;

    // miscellaneous
    private boolean isDarkModeOn = false;

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

    @FXML
    void importStudents(ActionEvent event){
        File file = browseFile("Select Students CSV");
        if (file != null) {
            ArrayList<Student> students = Importer.importStudents(file.toPath());

            studentList.getItems().clear();
            for (Student s : students) {
                studentList.getItems().add(s.ID());
            }
        }
    }

    @FXML
    void importCourses(ActionEvent event){
        File file = browseFile("Select Courses CSV");
        if (file != null) {
            ArrayList<Course> courses = Importer.importCourses(file.toPath());
            courseList.getItems().clear();
            for (Course c : courses) {
                courseList.getItems().add(c.getID());
            }
        }
    }

    @FXML
    void importClassroomCapacity(ActionEvent event){
        File file = browseFile("Select Classrooms CSV");
        if (file != null) {
            ArrayList<ClassRoom> rooms = Importer.importClassRooms(file.toPath());
            classroomList.getItems().clear();
            for (ClassRoom r : rooms) {
                classroomList.getItems().add(r.getName() + " (Cap: " + r.getCapacity() + ")");
            }
        }
    }

    @FXML
    void importAttendanceList(ActionEvent event){
        File file = browseFile("Select Attendance CSV");
        if (file != null) {
            ArrayList<Course> attendanceData = Importer.importAttandenceLists(file.toPath());
            attendanceList.getItems().clear();
            for (Course c : attendanceData) {
                int count = c.getEnrolledStudentIDs().size();
                attendanceList.getItems().add(c.getID() + ": " + count + " Students");
            }
        }
    }

    @FXML
    void exportTimetable(ActionEvent event){

    }

    private File browseFile(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        Stage stage = (Stage) mainContainer.getScene().getWindow();
        return fileChooser.showOpenDialog(stage);
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

    @FXML
    void handleCreateSchedule(ActionEvent event) {
        System.out.println("DEBUG: create the optimal schedule button pressed");
    }

    @FXML
    void handleDarkMode(ActionEvent event) {
        isDarkModeOn = !isDarkModeOn;

        Scene scene = mainContainer.getScene();

        scene.getStylesheets().clear();

        if (isDarkModeOn) {
            scene.getStylesheets().add(getClass().getResource("/css/dark.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/css/light.css").toExternalForm());
        }
    }
}
