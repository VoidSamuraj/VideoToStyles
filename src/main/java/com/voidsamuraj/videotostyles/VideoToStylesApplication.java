package com.voidsamuraj.videotostyles;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.IOException;
import java.util.Objects;

public class VideoToStylesApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(VideoToStylesApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 800);
        stage.setMinWidth(1280);
        stage.setMinHeight(800);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles/style.css")).toExternalForm());
        stage.setTitle("Video To ASCII");
        stage.setScene(scene);
        MediaView mv= (MediaView) scene.lookup("#mediaView");
        ImageView iv= (ImageView) scene.lookup("#imageView");
        mv.fitHeightProperty().bind(stage.heightProperty().subtract(50));
        iv.fitHeightProperty().bind(stage.heightProperty().subtract(50));

        stage.show();
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        launch();
    }




}