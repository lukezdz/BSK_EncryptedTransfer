package pl.edu.pg.bsk;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import pl.edu.pg.bsk.encryption.EncryptionMode;
import pl.edu.pg.bsk.encryption.EncryptionUtils;
import pl.edu.pg.bsk.encryption.SymmetricEncryption;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;
import pl.edu.pg.bsk.utils.Utils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import java.util.ResourceBundle;

public class KeysGenerationDialogController implements Initializable {
	private static final String NOT_SELECTED = "Not selected";

	@FXML private Label infoLabel;
	@FXML private Label passwordLabel;
	@FXML private Label privateDirectoryLabel;
	@FXML private Label publicDirectoryLabel;

	@FXML private TextField privateKeyTextField;
	@FXML private TextField publicKeyTextField;
	@FXML private PasswordField passwordField;

	@FXML private Button publicKeyLocationButton;
	@FXML private Button privateKeyLocationButton;

	@FXML private Button saveButton;
	@FXML private Button nextButton;
	@FXML private Button previousButton;

	private File selectedPrivateDirectory;
	private File selectedPublicDirectory;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		publicKeyLocationButton.setOnAction(this::setKeyLocationPublic);
		privateKeyLocationButton.setOnAction(this::setKeyLocationPrivate);

		saveButton.disableProperty().bind(
				Bindings.isEmpty(privateKeyTextField.textProperty())
						.or(Bindings.isEmpty(passwordField.textProperty()))
						.or(Bindings.equal(privateDirectoryLabel.textProperty(), NOT_SELECTED))
		);
		saveButton.setOnAction(this::saveAndClose);

		previousButton.setOnAction(this::handlePreviousButton);

		nextButton.disableProperty().bind(
				Bindings.isEmpty(publicKeyTextField.textProperty())
						.or(Bindings.equal(publicDirectoryLabel.textProperty(), NOT_SELECTED))
		);
		nextButton.setOnAction(this::handleNextButton);

		publicDirectoryLabel.setText(NOT_SELECTED);
		privateDirectoryLabel.setText(NOT_SELECTED);

		setPublicScene();
	}

	@FXML
	public void setKeyLocationPrivate(ActionEvent actionEvent) {
		Stage stage = getStage(actionEvent);
		DirectoryChooser chooser = new DirectoryChooser();
		selectedPrivateDirectory = chooser.showDialog(stage);
		privateDirectoryLabel.setText(selectedPrivateDirectory.getPath());
	}

	@FXML
	public void setKeyLocationPublic(ActionEvent actionEvent) {
		Stage stage = getStage(actionEvent);
		DirectoryChooser chooser = new DirectoryChooser();
		selectedPublicDirectory = chooser.showDialog(stage);
		publicDirectoryLabel.setText(selectedPublicDirectory.getPath());
	}

	@FXML
	public void saveAndClose(ActionEvent actionEvent) {
		Stage stage = getStage(actionEvent);

		File privateKeyFile = createFile(selectedPrivateDirectory, privateKeyTextField.getText(), "enc");
		File publicKeyFile = createFile(selectedPublicDirectory, publicKeyTextField.getText(), "ckey");

		try {
			KeyPair keys = EncryptionUtils.generateKeyPair();

			writePrivateKey(privateKeyFile, passwordField.getText(), keys.getPrivate());
			writePublicKey(publicKeyFile, keys.getPublic());
		} catch (IOException exception) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Saving keys failed");
			alert.setContentText("There was a problem with saving your key pair. Please try again.");
			Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
			alertStage.getIcons().add(Utils.getAppIcon());
			alert.showAndWait();
		} catch (EncryptionFailedException exception) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Saving keys failed");
			alert.setContentText("There was a problem with encrypting your private key. Please try again.");
			Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
			alertStage.getIcons().add(Utils.getAppIcon());
			alert.showAndWait();
		} catch (NoSuchAlgorithmException ignored) {}

		stage.close();
	}

	@FXML
	public void handleNextButton(ActionEvent actionEvent) {
		setPrivateScene();
	}

	@FXML
	public void handlePreviousButton(ActionEvent actionEvent) {
		setPublicScene();
	}

	private Stage getStage(ActionEvent actionEvent) {
		Node source = (Node) actionEvent.getSource();
		return (Stage) source.getScene().getWindow();
	}

	private void setPrivateScene() {
		privateKeyTextField.setVisible(true);
		privateKeyLocationButton.setVisible(true);
		passwordField.setVisible(true);
		passwordLabel.setVisible(true);
		previousButton.setVisible(true);
		saveButton.setVisible(true);
		privateDirectoryLabel.setVisible(true);

		nextButton.setVisible(false);
		publicKeyTextField.setVisible(false);
		publicKeyLocationButton.setVisible(false);
		publicDirectoryLabel.setVisible(false);

		infoLabel.setText("Select location, filename and password for private key");
	}

	private void setPublicScene() {
		nextButton.setVisible(true);
		publicKeyTextField.setVisible(true);
		publicKeyLocationButton.setVisible(true);
		publicDirectoryLabel.setVisible(true);

		privateKeyTextField.setVisible(false);
		privateKeyLocationButton.setVisible(false);
		passwordField.setVisible(false);
		passwordLabel.setVisible(false);
		previousButton.setVisible(false);
		saveButton.setVisible(false);
		privateDirectoryLabel.setVisible(false);

		infoLabel.setText("Select location and filename for public key");
	}

	private File createFile(File directory, String filename, String fileExt) {
		return new File(directory.getPath() + "/" + filename + "." + fileExt);
	}

	private void writePublicKey(File file, PublicKey key) throws IOException {
		FileUtils.writeByteArrayToFile(file, key.getEncoded());
	}

	private void writePrivateKey(File file, String password, PrivateKey key) throws EncryptionFailedException, IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			SecretKey secretKey =
					new SecretKeySpec(digest.digest(password.getBytes(StandardCharsets.UTF_8)), "AES");
			SymmetricEncryption symmetricEncryption = new SymmetricEncryption(secretKey);
			byte[] encoded = symmetricEncryption.encrypt(key.getEncoded(), EncryptionMode.AES_ECB, Optional.empty());

			FileUtils.writeByteArrayToFile(file, encoded);
		} catch (NoSuchAlgorithmException ignored) {}
	}
}
