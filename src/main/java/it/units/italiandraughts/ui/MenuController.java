package it.units.italiandraughts.ui;

import it.units.italiandraughts.ItalianDraughts;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MenuController {

    @FXML
    TextField player1Field, player2Field;

    @FXML
    private void startGame() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BoardLayout.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        BoardController boardController = fxmlLoader.getController();
        boardController.player1NameLabel.setText(player1Field.getText());
        boardController.player2NameLabel.setText(player2Field.getText());
        boardController.initializeWindow();
        Stage oldStage = (Stage) player1Field.getScene().getWindow();
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("ItalianDraughts");
        stage.getIcons().add(new Image(Objects.requireNonNull(ItalianDraughts.class.getResourceAsStream("ui/img/icon.png"))));
        ItalianDraughts.setupStage(stage);
        stage.show();
        oldStage.close();
    }

    @FXML
    protected void quitGame() {
        Platform.exit();
        System.exit(0);
    }

}