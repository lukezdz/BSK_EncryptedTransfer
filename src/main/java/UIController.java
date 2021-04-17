import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;

import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;


public class UIController implements Initializable {
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        chooseButton.setOnAction(this::handleChooseButton); //wybranie pliku
        textField.setOnAction(this::textInputButton); //wpisanie tekstu w polu tekstowym
        ECBButton.setOnAction(this::handleECBButton);
        CBCButton.setOnAction(this::handleCBCButton);
        CFBButton.setOnAction(this::handleCFBButton);
        OFBButton.setOnAction(this::handleOFBButton);
        CTRButton.setOnAction(this::handleCTRButton);

        generateKeysButton.setOnAction(this::handleGenerateKeysButton); //generowanie kluczy
        chooseKeysButton.setOnAction(this::handleChooseKeysButton); //wybranie pary kluczy

        executorService = new ForkJoinPool();
        chosenFile = null;
    }

    @FXML
    private Label prompt;
    @FXML
    private Button chooseButton;
    @FXML
    private TextField textField;
    @FXML
    private Button ECBButton;
    @FXML
    private Button CBCButton;
    @FXML
    private Button CFBButton;
    @FXML
    private Button OFBButton;
    @FXML
    private Button CTRButton;

    @FXML
    private Button generateKeysButton;
    @FXML
    private Button chooseKeysButton;

    private ExecutorService executorService;

    private File chosenFile;
    private File chosenKeys;

    @FXML
    private void handleECBButton(ActionEvent event) {

    }

    @FXML
    private void handleCFBButton(ActionEvent event) {

    }

    @FXML
    private void handleCBCButton(ActionEvent event) {

    }

    @FXML
    private void handleOFBButton(ActionEvent event) {

    }

    @FXML
    private void handleCTRButton(ActionEvent event) {

    }

    @FXML
    private void handleChooseButton(ActionEvent event) {
        boolean filesNotEmpty = chooseFiles();

        if (!filesNotEmpty) {
            updatePrompt("You have not selected any files.");
        } else {

        }
    }

    @FXML
    private void textInputButton(ActionEvent event) {
        textField = new TextField();
        textField.setPromptText("Text...");
        textField.setPrefColumnCount(10);
        textField.getText();
    }

    @FXML
    private void handleGenerateKeysButton(ActionEvent event) {

    }

    @FXML
    private boolean handleChooseKeysButton(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TEXT files (*.txt)", "*.txt"));

        chosenKeys = fileChooser.showOpenDialog(null);

        return chosenKeys != null;
    }

    private boolean chooseFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TEXT files (*.txt)", "*.txt"));

        chosenFile = fileChooser.showOpenDialog(null);

        return chosenFile != null;
    }

    private void updatePrompt(String message) {
        prompt.textProperty().set(message);
    }
}
