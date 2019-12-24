package greenbeaver.terraincognita.controllers;

import greenbeaver.terraincognita.model.UIHandler;
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
    ChoiceBox<String> variants;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        int varAmount = CellType.PORTAL.getUsedAmount();
        for (int i = 0; i < varAmount; i++) {
            variants.getItems().add(String.valueOf(i + 1));
        }
        variants.setValue(String.valueOf(UIHandler.getCurrentPortalNum() + 1));
    }

    public void submit(ActionEvent actionEvent) {
        UIHandler.setPortalNum(UIHandler.getCurrentPortal(), Integer.parseInt(variants.getValue()) - 1);
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
}
