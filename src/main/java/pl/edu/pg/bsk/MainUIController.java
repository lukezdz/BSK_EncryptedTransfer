package pl.edu.pg.bsk;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.edu.pg.bsk.encryption.EncryptionMode;
import pl.edu.pg.bsk.transfer.TransferHandler;
import pl.edu.pg.bsk.utils.ArrayObservableList;
import pl.edu.pg.bsk.utils.KeyPairWrapper;
import pl.edu.pg.bsk.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.util.List;
import java.util.ResourceBundle;

public class MainUIController implements Initializable {
	private static final String NO_KEYS_SELECTED = "Status: No keys selected";
	private static final String OFFLINE = "Status: Offline";
	private static final String READY = "Status: Ready";

	@FXML private MenuItem keysMenuGenerate;
	@FXML private MenuItem keysMenuSelect;
	@FXML private MenuItem keysMenuAbout;
	@FXML private MenuItem helpMenuAbout;

	@FXML private TextArea messageTextArea;
	@FXML private ListView<File> selectedFilesList;
	@FXML private ChoiceBox<EncryptionMode> modeChoiceBox;

	@FXML private Button sendButton;
	@FXML private Button addFileButton;

	@FXML private Label statusInfoLabel;

	private KeyPair selectedKeyPair = null;
	private TransferHandler transferHandler = null;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {


		modeChoiceBox.setConverter(EncryptionMode.getStringConverter());
		modeChoiceBox.setItems(getModesList());
		modeChoiceBox.setValue(EncryptionMode.AES_ECB);

		keysMenuGenerate.setOnAction(e -> {
			try {
				handleGenerateNewKeys(e);
			} catch (IOException exception) {
				showFXMLLoadError("generate new keys");
			}
		});
		keysMenuSelect.setOnAction(e -> {
			try {
				handleSelectKeys(e);
			} catch (IOException exception) {
				showFXMLLoadError("select key pair");
			}
		});
		keysMenuAbout.setOnAction(this::handleAboutKeys);

		statusInfoLabel.setText(NO_KEYS_SELECTED);

		sendButton.disableProperty().bind(
				Bindings.notEqual(statusInfoLabel.textProperty(), READY).and(
						Bindings.isEmpty(messageTextArea.textProperty())
								.or(Bindings.isEmpty(selectedFilesList.getItems())
										.or(Bindings.isEmpty(selectedFilesList.getSelectionModel().getSelectedItems())))
				)
		);
		sendButton.setOnAction(this::handleSendButton);

		addFileButton.setOnAction(this::handleAddFileButton);

		selectedFilesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
		stage.getIcons().add(Utils.getAppIcon());
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
		stage.getIcons().add(Utils.getAppIcon());
		stage.showAndWait();

		if (KeyPairWrapper.getInstance().getPair() != null) {
			statusInfoLabel.setText(OFFLINE);
			selectedKeyPair = KeyPairWrapper.getInstance().getPair();
			try {
				transferHandler = new TransferHandler(selectedKeyPair);
			} catch (IOException e) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setHeaderText("Failed to initialize server");
				alert.setContentText("There was a problem with creating a server to receive messages from other users. Check if your port "
						+ TransferHandler.SERVER_PORT + " is free and can be used.");
				Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
				alertStage.getIcons().add(Utils.getAppIcon());
				alert.show();
			}
		}
	}

	@FXML
	public void handleAboutKeys(ActionEvent actionEvent) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setHeaderText("RSA key pair");
		alert.setContentText("Key pair is used to establish secure connection with other users. " +
				"If no key pair is selected transferring and receiving data is impossible.");
		Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
		alertStage.getIcons().add(Utils.getAppIcon());
		alert.show();
	}

	@FXML
	public void handleSendButton(ActionEvent actionEvent) {

	}

	@FXML
	public void handleAddFileButton(ActionEvent actionEvent) {

	}

	private void showFXMLLoadError(String dialogName) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("About keys");
		alert.setHeaderText("Dialog error");
		alert.setContentText("Failed to open " + dialogName + " dialog");
		Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
		alertStage.getIcons().add(Utils.getAppIcon());
		alert.show();
	}
}
