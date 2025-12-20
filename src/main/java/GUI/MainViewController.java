package GUI;

import Core.ClassRoom;
import Core.Course;
import Core.Student;
import IO.Exporter;
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
import java.util.HashMap;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private BorderPane mainContainer;

    // ONLY VISUALS
    @FXML private ListView<String> studentList;
    @FXML private ListView<String> courseList;
    @FXML private ListView<String> classroomList;
    @FXML private ListView<String> attendanceList;

    //DATA PART
    private ArrayList<Student> allStudents = new ArrayList<>();
    private ArrayList<Course> allCourses = new ArrayList<>();
    private ArrayList<ClassRoom> allClassrooms = new ArrayList<>();

    // miscellaneous
    private boolean isDarkModeOn = false;
    private ScheduleView scheduleView;

    //related to export
    private HashMap<Course, Integer> finalSchedule = new HashMap<>();
    private HashMap<Integer, String[]> finalSlotMap = new HashMap<>();
    private HashMap<Course, ClassRoom> finalRoomMap = new HashMap<>();



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //preparing the schedule view
        scheduleView = new ScheduleView();

        //scheduleView.addLesson("HERAHNGI BI DERS", 0, 0, Color.GRAY);
        /*   I think we don't need this comment but i'm not sure so don't delete yet
        StackPane.setAlignment(schedule, javafx.geometry.Pos.CENTER);
        schedule.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        */
        /*  MOCK DATA TO TEST THE UI
        studentList.getItems().addAll("Mert Utma", "Mert Saadet", "Abdullah Demir");
        courseList.getItems().addAll("Math 153", "Physics 100", "CE 302");
        classroomList.getItems().addAll("M102 (Cap: 50)", "C502 (Cap: 45)");
        attendanceList.getItems().addAll("Full", " 5 missing");
         */


        //VERY CRUCIAL TO ENABLE DELETE BUTTONS
        studentList.setCellFactory(param -> new DeletableCell());
        courseList.setCellFactory(param -> new DeletableCell());
        classroomList.setCellFactory(param -> new DeletableCell());
        attendanceList.setCellFactory(param -> new DeletableCell());

        mainContainer.setCenter(scheduleView); //the timetable
    }

    @FXML
    void importStudents(ActionEvent event){
        File file = browseFile("Select Students CSV");
        if (file != null) {
            ArrayList<Student> students = Importer.importStudents(file.toPath());

            this.allStudents = students; //SAVING

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

            this.allCourses = courses;

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

            this.allClassrooms = rooms;

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

    // WHEN THE USER PRESSES EXPORT THE FILE
    @FXML
    void exportTimetable(ActionEvent event) {
        // check
        if (finalSchedule.isEmpty()) {
            System.out.println("Error: No schedule generated yet. Please create a schedule first.");
            return;
        }

        // file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Schedule");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("OptimalSchedule.csv");

        //another check
        if (mainContainer.getScene() != null) {
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            // 4. call exporter
            if (file != null) {
                Exporter.exportSchedule(
                        file.toPath(),
                        "Final Exams 2025",
                        finalSchedule,
                        finalSlotMap,
                        finalRoomMap
                );
                System.out.println("Export Successful to: " + file.getAbsolutePath());
            }
        }
    }

    private File browseFile(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        Stage stage = (Stage) mainContainer.getScene().getWindow();
        return fileChooser.showOpenDialog(stage);
    }


    //helper class
    class DeletableCell extends ListCell<String> {
        HBox hbox = new HBox();
        Label label = new Label("");
        Pane pane = new Pane();
        Button button = new Button("X");

        public DeletableCell() {
            super();

            hbox.getChildren().addAll(label, pane, button);
            HBox.setHgrow(pane, Priority.ALWAYS);
            button.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-background-color: transparent; -fx-cursor: hand;");

            button.setOnAction(event -> {
                String itemToRemove = getItem();

                // remove from the ui
                getListView().getItems().remove(itemToRemove);

                // remove from the actual data
                if (getListView() == courseList) {

                    allCourses.removeIf(c -> c.getID().equals(itemToRemove));
                    System.out.println("Deleted Course: " + itemToRemove);

                } else if (getListView() == studentList) {

                    allStudents.removeIf(s -> s.ID().equals(itemToRemove));
                    System.out.println("Deleted Student: " + itemToRemove);

                } else if (getListView() == classroomList) {

                    allClassrooms.removeIf(r ->
                            (r.getName() + " (Cap: " + r.getCapacity() + ")").equals(itemToRemove)
                    );
                    System.out.println("Deleted Room: " + itemToRemove);

                } else if (getListView() == attendanceList) {

                }
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

    //WHEN THE USER PRESSES CREATE THE MOST OPTIMAL TIMETABLE(SCHEDULE)
    @FXML
    void handleCreateSchedule(ActionEvent event) {
        if (allCourses.isEmpty() || allClassrooms.isEmpty()) {
            System.out.println("Error: Please import Courses and Classrooms first!");
            return;
        }

        System.out.println("Starting Scheduler Algorithm...");

        Core.Scheduler scheduler = new Core.Scheduler();


        scheduler.loadData(allCourses, allClassrooms, 60);


        scheduler.generate_schedule(false);

        // 4. Retrieve Results
        HashMap<Course, Integer> calculatedSchedule = scheduler.getSchedule();
        HashMap<Course, ClassRoom> calculatedRooms = scheduler.getRoomAssignments();
        ArrayList<Helpers.TimeSlot> timeSlots = scheduler.getActiveTimeSlots();

        if (calculatedSchedule == null || calculatedSchedule.isEmpty()) {
            System.out.println("Scheduler could not find a solution or was stopped!");
            return;
        }


        scheduleView.clearLessons();
        finalSchedule.clear();
        finalSlotMap.clear();
        finalRoomMap.clear();


        for (var entry : calculatedSchedule.entrySet()) {
            Course course = entry.getKey();
            int slotID = entry.getValue();

            // Get the actual TimeSlot object
            Helpers.TimeSlot ts = timeSlots.get(slotID);
            ClassRoom assignedRoom = calculatedRooms.get(course);



            // Day: Convert Date to Grid Column (0=Mon, 1=Tue...)
            int dayIndex = ts.getDate().getDayOfWeek().getValue() - 1;


            int timeIndex = ts.getTime().getHour() - 8;

            // Safety check for indices
            if (dayIndex < 0) dayIndex = 0;
            if (timeIndex < 0) timeIndex = 0;

            scheduleView.addLesson(
                    course.getID(),
                    dayIndex,
                    timeIndex,
                    Color.LIGHTBLUE
            );

            finalSchedule.put(course, slotID);
            if (assignedRoom != null) {
                finalRoomMap.put(course, assignedRoom);
            }


            String dateStr = ts.getDate().toString();
            String startStr = ts.getTime().toString();
            String endStr = ts.getTime().plusMinutes(60).toString();

            finalSlotMap.put(slotID, new String[]{dateStr, startStr, endStr});
        }

        System.out.println("Optimal Schedule Generated & Displayed.");
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
