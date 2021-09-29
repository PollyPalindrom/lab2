package by.bsuir.lab1;

import by.bsuir.lab1.controller.Controller;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;

public class Main extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL url = getClass().getResource("/main.fxml");
        Parent root = FXMLLoader.load(url);
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                Controller controller = new Controller().getInstance();
                controller.closePort();
            }
        });
        stage.setResizable(false);
        stage.setTitle("COM-ports");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
        System.exit(0);
    }
}