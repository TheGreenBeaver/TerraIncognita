package greenbeaver.terraincognita.controllers;

import greenbeaver.terraincognita.model.UIHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;

public class DangerousInputAlarmController {

    @FXML
    void confirm(ActionEvent actionEvent) {
        UIHandler.setContinueWithDangerousInput(true);
        ((Stage)((Node)actionEvent.getSource()).getScene().getWindow()).close();
    }

    @FXML
    void refuse(ActionEvent actionEvent) {
        UIHandler.setContinueWithDangerousInput(false);
        ((Stage)((Node)actionEvent.getSource()).getScene().getWindow()).close();
    }
}
