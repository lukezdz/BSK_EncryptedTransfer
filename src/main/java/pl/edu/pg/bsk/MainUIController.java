package pl.edu.pg.bsk;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.edu.pg.bsk.encryption.EncryptionMode;
import pl.edu.pg.bsk.utils.ArrayObservableList;
import pl.edu.pg.bsk.utils.KeyPairWrapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.util.List;
import java.util.ResourceBundle;

public class MainUIController implements Initializable {
	@FXML private ChoiceBox<EncryptionMode> modeChoiceBox;
	@FXML private Button selectKeysButton;
	@FXML private Button generateKeysButton;
	@FXML private Label keysInfoLabel;
	@FXML private Button fileSelectButton;
	@FXML private ListView<File> selectedFilesList;
	@FXML private TextArea messageTextArea;
	@FXML private Button sendButton;

	private KeyPair selectedKeyPair = null;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		modeChoiceBox.setConverter(EncryptionMode.getStringConverter());
		modeChoiceBox.setItems(getModesList());
		modeChoiceBox.setValue(EncryptionMode.AES_ECB);

		keysInfoLabel.setText("You have currently no keys selected.");
	}

	private ObservableList<EncryptionMode> getModesList() {
		ObservableList<EncryptionMode> list = new ArrayObservableList<>();
		list.addAll(List.of(
			EncryptionMode.AES_ECB,
			EncryptionMode.AES_CBC,
			EncryptionMode.AES_CFB,
			EncryptionMode.AES_OFB,
			EncryptionMode.AES_CTR
		));
		return list;
	}

	@FXML
	public void handleGenerateNewKeys(ActionEvent actionEvent) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("KeysGenerationDialog.fxml"));
		Parent parent = loader.load();

		Scene scene = new Scene(parent);
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setScene(scene);
		stage.setTitle("Generate key pair");
		stage.showAndWait();
	}

	@FXML
	public void handleSelectKeys(ActionEvent actionEvent) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("KeysSelectionDialog.fxml"));
		Parent parent = loader.load();

		Scene scene = new Scene(parent);
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setScene(scene);
		stage.setTitle("Select key pair");
		stage.showAndWait();

		if (KeyPairWrapper.getInstance().getPair() != null) {
			keysInfoLabel.setText("You have selected a key pair.");
			selectedKeyPair = KeyPairWrapper.getInstance().getPair();
		}
	}
}
