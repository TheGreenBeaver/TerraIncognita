package greenbeaver.terraincognita.controllers;

import greenbeaver.terraincognita.model.cellConstruction.CellType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

import java.net.URL;
import java.util.ResourceBundle;

public class PortalSettingsController implements Initializable {
    @FXML
    SplitMenuButton variants;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        int varAmount = CellType.PORTAL.getUsedAmount();
        for (int i = 0; i < varAmount; i++) {
            MenuItem variant = new MenuItem();
            variant.setText(String.valueOf(i));
        }
    }
}
