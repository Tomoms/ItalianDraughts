package it.units.italiandraughts.ui;

import it.units.italiandraughts.ItalianDraughts;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class MenuController {

    @FXML
    TextField player1Field, player2Field;

    @FXML
    protected void startGame() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BoardLayout.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        BoardController boardController = fxmlLoader.getController();
        boardController.player1Name.setText(player1Field.getText());
        boardController.player2Name.setText(player2Field.getText());
        Stage stage = ItalianDraughts.getPrimaryStage();
        stage.setScene(scene);

    }

    @FXML
    protected void quitGame() {
        Platform.exit();
        System.exit(0);
    }

}