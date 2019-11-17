package greenbeaver.terraincognita.controllers;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private Button fullScreenMode;

    @FXML
    private VBox mainContainer;

    private Node mazeEditorView;
    {
        try {
            mazeEditorView = FXMLLoader.load(getClass().getResource("/fxml/MazeEditorView.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Node helpView;
    {
        try {
            helpView = FXMLLoader.load(getClass().getResource("/fxml/HelpView.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadView(mazeEditorView);
    }

    private void loadView(Node view) {
        ObservableList<Node> children = mainContainer.getChildren();
        if (children.size() != 1) {
            children.remove(1);
        }
        children.add(view);
    }

    @FXML
    void loadHelp() {
        loadView(helpView);
    }

    @FXML
    void loadMazeEditor() {
        loadView(mazeEditorView);
    }

    @FXML
    void fullScreenMode() {
        Stage stage = (Stage)fullScreenMode.getScene().getWindow();
        if (!stage.isFullScreen()) {
            stage.setFullScreen(true);
            fullScreenMode.setText("Window");
        } else {
            stage.setFullScreen(false);
            fullScreenMode.setText("FullScreen");
        }
    }

    @FXML
    void fold(ActionEvent actionEvent) {
        ((Stage)((Node)actionEvent.getSource()).getScene().getWindow()).setIconified(true);
    }

    @FXML
    void closeApp(ActionEvent actionEvent) {
        ((Stage)((Node)actionEvent.getSource()).getScene().getWindow()).close();
    }
}

