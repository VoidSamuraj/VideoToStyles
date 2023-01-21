module com.example.videotostyles {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.media;
    requires opencv;


    opens com.voidsamuraj.videotostyles to javafx.fxml;
    exports com.voidsamuraj.videotostyles;
}