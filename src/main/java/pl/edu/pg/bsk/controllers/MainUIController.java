package pl.edu.pg.bsk.controllers;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.edu.pg.bsk.encryption.EncryptionMode;
import pl.edu.pg.bsk.exceptions.TransferException;
import pl.edu.pg.bsk.transfer.Metadata;
import pl.edu.pg.bsk.transfer.TransferHandler;
import pl.edu.pg.bsk.utils.ArrayObservableList;
import pl.edu.pg.bsk.utils.KeyPairWrapper;
import pl.edu.pg.bsk.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.List;
import java.util.ResourceBundle;

public class MainUIController extends NotifiableController {
	private static final String NO_KEYS_SELECTED = "Status: No keys selected";
	private static final String OFFLINE = "Status: Offline";
	private static final String READY = "Status: Ready";

	@FXML private MenuItem keysMenuGenerate;
	@FXML private MenuItem keysMenuSelect;
	@FXML private MenuItem keysMenuAbout;
	@FXML private MenuItem settingsMenuDownloadDirectory;
	@FXML private Menu settingsMenuAESEncryption;
	@FXML private MenuItem helpMenuAbout;

	@FXML private TextArea messageTextArea;
	@FXML private TextArea chatTextArea;
	@FXML private ChoiceBox<EncryptionMode> modeChoiceBox;
	@FXML private ListView<InetAddress> contactsListView;

	@FXML private Button sendButton;
	@FXML private Button sendFileButton;
	@FXML private Button addContactButton;

	@FXML private Label statusInfoLabel;

	@FXML private ProgressBar progressBar;
	@FXML private TextField contactTextField;

	private KeyPair selectedKeyPair = null;
	private TransferHandler transferHandler = null;
	private File downloadDestination = null;

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
		settingsMenuDownloadDirectory.setOnAction(this::handleSelectDownloadDirectory);

		helpMenuAbout.setOnAction(this::handleHelpAbout);

		statusInfoLabel.setText(NO_KEYS_SELECTED);

		sendButton.disableProperty().bind(
				Bindings.notEqual(statusInfoLabel.textProperty(), READY).or(
						Bindings.equal(messageTextArea.textProperty(), "")
				).or(
						Bindings.isEmpty(contactsListView.getSelectionModel().getSelectedItems())
				)
		);
		sendButton.setOnAction(this::handleSendButton);
		sendFileButton.disableProperty().bind(
				Bindings.notEqual(statusInfoLabel.textProperty(), READY).or(
					Bindings.isEmpty(contactsListView.getSelectionModel().getSelectedItems())
				)
		);
		sendFileButton.setOnAction(this::handleSendFileButton);

		chatTextArea.setEditable(false);

		addContactButton.disableProperty().bind(Bindings.equal(contactTextField.textProperty(), ""));
		addContactButton.setOnAction(this::handleAddContact);
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
				transferHandler = new TransferHandler(this, selectedKeyPair);
				statusInfoLabel.setText(READY);
			} catch (IOException e) {
				Alert alert = getQuickDialog(Alert.AlertType.ERROR, "Error", "Failed to initialize server",
						"There was a problem with creating a server to receive messages from other users. Check if your port "
								+ TransferHandler.SERVER_PORT + " is free and can be used.");
				alert.show();
			}
		}
	}

	@FXML
	public void handleAboutKeys(ActionEvent actionEvent) {
		Alert alert = getQuickDialog(Alert.AlertType.INFORMATION, "About keys", "RSA key pair",
				"Key pair is used to establish secure connection with other users. " +
				"If no key pair is selected transferring and receiving data is impossible.");
		alert.show();
	}

	@FXML
	public void handleSelectDownloadDirectory(ActionEvent actionEvent) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		downloadDestination = directoryChooser.showDialog(null);
		if (transferHandler != null) {
			transferHandler.setDownloadDir(downloadDestination);
		}
	}

	@FXML
	public void handleHelpAbout(ActionEvent actionEvent) {
		String message = "";
		Alert info = getQuickDialog(Alert.AlertType.INFORMATION, "About", "About", message);
		info.show();
	}

	@FXML
	public void handleSendButton(ActionEvent actionEvent) {
		try {
			transferHandler.sendEncryptedMessage(messageTextArea.getText(), modeChoiceBox.getValue(), contactsListView.getSelectionModel().getSelectedItem());
		} catch (TransferException e) {
			Alert alert = getQuickDialog(Alert.AlertType.ERROR, "Error", "Transfer failed", e.getMessage());
			alert.showAndWait();
		}
	}

	@FXML
	public void handleSendFileButton(ActionEvent actionEvent) {
		try {
			FileChooser chooser = new FileChooser();
			File file = chooser.showOpenDialog(null);
			Task<Void> sendingTask = transferHandler.sendEncryptedFile(file, modeChoiceBox.getValue(), contactsListView.getSelectionModel().getSelectedItem());
			progressBar.progressProperty().bind(sendingTask.progressProperty());
		} catch (TransferException e) {
			Alert alert = getQuickDialog(Alert.AlertType.ERROR, "Error", "Transfer failed", e.getMessage());
			alert.showAndWait();
		} catch (IOException exception) {
			Alert alert = getQuickDialog(Alert.AlertType.ERROR, "Error", "Socket error", exception.getMessage());
			alert.showAndWait();
		}
	}

	@FXML
	public void handleAddContact(ActionEvent actionEvent) {
		try {
			InetAddress address = InetAddress.getByName(contactTextField.getText());
			contactsListView.getItems().add(address);
			contactTextField.setText("");
		} catch (UnknownHostException e) {
			Alert alert = getQuickDialog(Alert.AlertType.ERROR, "Error", "Cannot add contact", "Provided IP address is invalid and cannot be added. Please try again.");
		}
	}

	private void showFXMLLoadError(String dialogName) {
		Alert alert = getQuickDialog(Alert.AlertType.ERROR, "About keys", "Dialog error",
				"Failed to open " + dialogName + " dialog");
		alert.showAndWait();
	}

	@Override
	public void notifyController(ControllerNotification notification) {
		Metadata.MetadataType metadataType = notification.getMetadata().getType();
		if (metadataType == Metadata.MetadataType.MESSAGE) {
			handleMessageNotification(notification);
		}
		else if (metadataType == Metadata.MetadataType.FILE) {
			handleFileNotification(notification);
		}
	}

	private void handleMessageNotification(ControllerNotification notification) {
		String message = notification.getFrom() + ": " + notification.getMessage();
		updateTextArea(message);
	}

	private void handleFileNotification(ControllerNotification notification) {
		String message = notification.getFrom() + ": Send you a file (" + notification.getFile().getName() + "). " +
				"It has been saved to " + downloadDestination.getAbsolutePath();
		//Alert dialog = getQuickDialog(Alert.AlertType.INFORMATION, "Notification", "New file", message);
		//dialog.show();
		updateTextArea(message);
	}

	private void updateTextArea(String message) {
		String currentText = chatTextArea.getText();
		currentText += "\n" + message;
		chatTextArea.setText(currentText);
	}

	private Alert getQuickDialog(Alert.AlertType type, String title, String header, String message) {
		Alert dialog = new Alert(type);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setContentText(message);
		((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(Utils.getAppIcon());

		return dialog;
	}
}
