package GUI;

import Core.ClassRoom;
import Core.Course;
import Core.Student;
import IO.Exporter;
import IO.Importer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML private BorderPane mainContainer;

    // --- VISUAL LISTS ---
    @FXML private ListView<String> studentList;
    @FXML private ListView<String> courseList;
    @FXML private ListView<String> classroomList;
    @FXML private ListView<String> attendanceList;

    // --- DATA STORAGE ---
    private ArrayList<Student> allStudents = new ArrayList<>();
    private ArrayList<Course> allCourses = new ArrayList<>();
    private ArrayList<ClassRoom> allClassrooms = new ArrayList<>();
    // NEW: We must store attendance separately to merge it later
    private ArrayList<Course> allAttendance = new ArrayList<>();

    // --- STATE & VIEWS ---
    private boolean isDarkModeOn = false;
    private ScheduleView scheduleView;

    // --- EXPORT DATA ---
    private HashMap<Course, Integer> finalSchedule = new HashMap<>();
    private HashMap<Integer, String[]> finalSlotMap = new HashMap<>();
    private HashMap<Course, ClassRoom> finalRoomMap = new HashMap<>();

    // --- CONTROLS ---
    @FXML private Spinner<Integer> intervalSpinner;
    @FXML private Spinner<Integer> daysSpinner;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Initialize ScheduleView
        scheduleView = new ScheduleView();

        // 2. Setup Interval Spinner (Time Slots)
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 120, 60, 15);
        intervalSpinner.setValueFactory(valueFactory);
        intervalSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            scheduleView.setSlotDuration(newVal);
        });

        // 3. Setup Days Spinner (Total Days)
        SpinnerValueFactory<Integer> daysFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 14, 5, 1);
        daysSpinner.setValueFactory(daysFactory);
        daysSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            scheduleView.setDayCount(newVal);
        });

        // 4. Setup Delete Buttons
        studentList.setCellFactory(param -> new DeletableCell());
        courseList.setCellFactory(param -> new DeletableCell());
        classroomList.setCellFactory(param -> new DeletableCell());
        attendanceList.setCellFactory(param -> new DeletableCell());

        // 5. Final Layout Setup
        mainContainer.setCenter(scheduleView);

        // Force load Light Mode CSS on startup
        mainContainer.getStylesheets().add(getClass().getResource("/css/light.css").toExternalForm());
    }

    @FXML
    void importStudents(ActionEvent event){
        File file = browseFile("Select Students CSV");
        if (file != null) {
            ArrayList<Student> students = Importer.importStudents(file.toPath());
            this.allStudents = students;
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

            // SAVE TO MEMORY
            this.allAttendance = attendanceData;

            attendanceList.getItems().clear();
            for (Course c : attendanceData) {
                int count = c.getEnrolledStudentIDs().size();
                attendanceList.getItems().add(c.getID() + ": " + count + " Students");
            }
        }
    }

    @FXML
    void handleCreateSchedule(ActionEvent event) {
        if (allCourses.isEmpty() || allClassrooms.isEmpty()) {
            System.out.println("Error: Please import Courses and Classrooms first!");
            return;
        }

        System.out.println("Starting Scheduler Algorithm...");
        Core.Scheduler scheduler = new Core.Scheduler();

        // --- CRITICAL FIX: MERGE ATTENDANCE INTO COURSES BEFORE SCHEDULING ---
        // The Scheduler.loadData assumes courses already have students in them.
        // We must map the attendance data (allAttendance) into the course objects (allCourses).

        HashMap<String, Course> courseMap = new HashMap<>();
        for (Course c : allCourses) {
            // Reset students to avoid duplicates if button is clicked twice
            c.getEnrolledStudentIDs().clear();
            courseMap.put(c.getID(), c);
        }

        for (Course attCourse : allAttendance) {
            Course realCourse = courseMap.get(attCourse.getID());
            if (realCourse != null) {
                for (String studentID : attCourse.getEnrolledStudentIDs()) {
                    realCourse.addEnrolledStudentID(studentID);
                }
            }
        }
        // ---------------------------------------------------------------------

        // 1. Load Data (Now that courses have students inside them)
        int stepSize = intervalSpinner.getValue();
        scheduler.loadData(allCourses, allClassrooms, stepSize);

        // 2. Run Generation
        int userRequestedDays = daysSpinner.getValue();
        LocalDate startDate = LocalDate.of(2025, 12, 15);

        scheduler.generate_schedule(userRequestedDays, startDate, false);

        // 3. Retrieve Results
        HashMap<Course, Integer> calculatedSchedule = scheduler.getSchedule();
        ArrayList<Helpers.TimeSlot> timeSlots = scheduler.getActiveTimeSlots();
        HashMap<Course, ClassRoom> calculatedRooms = scheduler.getRoomAssignments();

        if (calculatedSchedule == null || calculatedSchedule.isEmpty()) {
            System.out.println("No schedule found.");
            return;
        }

        // 4. Update UI Bounds (If scheduler used more days than spinner shows)
        if (!timeSlots.isEmpty()) {
            LocalDate lastDate = timeSlots.get(timeSlots.size() - 1).getDate();
            long actualDaysLong = ChronoUnit.DAYS.between(startDate, lastDate) + 1;
            int actualDayCount = (int) actualDaysLong;

            System.out.println("Scheduler used " + actualDayCount + " days.");

            SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                    (SpinnerValueFactory.IntegerSpinnerValueFactory) daysSpinner.getValueFactory();

            if (actualDayCount > factory.getMax()) {
                factory.setMax(actualDayCount);
            }

            if (actualDayCount != daysSpinner.getValue()) {
                factory.setValue(actualDayCount);
            }
        }

        // 5. Clear Old Data
        scheduleView.clearLessons();
        finalSchedule.clear();
        finalSlotMap.clear();
        finalRoomMap.clear();

        int startHour = 8;

        // 6. Draw Lessons
        for (var entry : calculatedSchedule.entrySet()) {
            Course course = entry.getKey();
            int slotID = entry.getValue();
            Helpers.TimeSlot ts = timeSlots.get(slotID);
            ClassRoom assignedRoom = calculatedRooms.get(course);

            long daysDiff = ChronoUnit.DAYS.between(startDate, ts.getDate());
            int dayIndex = (int) daysDiff;

            long minutesFromStart = java.time.Duration.between(
                    java.time.LocalTime.of(startHour, 30),
                    ts.getTime()
            ).toMinutes();
            int startIndex = (int) (minutesFromStart / stepSize);

            int duration = course.getDuration();
            int slotsSpan = (int) Math.ceil((double) duration / stepSize);

            if (dayIndex >= 0 && startIndex >= 0) {
                for (int i = 0; i < slotsSpan; i++) {
                    scheduleView.addLesson(
                            course.getID(),
                            dayIndex,
                            startIndex + i,
                            Color.LIGHTBLUE
                    );
                }
            }

            finalSchedule.put(course, slotID);
            if (assignedRoom != null) finalRoomMap.put(course, assignedRoom);
            finalSlotMap.put(slotID, new String[]{
                    ts.getDate().toString(),
                    ts.getTime().toString(),
                    ts.getTime().plusMinutes(duration).toString()
            });
        }
        System.out.println("Optimal Schedule Generated & Displayed.");
    }

    @FXML
    void exportTimetable(ActionEvent event) {
        if (finalSchedule.isEmpty()) {
            System.out.println("Error: No schedule generated yet.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Schedule");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("OptimalSchedule.csv");

        if (mainContainer.getScene() != null) {
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

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

    @FXML
    void handleManuel(ActionEvent event){
        try {
            File manualFile = new File("UserManual.pdf");
            if (Desktop.isDesktopSupported() && manualFile.exists()) {
                Desktop.getDesktop().open(manualFile);
            } else {
                System.out.println("Manual file not found or Desktop not supported.");
            }
        } catch (IOException e) {
            System.out.println("Error opening manual: " + e.getMessage());
        }
    }

    @FXML
    void handleDarkMode(ActionEvent event) {
        isDarkModeOn = !isDarkModeOn;
        mainContainer.getStylesheets().clear();

        if (isDarkModeOn) {
            mainContainer.getStylesheets().add(getClass().getResource("/css/dark.css").toExternalForm());
        } else {
            mainContainer.getStylesheets().add(getClass().getResource("/css/light.css").toExternalForm());
        }
    }

    private File browseFile(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        if (mainContainer.getScene() != null) {
            return fileChooser.showOpenDialog(mainContainer.getScene().getWindow());
        }
        return null;
    }

    // Helper class for List Delete Buttons
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
                getListView().getItems().remove(itemToRemove);

                if (getListView() == courseList) {
                    allCourses.removeIf(c -> c.getID().equals(itemToRemove));
                } else if (getListView() == studentList) {
                    allStudents.removeIf(s -> s.ID().equals(itemToRemove));
                } else if (getListView() == classroomList) {
                    allClassrooms.removeIf(r -> (r.getName() + " (Cap: " + r.getCapacity() + ")").equals(itemToRemove));
                }
                System.out.println("Deleted: " + itemToRemove);
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