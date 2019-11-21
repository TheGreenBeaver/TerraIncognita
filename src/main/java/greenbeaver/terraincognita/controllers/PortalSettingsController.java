package greenbeaver.terraincognita.controllers;

import greenbeaver.terraincognita.model.cellConstruction.CellType;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class PortalSettingsController implements Initializable {
    @FXML
    ChoiceBox<Integer> variants;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        int varAmount = CellType.PORTAL.getUsedAmount();
        for (int i = 0; i < varAmount; i++) {
            variants.getItems().add(i + 1);
        }
    }

    public void submit(ActionEvent actionEvent) {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
}
