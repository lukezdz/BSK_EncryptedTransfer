package pl.edu.pg.bsk.encryption;

import javafx.util.StringConverter;
import lombok.Getter;

public enum EncryptionMode {
	AES_ECB("AES/ECB/PKCS5Padding"),
	AES_CBC("AES/CBC/PKCS5Padding"),
	AES_CFB("AES/CFB/PKCS5Padding"),
	AES_OFB("AES/OFB/PKCS5Padding"),
	AES_CTR("AES/CTR/PKCS5Padding");

	@Getter
	private final String mode;

	EncryptionMode(String mode) {
		this.mode = mode;
	}

	public boolean needsInitializationVector() {
		return !this.mode.equals(AES_ECB.mode);
	}

	public static StringConverter<EncryptionMode> getStringConverter() {
		return new StringConverter<EncryptionMode>() {
			@Override
			public String toString(EncryptionMode encryptionMode) {
				return encryptionMode.getMode().split("/")[1];
			}

			@Override
			public EncryptionMode fromString(String s) {
				switch (s) {
					case "CBC": {
						return AES_CBC;
					}
					case "CFB": {
						return AES_CFB;
					}
					case "OFB": {
						return AES_OFB;
					}
					case "CTR": {
						return AES_CTR;
					}
					default: {
						return AES_ECB;
					}
				}
			}
		};
	}
}
