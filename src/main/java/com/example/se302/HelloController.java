package com.example.se302;



import java.nio.file.Path;

import IO.Validator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        if(Validator.validateFile(Path.of("docs\\sampleData_AllClassroomsAndTheirCapacities.csv")) && Validator.validateFile(Path.of("docs\\sampleData_AllAttendanceLists.csv")) && Validator.validateFile(Path.of("docs\\sampleData_AllCourses.csv")) && Validator.validateFile(Path.of("docs\\sampleData_AllStudents.csv"))) {
            welcomeText.setText("File is valid!");
        } else {
            welcomeText.setText("File is invalid!");
        }
    }
}
