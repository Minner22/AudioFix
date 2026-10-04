package pl.audiofix;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AudioFixApp extends Application {

    public static void main(String[] args) {

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(AudioFixApp.class.getResource("ui/main-view.fxml"));
        Parent root = loader.load();

        primaryStage.setScene(new Scene(root, 1100, 700));
        primaryStage.setTitle("AudioFix");
        primaryStage.show();
    }
}
