package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));  //importing fxml file
            Parent root = loader.load();


            Scene scene = new Scene(root); //this will be probably our only scene

            // 3. Configure and Show the Stage
            primaryStage.setTitle("CalendarBuilder");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading FXML file. Check the file path.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
