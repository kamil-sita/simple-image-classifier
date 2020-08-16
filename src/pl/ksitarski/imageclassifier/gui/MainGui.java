package pl.ksitarski.imageclassifier.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Application start.
 */
public class MainGui extends Application {

    /**
     * Main function of this application.
     */
    public static void main(String[] args) {
        launch();
    }

    //comment based on JavaFX Javadoc.
    /**
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     *
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     *
     * @param stage the primary stage for this application, onto which
     * the application scene can be set. The primary stage will be embedded in
     * the browser if the application was launched as an applet.
     * Applications may create other stages, if needed, but they will not be
     * primary stages and will not be embedded in the browser.
     */
    public void start(Stage stage) throws IOException {

        URL res = getClass().getResource("Gui.fxml");

        Parent root = FXMLLoader.load(res);
        stage.setTitle("Simple classifier");
        stage.setScene(new Scene(root, 1535, 800));
        stage.setMinHeight(800);
        stage.setMinWidth(1535);
        stage.setResizable(false);
        stage.show();
    }
}
