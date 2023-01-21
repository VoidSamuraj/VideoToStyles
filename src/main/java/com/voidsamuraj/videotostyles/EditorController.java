package com.voidsamuraj.videotostyles;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import org.opencv.core.Mat;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EditorController  implements Initializable {

    @FXML
    public TextField src_label;
    @FXML
    public TextField out_label;
    @FXML
    public Slider frame_slider;
    @FXML
    public CheckBox color;
    @FXML
    public CheckBox shift;
    @FXML
    public VBox menu;
    @FXML
    public Button save_button;
    @FXML
    public Slider divider_slider;
    @FXML
    public Label divider_label;
    @FXML
    public ProgressBar progressBar;
    @FXML
    private GridPane previewContainer;
    @FXML
    private MediaView mediaView;
    @FXML
    private ImageView imageView;
    @FXML
    private ChoiceBox<VideoToStyle.TypeOfStyle> stylePicker;
    @FXML
    private GridPane ascii_menu;
    @FXML
    private GridPane dots_menu;

    @FXML
    private Slider dot_slider;
    @FXML
    private Label dot_label;

    @FXML
    private Slider width_slider;
    @FXML
    private Label width_label;
    @FXML
    private Slider height_slider;
    @FXML
    private Label height_label;

    @FXML
    private Slider font_slider;
    @FXML
    private Label font_label;

    private Media media;
    private MediaPlayer mediaPlayer;
    @FXML
    private  void onStylePick(){
        switch (stylePicker.getValue()){
            case ASCII -> {
                ascii_menu.setManaged(true);
                ascii_menu.setVisible(true);
                dots_menu.setManaged(false);
                dots_menu.setVisible(false);
            }
            case DOTS ->{
                ascii_menu.setManaged(false);
                ascii_menu.setVisible(false);
                dots_menu.setManaged(true);
                dots_menu.setVisible(true);
            }
        }
        generatePreview();
    }
    @FXML
    private void onFrameSliderChange(){
        if(media!=null) {
            mediaPlayer.seek(media.getDuration().multiply(frame_slider.getValue()));
            imageView.setImage(mediaView.snapshot(new SnapshotParameters(),null));
        }
    }

    @FXML
    private void generatePreview()  {
        if(media!=null){
            WritableImage wi =mediaView.snapshot(new SnapshotParameters(),null);
            try {
                Mat matrix = VideoToStyle.writableImageToMat(wi);

                if(stylePicker.getValue()== VideoToStyle.TypeOfStyle.ASCII)
                    VideoToStyle.asciiImage(matrix,color.isSelected(),(int)Math.round(width_slider.getValue()),(int)Math.round(height_slider.getValue()),font_slider.getValue());
                else
                    VideoToStyle.dotsImage(matrix,color.isSelected(),shift.isSelected(),(int)Math.round(dot_slider.getValue()));
                Image toDisplay=VideoToStyle.fromMatToImage(matrix);
                ImageIO.write(SwingFXUtils.fromFXImage(toDisplay, null),"png",new File("M:\\download\\test.png"));
                imageView.setImage(toDisplay);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @FXML
    private void generateVideo(){
        Task<Void> task = new Task<>() {
            @Override
            protected Void call(){
                save_button.setDisable(true);
                progressBar.setVisible(true);
                VideoToStyle.processVideo(
                        src_label.getText(),
                        out_label.getText(),
                        (int)divider_slider.getValue(),
                        stylePicker.getValue(),
                        color.isSelected(),
                        (int)Math.round((stylePicker.getValue()== VideoToStyle.TypeOfStyle.ASCII)?width_slider.getValue():dot_slider.getValue()),
                        (int)Math.round(height_slider.getValue()),
                        font_slider.getValue(),
                        shift.isSelected(),
                        this::updateProgress
                );
                save_button.setDisable(false);
                progressBar.setVisible(false);
                return null;
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());

        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(task);



    }
    @FXML
    private void onDividerSliderChange(){divider_label.setText("1/"+((int)divider_slider.getValue()));}
    @FXML
    private void onDotsSliderChange(){
        dot_label.setText(""+((int)Math.round(dot_slider.getValue()))); }
    @FXML void onDotsSliderCommit(){
        onDotsSliderChange();
        generatePreview();
    }
    @FXML void onCheckBoxChange(){generatePreview();}
    @FXML
    private void onWidthChange(){width_label.setText(""+((int)Math.round(width_slider.getValue())));}
    @FXML void onWidthSliderCommit(){
        onWidthChange();
        generatePreview();
    }
    @FXML
    private void onHeightChange(){height_label.setText(""+((int)Math.round(height_slider.getValue())));}
    @FXML void onHeightSliderCommit(){
        onHeightChange();
        generatePreview();
    }
    @FXML
    private void onFontChange(){font_label.setText(String.format("%.1f",font_slider.getValue()));}
    @FXML void onFontSliderCommit(){
        onFontChange();
        generatePreview();
    }
    private FileChooser.ExtensionFilter getVideoFilter(){
        return new FileChooser.ExtensionFilter("Pliki wideo","*.mp4", "*.mkv", "*.avi", "*.wmv", "*.mov", "*.flv", "*.m4v", "*.mpg", "*.mpeg", "*.3gp", "*.webm", "*.ogg", "*.ts", "*.mts", "*.m2ts", "*.asf", "*.rm", "*.swf");
    }
    @FXML
    private void selectFileToOpen(){
        FileChooser fc=new FileChooser();
        fc.getExtensionFilters().addAll(getVideoFilter());
        File f=fc.showOpenDialog(previewContainer.getScene().getWindow());

        if(f!=null){
            if(f.exists()) {
                frame_slider.setDisable(false);
                save_button.setDisable(false);
                src_label.setText(f.getAbsolutePath());
                media=new Media(f.toURI().toString());
                mediaPlayer=new MediaPlayer(media);
                mediaView.setMediaPlayer(mediaPlayer);
                frame_slider.setValue(0.0);
                imageView.setImage(null);

            }
        }
    }


    @FXML
    private void selectFileToSave(){
        FileChooser fc=new FileChooser();
        fc.getExtensionFilters().addAll(getVideoFilter());
        File f=fc.showSaveDialog(previewContainer.getScene().getWindow());
        if(f!=null){
            out_label.setText(f.getAbsolutePath());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {


        stylePicker.getItems().addAll(VideoToStyle.TypeOfStyle.ASCII,VideoToStyle.TypeOfStyle.DOTS);
        stylePicker.setValue(VideoToStyle.TypeOfStyle.ASCII);

        HBox.setHgrow(previewContainer, Priority.ALWAYS);
        mediaView.fitWidthProperty().bind(previewContainer.widthProperty());
        imageView.fitWidthProperty().bind(previewContainer.widthProperty());

        mediaView.setPreserveRatio(true);
        imageView.setPreserveRatio(true);

    }

}

