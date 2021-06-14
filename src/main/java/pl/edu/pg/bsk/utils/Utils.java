package pl.edu.pg.bsk.utils;

import javafx.scene.image.Image;

public class Utils {
	public static Image getAppIcon()  {
		return new Image(Utils.class.getClassLoader().getResourceAsStream("EncryptedTransferIcon.png"));
	}
}
