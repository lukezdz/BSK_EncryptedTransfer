package pl.edu.pg.bsk.encryption;

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
}
