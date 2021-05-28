package pl.edu.pg.bsk;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class EncryptedTransferApplication extends Application {
	@Override
	public void start(Stage stage) throws Exception {
		Parent parent = FXMLLoader.load(
				Objects.requireNonNull(getClass().getClassLoader().getResource("MainUI.fxml"))
		);

		Scene scene = new Scene(parent);
		stage.setTitle("Encrypted transfer");
		stage.setScene(scene);
		stage.setOnCloseRequest(windowEvent -> Platform.exit());
		stage.show();
	}
}
