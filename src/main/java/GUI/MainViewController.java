package GUI;

import Core.ClassRoom;
import Core.Course;
import Core.Student;
import IO.Exporter;
import IO.Importer;
import IO.ErrorHandler; // Hata yakalayıcı sınıfımız

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

        @FXML
        private BorderPane mainContainer;

        // --- VISUAL LISTS ---
        @FXML
        private ListView<String> studentList;
        @FXML
        private ListView<String> courseList;
        @FXML
        private ListView<String> classroomList;
        @FXML
        private ListView<String> attendanceList;

        // --- HATA LOG BUTONU ---
        @FXML
        private Button errorLogButton;

        // --- DATA STORAGE ---
        private ArrayList<Student> allStudents = new ArrayList<>();
        private ArrayList<Course> allCourses = new ArrayList<>();
        private ArrayList<ClassRoom> allClassrooms = new ArrayList<>();
        private ArrayList<Course> allAttendance = new ArrayList<>();

        // Hataları hafızada tutacağımız liste (Observer Pattern için)
        private ObservableList<String> errorLogItems = FXCollections.observableArrayList();

        // --- STATE & VIEWS ---
        private boolean isDarkModeOn = false;
        private ScheduleView scheduleView;

        // --- EXPORT DATA ---
        private HashMap<Course, Integer> finalSchedule = new HashMap<>();
        private HashMap<Integer, String[]> finalSlotMap = new HashMap<>();
        private HashMap<Course, ClassRoom> finalRoomMap = new HashMap<>();

        // NEW: Store time slots for search functionality
        private ArrayList<Helpers.TimeSlot> generatedTimeSlots;

        // --- CONTROLS ---
        @FXML
        private Spinner<Integer> intervalSpinner;
        @FXML
        private Spinner<Integer> daysSpinner;
        @FXML
        private DatePicker startDatePicker;

        // NEW: Search Field
        @FXML
        private TextField studentIdField;

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

                // 5. Init Date Picker
                startDatePicker.setValue(LocalDate.now());
                startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue != null) {
                                scheduleView.setStartDate(newValue);
                                if (!allCourses.isEmpty() && !allClassrooms.isEmpty()) {
                                        handleCreateSchedule(null);
                                }
                        }
                });

                // 6. Layout Setup
                mainContainer.setCenter(scheduleView);
                mainContainer.getStylesheets().add(getClass().getResource("/css/light.css").toExternalForm());

                // 7. Error Handler Kurulumu
                setupErrorHandler();
        }

        private void setupErrorHandler() {
                // Başlangıç metni
                if (errorLogButton != null) {
                        errorLogButton.setText("Hata000");
                }

                // ErrorHandler dinleyicisi
                IO.ErrorHandler.getInstance().setOnErrorListener(errorMessage -> {
                        Platform.runLater(() -> {
                                // 1. Hatayı hafızadaki listeye ekle
                                errorLogItems.add(errorMessage);

                                // 2. Buton üzerindeki sayıyı güncelle (Max 999)
                                int count = IO.ErrorHandler.getInstance().getErrorCount();
                                if (count > 999) count = 999;

                                if (errorLogButton != null) {
                                        errorLogButton.setText(String.format("Hata%03d", count));
                                }
                        });
                });
        }

        @FXML
        void handleShowErrorLog(ActionEvent event) {
                // Eğer hiç hata yoksa bilgi ver
                if (errorLogItems.isEmpty()) {
                        showAlert("Bilgi", "Şu ana kadar herhangi bir hata kaydı oluşmadı.");
                        return;
                }

                // Özel bir Alert (Dialog) oluşturuyoruz
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hata Kayıtları");
                alert.setHeaderText("Toplam Hata Sayısı: " + IO.ErrorHandler.getInstance().getErrorCount());
                alert.setContentText("Hata detayları aşağıdadır:");

                // Alert içine gömmek için bir ListView oluşturuyoruz
                ListView<String> listView = new ListView<>(errorLogItems);
                listView.setPrefWidth(500);
                listView.setPrefHeight(300);

                // ListView'i Alert penceresinin "genişletilebilir" alanına koyuyoruz
                alert.getDialogPane().setExpandableContent(listView);
                alert.getDialogPane().setExpanded(true); // Otomatik açık gelsin

                alert.showAndWait();
        }

        @FXML
        void importStudents(ActionEvent event) {
                File file = browseFile("Öğrenci CSV dosyasını seçin");
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
        void importCourses(ActionEvent event) {
                File file = browseFile("Ders CSV dosyasını seçin");
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
        void importClassroomCapacity(ActionEvent event) {
                File file = browseFile("Sınıf-Kapasite CSV dosyasını seçin");
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
        void importAttendanceList(ActionEvent event) {
                File file = browseFile("Katılım Listesi CSV dosyasını seçin");
                if (file != null) {
                        ArrayList<Course> attendanceData = Importer.importAttandenceLists(file.toPath());
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
                        // Kullanıcıya da görsel hata verilebilir
                        ErrorHandler.getInstance().logError("Eksik Veri, Lütfen önce Dersleri ve Sınıfları içe aktarın.");
                        showAlert("Eksik Veri", "Lütfen önce Dersleri ve Sınıfları içe aktarın.");
                        return;
                }
                Core.Scheduler scheduler = new Core.Scheduler();

                // MERGE ATTENDANCE
                HashMap<String, Course> courseMap = new HashMap<>();
                for (Course c : allCourses) {
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

                // LOAD DATA
                int stepSize = intervalSpinner.getValue();
                scheduler.loadData(allCourses, allClassrooms, stepSize);

                int userRequestedDays = daysSpinner.getValue();
                LocalDate startDate = startDatePicker.getValue();
                if (startDate == null) {
                        startDate = LocalDate.now();
                        startDatePicker.setValue(startDate);
                }

                scheduler.generate_schedule(userRequestedDays, startDate, false);

                // RETRIEVE RESULTS
                HashMap<Course, Integer> calculatedSchedule = scheduler.getSchedule();
                ArrayList<Helpers.TimeSlot> timeSlots = scheduler.getActiveTimeSlots();
                HashMap<Course, ClassRoom> calculatedRooms = scheduler.getRoomAssignments();

                // * CRITICAL UPDATE: SAVE TIME SLOTS FOR SEARCH *
                this.generatedTimeSlots = timeSlots;

                if (calculatedSchedule == null || calculatedSchedule.isEmpty()) {
                        ErrorHandler.getInstance().logError("Program oluşturulamadı, Verilen kısıtlarla uygun bir program bulunamadı.");
                        showAlert("Program Oluşturulamadı", "Verilen kısıtlarla uygun bir program bulunamadı.");
                        return;
                }

                // UPDATE SPINNER IF NEEDED
                if (!timeSlots.isEmpty()) {
                        LocalDate lastDate = timeSlots.get(timeSlots.size() - 1).getDate();
                        long actualDaysLong = ChronoUnit.DAYS.between(startDate, lastDate) + 1;
                        int actualDayCount = (int) actualDaysLong;


                        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                                (SpinnerValueFactory.IntegerSpinnerValueFactory) daysSpinner.getValueFactory();

                        if (actualDayCount > factory.getMax()) {
                                factory.setMax(actualDayCount);
                        }
                        if (actualDayCount != daysSpinner.getValue()) {
                                factory.setValue(actualDayCount);
                        }
                }

                // DRAW LESSONS
                scheduleView.clearLessons();
                finalSchedule.clear();
                finalSlotMap.clear();
                finalRoomMap.clear();
                scheduleView.setStartDate(startDate);

                int startHour = 8; // Assuming 8:30 start

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
                                        String cellText = course.getID();
                                        if (assignedRoom != null) {
                                                cellText += "\n" + assignedRoom.getName();
                                        }
                                        scheduleView.addLesson(
                                                cellText,
                                                dayIndex,
                                                startIndex + i,

                                                Color.DARKBLUE // Changed to standard color or variable
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
        }

        // --- NEW: STUDENT SEARCH HANDLER ---
        @FXML
        void handleSearchStudent(ActionEvent event) {
                String studentID = studentIdField.getText().trim();

                if (studentID.isEmpty()) {
                        ErrorHandler.getInstance().logError("Hata, Lütfen önce öğrenci numarası girin.");
                        showAlert("Hata", "Lütfen önce öğrenci numarası girin.");
                        return;
                }

                if (finalSchedule == null || finalSchedule.isEmpty() || generatedTimeSlots == null) {
                        ErrorHandler.getInstance().logError("Hata, Lütfen önce tabloyu oluşturun.");
                        showAlert("Hata", "Lütfen önce tabloyu oluşturun.");
                        return;
                }

                Core.StudentProgramExtractor extractor = new Core.StudentProgramExtractor(finalSchedule, generatedTimeSlots);
                java.util.List<String> exams = extractor.getExamsForStudent(studentID);

                if (exams.isEmpty()) {
                        ErrorHandler.getInstance().logError("Hata, Öğrenci için sınav bulunamadı." + studentID);
                        showAlert("Hata", "Öğrenci için sınav bulunamadı: " + studentID);
                } else {
                        StringBuilder sb = new StringBuilder("Öğrenci için sınav programı " + studentID + ":\n\n");
                        for (String exam : exams) {
                                sb.append(exam).append("\n");
                        }
                        showAlert("Student Schedule", sb.toString());
                }
        }

        @FXML
        void exportTimetable(ActionEvent event) {
                if (finalSchedule.isEmpty()) {
                        ErrorHandler.getInstance().logError("Hata, Henüz bir program oluşturulmadı. Dışa aktarmadan önce programı oluşturun.");
                        showAlert("Hata", "Henüz bir program oluşturulmadı. Dışa aktarmadan önce programı oluşturun.");
                        return;
                }

                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Tabloyu Kaydet");
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
                                showAlert("Başarılı", "Dışarı aktarma başarılı: " + file.getAbsolutePath());
                        }
                }
        }

        @FXML
        void handleManuel(ActionEvent event) {
                try {
                        // 1. Try finding it in the current working directory (Common)
                        File manualFile = new File("UserManual.pdf");

                        // 2. If not found, try the 'app' folder (Standard jpackage structure)
                        if (!manualFile.exists()) {
                                manualFile = new File("app/UserManual.pdf");
                        }

                        // 3. If still not found, try 'docs' folder (IntelliJ Development environment)
                        if (!manualFile.exists()) {
                                manualFile = new File("docs/UserManual.pdf");
                        }

                        // 4. Open the file if we found it
                        if (manualFile.exists() && Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().open(manualFile);
                        } else {
                                ErrorHandler.getInstance().logError("Hata, Kullanım kılavuzu bulunamadı: " + manualFile.getAbsolutePath());
                                showAlert("Hata", "Kullanım kılavuzu bulunamadı.\n(Aranan son konum: " + manualFile.getAbsolutePath() + ")");
                        }

                } catch (IOException e) {
                        ErrorHandler.getInstance().logError("Hata, Dosya açılırken sorun oluştu: " + e.getMessage());
                        showAlert("Hata", "Dosya açılamadı.");
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

        private void showAlert(String title, String content) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
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