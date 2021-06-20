package pl.edu.pg.bsk.controllers;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import pl.edu.pg.bsk.encryption.AsymmetricEncryption;
import pl.edu.pg.bsk.encryption.EncryptionMode;
import pl.edu.pg.bsk.encryption.SymmetricEncryption;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;
import pl.edu.pg.bsk.exceptions.KeyMismatchException;
import pl.edu.pg.bsk.utils.KeyPairWrapper;
import pl.edu.pg.bsk.utils.Utils;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;
import java.util.ResourceBundle;

public class KeysSelectionDialogController implements Initializable {
	private final static String NOT_SELECTED = "Not selected";

	@FXML private Button publicKeyButton;
	@FXML private Button privateKeyButton;
	@FXML private Button loadButton;

	@FXML private Label publicKeyInfo;
	@FXML private Label privateKeyInfo;

	@FXML private PasswordField privateKeyPassword;

	private File privateKeyFile;
	private File publicKeyFile;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		publicKeyButton.setOnAction(this::handlePublicKeyButton);
		privateKeyButton.setOnAction(this::handlePrivateKeyButton);
		loadButton.setOnAction(this::handleLoadButton);

		publicKeyInfo.setText(NOT_SELECTED);
		privateKeyInfo.setText(NOT_SELECTED);

		loadButton.disableProperty().bind(
				Bindings.equal(publicKeyInfo.textProperty(), NOT_SELECTED).or(
						Bindings.equal(privateKeyInfo.textProperty(), NOT_SELECTED))
						.or(Bindings.isEmpty(privateKeyPassword.textProperty()))
		);
	}

	@FXML
	public void handlePrivateKeyButton(ActionEvent actionEvent) {
		Stage stage = getStage(actionEvent);
		FileChooser chooser = new FileChooser();
		FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Private keys", "*.enc");
		chooser.setSelectedExtensionFilter(filter);
		privateKeyFile = chooser.showOpenDialog(stage);

		privateKeyInfo.setText(privateKeyFile.getPath());
	}

	@FXML
	public void handlePublicKeyButton(ActionEvent actionEvent) {
		Stage stage = getStage(actionEvent);
		FileChooser chooser = new FileChooser();
		FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Public keys", "*.ckey");
		chooser.setSelectedExtensionFilter(filter);
		publicKeyFile = chooser.showOpenDialog(stage);

		publicKeyInfo.setText(publicKeyFile.getPath());
	}

	@FXML
	public void handleLoadButton(ActionEvent actionEvent) {
		Optional<PublicKey> publicKey = Optional.empty();
		Optional<PrivateKey> privateKey = Optional.empty();

		try {
			publicKey = Optional.of(getPublicKey());
			privateKey = Optional.of(getPrivateKey(privateKeyPassword.getText()));
		} catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | EncryptionFailedException ignored) {

		}

		if (publicKey.isEmpty() || privateKey.isEmpty()) {
			showError("Couldn't locate one (or both) file with keys.\n\nPlease try again.");
			return;
		}

		Optional<KeyPair> keys = Optional.empty();
		try {
			keys = Optional.of(verifyAndGetKeyPair(publicKey.get(), privateKey.get()));
		} catch (KeyMismatchException e) {
			showError("Selected keys do not match. Either you have selected one wrong key, or given incorrect password for private key.\n\nPlease try again.");
			return;
		}

		Stage stage = getStage(actionEvent);
		KeyPairWrapper.getInstance().setPair(keys.get());
		stage.close();
	}

	private PublicKey getPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		return KeyFactory.getInstance("RSA").generatePublic(
				new X509EncodedKeySpec(FileUtils.readFileToByteArray(publicKeyFile)));
	}

	private PrivateKey getPrivateKey(String password) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, EncryptionFailedException {
		byte[] loaded = FileUtils.readFileToByteArray(privateKeyFile);
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		SymmetricEncryption encryption = new SymmetricEncryption(new SecretKeySpec(digest.digest(password.getBytes(StandardCharsets.UTF_8)), "AES"));
		byte[] decrypted = encryption.decrypt(loaded, EncryptionMode.AES_ECB, Optional.empty());
		return KeyFactory.getInstance("RSA").generatePrivate(
				new PKCS8EncodedKeySpec(decrypted));
	}

	private KeyPair verifyAndGetKeyPair(PublicKey publicKey, PrivateKey privateKey) throws KeyMismatchException {
		KeyPair pair = new KeyPair(publicKey, privateKey);

		if (!verifyKeysMatch(pair)) {
			throw new KeyMismatchException();
		}

		return pair;
	}

	private Stage getStage(ActionEvent actionEvent) {
		Node source = (Node) actionEvent.getSource();
		return (Stage) source.getScene().getWindow();
	}

	private void showError(String errorMessage) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setHeaderText("Loading keys failed");
		alert.setContentText(errorMessage);
		Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
		alertStage.getIcons().add(Utils.getAppIcon());
		alert.showAndWait();
	}

	private boolean verifyKeysMatch(KeyPair pair) {
		String testText = "This is a test text to test if keys do actually match.";
		String decrypted = "";

		try {
			AsymmetricEncryption encryption = new AsymmetricEncryption(pair);
			byte[] encrypted = encryption.encryptWithPublic(testText.getBytes(StandardCharsets.UTF_8), pair.getPublic());
			decrypted = new String(encryption.decryptWithPrivate(encrypted));
		} catch (EncryptionFailedException e) {
			return false;
		}

		return testText.equals(decrypted);
	}
}
